<template>
  <div class="page-container">
    <div class="header-section">
      <h2>敏感词管理</h2>
      <div class="header-actions">
        <button @click="showAddModal" class="add-btn">新增敏感词</button>
      </div>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="search-section">
      <div class="search-row">
        <input 
          v-model="searchKeyword" 
          placeholder="搜索敏感词..." 
          class="search-input"
          @keyup.enter="searchSensitiveWords"
        />
        <select v-model="filterType" class="search-select" @change="filterSensitiveWords">
          <option value="">全部类型</option>
          <option value="1">通用</option>
          <option value="2">政治</option>
          <option value="3">色情</option>
          <option value="4">暴力</option>
          <option value="5">广告</option>
        </select>
        <select v-model="filterStatus" class="search-select" @change="filterSensitiveWords">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
        <button @click="searchSensitiveWords" class="search-btn">搜索</button>
        <button @click="resetFilters" class="reset-btn">重置</button>
      </div>
    </div>

    <!-- 批量操作区域 -->
    <div v-if="selectedWords.length > 0" class="batch-action-section">
      <div class="batch-info">
        已选择 {{ selectedWords.length }} 个敏感词
      </div>
      <div class="batch-actions">
        <button @click="batchEnable" class="batch-enable-btn">批量启用</button>
        <button @click="batchDisable" class="batch-disable-btn">批量禁用</button>
        <button @click="batchDelete" class="batch-delete-btn">批量删除</button>
        <button @click="clearSelection" class="clear-selection-btn">取消选择</button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 敏感词表格 -->
    <div class="table-container">
      <table class="sensitive-word-table">
        <thead>
          <tr>
            <th class="checkbox-column">
              <input 
                type="checkbox" 
                :checked="isAllSelected" 
                @change="toggleSelectAll"
              />
            </th>
            <th>ID</th>
            <th>敏感词</th>
            <th>类型</th>
            <th>描述</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="word in filteredWords" :key="word.wordId">
            <td class="checkbox-column">
              <input 
                type="checkbox" 
                :checked="selectedWords.includes(word.wordId)"
                @change="toggleSelectWord(word.wordId)"
              />
            </td>
            <td>{{ word.wordId }}</td>
            <td class="word-content">{{ word.word }}</td>
            <td>{{ getTypeText(word.type) }}</td>
            <td>{{ word.description || '-' }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(word.status)]">
                {{ getStatusText(word.status) }}
              </span>
            </td>
            <td>{{ formatDate(word.createTime) }}</td>
            <td class="action-cell">
              <button @click="showEditModal(word)" class="edit-btn">编辑</button>
              <button 
                @click="toggleStatus(word)" 
                :class="['status-toggle-btn', word.status === 1 ? 'disable-btn' : 'enable-btn']"
              >
                {{ word.status === 1 ? '禁用' : '启用' }}
              </button>
              <button @click="showDeleteConfirm(word)" class="delete-btn">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 空状态 -->
      <div v-if="!loading && filteredWords.length === 0" class="empty-state">
        <p>暂无敏感词数据</p>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1 || total > 0" class="pagination">
      <div class="page-size-selector">
        <span>每页显示：</span>
        <select v-model="pageSize" @change="onPageSizeChange" class="page-size-select">
          <option :value="10">10 条</option>
          <option :value="20">20 条</option>
          <option :value="50">50 条</option>
          <option :value="100">100 条</option>
        </select>
      </div>
      <button 
        @click="changePage(1)" 
        :disabled="currentPage === 1"
        class="page-btn"
        title="首页"
      >
        首页
      </button>
      <button 
        @click="changePage(currentPage - 1)" 
        :disabled="currentPage === 1"
        class="page-btn"
        title="上一页"
      >
        上一页
      </button>
      <span class="page-info">
        第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ total }} 条
      </span>
      <button 
        @click="changePage(currentPage + 1)" 
        :disabled="currentPage >= totalPages"
        class="page-btn"
        title="下一页"
      >
        下一页
      </button>
      <button 
        @click="changePage(totalPages)" 
        :disabled="currentPage >= totalPages"
        class="page-btn"
        title="末页"
      >
        末页
      </button>
    </div>

    <!-- 新增/编辑模态框 -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h3>{{ isEditMode ? '编辑敏感词' : '新增敏感词' }}</h3>
        <form @submit.prevent="saveSensitiveWord">
          <div class="form-group">
            <label>敏感词内容 *</label>
            <input 
              v-model="formData.word" 
              type="text" 
              placeholder="请输入敏感词"
              required
              maxlength="100"
            />
            <small class="form-hint">支持中文、英文、数字等字符</small>
          </div>
          
          <div class="form-group">
            <label>敏感词类型 *</label>
            <select v-model="formData.type" required>
              <option value="1">通用</option>
              <option value="2">政治</option>
              <option value="3">色情</option>
              <option value="4">暴力</option>
              <option value="5">广告</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>状态 *</label>
            <select v-model="formData.status" required>
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>描述说明</label>
            <textarea 
              v-model="formData.description" 
              placeholder="请输入描述说明"
              rows="3"
              maxlength="200"
            ></textarea>
          </div>
          
          <div class="modal-actions">
            <button type="button" @click="closeModal" class="cancel-btn">取消</button>
            <button type="submit" class="save-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 删除确认模态框 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click="closeDeleteModal">
      <div class="modal-content delete-modal" @click.stop>
        <h3>确认删除</h3>
        <p>确定要删除敏感词 "{{ deleteWordData.word }}" 吗？</p>
        <p class="warning-text">⚠️ 警告：此操作将永久删除数据，无法恢复！</p>
        <div class="modal-actions">
          <button type="button" @click="closeDeleteModal" class="cancel-btn">取消</button>
          <button @click="confirmDelete" class="delete-confirm-btn">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { 
  getSensitiveWordList, 
  deleteSensitiveWord, 
  createSensitiveWord, 
  updateSensitiveWord,
  batchDeleteSensitiveWords
} from '@/api/sensitiveWord'

export default {
  name: 'SensitiveWordManagement',
  setup() {
    const words = ref([])
    const loading = ref(false)
    const showModal = ref(false)
    const showDeleteModal = ref(false)
    const isEditMode = ref(false)
    const searchKeyword = ref('')
    const filterType = ref('')
    const filterStatus = ref('')
    const selectedWords = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const totalPages = ref(0)
    
    const formData = ref({
      word: '',
      type: '1',
      status: '1',
      description: ''
    })
    
    const deleteWordData = ref({})
    
    // 计算属性：过滤后的敏感词列表
    const filteredWords = computed(() => {
      return words.value
    })
    
    // 计算属性：是否全选
    const isAllSelected = computed(() => {
      return selectedWords.value.length > 0 && 
             selectedWords.value.length === words.value.length
    })
    
    // 获取敏感词列表
    const fetchSensitiveWords = async () => {
      loading.value = true
      try {
        const res = await getSensitiveWordList({
          page: currentPage.value,
          size: pageSize.value,
          keyword: searchKeyword.value || undefined,
          type: filterType.value || undefined,
          status: filterStatus.value || undefined
        })
        
        if (res.data && res.data.data) {
          const pageData = res.data.data
          words.value = pageData.records || []
          total.value = pageData.total || 0
          totalPages.value = pageData.totalPages || 0
        } else {
          words.value = []
          total.value = 0
          totalPages.value = 0
        }
      } catch (err) {
        console.error('获取敏感词列表失败:', err)
        alert('获取敏感词列表失败: ' + (err.message || '未知错误'))
        words.value = []
      } finally {
        loading.value = false
      }
    }
    
    // 显示新增模态框
    const showAddModal = () => {
      isEditMode.value = false
      formData.value = {
        word: '',
        type: '1',
        status: '1',
        description: ''
      }
      showModal.value = true
    }
    
    // 显示编辑模态框
    const showEditModal = (word) => {
      isEditMode.value = true
      formData.value = {
        wordId: word.wordId,
        word: word.word,
        type: word.type || '1',
        status: word.status.toString(),
        description: word.description || ''
      }
      showModal.value = true
    }
    
    // 关闭模态框
    const closeModal = () => {
      showModal.value = false
    }
    
    // 保存敏感词
    const saveSensitiveWord = async () => {
      try {
        const requestData = {
          word: formData.value.word.trim(),
          type: formData.value.type,
          status: parseInt(formData.value.status),
          description: formData.value.description.trim() || undefined
        }
        
        if (isEditMode.value) {
          // 编辑模式
          await updateSensitiveWord(formData.value.wordId, requestData)
          alert('敏感词更新成功')
        } else {
          // 新增模式
          await createSensitiveWord(requestData)
          alert('敏感词创建成功')
        }
        closeModal()
        fetchSensitiveWords()
      } catch (err) {
        console.error('保存失败:', err)
        alert('保存失败: ' + (err.message || '未知错误'))
      }
    }
    
    // 切换敏感词状态
    const toggleStatus = async (word) => {
      try {
        const newStatus = word.status === 1 ? 0 : 1
        const actionText = newStatus === 1 ? '启用' : '禁用'
        
        if (confirm(`确定要${actionText}敏感词 "${word.word}" 吗？`)) {
          await updateSensitiveWord(word.wordId, {
            ...word,
            status: newStatus
          })
          alert(`敏感词${actionText}成功`)
          fetchSensitiveWords()
        }
      } catch (err) {
        console.error('状态切换失败:', err)
        alert('状态切换失败: ' + (err.message || '未知错误'))
      }
    }
    
    // 显示删除确认
    const showDeleteConfirm = (word) => {
      deleteWordData.value = word
      showDeleteModal.value = true
    }
    
    // 关闭删除模态框
    const closeDeleteModal = () => {
      showDeleteModal.value = false
      deleteWordData.value = {}
    }
    
    // 确认删除
    const confirmDelete = async () => {
      try {
        if (confirm(`警告：此操作将永久删除敏感词 "${deleteWordData.value.word}"，无法恢复！确认执行吗？`)) {
          await deleteSensitiveWord(deleteWordData.value.wordId)
          alert('敏感词已永久删除')
          closeDeleteModal()
          
          // 删除后重新计算分页
          const currentTotal = total.value
          const currentOffset = (currentPage.value - 1) * pageSize.value
          
          // 如果当前页只剩一条数据且不是第一页，则返回上一页
          if (words.value.length === 1 && currentPage.value > 1 && currentOffset + 1 >= currentTotal) {
            currentPage.value = Math.max(1, currentPage.value - 1)
          }
          
          fetchSensitiveWords()
        }
      } catch (err) {
        console.error('删除失败:', err)
        alert('删除失败: ' + (err.message || '未知错误'))
      }
    }
    
    // 搜索敏感词
    const searchSensitiveWords = () => {
      currentPage.value = 1
      fetchSensitiveWords()
    }
    
    // 筛选敏感词
    const filterSensitiveWords = () => {
      currentPage.value = 1
      fetchSensitiveWords()
    }
    
    // 重置筛选条件
    const resetFilters = () => {
      searchKeyword.value = ''
      filterType.value = ''
      filterStatus.value = ''
      currentPage.value = 1
      fetchSensitiveWords()
    }
    
    // 切换选择单词
    const toggleSelectWord = (wordId) => {
      const index = selectedWords.value.indexOf(wordId)
      if (index > -1) {
        selectedWords.value.splice(index, 1)
      } else {
        selectedWords.value.push(wordId)
      }
    }
    
    // 切换全选
    const toggleSelectAll = () => {
      if (isAllSelected.value) {
        selectedWords.value = []
      } else {
        selectedWords.value = words.value.map(word => word.wordId)
      }
    }
    
    // 清除选择
    const clearSelection = () => {
      selectedWords.value = []
    }
    
    // 批量启用
    const batchEnable = async () => {
      if (selectedWords.value.length === 0) return
      if (confirm(`确定要启用选中的 ${selectedWords.value.length} 个敏感词吗？`)) {
        try {
          // 这里需要后端支持批量更新状态的接口
          for (const wordId of selectedWords.value) {
            await updateSensitiveWord(wordId, { status: 1 })
          }
          alert('批量启用成功')
          clearSelection()
          fetchSensitiveWords()
        } catch (err) {
          console.error('批量启用失败:', err)
          alert('批量启用失败: ' + (err.message || '未知错误'))
        }
      }
    }
    
    // 批量禁用
    const batchDisable = async () => {
      if (selectedWords.value.length === 0) return
      if (confirm(`确定要禁用选中的 ${selectedWords.value.length} 个敏感词吗？`)) {
        try {
          // 这里需要后端支持批量更新状态的接口
          for (const wordId of selectedWords.value) {
            await updateSensitiveWord(wordId, { status: 0 })
          }
          alert('批量禁用成功')
          clearSelection()
          fetchSensitiveWords()
        } catch (err) {
          console.error('批量禁用失败:', err)
          alert('批量禁用失败: ' + (err.message || '未知错误'))
        }
      }
    }
    
    // 批量删除
    const batchDelete = async () => {
      if (selectedWords.value.length === 0) return
      if (confirm(`确定要删除选中的 ${selectedWords.value.length} 个敏感词吗？此操作不可恢复！`)) {
        try {
          await batchDeleteSensitiveWords(selectedWords.value)
          alert('批量删除成功')
          clearSelection()
          
          // 批量删除后重新计算分页
          const deletedCount = selectedWords.value.length
          const currentTotal = total.value
          const currentOffset = (currentPage.value - 1) * pageSize.value
          
          // 如果删除后当前页数据不足且不是第一页，则调整页码
          if (words.value.length <= deletedCount && currentPage.value > 1) {
            const remainingItems = words.value.length - deletedCount
            const itemsNeeded = pageSize.value - remainingItems
            if (itemsNeeded > 0 && currentOffset + words.value.length >= currentTotal) {
              currentPage.value = Math.max(1, currentPage.value - 1)
            }
          }
          
          fetchSensitiveWords()
        } catch (err) {
          console.error('批量删除失败:', err)
          alert('批量删除失败: ' + (err.message || '未知错误'))
        }
      }
    }
    
    // 更改页面
    const changePage = (page) => {
      if (page >= 1 && page <= totalPages.value) {
        currentPage.value = page
        fetchSensitiveWords()
      }
    }
    
    
    // 获取类型文本
    const getTypeText = (type) => {
      const typeMap = {
        '1': '通用',
        '2': '政治',
        '3': '色情',
        '4': '暴力',
        '5': '广告'
      }
      return typeMap[type] || '未知'
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      return status === 1 ? '启用' : '禁用'
    }
    
    // 获取状态样式类
    const getStatusClass = (status) => {
      return status === 1 ? 'status-active' : 'status-inactive'
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour12: false })
    }
    
    // 改变每页显示数量
    const onPageSizeChange = () => {
      currentPage.value = 1
      fetchSensitiveWords()
    }
    
    onMounted(() => {
      fetchSensitiveWords()
    })
    
    return {
      words,
      loading,
      showModal,
      showDeleteModal,
      isEditMode,
      searchKeyword,
      filterType,
      filterStatus,
      selectedWords,
      currentPage,
      totalPages,
      total,
      pageSize,
      formData,
      deleteWordData,
      filteredWords,
      isAllSelected,
      showAddModal,
      showEditModal,
      closeModal,
      saveSensitiveWord,
      toggleStatus,
      showDeleteConfirm,
      closeDeleteModal,
      confirmDelete,
      searchSensitiveWords,
      filterSensitiveWords,
      resetFilters,
      toggleSelectWord,
      toggleSelectAll,
      clearSelection,
      batchEnable,
      batchDisable,
      batchDelete,
      changePage,
      onPageSizeChange,
      getTypeText,
      getStatusText,
      getStatusClass,
      formatDate
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.header-section h2 {
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.add-btn, .export-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.add-btn {
  background: #28a745;
  color: white;
}

.export-btn {
  background: #6c757d;
  color: white;
}

.add-btn:hover, .export-btn:hover {
  opacity: 0.9;
}

.search-section {
  margin-bottom: 20px;
  background: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
}

.search-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input, .search-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-select {
  width: 120px;
  min-width: 100px;
  max-width: 150px;
}

.search-input {
  width: 200px;
  min-width: 150px;
  max-width: 300px;
}

.search-btn, .reset-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.search-btn {
  background: #007bff;
  color: white;
}

.reset-btn {
  background: #6c757d;
  color: white;
}

.batch-action-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background: #e3f2fd;
  border-radius: 8px;
  border: 1px solid #bbdefb;
}

.batch-info {
  font-size: 14px;
  color: #1976d2;
  font-weight: bold;
}

.batch-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.batch-enable-btn, .batch-disable-btn, .batch-delete-btn, .clear-selection-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.batch-enable-btn {
  background: #28a745;
  color: white;
}

.batch-disable-btn {
  background: #ffc107;
  color: #212529;
}

.batch-delete-btn {
  background: #dc3545;
  color: white;
}

.clear-selection-btn {
  background: #6c757d;
  color: white;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  color: white;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.table-container {
  overflow-x: auto;
  margin-bottom: 20px;
}

.sensitive-word-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.sensitive-word-table th, .sensitive-word-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.sensitive-word-table th {
  background: #f8f9fa;
  font-weight: 600;
  position: sticky;
  top: 0;
}

.checkbox-column {
  width: 50px;
  text-align: center;
}

.word-content {
  font-weight: 500;
  color: #333;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: #d4edda;
  color: #155724;
}

.status-inactive {
  background: #f8d7da;
  color: #721c24;
}

.action-cell {
  white-space: nowrap;
}

.edit-btn, .status-toggle-btn, .delete-btn {
  padding: 4px 8px;
  margin: 0 2px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.edit-btn {
  background: #ffc107;
  color: #212529;
}

.edit-btn:hover {
  background: #e0a800;
  color: #212529;
}

.disable-btn {
  background: #dc3545;
  color: white;
}

.enable-btn {
  background: #28a745;
  color: white;
}

.delete-btn {
  background: #dc3545;
  color: white;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.page-size-select {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.page-size-select:hover {
  border-color: #4CAF50;
}

.page-size-select:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  color: #333;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
  font-size: 14px;
}

.page-btn:hover:not(:disabled) {
  background-color: #f8f9fa;
  border-color: #c6c6c6;
}

.page-btn:active:not(:disabled) {
  background-color: #e9ecef;
  border-color: #adb5bd;
  transform: scale(0.98);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background-color: #f8f9fa;
}

.page-info {
  font-size: 14px;
  color: #666;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #555;
}

.form-hint {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: #888;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}


.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.cancel-btn, .save-btn, .delete-confirm-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background: #6c757d;
  color: white;
}

.save-btn {
  background: #007bff;
  color: white;
}

.delete-confirm-btn {
  background: #dc3545;
  color: white;
}

.warning-text {
  color: #dc3545;
  font-size: 14px;
  margin: 10px 0;
}

.delete-modal p {
  margin: 10px 0;
}

@media (max-width: 768px) {
  .search-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input, .search-select {
    width: 100%;
  }
  
  .header-section {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .header-actions {
    justify-content: center;
  }
  
  .batch-action-section {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }
  
  .batch-actions {
    justify-content: center;
  }
  
  .sensitive-word-table {
    font-size: 12px;
  }
  
  .sensitive-word-table th, .sensitive-word-table td {
    padding: 8px 4px;
  }
  
  .action-cell button {
    display: block;
    width: 100%;
    margin: 2px 0;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .search-select {
    width: 100px;
  }
  
  .search-input {
    width: 150px;
  }
}

@media (min-width: 1025px) {
  .search-select {
    width: 120px;
  }
  
  .search-input {
    width: 200px;
  }
}
</style>
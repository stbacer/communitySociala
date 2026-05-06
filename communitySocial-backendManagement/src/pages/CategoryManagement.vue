<template>
  <div class="page-container">
    <div class="header-section">
      <h2>板块管理</h2>
      <button @click="showAddModal" class="add-btn">新增板块</button>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="search-section">
      <div class="search-row">
        <input 
          v-model="searchKeyword" 
          placeholder="搜索板块名称..." 
          class="search-input"
          @keyup.enter="searchCategories"
        />
        <select v-model="filterStatus" class="search-select" @change="filterCategories">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
        <button @click="searchCategories" class="search-btn">搜索</button>
        <button @click="resetFilters" class="reset-btn">重置</button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 板块表格 -->
    <div class="table-container">
      <table class="category-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>板块名称</th>
            <th>描述</th>
            <th>排序</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="category in filteredCategories" :key="category.categoryId">
            <td>{{ category.categoryId }}</td>
            <td>{{ category.name }}</td>
            <td>{{ category.description || '-' }}</td>
            <td>{{ category.sortOrder || 0 }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(category.status)]">
                {{ getStatusText(category.status) }}
              </span>
            </td>
            <td>{{ formatDate(category.createTime) }}</td>
            <td class="action-cell">
              <button @click="showEditModal(category)" class="edit-btn">编辑</button>
              <button 
                @click="toggleStatus(category)" 
                :class="['status-toggle-btn', category.status === 1 ? 'disable-btn' : 'enable-btn']"
              >
                {{ category.status === 1 ? '禁用' : '启用' }}
              </button>
              <button @click="showDeleteConfirm(category)" class="delete-btn">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 空状态 -->
      <div v-if="!loading && filteredCategories.length === 0" class="empty-state">
        <p>暂无板块数据</p>
      </div>
    </div>

    <!-- 新增/编辑模态框 -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h3>{{ isEditMode ? '编辑板块' : '新增板块' }}</h3>
        <form @submit.prevent="saveCategory">
          <div class="form-group">
            <label>板块名称 *</label>
            <input 
              v-model="formData.name" 
              type="text" 
              placeholder="请输入板块名称"
              required
              maxlength="50"
            />
          </div>
          
          <div class="form-group">
            <label>板块描述</label>
            <textarea 
              v-model="formData.description" 
              placeholder="请输入板块描述"
              rows="3"
              maxlength="200"
            ></textarea>
          </div>
          
          <div class="form-group">
            <label>排序</label>
            <input 
              v-model.number="formData.sortOrder" 
              type="number" 
              placeholder="请输入排序数字"
              min="0"
            />
          </div>
          
          <div class="form-group">
            <label>状态</label>
            <select v-model="formData.status">
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </select>
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
        <p>确定要删除板块 "{{ deleteCategoryData.name }}" 吗？</p>
        <p class="warning-text">注意：删除后将无法恢复</p>
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
import { getCategoryList, deleteCategory, createCategory, updateCategory } from '@/api/category'

export default {
  name: 'CategoryManagement',
  setup() {
    const categories = ref([])
    const loading = ref(false)
    const showModal = ref(false)
    const showDeleteModal = ref(false)
    const isEditMode = ref(false)
    const searchKeyword = ref('')
    const filterStatus = ref('')
    
    const formData = ref({
      name: '',
      description: '',
      sortOrder: 0,
      status: '1'
    })
    
    const deleteCategoryData = ref({})
    
    // 计算属性：过滤后的板块列表
    const filteredCategories = computed(() => {
      let result = categories.value
      
      // 关键词搜索
      if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        result = result.filter(cat => 
          cat.name.toLowerCase().includes(keyword)
        )
      }
      
      // 状态筛选
      if (filterStatus.value !== '') {
        result = result.filter(cat => 
          cat.status.toString() === filterStatus.value
        )
      }
      
      // 按排序字段排序
      return result.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    })
    
    // 获取板块列表
    const fetchCategories = async () => {
      loading.value = true
      try {
        const res = await getCategoryList()
        categories.value = res.data.data || []
      } catch (err) {
        console.error('获取板块列表失败:', err)
        alert('获取板块列表失败: ' + (err.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }
    
    // 显示新增模态框
    const showAddModal = () => {
      isEditMode.value = false
      formData.value = {
        name: '',
        description: '',
        sortOrder: 0,
        status: '1'
      }
      showModal.value = true
    }
    
    // 显示编辑模态框
    const showEditModal = (category) => {
      isEditMode.value = true
      formData.value = {
        categoryId: category.categoryId,
        name: category.name,
        description: category.description || '',
        sortOrder: category.sortOrder || 0,
        status: category.status.toString()
      }
      showModal.value = true
    }
    
    // 关闭模态框
    const closeModal = () => {
      showModal.value = false
    }
    
    // 保存板块
    const saveCategory = async () => {
      try {
        if (isEditMode.value) {
          // 编辑模式
          await updateCategory(formData.value.categoryId, {
            name: formData.value.name,
            description: formData.value.description,
            sortOrder: formData.value.sortOrder,
            status: parseInt(formData.value.status)
          })
          alert('板块更新成功')
        } else {
          // 新增模式
          await createCategory({
            name: formData.value.name,
            description: formData.value.description,
            sortOrder: formData.value.sortOrder,
            status: parseInt(formData.value.status)
          })
          alert('板块创建成功')
        }
        closeModal()
        fetchCategories()
      } catch (err) {
        console.error('保存失败:', err)
        alert('保存失败: ' + (err.message || '未知错误'))
      }
    }
    
    // 切换板块状态
    const toggleStatus = async (category) => {
      try {
        const newStatus = category.status === 1 ? 0 : 1
        const actionText = newStatus === 1 ? '启用' : '禁用'
        
        if (confirm(`确定要${actionText}板块 "${category.name}" 吗？`)) {
          await updateCategory(category.categoryId, {
            ...category,
            status: newStatus
          })
          alert(`板块${actionText}成功`)
          fetchCategories()
        }
      } catch (err) {
        console.error('状态切换失败:', err)
        alert('状态切换失败: ' + (err.message || '未知错误'))
      }
    }
    
    // 显示删除确认
    const showDeleteConfirm = (category) => {
      deleteCategoryData.value = category
      showDeleteModal.value = true
    }
    
    // 关闭删除模态框
    const closeDeleteModal = () => {
      showDeleteModal.value = false
      deleteCategoryData.value = {}
    }
    
    // 确认删除
    const confirmDelete = async () => {
      try {
        await deleteCategory(deleteCategoryData.value.categoryId)
        alert('板块删除成功')
        closeDeleteModal()
        fetchCategories()
      } catch (err) {
        console.error('删除失败:', err)
        alert('删除失败: ' + (err.message || '未知错误'))
      }
    }
    
    // 搜索板块
    const searchCategories = () => {
      // 搜索逻辑已在计算属性中实现
    }
    
    // 筛选板块
    const filterCategories = () => {
      // 筛选逻辑已在计算属性中实现
    }
    
    // 重置筛选条件
    const resetFilters = () => {
      searchKeyword.value = ''
      filterStatus.value = ''
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case 1: return '启用'
        case 0: return '禁用'
        default: return '未知'
      }
    }
    
    // 获取状态样式类
    const getStatusClass = (status) => {
      switch (status) {
        case 1: return 'status-active'
        case 0: return 'status-inactive'
        default: return 'status-unknown'
      }
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour12: false })
    }
    
    onMounted(() => {
      fetchCategories()
    })
    
    return {
      categories,
      loading,
      showModal,
      showDeleteModal,
      isEditMode,
      searchKeyword,
      filterStatus,
      formData,
      deleteCategoryData,
      filteredCategories,
      showAddModal,
      showEditModal,
      closeModal,
      saveCategory,
      toggleStatus,
      showDeleteConfirm,
      closeDeleteModal,
      confirmDelete,
      searchCategories,
      filterCategories,
      resetFilters,
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
}

.header-section h2 {
  margin: 0;
  color: #333;
}

.add-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.add-btn:hover {
  background: #218838;
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

.category-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.category-table th, .category-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.category-table th {
  background: #f8f9fa;
  font-weight: 600;
  position: sticky;
  top: 0;
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

.status-unknown {
  background: #d1d1d1;
  color: #666;
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

.save-btn, .delete-confirm-btn {
  background: #007bff;
  color: white;
}

.delete-confirm-btn {
  background: #dc3545;
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
    gap: 10px;
    align-items: stretch;
  }
  
  .category-table {
    font-size: 12px;
  }
  
  .category-table th, .category-table td {
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
<template>
  <div class="page-container">
    <div class="header-section">
      <h2>操作日志管理</h2>
      <div class="header-actions">
        <button @click="refreshLogs" class="refresh-btn">刷新</button>
        <button @click="showStatistics" class="stats-btn">统计分析</button>
        <button @click="cleanOld" class="clean-btn">清理旧日志</button>
      </div>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="search-section">
      <div class="search-row">
        <input 
          v-model="searchOperator" 
          placeholder="搜索操作用户..." 
          class="search-input"
          @keyup.enter="searchLogs"
        />
        <select v-model="searchOperationType" class="search-select" @change="searchLogs">
          <option value="">全部操作类型</option>
          <option v-for="type in operationTypeOptions" :key="type.value" :value="type.value">
            {{ type.label }}
          </option>
        </select>
        <!-- <input 
          v-model="startTime" 
          type="datetime-local" 
          placeholder="开始时间" 
          class="datetime-input"
        />
        <input 
          v-model="endTime" 
          type="datetime-local" 
          placeholder="结束时间" 
          class="datetime-input"
        /> -->
        <button @click="searchLogs" class="search-btn">搜索</button>
        <button @click="resetFilters" class="reset-btn">重置</button>
      </div>
    </div>

    <!-- 批量操作区域 -->
    <div v-if="selectedLogs.length > 0" class="batch-action-section">
      <div class="batch-info">
        已选择 {{ selectedLogs.length }} 条日志
      </div>
      <div class="batch-actions">
        <button @click="batchDelete" class="batch-delete-btn">批量删除</button>
        <button @click="clearSelection" class="clear-selection-btn">取消选择</button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 日志表格 -->
    <div class="table-container">
      <table class="log-table">
        <thead>
          <tr>
            <th class="checkbox-column">
              <input 
                type="checkbox" 
                :checked="isAllSelected" 
                @change="toggleSelectAll"
              />
            </th>
            <th>日志 ID</th>
            <th>操作用户</th>
            <th>操作类型</th>
            <th class="content-column">操作详情</th>
            <th>操作时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.logId">
            <td class="checkbox-column">
              <input 
                type="checkbox" 
                :checked="selectedLogs.includes(log.logId)"
                @change="toggleSelectLog(log.logId)"
              />
            </td>
            <td>{{ log.logId }}</td>
            <td>{{ formatOperator(log.userId, log.operatorName) }}</td>
            <td>
              <span :class="['operation-badge', getOperationClass(log.operation, log.module)]">
                {{ getOperationText(log.operation, log.module) }}
              </span>
            </td>
            <td class="content-column">{{ formatLogContent(log) }}</td>
            <td>{{ formatDateTime(log.createTime) }}</td>
            <td class="action-cell">
              <button @click="showDetail(log.logId)" class="detail-btn">详情</button>
              <button @click="remove(log.logId)" class="delete-btn">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 空状态 -->
      <div v-if="!loading && logs.length === 0" class="empty-state">
        <p>暂无操作日志数据</p>
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

    <!-- 统计图表模态框 -->
    <div v-if="showStatsModal" class="modal-overlay" @click="closeStatsModal">
      <div class="modal-content stats-modal" @click.stop>
        <h3>操作统计分析</h3>
        <div v-if="statistics.length > 0" class="stats-container">
          <div class="chart-container">
            <h4>操作类型分布</h4>
            <div class="chart-bars">
              <div 
                v-for="stat in statistics" 
                :key="stat.operation" 
                class="chart-bar"
              >
                <div class="bar-label">{{ getOperationText(stat.operation, stat.module) }}</div>
                <div class="bar-wrapper">
                  <div 
                    class="bar-fill" 
                    :style="{ width: getBarWidth(stat.count) }"
                  ></div>
                  <span class="bar-value">{{ stat.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="no-stats">
          <p>暂无统计数据</p>
        </div>
        <div class="modal-actions">
          <button @click="closeStatsModal" class="close-btn">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getLogList, deleteLog, batchDeleteLogs, getLogStatistics, getOperationTypes } from '../api/log'

export default {
  name: 'LogManagement',
  setup() {
    const router = useRouter()
    const logs = ref([])
    const loading = ref(false)
    const showStatsModal = ref(false)
    const statistics = ref([])
    
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const totalPages = ref(0)
    
    const searchOperator = ref('')
    const searchOperationType = ref('')
    const startTime = ref('')
    const endTime = ref('')
    const selectedLogs = ref([])
    const operationTypeOptions = ref([])
    const isAllSelected = computed(() => {
      return selectedLogs.value.length > 0 && 
             selectedLogs.value.length === logs.value.length
    })
    
    // 加载操作类型选项
    const loadOperationTypes = async () => {
      useFallbackOperationTypes()
    }

    const useFallbackOperationTypes = () => {
      operationTypeOptions.value = [
        { label: '登录', value: 'LOGIN' },
        { label: '管理员审核', value: 'APPROVE_AUTH' },
        { label: '收藏', value: 'COLLECT' },
        { label: '点赞', value: 'LIKE' },
        { label: '实名审核', value: '实名审核' },
        { label: '举报', value: 'REPORT' },
        { label: '删除', value: 'DELETE' },
        { label: '帖子', value: '帖子' },
        { label: '评论', value: 'CREATE' },
        { label: '取消收藏', value: 'UNCOLLECT' },
        { label: '取消点赞', value: 'UNLIKE' }
      ]

    }
    
    // 格式化操作类型标签
    const formatTypeLabel = (type) => {
      const map = {
        'LOGIN': '登录',
        'APPROVE_AUTH': '管理员审核',
        'COLLECT': '收藏',
        'LIKE': '点赞',
        '实名审核': '实名审核',
        'REPORT': '举报',
        'DELETE': '删除',
        '帖子': '帖子',
        'CREATE': '创建',
        'UNCOLLECT': '取消收藏',
        'UNLIKE': '取消点赞'
      }
      return map[type] || type
    }
    
    // 获取日志列表（真实 API）
    const fetchLogs = async (page = 1) => {
      loading.value = true
      
      try {
        const params = {
          page,
          size: pageSize.value,
          operator: searchOperator.value || undefined,
          operationType: searchOperationType.value || undefined,
          startTime: startTime.value || undefined,
          endTime: endTime.value || undefined
        }
        const response = await getLogList(params)
        const { data } = response
        
        if (data.code === 200) {
          logs.value = data.data.records || []
          total.value = data.data.total || 0
          
          // 根据实际总数和每页大小计算总页数
          const calculatedTotalPages = Math.ceil(total.value / pageSize.value)
          totalPages.value = data.data.totalPages || calculatedTotalPages
          
          // 如果当前页码大于总页数，跳转到最后一页
          if (page > totalPages.value && totalPages.value > 0) {
            currentPage.value = totalPages.value
            fetchLogs(totalPages.value)
            return
          }
          
          currentPage.value = page
        } else {
          alert(data.message || '加载日志失败')
        }
      } catch (error) {
        console.error('加载日志异常:', error)
        alert('加载日志失败：' + (error.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }
    
    // 刷新日志
    const refreshLogs = () => {
      fetchLogs(currentPage.value)
    }
    
    // 搜索日志
    const searchLogs = () => {
      currentPage.value = 1
      fetchLogs(1)
    }
    
    // 重置筛选条件
    const resetFilters = () => {
      searchOperator.value = ''
      searchOperationType.value = ''
      startTime.value = ''
      endTime.value = ''
      currentPage.value = 1
      fetchLogs(1)
    }
    
    // 切换选择日志
    const toggleSelectLog = (logId) => {
      const index = selectedLogs.value.indexOf(logId)
      if (index > -1) {
        selectedLogs.value.splice(index, 1)
      } else {
        selectedLogs.value.push(logId)
      }
    }
    
    // 切换全选
    const toggleSelectAll = () => {
      if (isAllSelected.value) {
        selectedLogs.value = []
      } else {
        selectedLogs.value = logs.value.map(log => log.logId)
      }
    }
    
    // 清除选择
    const clearSelection = () => {
      selectedLogs.value = []
    }
    
    // 批量删除（真实 API）
    const batchDelete = async () => {
      if (selectedLogs.value.length === 0) {
        alert('请先选择要删除的日志')
        return
      }
      
      if (confirm(`确定要删除选中的 ${selectedLogs.value.length} 条日志吗？此操作不可恢复！`)) {
        loading.value = true
        
        try {
          const response = await batchDeleteLogs(selectedLogs.value)
          const { data } = response
          
          if (data.code === 200) {
            const result = data.data || {}
            const successCount = result.successCount || selectedLogs.value.length
            const failedCount = result.failedCount || 0
            
            alert(`批量删除成功\n成功：${successCount} 条\n失败：${failedCount} 条`)
            clearSelection()
            refreshLogs()
          } else {
            alert(data.message || '批量删除失败')
            loading.value = false
          }
        } catch (error) {
          alert('批量删除失败：' + (error.response?.data?.message || error.message))
          loading.value = false
        }
      }
    }
    
    // 显示详情（跳转到详情页）
    const showDetail = (logId) => {
      router.push(`/logs/${logId}`)
    }
    
    // 关闭详情模态框（已移除，改为路由跳转）
    // const closeDetailModal = () => {
    //   showDetailModal.value = false
    //   currentLogDetail.value = null
    // }
    
    // 删除单条日志（真实 API）
    const remove = async (logId) => {
      if (confirm('确认删除该日志？此操作不可恢复！')) {
        loading.value = true
        
        try {
          const response = await deleteLog(logId)
          const { data } = response
          
          if (data.code === 200) {
            alert('删除成功')
            refreshLogs()
          } else {
            alert(data.message || '删除失败')
            loading.value = false
          }
        } catch (error) {
          alert('删除失败：' + (error.response?.data?.message || error.message))
          loading.value = false
        }
      }
    }
    
    // 清理旧日志（模拟）
    const cleanOld = async () => {
      if (confirm('确认清理 30 天前的所有日志？')) {
        // 模拟清理
        await new Promise(resolve => setTimeout(resolve, 500))
        alert('清理成功')
        refreshLogs()
      }
    }
    
    // 显示统计（模拟）
    const showStatistics = async () => {
      // 模拟统计计算
      await new Promise(resolve => setTimeout(resolve, 300))
      
      const operationCounts = {}
      logs.value.forEach(log => {
        operationCounts[log.operation] = (operationCounts[log.operation] || 0) + 1
      })
      
      statistics.value = Object.entries(operationCounts).map(([operation, count]) => ({
        operation,
        count
      }))
      
      showStatsModal.value = true
    }
    
    // 关闭统计模态框
    const closeStatsModal = () => {
      showStatsModal.value = false
      statistics.value = []
    }
    
    // 获取操作类型文本
    const getOperationText = (operation, module) => {
      if (module === 'COMMENT' && operation === 'CREATE') {
        return '评论'
      }
      
      const operationMap = {
        'LOGIN': '登录',
        'APPROVE_AUTH': '管理员审核',
        'COLLECT': '收藏',
        'LIKE': '点赞',
        '实名审核': '实名审核',
        'REPORT': '举报',
        'DELETE': '删除',
        '帖子': '帖子',
        'CREATE': '创建',
        'UNCOLLECT': '取消收藏',
        'UNLIKE': '取消点赞'
      }
      return operationMap[operation] || operation
    }
    
    // 获取操作类型样式类
    const getOperationClass = (operation, module) => {
      if (module === 'COMMENT' && operation === 'CREATE') {
        return 'comment'
      }
      
      const classMap = {
        'LOGIN': 'login',
        'APPROVE_AUTH': 'admin-approve',
        'COLLECT': 'collect',
        'LIKE': 'like',
        '实名审核': 'auth-review',
        'REPORT': 'report',
        'DELETE': 'delete',
        '帖子': 'post',
        'CREATE': 'create',
        'UNCOLLECT': 'uncollect',
        'UNLIKE': 'unlike'
      }
      return classMap[operation] || 'default'
    }
    
    // 格式化操作用户信息
    const formatOperator = (userId, operatorName) => {
      if (!userId && !operatorName) return '-'
      const id = userId || '未知 ID'
      const name = operatorName || '匿名用户'
      return `${name}(${id})`
    }

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour12: false })
    }
    
    // 格式化操作日志内容为统一格式
    const formatLogContent = (log) => {
      if (!log.content) return '-'
      
      // 直接使用后端返回的 content 字段
      // 格式：[用户昵称]（id:[用户 id]）+ [动作描述] + [目标对象]（id:[目标 id]）时间：xxxx.xx.xx xx:xx:xx
      return log.content
    }
    
    // 格式化日期时间 (YYYY.MM.DD HH:mm:ss)
    const formatDateTime = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}.${month}.${day} ${hours}:${minutes}:${seconds}`
    }
    
    // 获取柱状图宽度
    const getBarWidth = (count) => {
      if (statistics.value.length === 0) return '0%'
      const maxCount = Math.max(...statistics.value.map(s => s.count))
      return maxCount > 0 ? ((count / maxCount) * 100) + '%' : '0%'
    }
    
    // 切换页面
    const changePage = (page) => {
      if (page < 1) page = 1
      if (page > totalPages.value && totalPages.value > 0) page = totalPages.value
      
      if (page !== currentPage.value) {
        currentPage.value = page
        fetchLogs(page)
      }
    }
    
    // 改变每页显示数量
    const onPageSizeChange = () => {
      currentPage.value = 1
      fetchLogs(1)
    }
    
    onMounted(() => {
      loadOperationTypes()  // 加载操作类型选项
      fetchLogs()
    })
    
    return {
      logs,
      loading,
      showStatsModal,
      statistics,
      currentPage,
      totalPages,
      total,
      pageSize,
      searchOperator,
      searchOperationType,
      startTime,
      endTime,
      selectedLogs,
      isAllSelected,
      operationTypeOptions,
      refreshLogs,
      searchLogs,
      resetFilters,
      toggleSelectLog,
      toggleSelectAll,
      clearSelection,
      batchDelete,
      showDetail,
      remove,
      cleanOld,
      showStatistics,
      closeStatsModal,
      getOperationText,
      getOperationClass,
      formatOperator,
      formatDate,
      formatDateTime,
      formatLogContent,
      getBarWidth,
      changePage,
      onPageSizeChange,
      loadOperationTypes,
      formatTypeLabel
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
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

.header-actions button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.refresh-btn {
  background-color: #4CAF50;
  color: white;
}

.refresh-btn:hover {
  background-color: #45a049;
}

.stats-btn {
  background-color: #2196F3;
  color: white;
}

.stats-btn:hover {
  background-color: #0b7dda;
}

.clean-btn {
  background-color: #ff9800;
  color: white;
}

.clean-btn:hover {
  background-color: #e68900;
}

.search-section {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.search-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.search-input, .search-select, .datetime-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-input, .datetime-input {
  min-width: 150px;
}

.search-select {
  width: 120px;
}

.search-btn, .reset-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.search-btn {
  background-color: #2196F3;
  color: white;
}

.search-btn:hover {
  background-color: #0b7dda;
}

.reset-btn {
  background-color: #9E9E9E;
  color: white;
}

.reset-btn:hover {
  background-color: #757575;
}

.batch-action-section {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 4px;
  padding: 10px 15px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.batch-info {
  font-weight: 500;
  color: #856404;
}

.batch-actions {
  display: flex;
  gap: 10px;
}

.batch-delete-btn {
  background-color: #dc3545;
  color: white;
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.batch-delete-btn:hover {
  background-color: #c82333;
}

.clear-selection-btn {
  background-color: #6c757d;
  color: white;
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.clear-selection-btn:hover {
  background-color: #5a6268;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
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
  border-top: 4px solid #3498db;
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
  border: 1px solid #ddd;
  border-radius: 8px;
  margin-bottom: 20px;
  background: white;
}

.log-table {
  width: 100%;
  border-collapse: collapse;
}

.log-table th, .log-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.log-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
  position: sticky;
  top: 0;
}

.checkbox-column {
  width: 40px;
  text-align: center;
}

.content-column {
  min-width: 300px;
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.url-column {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.description-column {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #555;
}

.operation-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.operation-badge.query {
  background-color: #d1ecf1;
  color: #0c5460;
}

.operation-badge.create {
  background-color: #d4edda;
  color: #155724;
}

.operation-badge.update {
  background-color: #fff3cd;
  color: #856404;
}

.operation-badge.delete {
  background-color: #f8d7da;
  color: #721c24;
}

.operation-badge.like {
  background-color: #ffc107;
  color: #856404;
}

.operation-badge.unlike {
  background-color: #e0e0e0;
  color: #666666;
}

.operation-badge.collect {
  background-color: #ff9800;
  color: #ffffff;
}

.operation-badge.uncollect {
  background-color: #bdbdbd;
  color: #666666;
}

.operation-badge.report {
  background-color: #e53935;
  color: #ffffff;
}

.operation-badge.comment {
  background-color: #9c27b0;
  color: #ffffff;
}

.operation-badge.auth-review {
  background-color: #2196F3;
  color: #ffffff;
}

.operation-badge.admin-approve {
  background-color: #4CAF50;
  color: #ffffff;
}

.operation-badge.post {
  background-color: #673AB7;
  color: #ffffff;
}

.action-cell {
  display: flex;
  gap: 8px;
}

.detail-btn, .delete-btn {
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.detail-btn {
  background-color: #17a2b8;
  color: white;
}

.detail-btn:hover {
  background-color: #138496;
}

.delete-btn {
  background-color: #dc3545;
  color: white;
}

.delete-btn:hover {
  background-color: #c82333;
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
}

.page-btn:hover:not(:disabled) {
  background-color: #f8f9fa;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
}

.log-detail {
  margin-bottom: 20px;
}

.detail-item {
  margin-bottom: 12px;
  display: flex;
}

.detail-item label {
  font-weight: 600;
  width: 100px;
  color: #555;
  flex-shrink: 0;
}

.detail-item span {
  flex: 1;
  color: #333;
  word-break: break-all;
}

.url-text {
  word-break: break-all;
  color: #17a2b8;
}

.ua-text {
  font-size: 12px;
  color: #666;
}

.params-text {
  background: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  max-height: 150px;
  overflow-y: auto;
}

.stats-modal {
  max-width: 500px;
}

.stats-container {
  margin-bottom: 20px;
}

.chart-container h4 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
}

.chart-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chart-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-label {
  width: 80px;
  font-size: 14px;
  color: #333;
}

.bar-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-fill {
  height: 20px;
  background: linear-gradient(90deg, #4CAF50, #8BC34A);
  border-radius: 10px;
  transition: width 0.3s ease;
}

.bar-value {
  font-weight: 600;
  color: #333;
  min-width: 40px;
  text-align: right;
}

.no-stats {
  text-align: center;
  padding: 30px;
  color: #666;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.close-btn {
  padding: 8px 16px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.close-btn:hover {
  background-color: #5a6268;
}

@media (max-width: 768px) {
  .page-container {
    padding: 10px;
  }
  
  .header-section {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  
  .search-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input, .search-select, .datetime-input {
    width: 100%;
  }
  
  .log-table {
    font-size: 12px;
  }
  
  .log-table th, .log-table td {
    padding: 8px;
  }
  
  .modal-content {
    width: 95%;
    padding: 15px;
  }
  
  .pagination {
    flex-direction: column;
    gap: 10px;
    align-items: center;
  }
  
  .page-size-selector {
    order: 1;
  }
  
  .page-info {
    order: 2;
  }
  
  .page-btn:first-child,
  .page-btn:last-child {
    display: none;
  }
}
</style>

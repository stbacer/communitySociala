<template>
  <div class="page-container">
    <h2>管理员账号审核</h2>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 待审核列表 -->
    <div class="table-container">
      <table class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>手机号</th>
            <th>邮箱</th>
            <th>真实姓名</th>
            <th>身份证号</th>
            <th>所在社区</th>
            <th>申请时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in pendingUsers" :key="user.userId">
            <td>{{ user.userId }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.nickname }}</td>
            <td>{{ user.phone || '-' }}</td>
            <td>{{ user.realName || '-' }}</td>
            <td>{{ user.idCard || '-' }}</td>
            <td>{{ user.community || '-' }}</td>
            <td>{{ formatDate(user.createTime) }}</td>
            <td class="action-cell">
              <button @click="viewUserDetail(user)" class="view-btn">查看</button>
              <button @click="approveUser(user)" class="approve-btn">通过</button>
              <button @click="rejectUser(user)" class="reject-btn">拒绝</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 空状态 -->
      <div v-if="!loading && pendingUsers.length === 0" class="empty-state">
        <p>暂无待审核的管理员申请</p>
      </div>
    </div>
    
    <!-- 用户详情模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="closeViewModal">
      <div class="modal-content" @click.stop>
        <h3>用户详情</h3>
        <div v-if="currentUser" class="user-detail">
          <div class="detail-grid">
            <div class="detail-section">
              <h4>基本信息</h4>
              <div class="detail-row">
                <label>用户 ID:</label>
                <span>{{ currentUser.userId }}</span>
              </div>
              <div class="detail-row">
                <label>用户名:</label>
                <span>{{ currentUser.username }}</span>
              </div>
              <div class="detail-row">
                <label>昵称:</label>
                <span>{{ currentUser.nickname }}</span>
              </div>
              <div class="detail-row">
                <label>手机号:</label>
                <span>{{ currentUser.phone || '未设置' }}</span>
              </div>
              <div class="detail-row">
                <label>邮箱:</label>
                <span>{{ currentUser.phone || '未设置' }}</span>
              </div>
            </div>
            
            <div class="detail-section">
              <h4>认证信息</h4>
              <div class="detail-row">
                <label>真实姓名:</label>
                <span>{{ currentUser.realName || '未填写' }}</span>
              </div>
              <div class="detail-row">
                <label>身份证号:</label>
                <span>{{ currentUser.idCard || '未填写' }}</span>
              </div>
              <div class="detail-row">
                <label>所在社区:</label>
                <span>{{ currentUser.community || '未填写' }}</span>
              </div>
              <div class="detail-row">
                <label>用户角色:</label>
                <span>{{ getUserRoleText(currentUser.userRole) }}</span>
              </div>
              <div class="detail-row">
                <label>认证状态:</label>
                <span>{{ getAuthStatusText(currentUser.authStatus) }}</span>
              </div>
              <div class="detail-row">
                <label>申请时间:</label>
                <span>{{ formatDate(currentUser.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="closeViewModal" class="close-btn">关闭</button>
        </div>
      </div>
    </div>
    
    <!-- 拒绝原因模态框 -->
    <div v-if="showRejectModal" class="modal-overlay" @click="closeRejectModal">
      <div class="modal-content" @click.stop>
        <h3>拒绝原因</h3>
        <textarea 
          v-model="rejectReason" 
          placeholder="请输入拒绝原因（选填）"
          rows="4"
          maxlength="500"
        ></textarea>
        <div class="modal-actions">
          <button @click="closeRejectModal" class="cancel-btn">取消</button>
          <button @click="confirmReject" class="confirm-btn">确认拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import apiClient from '@/api'

export default {
  name: 'AdminReview',
  setup() {
    const pendingUsers = ref([])
    const loading = ref(false)
    const showViewModal = ref(false)
    const showRejectModal = ref(false)
    const currentUser = ref(null)
    const rejectReason = ref('')
    
    // 获取待审核管理员列表
    const fetchPendingUsers = async () => {
      loading.value = true
      try {
        const res = await apiClient.get('/admin/user/pending-admins', {
          params: {
            userRole: 2,
            authStatus: 1
          }
        })
        
        if (res.data.code === 200) {
          pendingUsers.value = res.data.data || []
        }
      } catch (err) {
        console.error('获取待审核列表失败:', err)
        alert('获取待审核列表失败：' + (err.response?.data?.message || err.message))
      } finally {
        loading.value = false
      }
    }
    
    // 查看详情
    const viewUserDetail = (user) => {
      currentUser.value = user
      showViewModal.value = true
    }
    
    // 关闭详情模态框
    const closeViewModal = () => {
      showViewModal.value = false
      currentUser.value = null
    }
    
    // 通过审核
    const approveUser = async (user) => {
      if (confirm(`确定要通过用户 "${user.username}" 的审核吗？`)) {
        try {
          await apiClient.put(`/admin/user/approve-admin/${user.userId}`)
          alert('审核通过成功')
          fetchPendingUsers()
        } catch (err) {
          console.error('审核通过失败:', err)
          alert('审核通过失败：' + (err.response?.data?.message || err.message))
        }
      }
    }
    
    // 显示拒绝模态框
    const showRejectDialog = (user) => {
      currentUser.value = user
      rejectReason.value = ''
      showRejectModal.value = true
    }
    
    // 拒绝审核
    const rejectUser = async (user) => {
      showRejectDialog(user)
    }
    
    // 确认拒绝
    const confirmReject = async () => {
      try {
        await apiClient.put(`/admin/user/reject-admin/${currentUser.value.userId}`, {
          reason: rejectReason.value
        })
        alert('已拒绝该用户的申请')
        closeRejectModal()
        fetchPendingUsers()
      } catch (err) {
        console.error('拒绝失败:', err)
        alert('拒绝失败：' + (err.response?.data?.message || err.message))
      }
    }
    
    // 关闭拒绝模态框
    const closeRejectModal = () => {
      showRejectModal.value = false
      currentUser.value = null
      rejectReason.value = ''
    }
    
    // 工具函数
    const getUserRoleText = (role) => {
      const map = { 1: '普通用户', 2: '社区管理员', 3: '超级管理员' }
      return map[role] || '未知'
    }
    
    const getAuthStatusText = (status) => {
      const map = { 0: '未认证', 1: '审核中', 2: '已认证', 3: '认证失败' }
      return map[status] || '未知'
    }
    
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN')
    }
    
    onMounted(() => {
      fetchPendingUsers()
    })
    
    return {
      pendingUsers,
      loading,
      showViewModal,
      showRejectModal,
      currentUser,
      rejectReason,
      viewUserDetail,
      closeViewModal,
      approveUser,
      rejectUser,
      confirmReject,
      closeRejectModal,
      getUserRoleText,
      getAuthStatusText,
      formatDate
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 2000;
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

.admin-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.admin-table th, .admin-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.admin-table th {
  background: #f8f9fa;
  font-weight: 600;
  position: sticky;
  top: 0;
}

.action-cell {
  white-space: nowrap;
}

.view-btn, .approve-btn, .reject-btn {
  padding: 4px 8px;
  margin: 0 2px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.view-btn {
  background: #17a2b8;
  color: white;
}

.approve-btn {
  background: #28a745;
  color: white;
}

.reject-btn {
  background: #dc3545;
  color: white;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
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
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  width: 90%;
}

.modal-content h3 {
  margin-top: 0;
  margin-bottom: 20px;
  text-align: center;
}

.user-detail {
  margin-bottom: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.detail-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.detail-section h4 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #495057;
  border-bottom: 2px solid #007bff;
  padding-bottom: 5px;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
  align-items: flex-start;
}

.detail-row label {
  width: 100px;
  font-weight: 600;
  color: #495057;
  flex-shrink: 0;
}

.detail-row span {
  flex: 1;
  word-break: break-word;
}

.modal-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}

.close-btn, .cancel-btn, .confirm-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.close-btn, .cancel-btn {
  background: #6c757d;
  color: white;
}

.confirm-btn {
  background: #dc3545;
  color: white;
}

textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
}
</style>

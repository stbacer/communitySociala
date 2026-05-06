<template>
  <div class="admin-layout">
    <nav class="sidebar">
      <ul>
        <li><router-link to="/overview" data-label="系统统计">系统统计</router-link></li>
        <li><router-link to="/users" data-label="用户管理">用户管理</router-link></li>
        <li><router-link to="/categories" data-label="版块管理">版块管理</router-link></li>
        <li><router-link to="/sensitive-words" data-label="敏感词管理">敏感词管理</router-link></li>
        <!-- <li><router-link to="/config" data-label="系统配置">系统配置</router-link></li> -->
        <li><router-link to="/logs" data-label="操作日志">操作日志</router-link></li>

        <!-- <li><router-link to="/admins" data-label="管理员管理">管理员管理</router-link></li> -->
<!--        <li><router-link to="/monitor" data-label="系统监控">系统监控</router-link></li>-->
      </ul>
    </nav>

    <main class="content">
      <!-- 顶部导航栏 -->
      <div class="top-bar">
        <div class="user-info" @click="toggleDropdown">
          <img :src="userAvatar" alt="头像" class="avatar" @click.stop="openAvatarUpload" />
          <span class="username">{{ currentUser?.nickname || currentUser?.username || '管理员' }}</span>
          <span class="dropdown-arrow">▼</span>
        </div>
        <!-- 下拉菜单 -->
        <div v-if="showDropdown" class="user-dropdown">
          <button @click="handleLogout" class="dropdown-item logout-btn">退出登录</button>
        </div>
      </div>

      <!-- 编辑个人信息弹窗 -->
      <div v-if="showProfileEdit" class="modal-overlay" @click="showProfileEdit = false">
        <div class="modal-content" @click.stop>
          <h3>编辑个人信息</h3>
          <form @submit.prevent="updateProfile">
            <div class="form-group">
              <label>昵称</label>
              <input v-model="profileForm.nickname" type="text" placeholder="请输入昵称" />
            </div>
            <div class="form-group">
              <label>性别</label>
              <select v-model="profileForm.gender">
                <option value="0">未知</option>
                <option value="1">男</option>
                <option value="2">女</option>
              </select>
            </div>
            <div class="form-group">
              <label>手机号</label>
              <input v-model="profileForm.phone" type="text" placeholder="请输入手机号" />
            </div>
            <div class="form-group">
              <label>个性签名</label>
              <textarea v-model="profileForm.signature" rows="3" placeholder="请输入个性签名"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" @click="showProfileEdit = false" class="cancel-btn">取消</button>
              <button type="submit" class="submit-btn" :disabled="saving">保存</button>
            </div>
          </form>
        </div>
      </div>

      <!-- 更换头像弹窗 -->
      <div v-if="showAvatarUpload" class="modal-overlay" @click="closeAvatarUpload">
        <div class="modal-content avatar-modal" @click.stop>
          <h3>更换头像</h3>
          <div class="avatar-upload-area">
            <div class="current-avatar">
              <p>当前头像：</p>
              <img :src="userAvatar" alt="当前头像" class="avatar-preview" />
            </div>
            <div class="upload-section">
              <p>新头像：</p>
              <div class="upload-options">
                <label class="upload-btn">
                  <input type="file" accept="image/*" @change="handleFileSelect" style="display: none;" />
                  <span>📁 选择图片</span>
                </label>
                <span class="or-text">或</span>
                <button @click="useDefaultAvatar" class="default-btn">使用默认头像</button>
              </div>
              <!-- 图片预览 -->
              <div v-if="selectedFile" class="preview-container">
                <img :src="previewUrl" alt="预览" class="preview-image" />
                <button @click="uploadAvatar" class="confirm-upload-btn" :disabled="uploading">
                  {{ uploading ? '上传中...' : '确认上传' }}
                </button>
              </div>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" @click="closeAvatarUpload" class="cancel-btn">关闭</button>
          </div>
        </div>
      </div>

      <div class="page-container">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script>
import apiClient from '@/api'

export default {
  name: 'AdminLayout',
  data() {
    return {
      currentUser: null,
      showProfileEdit: false,
      showAvatarUpload: false,
      showDropdown: false,
      saving: false,
      uploading: false,
      selectedFile: null,
      previewUrl: '',
      profileForm: {
        nickname: '',
        gender: 0,
        phone: '',
        signature: ''
      }
    }
  },
  computed: {
    userAvatar() {
      if (this.currentUser?.avatarUrl) {
        // 如果是完整 URL，直接返回
        if (this.currentUser.avatarUrl.startsWith('http')) {
          return this.currentUser.avatarUrl
        }
        // 否则拼接后端地址
        return `http://localhost:8080${this.currentUser.avatarUrl}`
      }
      // 默认头像
      return 'http://localhost:8080/image/avatar/default.png'
    }
  },
  async mounted() {
    await this.fetchCurrentUser()
    // 添加点击事件监听，点击其他地方关闭下拉菜单
    document.addEventListener('click', this.closeDropdownOutside)
  },
  beforeDestroy() {
    // 移除事件监听
    document.removeEventListener('click', this.closeDropdownOutside)
  },
  methods: {
    // 打开更换头像弹窗
    openAvatarUpload() {
      this.showAvatarUpload = true
      this.selectedFile = null
      this.previewUrl = ''
    },
    
    // 关闭更换头像弹窗
    closeAvatarUpload() {
      this.showAvatarUpload = false
      this.selectedFile = null
      this.previewUrl = ''
    },
    
    // 处理文件选择
    handleFileSelect(event) {
      const file = event.target.files[0]
      if (!file) return
      
      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        alert('请选择图片文件')
        return
      }
      
      // 验证文件大小（5MB）
      if (file.size > 5 * 1024 * 1024) {
        alert('图片大小不能超过 5MB')
        return
      }
      
      this.selectedFile = file
      
      // 生成预览
      const reader = new FileReader()
      reader.onload = (e) => {
        this.previewUrl = e.target.result
      }
      reader.readAsDataURL(file)
    },
    
    // 使用默认头像
    useDefaultAvatar() {
      if (confirm('确定要使用默认头像吗？')) {
        this.uploadDefaultAvatar()
      }
    },
    
    // 上传默认头像
    async uploadDefaultAvatar() {
      try {
        this.uploading = true
        // 直接使用默认头像 URL，调用更新接口
        const defaultAvatarUrl = '/image/avatar/default.png'
        
        const res = await apiClient.put('/sadmin/system/current-user', {
          avatarUrl: defaultAvatarUrl
        })
        
        if (res.data.code === 200) {
          alert('头像设置成功')
          this.currentUser.avatarUrl = defaultAvatarUrl
          sessionStorage.setItem('userInfo', JSON.stringify(this.currentUser))
          this.closeAvatarUpload()
        } else {
          alert(res.data.message || '设置失败')
        }
      } catch (err) {
        console.error('使用默认头像失败:', err)
        alert(err.response?.data?.message || '操作失败')
      } finally {
        this.uploading = false
      }
    },
    
    // 上传头像
    async uploadAvatar() {
      if (!this.selectedFile) {
        alert('请先选择图片')
        return
      }
      
      try {
        this.uploading = true
        
        // 使用 FormData 上传文件
        const formData = new FormData()
        formData.append('file', this.selectedFile)
        
        // 使用居民端用户上传接口（管理员也是用户）
        const res = await apiClient.post('/resident/user/upload-avatar', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        if (res.data.code === 200) {
          alert('头像上传成功')
          this.currentUser.avatarUrl = res.data.data
          sessionStorage.setItem('userInfo', JSON.stringify(this.currentUser))
          this.closeAvatarUpload()
        } else {
          alert(res.data.message || '上传失败')
        }
      } catch (err) {
        console.error('上传头像失败:', err)
        alert(err.response?.data?.message || '上传失败')
      } finally {
        this.uploading = false
      }
    },
    async fetchCurrentUser() {
      try {
        const res = await apiClient.get('/sadmin/system/current-user')
        if (res.data.code === 200) {
          this.currentUser = res.data.data
          // 填充表单数据
          this.profileForm = {
            nickname: this.currentUser.nickname || '',
            gender: this.currentUser.gender || 0,
            phone: this.currentUser.phone || '',
            signature: this.currentUser.signature || ''
          }
        }
      } catch (err) {
        console.error('获取当前用户信息失败:', err)
      }
    },
    async updateProfile() {
      try {
        this.saving = true
        const res = await apiClient.put('/sadmin/system/current-user', this.profileForm)
        if (res.data.code === 200) {
          alert('更新成功')
          this.currentUser = res.data.data
          this.showProfileEdit = false
          // 更新 sessionStorage 中的用户信息
          sessionStorage.setItem('userInfo', JSON.stringify(this.currentUser))
        } else {
          alert(res.data.message || '更新失败')
        }
      } catch (err) {
        console.error('更新用户信息失败:', err)
        alert(err.response?.data?.message || '更新失败')
      } finally {
        this.saving = false
      }
    },
    async handleLogout() {
      if (!confirm('确定要退出登录吗？')) {
        return
      }
      try {
        await apiClient.post('/sadmin/system/logout')
      } catch (err) {
        console.error('退出登录失败:', err)
      } finally {
        // 清除本地存储
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('userInfo')
        // 跳转到登录页
        this.$router.push({ name: 'Login' })
      }
    },
    
    // 切换下拉菜单显示
    toggleDropdown() {
      this.showDropdown = !this.showDropdown
    },
    
    // 点击其他地方关闭下拉菜单
    closeDropdownOutside(event) {
      const userInfo = event.target.closest('.user-info')
      const dropdown = event.target.closest('.user-dropdown')
      if (!userInfo && !dropdown) {
        this.showDropdown = false
      }
    }
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 220px;
  background: #2d3a4b;
  color: white;
  padding: 1rem;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  overflow-y: auto;
  flex-shrink: 0;
}

.sidebar ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar li {
  margin: 0.75rem 0;
}

.sidebar a {
  color: white;
  text-decoration: none;
  display: block;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.sidebar a.router-link-active,
.sidebar a:hover {
  background: rgba(255, 255, 255, 0.1);
}

.content {
  flex: 1;
  margin-left: 220px;
  padding: 1rem 2rem;
  background: var(--color-background-soft);
  overflow-y: auto;
  min-height: 100vh;
}

.page-container {
  /* full‑width container with internal padding only */
  width: 100%;
  padding: 0 1rem;
}

@media (max-width: 768px) {
  .sidebar {
    width: 60px;
    padding: 0.5rem;
  }
  .sidebar li {
    margin: 0.5rem 0;
  }
  .sidebar a {
    font-size: 0;
  }
  .sidebar a::after {
    content: attr(data-label);
    font-size: 0.75rem;
    display: block;
    text-align: center;
    margin-top: 4px;
  }
  .content {
    margin-left: 60px;
    padding: 1rem;
  }
}

/* 顶部导航栏样式 */
.top-bar {
  background: white;
  padding: 0.75rem 2rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 1rem;
  border-radius: 4px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 4px;
  transition: all 0.3s ease;
  position: relative;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  object-fit: cover;
  border: 2px solid #e0e0e0;
  transition: border-color 0.3s ease;
}

.avatar:hover {
  border-color: #4CAF50;
}

.username {
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.dropdown-arrow {
  font-size: 10px;
  color: #666;
  transition: transform 0.3s ease;
}

.user-info:hover .dropdown-arrow {
  transform: rotate(180deg);
}

/* 下拉菜单样式 */
.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: white;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 120px;
  z-index: 101;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-item {
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.3s ease;
  border-radius: 4px;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.dropdown-item.logout-btn {
  color: #f44336;
}

.dropdown-item.logout-btn:hover {
  background-color: #ffebee;
}

/* 弹窗样式 */
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
  padding: 24px;
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
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #555;
  font-weight: 500;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.cancel-btn,
.submit-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.cancel-btn {
  background-color: #e0e0e0;
  color: #333;
}

.cancel-btn:hover {
  background-color: #d0d0d0;
}

.submit-btn {
  background-color: #4CAF50;
  color: white;
}

.submit-btn:hover {
  background-color: #45a049;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 头像上传弹窗特殊样式 */
.avatar-modal {
  max-width: 600px;
}

.avatar-upload-area {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
}

.current-avatar,
.upload-section {
  flex: 1;
  text-align: center;
}

.current-avatar p,
.upload-section p {
  margin-bottom: 12px;
  color: #666;
  font-weight: 500;
}

.avatar-preview {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.upload-options {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.upload-btn {
  display: inline-block;
  padding: 10px 20px;
  background-color: #4CAF50;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-btn:hover {
  background-color: #45a049;
}

.or-text {
  color: #999;
  font-size: 14px;
}

.default-btn {
  padding: 10px 20px;
  background-color: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.default-btn:hover {
  background-color: #1976D2;
}

.preview-container {
  margin-top: 16px;
  text-align: center;
}

.preview-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  border: 2px solid #e0e0e0;
  margin-bottom: 12px;
}

.confirm-upload-btn {
  padding: 10px 30px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.confirm-upload-btn:hover {
  background-color: #45a049;
}

.confirm-upload-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .avatar-upload-area {
    flex-direction: column;
  }
}
</style>
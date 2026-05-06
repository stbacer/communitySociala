<template>
  <div class="login-container">
    <div class="login-wrapper">
      <!-- 左侧 Logo 区域 -->
      <div class="login-left">
        <div class="logo-content">
          <img src="https://lyjsqsj.oss-cn-chengdu.aliyuncs.com/logo.png" alt="Logo" class="logo-image" />
          <h1 class="app-title">社区社交平台</h1>
          <p class="app-subtitle">后台管理系统</p>
        </div>
      </div>
      
      <!-- 右侧登录表单区域 -->
      <div class="login-right">
        <div class="login-form-container">
          <h2 class="form-title">超级管理员登录</h2>
          <form @submit.prevent="handleLogin" class="login-form">
            <div class="form-group">
              <label class="form-label">手机号</label>
              <input 
                v-model="phone" 
                type="text" 
                placeholder="请输入手机号" 
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">密码</label>
              <input 
                v-model="password" 
                type="password" 
                placeholder="请输入密码" 
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">验证码</label>
              <div class="captcha-wrapper">
                <input 
                  v-model="captchaCode" 
                  type="text" 
                  placeholder="请输入验证码" 
                  maxlength="4" 
                  class="form-input captcha-input"
                />
                <img 
                  v-if="captchaImage" 
                  :src="captchaImage" 
                  alt="验证码" 
                  class="captcha-image" 
                  @click="refreshCaptcha" 
                />
              </div>
            </div>
            <button type="submit" class="login-btn">登录</button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import apiClient from '@/api'
export default {
  name: 'Login',
  data() {
    return {
      phone: '',
      password: '',
      captchaCode: '',
      captchaId: '',
      captchaImage: ''
    }
  },
  mounted() {
    this.refreshCaptcha()
  },
  methods: {
    async refreshCaptcha() {
      try {
        const res = await apiClient.get('/sadmin/auth/captcha/image')
        if (res.data.code === 200) {
          this.captchaId = res.data.data.captchaId
          this.captchaImage = res.data.data.captchaImage
        }
      } catch (err) {
        console.error('获取验证码失败:', err)
      }
    },
    async handleLogin() {
      // 验证验证码
      if (!this.captchaCode || this.captchaCode.length !== 4) {
        alert('请输入 4 位验证码')
        return
      }
      
      try {
        const res = await apiClient.post('/sadmin/auth/login', {
          phone: this.phone,
          password: this.password,
          captchaId: this.captchaId,
          captchaCode: this.captchaCode
        })
        
        // 检查登录响应
        if (res.data.code === 200) {
          const token = res.data.data.token
          const userInfo = res.data.data.userInfo
          
          // 存储 token 和用户信息（使用 localStorage 与 API 拦截器保持一致）
          localStorage.setItem('token', token)
          localStorage.setItem('userInfo', JSON.stringify(userInfo))
          
          // 设置默认请求头
          apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
          
          // 检查用户角色权限
          if (userInfo && userInfo.userRole === 3) {
            // 超级管理员权限
            this.$router.push({ name: 'UserManagement' })
          } else {
            // 非超级管理员，拒绝登录
            alert('权限不足：仅超级管理员可登录后端管理系统')
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            this.refreshCaptcha()
            this.captchaCode = ''
          }
        } else {
          alert(res.data.message || '登录失败')
          this.refreshCaptcha()
          this.captchaCode = ''
        }
      } catch (err) {
        console.error('登录错误:', err)
        if (err.response && err.response.data) {
          alert(err.response.data.message || '登录失败')
        } else {
          alert('网络连接失败，请检查后端服务是否启动')
        }
        this.refreshCaptcha()
        this.captchaCode = ''
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  overflow: hidden;
}

.login-wrapper {
  width: 100%;
  max-width: 900px;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  min-height: 500px;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

.logo-content {
  text-align: center;
  color: white;
}

.logo-image {
  width: 120px;
  height: 120px;
  object-fit: contain;
  margin-bottom: 30px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.app-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 10px 0;
  letter-spacing: 2px;
}

.app-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
  font-weight: 300;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: white;
}

.login-form-container {
  width: 100%;
  max-width: 320px;
}

.form-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #555;
}

.form-input {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  outline: none;
}

.form-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.captcha-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 100px;
  height: 50px;
  cursor: pointer;
  border: 1px solid #ddd;
  border-radius: 8px;
  transition: transform 0.2s ease;
}

.captcha-image:hover {
  transform: scale(1.05);
}

.login-btn {
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 10px;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(102, 126, 234, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    max-width: 400px;
  }
  
  .login-left {
    padding: 30px;
  }
  
  .logo-image {
    width: 80px;
    height: 80px;
  }
  
  .app-title {
    font-size: 22px;
  }
  
  .app-subtitle {
    font-size: 14px;
  }
  
  .login-right {
    padding: 30px;
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: 10px;
  }
  
  .login-right {
    padding: 20px;
  }
  
  .form-title {
    font-size: 20px;
  }
}
</style>
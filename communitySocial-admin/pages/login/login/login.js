// pages/login/login.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    phone: '',
    password: '',
    captchaCode: '',
    captchaInputValue: '', // 用于输入框的双向绑定
    captchaId: '',
    captchaImage: '',
    loading: false
  },

  onLoad() {
    // 检查是否已登录
    if (auth.isLoggedIn()) {
      wx.switchTab({
        url: '/pages/index/index/index'
      })
      return
    }
    
    // 加载验证码
    this.loadCaptcha()
  },

  // 输入手机号
  onPhoneInput(e) {
    this.setData({
      phone: e.detail.value
    })
  },

  // 输入密码
  onPasswordInput(e) {
    this.setData({
      password: e.detail.value
    })
  },

  // 输入验证码
  onCaptchaInput(e) {
    let value = e.detail.value
    
    // 只保留字母和数字，并转换为大写
    value = value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase()
    
    // 限制最多4位
    if (value.length > 4) {
      value = value.substring(0, 4)
    }
    
    this.setData({
      captchaCode: value,
      captchaInputValue: value // 同步更新输入框显示
    })
  },

  // 加载验证码
  loadCaptcha() {
    api.getCaptchaImage().then(res => {
      if (res && res.data) {
        this.setData({
          captchaId: res.data.captchaId,
          captchaImage: res.data.captchaImage
        })
      }
    }).catch(error => {
      // 错误已在 request 拦截器中处理
    })
  },

  // 刷新验证码
  refreshCaptcha() {
    this.loadCaptcha()
    this.setData({
      captchaCode: '',
      captchaInputValue: '' // 清空输入框
    })
  },

  // 登录按钮点击
  onLogin() {
    const { phone, password, captchaCode, captchaId } = this.data
      
    // 表单验证
    if (!phone || !phone.trim()) {
      wx.showToast({
        title: '请输入手机号',
        icon: 'none'
      })
      return
    }
      
    // 验证手机号格式
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(phone.trim())) {
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      })
      return
    }
      
    if (!password || !password.trim()) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      })
      return
    }
    
    // 验证验证码 - 先 trim 再检查
    const trimmedCaptcha = captchaCode ? String(captchaCode).trim().toUpperCase() : ''
    
    if (trimmedCaptcha.length !== 4) {
      wx.showToast({
        title: '请输入 4 位验证码',
        icon: 'none'
      })
      // 刷新验证码
      this.refreshCaptcha()
      return
    }
      
    // 调用登录 API
    this.setData({ loading: true })
      
    api.adminLogin({
      phone: phone.trim(),
      password: password.trim(),
      captchaId: captchaId,
      captchaCode: trimmedCaptcha
    }).then(res => {
      // 保存 token 和手机号
      wx.setStorageSync('admin_token', res.data)
      wx.setStorageSync('admin_phone', phone.trim())
      
      wx.showToast({
        title: '登录成功',
        icon: 'success'
      })
      
      // 获取当前用户信息，检查实名认证状态
      return api.getCurrentUser()
    }).then(userInfo => {
      // 检查 authStatus：0-未认证，1-审核中，2-已认证，3-认证失败
      const authStatus = userInfo.data?.authStatus || 0
      
      if (authStatus === 2) {
        // 已实名认证，直接进入小程序
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/index/index/index'
          })
        }, 1500)
      } else if (authStatus === 1) {
        // 审核中，提示等待审核
        wx.showModal({
          title: '等待审核',
          content: '您的实名认证正在审核中，请耐心等待',
          showCancel: false,
          success: () => {
            wx.navigateBack()
          }
        })
      } else if (authStatus === 3) {
        // 认证失败，提示重新认证
        wx.showModal({
          title: '认证失败',
          content: '您的实名认证未通过审核，请重新提交认证申请',
          showCancel: false,
          confirmText: '去认证',
          success: (res) => {
            if (res.confirm) {
              wx.navigateTo({
                url: '/pages/userAuth/authSubmit/authSubmit'
              })
            }
          }
        })
      } else {
        // 未认证（authStatus=0），跳转到实名认证页
        setTimeout(() => {
          wx.navigateTo({
            url: '/pages/userAuth/authSubmit/authSubmit'
          })
        }, 1500)
      }
    }).catch(err => {
      wx.showToast({
        title: err.message || '登录失败',
        icon: 'none'
      })
      // 登录失败刷新验证码
      this.refreshCaptcha()
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  // 忘记密码（暂未实现）
  onForgetPassword() {
    wx.showToast({
      title: '请联系系统管理员重置密码',
      icon: 'none'
    })
  },

  // 跳转到注册页
  goToRegister() {
    wx.navigateTo({
      url: '/pages/login/regist/regist'
    })
  }
})
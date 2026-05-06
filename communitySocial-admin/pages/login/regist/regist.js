const api = require('../../../utils/api.js')

Page({
  data: {
    formData: {
      password: '',
      confirmPassword: '',
      nickname: '',
      phone: ''
    },
    captchaCode: '',
    captchaId: '',
    captchaImage: '',
    loading: false
  },

  onLoad() {
    // 加载验证码
    this.loadCaptcha()
  },

  // 输入框变化处理
  onInput(e) {
    const { field } = e.currentTarget.dataset
    const value = e.detail.value
    this.setData({
      [`formData.${field}`]: value
    })
  },

  // 输入验证码
  onCaptchaInput(e) {
    this.setData({
      captchaCode: e.detail.value.toUpperCase()
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
      captchaCode: ''
    })
  },

  // 表单验证
  validateForm() {
    const { formData, captchaCode } = this.data

    // 手机号验证
    if (!formData.phone || !/^1[3-9]\d{9}$/.test(formData.phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return false
    }

    // 密码验证
    if (!formData.password || formData.password.length < 6) {
      wx.showToast({ title: '密码长度不少于 6 位', icon: 'none' })
      return false
    }

    // 确认密码验证
    if (formData.password !== formData.confirmPassword) {
      wx.showToast({ title: '两次输入的密码不一致', icon: 'none' })
      return false
    }

    // 昵称验证
    if (!formData.nickname) {
      wx.showToast({ title: '请输入昵称', icon: 'none' })
      return false
    }

    // 验证码验证
    if (!captchaCode || captchaCode.length !== 4) {
      wx.showToast({ title: '请输入 4 位验证码', icon: 'none' })
      return false
    }

    return true
  },

  // 提交注册
  async handleSubmit() {
    if (!this.validateForm()) {
      return
    }

    this.setData({ loading: true })

    try {
      const { formData, captchaId, captchaCode } = this.data
      
      // 调用注册 API（authStatus=0 表示未认证，等待后续实名认证）
      await api.registerAdmin({
        password: formData.password,
        nickname: formData.nickname,
        phone: formData.phone,
        userRole: 2,
        status: 1,
        authStatus: 0,  // 未认证状态
        captchaId: captchaId,
        captchaCode: captchaCode
      })

      wx.showModal({
        title: '注册成功',
        content: '您的账号已创建成功，请登录并完成实名认证',
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
    } catch (err) {
      wx.showToast({ 
        title: err.response?.data?.message || '注册失败，请稍后重试', 
        icon: 'none',
        duration: 2000
      })
      // 注册失败刷新验证码
      this.refreshCaptcha()
    } finally {
      this.setData({ loading: false })
    }
  },

  // 返回登录页
  goBack() {
    wx.navigateBack()
  }
})

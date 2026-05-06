// pages/login/regist/regist.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    phoneNumber: '',
    password: '',
    confirmPassword: '',
    nickname: '',
    captchaCode: '',
    captchaId: '',
    captchaImage: '',
    agreed: false,
    isRegistering: false,
    canSubmit: false
  },

  onLoad() {
    // 检查是否已登录
    if (auth.isLoggedIn()) {
      wx.switchTab({
        url: '/pages/index/index'
      })
      return
    }
    
    // 加载验证码
    this.loadCaptcha()
  },

  // 手机号输入
  onPhoneInput(e) {
    const value = e.detail.value.trim()
    this.setData({
      phoneNumber: value
    }, () => {
      this.checkCanSubmit()
    })
  },

  // 密码输入
  onPasswordInput(e) {
    const value = e.detail.value
    this.setData({
      password: value
    }, () => {
      this.checkCanSubmit()
    })
  },

  // 确认密码输入
  onConfirmPasswordInput(e) {
    const value = e.detail.value
    this.setData({
      confirmPassword: value
    }, () => {
      this.checkCanSubmit()
    })
  },

  // 昵称输入（必填）
  onNicknameInput(e) {
    const value = e.detail.value.trim()
    this.setData({
      nickname: value
    }, () => {
      this.checkCanSubmit()
    })
  },

  // 协议勾选
  onAgreeChange(e) {
    this.setData({
      agreed: e.detail.value.length > 0
    }, () => {
      this.checkCanSubmit()
    })
  },

  // 检查是否可以提交
  checkCanSubmit() {
    const { phoneNumber, password, confirmPassword, nickname, agreed } = this.data
    
    // 验证手机号格式
    const phoneRegex = /^1[3-9]\d{9}$/
    const isPhoneValid = phoneRegex.test(phoneNumber)
    
    // 验证密码长度（至少 6 位）
    const isPasswordValid = password.length >= 6
    
    // 验证两次密码是否一致
    const isPasswordMatch = password && confirmPassword && password === confirmPassword
    
    // 验证昵称不为空（后端要求必填）
    const isNicknameValid = nickname && nickname.trim().length > 0
    
    // 所有条件都满足才可以提交
    const canSubmit = isPhoneValid && isPasswordValid && isPasswordMatch && isNicknameValid && agreed
    
    this.setData({
      canSubmit
    })
  },

  // 验证码输入
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
    })
  },

  // 刷新验证码
  refreshCaptcha() {
    this.loadCaptcha()
    this.setData({
      captchaCode: ''
    })
  },

  // 查看协议
  onViewAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '请阅读并同意用户协议和隐私政策。\n\n我们承诺保护您的个人信息安全，仅用于社区服务相关用途。',
      showCancel: false,
      confirmText: '我知道了'
    })
  },

  // 返回登录页
  onBackToLogin() {
    wx.navigateBack()
  },

  // 注册按钮点击
  onRegisterTap() {
    if (this.data.isRegistering) return

    // 验证输入
    if (!this.data.phoneNumber) {
      wx.showToast({
        title: '请输入手机号',
        icon: 'none'
      })
      return
    }

    // 验证手机号格式
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(this.data.phoneNumber)) {
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      })
      return
    }

    if (!this.data.password) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      })
      return
    }

    // 验证密码长度
    if (this.data.password.length < 6) {
      wx.showToast({
        title: '密码至少 6 位',
        icon: 'none'
      })
      return
    }

    if (!this.data.confirmPassword) {
      wx.showToast({
        title: '请再次输入密码',
        icon: 'none'
      })
      return
    }

    // 验证两次密码是否一致
    if (this.data.password !== this.data.confirmPassword) {
      wx.showToast({
        title: '两次密码不一致',
        icon: 'none'
      })
      return
    }

    // 验证昵称
    if (!this.data.nickname || !this.data.nickname.trim()) {
      wx.showToast({
        title: '请输入昵称',
        icon: 'none'
      })
      return
    }

    // 验证验证码
    if (!this.data.captchaCode || this.data.captchaCode.length !== 4) {
      wx.showToast({
        title: '请输入 4 位验证码',
        icon: 'none'
      })
      return
    }

    // 检查协议勾选
    if (!this.data.agreed) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      })
      return
    }

    // 检查网络状态
    wx.getNetworkType({
      success: (res) => {
        if (res.networkType === 'none') {
          wx.showToast({
            title: '当前无网络连接',
            icon: 'none'
          })
          return
        }
        
        // 执行注册
        this.performRegister()
      },
      fail: () => {
        wx.showToast({
          title: '无法获取网络状态',
          icon: 'none'
        })
      }
    })
  },

  // 执行注册
  async performRegister() {
    this.setData({ isRegistering: true })

    try {
      // 准备注册数据 - 注意：后端期望的字段名是 phone 而不是 phoneNumber
      const registerData = {
        phone: this.data.phoneNumber,  // 使用 phone 字段名
        password: this.data.password,
        nickname: this.data.nickname.trim(),  // 去除首尾空格
        captchaId: this.data.captchaId,
        captchaCode: this.data.captchaCode
      }

      // 调用后端注册接口
      const result = await api.register(registerData)

      if (result && result.data) {
        // 注册成功，显示成功提示
        wx.showToast({
          title: '注册成功',
          icon: 'success',
          duration: 1500
        })

        // 延迟跳转到登录页
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      }
    } catch (error) {
      let errorMsg = '注册失败'
      
      // 根据错误类型提供具体提示
      if (error.code === 400) {
        errorMsg = error.message || '注册信息有误'
      } else if (error.code === 409) {
        // 409 Conflict 通常表示资源已存在
        errorMsg = '该手机号已被注册'
      } else if (error.message) {
        errorMsg = error.message
      } else if (typeof error === 'object' && error.errMsg) {
        errorMsg = error.errMsg.includes('network') ? '网络连接失败' : '注册失败，请重试'
      }
      
      wx.showToast({
        title: errorMsg,
        icon: 'none',
        duration: 3000
      })
    } finally {
      this.setData({ isRegistering: false })
    }
  },

  onShow() {
    // 页面显示时再次检查登录状态
    if (auth.isLoggedIn()) {
      wx.switchTab({
        url: '/pages/index/index'
      })
    }
  },

  onUnload() {
    // 清理工作
  }
})

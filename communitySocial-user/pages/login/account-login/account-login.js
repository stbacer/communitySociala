// pages/login/account-login/account-login.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    phoneNumber: '',
    password: '',
    captchaCode: '',
    captchaId: '',
    captchaImage: '',
    isLoggingIn: false,
    agreed: true
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
    this.setData({
      phoneNumber: e.detail.value
    })
  },

  // 密码输入
  onPasswordInput(e) {
    this.setData({
      password: e.detail.value
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

  // 协议勾选
  onAgreeChange(e) {
    this.setData({
      agreed: e.detail.value.length > 0
    })
  },

  // 忘记密码
  onForgetPassword() {
    wx.navigateTo({
      url: '/pages/login/forgot-password/forgot-password'
    })
  },

  // 跳转到注册页面
  onSwitchToRegist() {
    wx.navigateTo({
      url: '/pages/login/regist/regist'
    })
  },

  // 微信登录
  onWechatLogin() {
    wx.redirectTo({
      url: '/pages/login/login'
    })
  },

  // 查看协议
  onViewAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '请阅读并同意用户协议和隐私政策',
      showCancel: false
    })
  },

  // 登录按钮点击
  onLoginTap() {
    if (this.data.isLoggingIn) return

    // 验证输入
    if (!this.data.phoneNumber) {
      wx.showToast({
        title: '请输入手机号',
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

    // 验证手机号格式
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(this.data.phoneNumber)) {
      wx.showToast({
        title: '手机号格式不正确',
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
        
        // 执行登录
        this.performLogin()
      },
      fail: () => {
        wx.showToast({
          title: '无法获取网络状态',
          icon: 'none'
        })
      }
    })
  },

  // 执行登录
  async performLogin() {
    this.setData({ isLoggingIn: true })

    try {
      // 调用后端账号密码登录接口
      const result = await api.accountLogin({
        phoneNumber: this.data.phoneNumber,
        password: this.data.password,
        captchaId: this.data.captchaId,
        captchaCode: this.data.captchaCode
      })

      if (result && result.data) {
        const token = result.data.token || result.data.accessToken || result.data.jwtToken
        const userInfo = result.data.userInfo || result.data.user || result.data.userData

        if (!token) {
          throw new Error('服务器未返回有效的 token')
        }

        if (!userInfo) {
          throw new Error('服务器未返回用户信息')
        }

        // 保存登录信息
        wx.setStorageSync('user_token', token)
        auth.saveUserInfo(userInfo)

        wx.showToast({
          title: '登录成功',
          icon: 'success',
          duration: 1500
        })

        // 确保数据保存完成后再跳转
        setTimeout(() => {
          if (auth.isLoggedIn()) {
            wx.switchTab({
              url: '/pages/index/index',
              success: () => {

              },
              fail: (error) => {
                wx.redirectTo({
                  url: '/pages/index/index'
                })
              }
            })
          } else {
            wx.showToast({
              title: '登录状态异常，请重试',
              icon: 'none'
            })
          }
        }, 1500)
      }
    } catch (error) {
      let errorMsg = '登录失败'
      
      if (error.code === 401) {
        errorMsg = '账号或密码错误'
      } else if (error.message) {
        errorMsg = error.message
      } else if (typeof error === 'object' && error.errMsg) {
        errorMsg = error.errMsg.includes('network') ? '网络连接失败' : '登录失败，请重试'
      }
      
      wx.showToast({
        title: errorMsg,
        icon: 'none',
        duration: 3000
      })
    } finally {
      this.setData({ isLoggingIn: false })
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

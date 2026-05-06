// pages/login/forgot-password/forgot-password.js
const api = require('../../../utils/api.js')

Page({
  data: {
    phoneNumber: '',
    captchaCode: '',
    captchaId: '',
    captchaImage: '',
    canSubmit: false,
    isSubmitting: false
  },

  onLoad() {
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

  // 验证码输入
  onCaptchaInput(e) {
    const value = e.detail.value.trim()
    this.setData({
      captchaCode: value
    }, () => {
      this.checkCanSubmit()
    })
  },

  // 检查是否可以提交
  checkCanSubmit() {
    const { phoneNumber, captchaCode } = this.data
    
    // 验证手机号格式
    const phoneRegex = /^1[3-9]\d{9}$/
    const isPhoneValid = phoneRegex.test(phoneNumber)
    
    // 验证验证码不为空
    const isCaptchaValid = captchaCode && captchaCode.length === 4
    
    // 所有条件都满足才可以提交
    const canSubmit = isPhoneValid && isCaptchaValid
    
    this.setData({
      canSubmit
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
      wx.showToast({
        title: '加载验证码失败',
        icon: 'none'
      })
    })
  },

  // 刷新验证码
  refreshCaptcha() {
    this.loadCaptcha()
  },

  // 返回登录页
  onBackToLogin() {
    wx.navigateBack()
  },

  // 提交按钮点击
  onSubmitTap() {
    if (this.data.isSubmitting) return

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

    if (!this.data.captchaCode) {
      wx.showToast({
        title: '请输入验证码',
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
        
        // 执行重置密码
        this.performResetPassword()
      },
      fail: () => {
        wx.showToast({
          title: '无法获取网络状态',
          icon: 'none'
        })
      }
    })
  },

  // 执行重置密码
  async performResetPassword() {
    this.setData({ isSubmitting: true })

    try {
      // 调用后端重置密码接口
      const result = await api.resetPassword({
        phone: this.data.phoneNumber,
        captchaId: this.data.captchaId,
        captchaCode: this.data.captchaCode
      })

      if (result && result.code === 200) {
        // 重置成功，显示成功提示
        wx.showToast({
          title: '密码重置成功',
          icon: 'success',
          duration: 2000
        })

        // 延迟跳转到登录页
        setTimeout(() => {
          wx.navigateBack()
        }, 2000)
      }
    } catch (error) {
      let errorMsg = '重置失败'
      
      // 根据错误类型提供具体提示
      if (error.code === 400) {
        errorMsg = error.message || '验证码错误'
      } else if (error.code === 404) {
        errorMsg = '该手机号不存在'
      } else if (error.message) {
        errorMsg = error.message
      } else if (typeof error === 'object' && error.errMsg) {
        errorMsg = error.errMsg.includes('network') ? '网络连接失败' : '重置失败，请重试'
      }
      
      wx.showToast({
        title: errorMsg,
        icon: 'none',
        duration: 3000
      })
      
      // 刷新验证码
      this.loadCaptcha()
      this.setData({
        captchaCode: ''
      })
    } finally {
      this.setData({ isSubmitting: false })
    }
  },

  onShow() {
    // 页面显示时重新加载验证码
    if (!this.data.captchaImage) {
      this.loadCaptcha()
    }
  },

  onUnload() {
    // 清理工作
  }
})

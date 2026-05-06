// pages/login/login.js
const auth = require('../../utils/auth.js')
const api = require('../../utils/api.js')

Page({
  data: {
    canIUse: wx.canIUse('button.open-type.getUserInfo'),
    isLoggingIn: false
  },

  onLoad() {
    // 检查是否已经登录

    if (auth.isLoggedIn()) {
      // 检查认证状态
      const authStatus = auth.checkAuthStatus()
      
      if (authStatus === 1) {
        // 待审核状态：清除token并停留在登录页
        wx.removeStorageSync('user_token')
        wx.removeStorageSync('user_info')
      } else if (authStatus === 0 || authStatus === 3) {
        // 未认证或认证失败：跳转到实名认证页
        wx.redirectTo({
          url: '/pages/profile/auth/auth'
        })
      } else {
        // 已认证：跳转到首页
        wx.switchTab({
          url: '/pages/index/index',
          fail: (error) => {
            wx.redirectTo({
              url: '/pages/index/index'
            })
          }
        })
      }
    } else {

    }
  },

  // 微信授权登录
  onGetUserInfo(e) {
    if (this.data.isLoggingIn) return
    
    if (e.detail.userInfo) {
      this.setData({ isLoggingIn: true })
      
      // 获取微信登录凭证
      wx.login({
        success: (res) => {
          if (res.code) {
            this.wechatLogin(res.code, e.detail.userInfo)
          } else {
            wx.showToast({
              title: '登录失败，请重试',
              icon: 'none'
            })
            this.setData({ isLoggingIn: false })
          }
        },
        fail: () => {
          wx.showToast({
            title: '登录失败，请重试',
            icon: 'none'
          })
          this.setData({ isLoggingIn: false })
        }
      })
    } else {
      wx.showToast({
        title: '需要授权才能登录',
        icon: 'none'
      })
    }
  },

  // 调用后端微信登录接口
  async wechatLogin(code, userInfo) {
    try {
      // 不传递头像信息到后端，让后端只从数据库提取数据
      const loginData = {
        code: code,
        // 移除avatarUrl字段，不进行头像验证
        gender: userInfo.gender || 0,
        nickName: userInfo.nickName || '微信用户'
      }
      
      // 调用真实的微信登录API
      const result = await api.wechatLogin(loginData)

      if (result && result.data) {

        // 检查返回的数据结构
        const token = result.data.token || result.data.accessToken || result.data.jwtToken
        const serverUserInfo = result.data.userInfo || result.data.user || result.data.userData


        if (!token) {
          throw new Error('服务器未返回有效的token')
        }
        
        if (!serverUserInfo) {
          throw new Error('服务器未返回用户信息')
        }
        
        // 保存登录信息
        wx.setStorageSync('user_token', token)
        auth.saveUserInfo(serverUserInfo)
        
        wx.showToast({
          title: '登录成功',
          icon: 'success',
          duration: 1500
        })
        
        // 确保数据保存完成后再跳转
        setTimeout(() => {
          if (auth.isLoggedIn()) {
            // 检查认证状态
            const authStatus = auth.checkAuthStatus()
            
            if (authStatus === 1) {
              // 待审核状态：提示后返回登录页
              wx.showModal({
                title: '账号审核中',
                content: '您的实名认证正在审核中，请耐心等待管理员审核。审核通过后将自动解锁全部功能。',
                showCancel: false,
                confirmText: '我知道了',
                success: () => {
                  // 清除token和用户信息
                  wx.removeStorageSync('user_token')
                  wx.removeStorageSync('user_info')
                  // 停留在登录页（不需要跳转）
                }
              })
            } else if (authStatus === 0 || authStatus === 3) {
              // 未认证或认证失败：跳转到实名认证页
              wx.redirectTo({
                url: '/pages/profile/auth/auth',
                fail: (error) => {
                  wx.switchTab({
                    url: '/pages/index/index'
                  })
                }
              })
            } else {
              // 已认证，跳转到首页
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
            }
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
      
      // 根据错误类型提供具体提示
      if (error.code === 401) {
        errorMsg = '登录已过期，请重新登录'
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

  // 页面加载时检查登录状态
  onShow() {
    // 页面显示时再次检查登录状态

    if (auth.isLoggedIn()) {
      // 检查认证状态
      const authStatus = auth.checkAuthStatus()
      
      if (authStatus === 1) {
        // 待审核状态：清除token并停留在登录页
        wx.removeStorageSync('user_token')
        wx.removeStorageSync('user_info')
      } else if (authStatus === 0 || authStatus === 3) {
        // 未认证或认证失败：跳转到实名认证页
        wx.redirectTo({
          url: '/pages/profile/auth/auth'
        })
      } else {
        // 已认证：跳转到首页
        wx.switchTab({
          url: '/pages/index/index',
          fail: (error) => {
            wx.redirectTo({
              url: '/pages/index/index'
            })
          }
        })
      }
    }
  },
  
  // 处理登录按钮点击
  onLoginTap() {
    if (this.data.isLoggingIn) return
    
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
        
        // 触发微信授权
        this.triggerWechatAuth()
      },
      fail: () => {
        wx.showToast({
          title: '无法获取网络状态',
          icon: 'none'
        })
      }
    })
  },
  
  // 跳转到账号密码登录页面
  onAccountLoginTap() {
    wx.navigateTo({
      url: '/pages/login/account-login/account-login'
    })
  },
  
  // 触发微信授权
  triggerWechatAuth() {
    wx.getUserProfile({
      desc: '用于完善会员资料',
      success: (res) => {
        this.onGetUserInfo({
          detail: {
            userInfo: res.userInfo
          }
        })
      },
      fail: (err) => {

        wx.showToast({
          title: '需要授权才能登录',
          icon: 'none'
        })
      }
    })
  },

  // 游客模式（临时功能）
  guestLogin() {
    wx.showModal({
      title: '提示',
      content: '游客模式功能尚未开放，建议使用微信授权登录',
      showCancel: false
    })
  },
  
  // 处理页面卸载
  onUnload() {
    // 清理可能的定时器
    if (this.loginTimer) {
      clearTimeout(this.loginTimer)
    }
  }
})
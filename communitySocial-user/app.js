// app.js
const auth = require('./utils/auth.js')
const wsClient = require('./utils/websocket.js')

App({
  async onLaunch() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 初始化应用
    await this.initApp()
  },
  
  // 应用初始化
  async initApp() {
    // 检查登录状态
    this.checkLoginStatus()
      
    // 获取系统信息
    await this.getSystemInfo()
      
    // 设置全局错误处理
    this.setupGlobalErrorHandling()
      
    // 启动定时检查 token 有效性
    this.startTokenValidation()
      
    // 初始化 WebSocket 连接（如果已登录）
    if (auth.isLoggedIn()) {
      const userInfo = wx.getStorageSync('user_info')
      if (userInfo && userInfo.userId) {
        this.initWebSocket(userInfo.userId)
      }
    }
  },
    
  // 初始化 WebSocket 连接
  initWebSocket(userId) {

    wsClient.connect(userId)
      .then(() => {

        // 注册全局消息处理器
        wsClient.onMessage((message) => {

          // 解析消息，后端推送的消息格式为：{type, title, data}
          let messageType = message.type
          let messageData = message.data
          
          // 如果是新消息通知，触发全局事件
          if (messageType === 'new_message' || message.type === 'new_message') {

            // 发布全局事件，通知消息页面更新
            this.emit('new_message', messageData || message)
          }
          
          // 心跳响应处理
          if (messageType === 'pong') {

          }
        })
      })
      .catch(err => {
      })
  },

  // 检查登录状态
  checkLoginStatus() {
    const currentPage = getCurrentPages()[getCurrentPages().length - 1]
    const currentRoute = currentPage ? currentPage.route : ''
    
    // 如果已经在登录页或实名认证页，不需要跳转
    if (currentRoute === 'pages/login/login' || currentRoute === 'pages/profile/auth/auth') {
      return
    }
    
    if (!auth.isLoggedIn() || !auth.isTokenValid()) {

      // 未登录，跳转到登录页
      wx.redirectTo({
        url: '/pages/login/login'
      })
    } else {
      // 已登录，检查认证状态
      const authStatus = auth.checkAuthStatus()
      
      if (authStatus === 1) {
        // 待审核状态：清除token并跳转到登录页
        wx.removeStorageSync('user_token')
        wx.removeStorageSync('user_info')
        
        wx.redirectTo({
          url: '/pages/login/login',
          fail: (err) => {
          }
        })
      } else if (authStatus === 0 || authStatus === 3) {
        // 未认证或认证失败：跳转到实名认证页
        wx.redirectTo({
          url: '/pages/profile/auth/auth',
          fail: (err) => {
          }
        })
      } else {
        // 已认证，刷新用户信息
        this.refreshUserInfo()
      }
    }
  },
  
  // 刷新用户信息
  async refreshUserInfo() {
    try {
      const userInfo = await auth.refreshUserInfo()
      this.globalData.userInfo = userInfo

    } catch (error) {
    }
  },
  
  // 设置全局错误处理
  setupGlobalErrorHandling() {
    // 监听未捕获的Promise错误
    if (typeof Promise !== 'undefined') {
      const originalCatch = Promise.prototype.catch
      Promise.prototype.catch = function(handler) {
        return originalCatch.call(this, (error) => {
          // 可以在这里添加全局错误上报逻辑
          return handler(error)
        })
      }
    }
  },
  
  // 启动token有效性检查
  startTokenValidation() {
    // 每30分钟检查一次token有效性
    this.globalData.tokenValidationTimer = setInterval(() => {
      if (auth.isLoggedIn() && !auth.isTokenValid()) {
        auth.logout(true)
      }
    }, 30 * 60 * 1000)
  },

  globalData: {
    userInfo: null,
    systemInfo: null,
    tokenValidationTimer: null,
    loginTimer: null,
    // 事件处理器
    eventHandlers: {}
  },
  
  // 全局事件方法：注册事件监听器
  on(eventName, handler) {
    if (!this.globalData.eventHandlers[eventName]) {
      this.globalData.eventHandlers[eventName] = []
    }
    this.globalData.eventHandlers[eventName].push(handler)
  },
  
  // 全局事件方法：移除事件监听器
  off(eventName, handler) {
    if (this.globalData.eventHandlers[eventName]) {
      const index = this.globalData.eventHandlers[eventName].indexOf(handler)
      if (index > -1) {
        this.globalData.eventHandlers[eventName].splice(index, 1)
      }
    }
  },
  
  // 全局事件方法：触发事件
  emit(eventName, data) {
    if (this.globalData.eventHandlers[eventName]) {
      this.globalData.eventHandlers[eventName].forEach(handler => {
        if (typeof handler === 'function') {
          handler(data)
        }
      })
    }
  },

  // 全局方法：获取系统信息
  async getSystemInfo() {
    if (!this.globalData.systemInfo) {
      try {
        // 使用新的 API 替代废弃的 getSystemInfoSync
        const [systemSetting, appBaseInfo, deviceInfo, windowInfo] = await Promise.all([
          new Promise(resolve => {
            wx.getSystemSetting({
              success: resolve,
              fail: () => resolve({})
            })
          }),
          new Promise(resolve => {
            wx.getAppBaseInfo({
              success: resolve,
              fail: () => resolve({})
            })
          }),
          new Promise(resolve => {
            wx.getDeviceInfo({
              success: resolve,
              fail: () => resolve({})
            })
          }),
          new Promise(resolve => {
            wx.getWindowInfo({
              success: resolve,
              fail: () => resolve({})
            })
          })
        ])
        
        // 合并所有信息
        this.globalData.systemInfo = {
          ...systemSetting,
          ...appBaseInfo,
          ...deviceInfo,
          ...windowInfo,
          // 保持兼容性的一些常用字段
          platform: deviceInfo.platform || 'unknown',
          version: appBaseInfo.version || '',
          SDKVersion: appBaseInfo.SDKVersion || '',
          windowHeight: windowInfo.windowHeight || 0,
          windowWidth: windowInfo.windowWidth || 0,
          pixelRatio: windowInfo.pixelRatio || 1
        }
      } catch (error) {
        // 降级处理，使用旧的方法尝试获取
        try {
          const oldSystemInfo = wx.getSystemInfoSync()
          // 将旧格式转换为新格式
          this.globalData.systemInfo = {
            pixelRatio: oldSystemInfo.pixelRatio || 1,
            windowHeight: oldSystemInfo.windowHeight || 0,
            windowWidth: oldSystemInfo.windowWidth || 0,
            platform: oldSystemInfo.platform || 'unknown',
            version: oldSystemInfo.version || '',
            SDKVersion: oldSystemInfo.SDKVersion || ''
          }
        } catch (fallbackError) {
          this.globalData.systemInfo = {}
        }
      }
    }
    return this.globalData.systemInfo
  },
  
  // 全局方法：检查网络状态
  checkNetworkStatus() {
    return new Promise((resolve) => {
      wx.getNetworkType({
        success: (res) => {
          resolve({
            isConnected: res.networkType !== 'none',
            networkType: res.networkType
          })
        },
        fail: () => {
          resolve({ isConnected: false, networkType: 'unknown' })
        }
      })
    })
  },
  
  // 应用销毁时清理资源
  onUnload() {
    // 清理定时器
    if (this.globalData.tokenValidationTimer) {
      clearInterval(this.globalData.tokenValidationTimer)
    }
    if (this.globalData.loginTimer) {
      clearTimeout(this.globalData.loginTimer)
    }
    
    // 断开 WebSocket 连接
    wsClient.disconnect()
  }
})

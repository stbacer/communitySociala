// 认证工具类

// 检查是否已登录
const isLoggedIn = () => {
  try {
    const token = wx.getStorageSync('user_token')
    const userInfo = wx.getStorageSync('user_info')
    
    // 检查token是否存在且有效
    const hasValidToken = !!token && token.length> 10
    // 检查用户信息是否存在
   const hasUserInfo = !!userInfo && typeof userInfo === 'object'
    
    return hasValidToken && hasUserInfo
  } catch (error) {
    return false
  }
}

// 检查是否需要实名认证（已登录但未认证）
const needsAuthentication = () => {
  if (!isLoggedIn()) {
    return false // 未登录不需要认证
  }
  
  const authStatus = checkAuthStatus()
  // status=0: 未认证，需要去认证
  // status=1: 审核中，不需要再去认证
  // status=2: 已认证，不需要去认证
  // status=3: 认证失败，需要重新认证
  return authStatus === 0 || authStatus === 3
}

// 获取用户信息
const getUserInfo = () => {
  return wx.getStorageSync('user_info')
}

// 保存用户信息
const saveUserInfo = (userInfo) => {
  wx.setStorageSync('user_info', userInfo)
}

// 登录
const login = async (code, userData) => {
  try {
    // 检查网络状态
    const networkInfo = await new Promise((resolve, reject) => {
      wx.getNetworkType({
        success: resolve,
        fail: reject
      })
    })
    
    if (networkInfo.networkType === 'none') {
      throw new Error('当前无网络连接')
    }
    
    // 调用微信登录接口获取code
    const loginResult = await new Promise((resolve, reject) => {
      wx.login({
        success: resolve,
        fail: reject
      })
    })
    
    if (!loginResult.code) {
      throw new Error('获取登录凭证失败')
    }
    
    // 构造登录数据
    const loginData = {
      code: loginResult.code,
      avatarUrl: userData.avatarUrl || '',
      gender: userData.gender || 0,
      nickName: userData.nickName || '微信用户'
    }
    
    // 调用实际的登录API
    const api = require('./api.js')
    const result = await api.wechatLogin(loginData)
    
    if (result && result.data) {
      const { token, userInfo } = result.data
      wx.setStorageSync('user_token', token)
      saveUserInfo(userInfo)
      return { token, userInfo }
    } else {
      throw new Error('登录响应格式错误')
    }
    
  } catch (error) {
    throw error
  }
}

// 登出
const logout = (redirectToLogin = true) => {
  try {
    // 清除所有存储数据
    wx.removeStorageSync('user_token')
    wx.removeStorageSync('user_info')
    wx.removeStorageSync('current_location')
    
    // 清除可能的定时器
    if (typeof getApp() !== 'undefined') {
      const app = getApp()
      if (app.globalData.loginTimer) {
        clearTimeout(app.globalData.loginTimer)
        app.globalData.loginTimer = null
      }
    }
    
    // 跳转到登录页
    if (redirectToLogin) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
    }
    
  } catch (error) {
  }
}

// 检查实名认证状态
const checkAuthStatus = () => {
  const userInfo = getUserInfo()
  if (!userInfo) return 0 // 未认证
  return userInfo.authStatus || 0 // 0未认证，1认证中，2已认证，3认证失败
}

// 获取认证状态描述
const getAuthStatusText = () => {
  const status = checkAuthStatus()
  const statusMap = {
    0: '未认证',
    1: '认证审核中',
    2: '已认证',
    3: '认证失败'
  }
  return statusMap[status] || '未知状态'
}

// 检查token是否有效
const isTokenValid = () => {
  const token = wx.getStorageSync('user_token')
  if (!token) return false
  
  // 简单的token有效性检查（实际项目中应该解析JWT）
  return token.length > 10
}

// 刷新用户信息
const refreshUserInfo = async () => {
  try {
    const api = require('./api.js')
    const result = await api.getCurrentUser()
    if (result && result.data) {
      saveUserInfo(result.data)
      return result.data
    }
  } catch (error) {
    // 如果刷新失败，可能是token过期，需要重新登录
    if (error.code === 401) {
      logout(true)
    }
    throw error
  }
}

// 检查是否可以发布内容（需要实名认证）
const canPublishContent = () => {
  return checkAuthStatus() === 2
}

// 检查是否已实名认证
const isAuthenticated = () => {
  return checkAuthStatus() === 2
}

// 检查认证状态并引导用户
const checkAuthAndGuide = (options = {}) => {
  if (isAuthenticated()) {
    return true
  }
  
  wx.showModal({
    title: '提示',
    content: '您尚未完成实名认证，无法使用私信功能。请先前往个人中心认证。',
    confirmText: '去认证',
    cancelText: '取消',
    success(res) {
      if (res.confirm) {
        wx.navigateTo({ url: '/pages/profile/auth/auth' })
        if (options.onConfirm) {
          options.onConfirm()
        }
      } else {
        if (options.onCancel) {
          options.onCancel()
        }
      }
    }
  })
  
  return false
}

module.exports = {
  isLoggedIn,
  getUserInfo,
  saveUserInfo,
  login,
  logout,
  checkAuthStatus,
  getAuthStatusText,
  isTokenValid,
  refreshUserInfo,
  canPublishContent,
  isAuthenticated,
  checkAuthAndGuide,
  needsAuthentication
}
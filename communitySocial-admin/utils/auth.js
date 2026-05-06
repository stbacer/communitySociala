// 认证工具类

// 检查是否已登录
const isLoggedIn = () => {
  const token = wx.getStorageSync('admin_token')
  return !!token
}

// 检查管理员是否已登录（别名方法）
const isAdminLoggedIn = () => {
  return isLoggedIn()
}

// 获取用户信息
const getUserInfo = () => {
  return wx.getStorageSync('admin_user_info')
}

// 保存用户信息
const saveUserInfo = (userInfo) => {
  wx.setStorageSync('admin_user_info', userInfo)
}

// 登录
const login = (username, password) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const token = 'mock_token_' + Date.now()
      wx.setStorageSync('admin_token', token)
      const userInfo = {
        username: username,
        nickname: '管理员',
        role: 'admin'
      }
      saveUserInfo(userInfo)
      resolve({ token, userInfo })
    }, 1000)
  })
}

// 登出
const logout = () => {
  wx.removeStorageSync('admin_token')
  wx.removeStorageSync('admin_user_info')
  wx.redirectTo({
    url: '/pages/login/login'
  })
}

// 检查权限
const checkPermission = (requiredRole) => {
  const userInfo = getUserInfo()
  if (!userInfo) return false
  
  const userRole = userInfo.role
  if (requiredRole === 'admin') {
    return userRole === 'admin' || userRole === 'super_admin'
  }
  if (requiredRole === 'super_admin') {
    return userRole === 'super_admin'
  }
  return true
}

module.exports = {
  isLoggedIn,
  isAdminLoggedIn,
  getUserInfo,
  saveUserInfo,
  login,
  logout,
  checkPermission
}
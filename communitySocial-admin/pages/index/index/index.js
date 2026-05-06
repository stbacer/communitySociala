// pages/dashboard/dashboard.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    userInfo: null,
    currentTime: '',
    stats: {
      pendingUsers: 0,
      pendingPosts: 0,
      pendingReports: 0,
      totalUsers: 0,
      todayActiveUsers: 0,
      todayPosts: 0
    },
    loading: true
  },

  onLoad() {
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login/login'
      })
      return
    }
    
    // 设置当前时间
    this.setCurrentTime()
    this.loadDashboardData()
  },

  onShow() {
    // 页面显示时刷新数据
    if (auth.isLoggedIn()) {
      this.setCurrentTime()
      this.loadDashboardData()
    }
  },

  // 加载仪表板数据
  loadDashboardData() {
    this.setData({ loading: true })
      
    // 获取用户信息
    api.getCurrentUser().then(res => {
      this.setData({
        userInfo: res.data
      })
    })
      
    // 获取真实统计数据
    api.getDashboardStats().then(res => {
      if (res.code === 200 && res.data) {
        this.setData({
          stats: {
            pendingUsers: res.data.pendingUsers || 0,
            pendingPosts: res.data.pendingPosts || 0,
            pendingReports: 0,
            totalUsers: res.data.totalUsers || 0,
            todayActiveUsers: res.data.todayActiveUsers || 0,
            todayPosts: res.data.todayPosts || 0
          },
          loading: false
        })
      } else {
        throw new Error(res.message || '获取统计数据失败')
      }
    }).catch(err => {
      // 失败时使用默认值
      this.setData({
        stats: {
          pendingUsers: 0,
          pendingPosts: 0,
          pendingReports: 0,
          totalUsers: 0,
          todayActiveUsers: 0,
          todayPosts: 0
        },
        loading: false
      })
      wx.showToast({
        title: '获取统计数据失败',
        icon: 'none'
      })
    })
  },

  // 跳转到认证审核页面
  goToUserAuth() {
    wx.navigateTo({
      url: '/pages/index/userAuth/userAuth/userAuth',
      fail: (err) => {
        wx.showToast({
          title: '跳转失败',
          icon: 'none'
        })
      }
    })
  },

  // 跳转到帖子管理页面
  goToPostManage() {
    wx.navigateTo({
      url: '/pages/index/postManage/postManage/postManage',
      fail: (err) => {
        wx.showToast({
          title: '跳转失败',
          icon: 'none'
        })
      }
    })
  },

  // 跳转到举报处理页面
  goToReportManage() {
    wx.navigateTo({
      url: '/pages/index/reportManage/reportManage/reportManage',
      fail: (err) => {
        wx.showToast({
          title: '跳转失败',
          icon: 'none'
        })
      }
    })
  },

  // 跳转到数据统计页面
  goToStatistics() {
    wx.navigateTo({
      url: '/pages/index/statistics/statistics',
      fail: (err) => {
        wx.showToast({
          title: '跳转失败',
          icon: 'none'
        })
      }
    })
  },

  // 下拉刷新
  onPullDownRefresh() {

    this.loadDashboardData()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 500)
  },

  // 设置当前时间
  setCurrentTime() {
    const now = new Date()
    const timeStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
    this.setData({
      currentTime: timeStr
    })
  },

  // 格式化时间
  formatTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  },

  // 退出登录
  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          auth.logout()
        }
      }
    })
  },


})
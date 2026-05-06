// API工具类
const BASE_URL = 'http://127.0.0.1:8080'

// 请求拦截器
const request = (options) => {
  
  // 获取token
  const token = wx.getStorageSync('admin_token')

  // 设置请求头
  const header = {
    'Content-Type': 'application/json'
  }

  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    // 清理数据中的 null 和 undefined 值
    const cleanData = {}
    if (options.data) {
      Object.keys(options.data).forEach(key => {
        const value = options.data[key]
        // 只保留非 null、非 undefined 的值（允许空字符串，因为验证码可能是必填项）
        if (value !== null && value !== undefined) {
          cleanData[key] = value
        }
      })
    }
    
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: cleanData,
      header: header,
      success: (res) => {
        
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data)
          } else {
            wx.showToast({
              title: res.data.message || '请求失败',
              icon: 'none'
            })
            reject(res.data)
          }
        } else if (res.statusCode === 401) {
          wx.removeStorageSync('admin_token')
          wx.redirectTo({
            url: '/pages/login/login'
          })
          reject({ code: 401, message: '登录已过期' })
        } else {
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

// API接口定义
const api = {
  adminLogin: (data) => request({
    url: '/admin/auth/login',
    method: 'POST',
    data: data
  }),

  // 社区管理员注册
  registerAdmin: (data) => request({
    url: '/admin/auth/register',
    method: 'POST',
    data: data
  }),

  // 获取图片验证码
  getCaptchaImage: () => request({
    url: '/admin/captcha/image',
    method: 'GET',
    showLoading: false
  }),

  // 获取当前用户信息
  getCurrentUser: () => request({
    url: '/admin/current-user',
    method: 'GET'
  }),

  // 获取用户详情
  getUserInfo: (userId) => request({
    url: `/admin/user/detail/${userId}`,
    method: 'GET'
  }),

  // 更新用户信息
  updateUserInfo: (data) => request({
    url: `/admin/user/update/${data.userId}`,
    method: 'PUT',
    data: data
  }),

  // 获取待审核实名认证用户列表
  getPendingAuthUsers: (params) => request({
    url: '/admin/user/pending-auth',
    method: 'GET',
    data: params
  }),

  // 审核实名认证
  reviewAuth: (data) => request({
    url: '/admin/user/auth-review',
    method: 'PUT',
    data: data
  }),

  // 提交实名认证
  submitAuth: (data) => request({
    url: '/admin/user/auth-submit',
    method: 'POST',
    data: data
  }),

  // 获取待审核帖子列表
  getPendingPosts: (params) => request({
    url: '/admin/post/pending',
    method: 'GET',
    data: params
  }),
  
  // 根据状态获取帖子列表
  getPostsByStatus: (params) => request({
    url: '/admin/posts/list',
    method: 'GET',
    data: params
  }),
  
  // 获取帖子统计信息
  getPostStatistics: () => request({
    url: '/admin/posts/statistics',
    method: 'GET'
  }),
  
  // 获取审核历史
  getReviewHistory: (postId) => request({
    url: `/admin/posts/${postId}/review-history`,
    method: 'GET'
  }),

  // 审核帖子
  reviewPost: (data) => request({
    url: '/admin/post/review',
    method: 'PUT',
    data: data
  }),

  // 置顶帖子
  topPost: (postId) => request({
    url: `/admin/post/top/${postId}`,
    method: 'PUT'
  }),

  // 删除帖子
  deletePost: (postId) => request({
    url: `/admin/post/${postId}`,
    method: 'DELETE'
  }),

  // 获取待处理举报列表
  getPendingReports: (params) => request({
    url: '/admin/report/pending',
    method: 'GET',
    data: params
  }),

  // 处理举报
  handleReport: (data) => request({
    url: '/admin/report/handle',
    method: 'PUT',
    data: data
  }),
  
  // 获取举报列表
  getReportList: (params) => request({
    url: '/admin/report/list',
    method: 'GET',
    data: params
  }),
  
  // 获取举报详情
  getReportDetail: (reportId) => request({
    url: `/admin/report/${reportId}`,
    method: 'GET'
  }),
  
  // 获取举报统计信息
  getReportStatistics: () => request({
    url: '/admin/report/statistics',
    method: 'GET'
  }),
  
  // 批量处理举报
  batchHandleReports: (data) => request({
    url: '/admin/report/batch-handle',
    method: 'PUT',
    data: data
  }),
  
  // 导出举报数据
  exportReports: (reportIds) => request({
    url: '/admin/report/export',
    method: 'POST',
    data: reportIds
  }),
  
  // 获取举报详情
  getReportDetail: (reportId) => request({
    url: `/admin/report/${reportId}`,
    method: 'GET'
  }),

  // 获取统计数据
  getStatistics: () => request({
    url: '/admin/statistics/overview',
    method: 'GET'
  }),

  // 获取活跃用户统计
  getActiveUsersStats: (days) => request({
    url: `/admin/statistics/active-users?days=${days}`,
    method: 'GET'
  }),
  
  // 获取管理员首页统计数据
  getDashboardStats: () => request({
    url: '/admin/dashboard/stats',
    method: 'GET'
  }),

  // 获取分类统计数据
  getCategoryStatistics: (timeRange) => request({
    url: `/admin/statistics/category?timeRange=${timeRange || ''}`,
    method: 'GET'
  }),

  // 获取板块分类列表
  getCategoryList: () => request({
    url: '/admin/category/list',
    method: 'GET'
  }),

  // 获取社区统计数据（社区管理员用）
  getCommunityStatistics: () => request({
    url: '/admin/statistics/community',
    method: 'GET'
  }),

  // 获取近 N 日活跃用户趋势
  getDauTrend: (params) => request({
    url: '/admin/statistics/dau-trend',
    method: 'GET',
    data: params
  }),

  // 获取用户发帖量排名
  getUserPostRankings: (params) => request({
    url: '/admin/statistics/user-posts',
    method: 'GET',
    data: params
  }),

  // 修改密码
  changePassword: (data) => request({
    url: '/admin/management/change-password',
    method: 'POST',
    data: data
  }),

  // 修改手机号
  changePhone: (data) => request({
    url: '/admin/management/change-phone',
    method: 'POST',
    data: data
  }),

  // ========== 省市区数据相关接口 ==========
  
  // 获取省份列表（从 Redis 缓存）
  getProvinces: () => request({
    url: '/sadmin/user/provinces',
    method: 'GET'
  }),

  // 根据省份代码获取城市列表（从 Redis 缓存）
  getCitiesByProvinceCode: (provinceCode) => request({
    url: `/sadmin/user/cities/${provinceCode}`,
    method: 'GET'
  }),

  // 根据城市代码获取区县列表（从 Redis 缓存）
  getDistrictsByCityCode: (cityCode) => request({
    url: `/sadmin/user/districts/${cityCode}`,
    method: 'GET'
  })
}

module.exports = api
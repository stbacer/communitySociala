// API 工具类

// const BASE_URL = 'https://21ee023c.r8.cpolar.cn'
const BASE_URL = 'http://127.0.0.1:8080'


// 处理图片URL，如果是相对路径则拼接完整URL
const handleImageUrl = (url) => {
  if (!url) return url
  
  // 如果是相对路径，拼接BASE_URL
  if (url.startsWith('/')) {
    return BASE_URL + url
  }
  
  // 如果已经是完整URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  
  // 其他情况也拼接BASE_URL
  return BASE_URL + '/' + url
}


// 请求拦截器
const request = async (options) => {
  // 检查网络状态
  const networkInfo = await new Promise((resolve) => {
    wx.getNetworkType({
      success: resolve,
      fail: () => resolve({ networkType: 'unknown' })
    })
  })
  
  if (networkInfo.networkType === 'none') {
    wx.showToast({
      title: '当前无网络连接',
      icon: 'none'
    })
    throw new Error('网络连接失败')
  }

  // 显示加载提示
  if (options.showLoading !== false) {
    wx.showLoading({
      title: options.loadingText || '加载中...',
      mask: true
    })
  }

  // 获取token
  const token = wx.getStorageSync('user_token')

  // 设置请求头
  const header = {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest'
  }

  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }

  // 添加时间戳防止缓存
  const urlWithTimestamp = options.url + (options.url.includes('?') ? '&' : '?') + `_t=${Date.now()}`

  return new Promise((resolve, reject) => {
    // 清理数据中的 null 和 undefined 值
    const cleanData = {}
    if (options.data) {
      Object.keys(options.data).forEach(key => {
        const value = options.data[key]
        // 只保留非 null、非 undefined、非空字符串的值
        if (value !== null && value !== undefined && value !== '') {
          cleanData[key] = value
        }
      })
    }
    
    wx.request({
      url: BASE_URL + urlWithTimestamp,
      method: options.method || 'GET',
      data: cleanData,
      header: header,
      timeout: options.timeout || 15000,
      success: (res) => {
        if (options.showLoading !== false) {
          wx.hideLoading()
        }
        
        // 处理不同状态码
        switch (res.statusCode) {
          case 200:
            if (res.data.code === 200) {
              resolve(res.data)
            } else {
              // 业务错误处理
              const errorMsg = res.data.message || '请求失败'
              if (options.showError !== false) {
                wx.showToast({
                  title: errorMsg,
                  icon: 'none',
                  duration: options.errorDuration || 2000
                })
              }
              reject(res.data)
            }
            break
            
          case 401:
            // token过期或未授权
            wx.removeStorageSync('user_token')
            wx.removeStorageSync('user_info')
            
            if (options.handleUnauthorized !== false) {
              wx.showModal({
                title: '登录已过期',
                content: '请重新登录以继续使用',
                showCancel: false,
                confirmText: '去登录',
                success: () => {
                  wx.redirectTo({
                    url: '/pages/login/login'
                  })
                }
              })
            }
            reject({ code: 401, message: '登录已过期' })
            break
            
          case 403:
            wx.showToast({
              title: '权限不足',
              icon: 'none'
            })
            reject({ code: 403, message: '权限不足' })
            break
            
          case 404:
            wx.showToast({
              title: '请求的资源不存在',
              icon: 'none'
            })
            reject({ code: 404, message: '资源不存在' })
            break
            
          case 500:
          case 502:
          case 503:
          case 504:
            wx.showToast({
              title: '服务器开小差了，请稍后再试',
              icon: 'none'
            })
            reject({ code: res.statusCode, message: '服务器错误' })
            break
            
          default:
            const defaultErrorMsg = `请求失败 (${res.statusCode})`
            if (options.showError !== false) {
              wx.showToast({
                title: defaultErrorMsg,
                icon: 'none'
              })
            }
            reject({ code: res.statusCode, message: defaultErrorMsg })
        }
      },
      fail: (err) => {
        if (options.showLoading !== false) {
          wx.hideLoading()
        }
        
        let errorMsg = '网络连接失败'
        
        if (err.errMsg) {
          if (err.errMsg.includes('timeout')) {
            errorMsg = '请求超时，请检查网络'
          } else if (err.errMsg.includes('abort')) {
            errorMsg = '请求被取消'
          }
        }
        
        if (options.showError !== false) {
          wx.showToast({
            title: errorMsg,
            icon: 'none',
            duration: options.errorDuration || 2000
          })
        }
        reject(err)
      }
    })
  })
}

// 重试机制
const retryRequest = async (requestFn, options = {}) => {
  const maxRetries = options.maxRetries || 3
  const retryDelay = options.retryDelay || 1000
  
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await requestFn()
    } catch (error) {
      // 如果是401或403错误，不重试
      if (error.code === 401 || error.code === 403) {
        throw error
      }
      
      // 如果是最后一次尝试，抛出错误
      if (i === maxRetries - 1) {
        throw error
      }
      
      // 等待后重试
      await new Promise(resolve => setTimeout(resolve, retryDelay * (i + 1)))
    }
  }
}

// API 接口定义
const api = {
  // 账号密码登录
  accountLogin: (data) => request({
    url: '/resident/auth/login',
    method: 'POST',
    data: {
      phone: data.phoneNumber,  // 使用 phone 字段传递手机号
      password: data.password,
      captchaId: data.captchaId,
      captchaCode: data.captchaCode
    },
    showLoading: true,
    loadingText: '正在登录...',
    timeout: 20000
  }),

  // 微信授权登录
  wechatLogin: (data) => request({
    url: '/resident/auth/wechat-login',
    method: 'POST',
    data: data,
    showLoading: true,
    loadingText: '正在登录...',
    timeout: 20000
  }),

  // 用户注册
  register: (data) => request({
    url: '/resident/auth/register',
    method: 'POST',
    data: data
  }),

  // 获取当前用户信息
  getCurrentUser: () => request({
    url: '/resident/user/info',
    method: 'GET'
  }),
  
  // 获取当前用户个人统计数据
  getUserStats: () => request({
    url: '/resident/user/stats',
    method: 'GET'
  }),

  // 提交实名认证
  submitAuth: (data) => request({
    url: '/resident/user/auth-submit',
    method: 'POST',
    data: data
  }),

  // 发布帖子
  createPost: (data) => request({
    url: '/resident/post/create',
    method: 'POST',
    data: data
  }),

  // 获取帖子列表
  getPostList: (params) => request({
    url: '/resident/post/list',
    method: 'GET',
    data: params
  }),
  
  // 获取按时间排序的帖子列表
  getPostsByTime: (params) => request({
    url: '/resident/post/list/time',
    method: 'GET',
    data: params
  }),
  
  // 获取按热度排序的帖子列表
  getHotPosts: (params) => request({
    url: '/resident/post/list/hot',
    method: 'GET',
    data: params
  }),
  
  // 获取附近帖子列表
  getNearbyPosts: (params) => request({
    url: '/resident/post/list/nearby',
    method: 'GET',
    data: params
  }),

  // 获取帖子详情
  getPostDetail: (postId) => request({
    url: `/resident/post/${postId}`,
    method: 'GET'
  }),

  // 删除帖子
  deletePost: (postId) => request({
    url: `/resident/post/${postId}`,
    method: 'DELETE'
  }),
  
  // 更新帖子
  updatePost: (data) => request({
    url: `/resident/post/update`,
    method: 'PUT',
    data: data
  }),

  // 点赞/取消点赞
  toggleLike: (data) => request({
    url: '/resident/interaction/like',
    method: data.isLike ? 'POST' : 'DELETE',
    data: data
  }),

  // 收藏/取消收藏
  toggleCollect: (data) => request({
    url: '/resident/interaction/collect',
    method: data.isCollect ? 'POST' : 'DELETE',
    data: data
  }),

  // 发布评论
  createComment: (data) => request({
    url: '/resident/comment/create',
    method: 'POST',
    data: data
  }),

  // 获取评论列表
  getComments: (postId, params) => request({
    url: `/resident/comment/post/${postId}`,
    method: 'GET',
    data: params
  }),

  // 删除评论
  deleteComment: (commentId) => request({
    url: `/resident/comment/${commentId}`,
    method: 'DELETE'
  }),
  
  // 编辑评论
  updateComment: (data) => request({
    url: `/resident/comment/update`,
    method: 'PUT',
    data: data
  }),

  // 点赞评论
  likeComment: (commentId) => request({
    url: `/resident/comment/like/${commentId}`,
    method: 'POST'
  }),

  // 取消点赞评论
  unlikeComment: (commentId) => request({
    url: `/resident/comment/like/${commentId}`,
    method: 'DELETE'
  }),

  // 获取会话列表
  getConversations: (params) => request({
    url: '/resident/message/conversations',
    method: 'GET',
    data: params
  }),

  // 获取消息记录
  getMessageRecords: (contactId, params) => request({
    url: '/resident/message/records',
    method: 'GET',
    data: { contactId, ...params }
  }),

  // 发送消息
  sendMessage: (data) => request({
    url: '/resident/message/send',
    method: 'POST',
    data: data
  }),

  // 标记消息为已读
  markMessageRead: (contactId) => request({
    url: `/resident/message/read/${contactId}`,
    method: 'PUT'
  }),
  
  // 批量标记消息为已读
  markMessagesRead: (messageIds) => request({
    url: '/resident/message/mark-read',
    method: 'POST',
    data: { messageIds }
  }),
  
  // 提交举报
  submitReport: (data) => request({
    url: '/resident/report/submit',
    method: 'POST',
    data: data
  }),
  
  // 获取用户举报历史
  getUserReports: (params) => request({
    url: '/resident/report/my-reports',
    method: 'GET',
    data: params
  }),
  
  // 获取举报详情
  getReportDetail: (reportId) => request({
    url: `/resident/report/${reportId}`,
    method: 'GET'
  }),

  // 获取未读消息数量
  getUnreadCount: () => request({
    url: '/resident/message/unread-count',
    method: 'GET'
  }),

  // 获取用户详情（用于获取头像等信息）- 返回完整数据
  getUserProfileComplete: (userId, page = 1, size = 5) => request({
    url: `/resident/user/profile/${userId}`,
    method: 'GET',
    data: {
      page: page,
      size: size
    },
    showLoading: false,  // 首次加载时静默处理
    showError: false     // 首次加载时静默处理错误
  }),
  
  // 获取用户详情（兼容旧接口）
  getUserDetail: (userId) => request({
    url: `/resident/user/${userId}`,
    method: 'GET'
  }),

  // 撤回消息
  recallMessage: (messageId) => request({
    url: `/resident/message/recall/${messageId}`,
    method: 'DELETE'
  }),

  // 删除消息
  deleteMessage: (messageId) => request({
    url: `/resident/message/${messageId}`,
    method: 'DELETE'
  }),

  // 提交举报
  submitReport: (data) => request({
    url: '/resident/report/submit',
    method: 'POST',
    data: {
      targetType: data.targetType,  // 1:帖子, 2:评论, 3:用户
      targetId: data.targetId,      // 目标ID
      reason: data.reason           // 举报原因
    }
  }),
  
  // 获取用户举报历史
  getUserReports: (params) => request({
    url: '/resident/report/my-reports',
    method: 'GET',
    data: params
  }),

  // 获取分类列表
  getCategoryList: () => request({
    url: '/resident/category/list',
    method: 'GET'
  }),
  
  // 上传头像文件
  uploadAvatar: (filePath) => {
    const token = wx.getStorageSync('user_token')
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: BASE_URL + '/resident/user/upload-avatar',
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': `Bearer ${token}`
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200) {
              // 处理返回的图片URL，确保是完整URL
              if (data.data && typeof data.data === 'string') {
                data.data = handleImageUrl(data.data)
              }
              resolve(data)
            } else {
              reject(data)
            }
          } catch (e) {
            reject({ code: 500, message: '上传失败' })
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  },
  
  // 上传图片文件
  uploadImage: (filePath) => {
    const token = wx.getStorageSync('user_token')
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: BASE_URL + '/image/upload',
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': `Bearer ${token}`
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200) {
              // 处理返回的图片URL，确保是完整URL
              if (data.data && typeof data.data === 'string') {
                data.data = handleImageUrl(data.data)
              }
              resolve(data)
            } else {
              reject(data)
            }
          } catch (e) {
            reject({ code: 500, message: '上传失败' })
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  },
  
  // 更新用户个人信息
  updateUserProfile: (data) => request({
    url: '/resident/user/profile',
    method: 'PUT',
    data: data
  }),
  
  // 修改密码
  changePassword: (data) => request({
    url: '/resident/user/change-password',
    method: 'POST',
    data: data
  }),
  
  // 修改手机号
  changePhone: (data) => request({
    url: '/resident/user/change-phone',
    method: 'POST',
    data: data
  }),
  
  // 获取用户发布的帖子列表
  getUserPosts: (userId, params) => request({
    url: `/resident/post/user/${userId}`,
    method: 'GET',
    data: params,
    timeout: 10000,  // 设置较短的超时时间
    showError: false  // 首次加载时静默处理错误，避免频繁提示
  }),
  
  // 获取当前登录用户的帖子列表（包括待审核和已审核）
  getMyPosts: (params) => request({
    url: '/resident/post/my-posts',
    method: 'GET',
    data: params,
    timeout: 10000,
    showError: false
  }),
  
  // 获取用户的评论列表
  getUserComments: (userId, params) => request({
    url: `/resident/comment/user/${userId}`,
    method: 'GET',
    data: params,
    timeout: 10000,
    showError: false
  }),
  
  // 获取用户的收藏列表
  getUserCollections: (userId, params) => request({
    url: `/resident/interaction/collection/user/${userId}`,
    method: 'GET',
    data: params,
    timeout: 10000,
    showError: false
  }),
  
  // 取消收藏（按帖子 ID）
  cancelCollect: (postId) => request({
    url: '/resident/interaction/collection',
    method: 'DELETE',
    data: { postId }
  }),
  
  // 获取社区管理员的省市区和社区列表（用于居民端实名认证）
  getAdminRegions: () => request({
    url: '/resident/user/admin-regions',
    method: 'GET'
  }),
  
  // ==================== 关注相关 API ====================
  
  // 关注用户
  followUser: (userId) => request({
    url: '/resident/interaction/follow',
    method: 'POST',
    data: { followedUserId: userId }
  }),
  
  // 取消关注用户
  unfollowUser: (userId) => request({
    url: `/resident/interaction/follow/${userId}`,
    method: 'DELETE'
  }),
  
  // 获取关注状态
  getFollowStatus: (userId) => request({
    url: `/resident/interaction/follow/status/${userId}`,
    method: 'GET'
  }),
  
  // 获取关注列表
  getFollowingList: (userId, params) => request({
    url: `/resident/interaction/follow/following/${userId}`,
    method: 'GET',
    data: params
  }),
  
  // 获取粉丝列表
  getFollowerList: (userId, params) => request({
    url: `/resident/interaction/follow/follower/${userId}`,
    method: 'GET',
    data: params
  }),

  // ==================== 忘记密码相关 API ====================
  
  // 获取图片验证码
  getCaptchaImage: () => request({
    url: '/resident/auth/captcha/image',
    method: 'GET',
    showLoading: false
  }),

  // 重置密码
  resetPassword: (data) => request({
    url: '/resident/auth/reset-password',
    method: 'POST',
    data: data
  })
}

// 导出URL处理函数供其他模块使用
api.handleImageUrl = handleImageUrl

module.exports = api
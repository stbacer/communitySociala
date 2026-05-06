// pages/profile/mycomments/mycomments.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    comments: [],
    loading: false,
    refreshing: false,
    hasMore: true,
    currentPage: 1,
    pageSize: 10,
    total: 0,
    cacheKey: 'my_comments_cache'
  },

  onLoad(options) {

    this.loadMyComments()
  },

  onShow() {
    // 页面显示时检查是否需要刷新数据
    const lastRefreshTime = wx.getStorageSync('my_comments_last_refresh')
    const now = new Date().getTime()
    
    // 如果距离上次刷新超过 5 分钟，或者列表为空，则刷新数据
    if (!lastRefreshTime || now - lastRefreshTime > 5 * 60 * 1000 || this.data.comments.length === 0) {
      this.refreshComments()
      wx.setStorageSync('my_comments_last_refresh', now)
    }
  },

  // 加载我的评论
  async loadMyComments() {if (this.data.loading) {

      return
    }
    
    this.setData({ loading: true })
    
    try {
      const userInfo = auth.getUserInfo()

      if (!userInfo) {

        wx.showToast({
          title: '请先登录',
          icon: 'none'
        })
        wx.redirectTo({
          url: '/pages/login/login'
        })
        this.setData({ loading: false })
        return
      }

      const params = {
        page: this.data.currentPage,
        size: this.data.pageSize
      }

      // 尝试从缓存获取数据 (仅限第一页)
      if (this.data.currentPage === 1) {
        const cachedData = this.getCachedComments()

        if (cachedData && cachedData.comments.length > 0) {

          this.setData({
            comments: cachedData.comments,
            total: cachedData.total,
            hasMore: cachedData.hasMore,
            loading: false
          })

          // 同时发起后台刷新
          this.backgroundRefresh()
          return
        } else {

        }
      }

      const response = await api.getUserComments(userInfo.userId, params)

      if (response.code === 200) {
        const newComments = response.data.records || response.data.list || []
        const total = response.data.total || 0

        // 处理评论数据
        const processedComments = newComments.map(comment => ({
          ...comment,
          createTime: this.formatTime(comment.createTime),
          postInfo: comment.postInfo || null,
          replyToNickname: comment.replyToNickname || null,
          replyContent: comment.replyContent || null
        }))

        this.setData({
          comments: this.data.currentPage === 1 ? processedComments : [...this.data.comments, ...processedComments],
          total: total,
          hasMore: processedComments.length === this.data.pageSize && this.data.comments.length + processedComments.length < total,
          loading: false,
          refreshing: false
        })

        // 缓存第一页数据
        if (this.data.currentPage === 1) {
          this.cacheComments(processedComments, total)
        }
      } else {

        throw new Error(response.message || '获取评论失败')
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
      // 确保在任何错误情况下都关闭加载状态
      this.setData({
        loading: false,
        refreshing: false
      })
    }
  },

  // 缓存评论数据
  cacheComments(comments, total) {
    const cacheData = {
      comments: comments,
      total: total,
      hasMore: comments.length < total,
      timestamp: new Date().getTime()
    }
    wx.setStorageSync(this.data.cacheKey, cacheData)
  },
  
  // 获取缓存的评论数据
  getCachedComments() {
    try {
      const cached = wx.getStorageSync(this.data.cacheKey)
      if (cached && cached.timestamp) {
        const now = new Date().getTime()
        // 缓存有效期 10 分钟
        if (now - cached.timestamp < 10 * 60 * 1000) {
          return cached
        }
      }
      return null
    } catch (error) {
      return null
    }
  },
  
  // 后台刷新数据
  async backgroundRefresh() {
    try {
      const userInfo = auth.getUserInfo()
      if (!userInfo) return
      
      const params = {
        page: 1,
        size: this.data.pageSize
      }
      
      const response = await api.getUserComments(userInfo.userId, params)
      if (response.code === 200) {
        const newComments = response.data.records || response.data.list || []
        const total = response.data.total || 0
        
        // 处理评论数据
        const processedComments = newComments.map(comment => ({
          ...comment,
          createTime: this.formatTime(comment.createTime),
          postInfo: comment.postInfo || null,
          replyToNickname: comment.replyToNickname || null,
          replyContent: comment.replyContent || null
        }))
        
        // 更新缓存
        this.cacheComments(processedComments, total)
        
        // 如果当前显示的是第一页，更新界面
        if (this.data.currentPage === 1) {
          this.setData({
            comments: processedComments,
            total: total,
            hasMore: processedComments.length < total
          })
        }
      }
    } catch (error) {
    }
  },

  // 刷新评论
  refreshComments() {

    this.setData({
      currentPage: 1,
      comments: [],
      refreshing: true
    }, () => {
      this.loadMyComments()
    })
  },

  // 滚动加载更多
  onScrollToLower() {

    if (this.data.hasMore && !this.data.loading) {
      this.setData({
        currentPage: this.data.currentPage + 1
      }, () => {
        this.loadMyComments()
      })
    }
  },

  // 下拉刷新
  onRefresh() {

    this.refreshComments()
  },

  // 格式化时间
  formatTime(time) {
    if (!time) return ''
    
    const date = new Date(time)
    const now = new Date()
    const diff = now - date
    
    const minute = 60 * 1000
    const hour = 60 * minute
    const day = 24 * hour
    const month = 30 * day
    const year = 12 * month
    
    if (diff < minute) {
      return '刚刚'
    } else if (diff < hour) {
      return Math.floor(diff / minute) + '分钟前'
    } else if (diff < day) {
      return Math.floor(diff / hour) + '小时前'
    } else if (diff < month) {
      return Math.floor(diff / day) + '天前'
    } else if (diff < year) {
      return Math.floor(diff / month) + '个月前'
    } else {
      return Math.floor(diff / year) + '年前'
    }
  },

  // 返回列表页
  onBack() {
    wx.navigateBack({
      delta: 1
    })
  },

  // 点击评论
  onCommentTap(e) {
    const commentId = e.currentTarget.dataset.commentid
    const comment = this.data.comments.find(c => c.commentId === commentId)
    
    if (comment && comment.postId) {
      // 跳转到帖子详情页
      wx.navigateTo({
        url: `/pages/post/detail?id=${comment.postId}`
      })
    }
  },

  // 删除评论
  async onDeleteComment(e) {
    const commentId = e.currentTarget.dataset.commentid
    const index = e.currentTarget.dataset.index
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条评论吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            const userInfo = auth.getUserInfo()
            if (!userInfo) {
              wx.showToast({
                title: '请先登录',
                icon: 'none'
              })
              return
            }
            
            const response = await api.deleteComment(commentId, userInfo.userId)
            if (response.code === 200) {
              wx.showToast({
                title: '删除成功',
                icon: 'success'
              })
              
              // 从列表中移除
              const newComments = [...this.data.comments]
              newComments.splice(index, 1)
              this.setData({
                comments: newComments
              })
            } else {
              throw new Error(response.message || '删除失败')
            }
          } catch (error) {
            wx.showToast({
              title: error.message || '删除失败',
              icon: 'none'
            })
          }
        }
      }
    })
  },

  // 回复评论
  onReplyComment(e) {
    const commentId = e.currentTarget.dataset.commentid
    const postId = e.currentTarget.dataset.postid
    
    // 跳转到回复页面或打开回复弹窗
    wx.navigateTo({
      url: `/pages/post/detail?id=${postId}&replyCommentId=${commentId}`
    })
  },

  // 分享
  onShareAppMessage() {
    return {
      title: '我的评论',
      path: '/pages/profile/mycomments/mycomments'
    }
  }
})

// pages/profile/myposts/myposts.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    posts: [],
    loading: false,  // 修改：默认不显示加载状态
    refreshing: false,
    hasMore: true,
    currentPage: 1,
    pageSize: 5,
    total: 0,
    cacheKey: 'my_posts_cache',
    showDebug: false  // 调试模式开关
  },

  onLoad(options) {

    // 初始化状态
    this.setData({
      loading: false,
      refreshing: false,
      posts: [],
      currentPage: 1
    })
    this.loadMyPosts()
  },
  
  onShow() {
    // 页面显示时检查是否需要刷新数据
    const lastRefreshTime = wx.getStorageSync('my_posts_last_refresh')
    const now = new Date().getTime()
      
    // 只有在列表为空时才自动刷新，避免频繁刷新影响用户体验
    if (this.data.posts.length === 0) {

      this.refreshPosts()
      wx.setStorageSync('my_posts_last_refresh', now)
    } else {

    }
  },

  // 加载我的帖子
  async loadMyPosts() {// 防止重复加载，但如果是刷新操作则允许
    if (this.data.loading && !this.data.refreshing) {

      return
    }
      
    // 如果不是刷新操作，设置加载状态
    if (!this.data.refreshing) {
      this.setData({ loading: true })

    }
      
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
        this.setData({ loading: false, refreshing: false })
        return
      }
  
      const params = {
        page: this.data.currentPage,
        size: this.data.pageSize
      }

      // 尝试从缓存获取数据（仅限第一页且不是刷新操作）
      if (this.data.currentPage === 1 && !this.data.refreshing) {
        const cachedData = this.getCachedPosts()

        if (cachedData && cachedData.posts.length > 0) {

          this.setData({
            posts: cachedData.posts,
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

      // 使用新的 API 接口获取当前登录用户的所有帖子（包括待审核和已审核）
      const response = await api.getMyPosts(params)

      if (response.code === 200) {
        const newPosts = response.data.records || response.data.list || []
        const total = response.data.total || 0

        // 处理帖子数据
        const processedPosts = newPosts.map(post => ({
          ...post,
          publishTime: this.formatTime(post.publishTime),
          imageUrls: post.imageUrls ? (Array.isArray(post.imageUrls) ? post.imageUrls : []) : []
        }))

        this.setData({
          posts: this.data.currentPage === 1 ? processedPosts : [...this.data.posts, ...processedPosts],
          total: total,
          hasMore: processedPosts.length === this.data.pageSize && this.data.posts.length + processedPosts.length < total,
          loading: false,
          refreshing: false
        })

        // 缓存第一页数据
        if (this.data.currentPage === 1) {
          this.cachePosts(processedPosts, total)
        }
      } else {

        throw new Error(response.message || '获取帖子失败')
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

  // 缓存帖子数据
  cachePosts(posts, total) {
    const cacheData = {
      posts: posts,
      total: total,
      hasMore: posts.length < total,
      timestamp: new Date().getTime()
    }
    wx.setStorageSync(this.data.cacheKey, cacheData)
  },
  
  // 获取缓存的帖子数据
  getCachedPosts() {
    try {
      const cached = wx.getStorageSync(this.data.cacheKey)
      if (cached && cached.timestamp) {
        const now = new Date().getTime()
        // 缓存有效期10分钟
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

      // 使用新的 API 接口获取当前登录用户的所有帖子
      const response = await api.getMyPosts(params)
      if (response.code === 200) {
        const newPosts = response.data.records || response.data.list || []
        const total = response.data.total || 0
        
        // 处理帖子数据
        const processedPosts = newPosts.map(post => ({
          ...post,
          publishTime: this.formatTime(post.publishTime),
          imageUrls: post.imageUrls ? (Array.isArray(post.imageUrls) ? post.imageUrls : []) : []
        }))
        
        // 仅更新缓存，不更新界面（除非当前就是第一页且用户没有滚动）
        this.cachePosts(processedPosts, total)

      }
    } catch (error) {
      // 后台刷新失败不影响界面状态
    }
  },
  
  // 清除缓存
  clearCache() {
    try {
      wx.removeStorageSync(this.data.cacheKey)
      wx.removeStorageSync('my_posts_last_refresh')
    } catch (error) {
    }
  },

  // 刷新帖子
  async refreshPosts() {

    this.clearCache()  // 刷新时清除缓存
    this.setData({
      currentPage: 1,
      refreshing: true,
      hasMore: true,
      loading: false  // 确保 loading 为 false，避免被跳过
    })
    await this.loadMyPosts()
  },
  onRefresh() {

    this.refreshPosts()
  },

  // 上拉加载更多
  onScrollToLower() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({
        currentPage: this.data.currentPage + 1
      })
      this.loadMyPosts()
    }
  },

  // 帖子点击事件
  onPostTap(e) {
    const postId = e.currentTarget.dataset.postid

    wx.navigateTo({
      url: `/pages/post/detail?postId=${postId}`
    })
  },
  onDeletePost(e) {
    const postId = e.currentTarget.dataset.postid
    const index = e.currentTarget.dataset.index
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这篇帖子吗？删除后无法恢复',
      confirmColor: '#ff4757',
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({
              title: '删除中...'
            })
            
            await api.deletePost(postId)
            
            wx.hideLoading()
            wx.showToast({
              title: '删除成功',
              icon: 'success'
            })
            
            // 从列表中移除
            const newPosts = [...this.data.posts]
            newPosts.splice(index, 1)
            this.setData({
              posts: newPosts,
              total: this.data.total - 1
            })
            
          } catch (error) {
            wx.hideLoading()
            wx.showToast({
              title: error.message || '删除失败',
              icon: 'none'
            })
          }
        }
      }
    })
  },

  // 编辑帖子
  onEditPost(e) {
    const postId = e.currentTarget.dataset.postid
    if (!postId) {
      wx.showToast({
        title: '帖子ID不存在',
        icon: 'none'
      })
      return
    }
    
    // 将帖子ID和编辑模式存储到全局数据或本地存储
    wx.setStorageSync('edit_post_id', postId)
    wx.setStorageSync('edit_post_mode', 'edit')
    
    // 使用 switchTab 跳转到 tabBar 页面
    wx.switchTab({
      url: '/pages/post/create',
      success: () => {
      },
      fail: (err) => {
        wx.showToast({
          title: '跳转失败',
          icon: 'none'
        })
      }
    })
  },

  // 去发帖
  onCreatePost() {
    wx.navigateTo({
      url: '/pages/post/create'
    })
  },
  
  // 返回上一页
  onBack() {
    wx.navigateBack()
  },

  // 时间格式化
  formatTime(timeString) {
    if (!timeString) return ''
    
    try {
      const date = new Date(timeString)
      const now = new Date()
      const diff = now.getTime() - date.getTime()
      const minutes = Math.floor(diff / (1000 * 60))
      const hours = Math.floor(diff / (1000 * 60 * 60))
      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      
      if (minutes < 1) {
        return '刚刚'
      } else if (minutes < 60) {
        return `${minutes}分钟前`
      } else if (hours < 24) {
        return `${hours}小时前`
      } else if (days < 7) {
        return `${days}天前`
      } else {
        return date.toLocaleDateString()
      }
    } catch (error) {
      return timeString
    }
  },

  // 页面滚动事件
  onPageScroll(e) {
    // 可以在这里处理滚动相关的逻辑
  },

  // 分享功能
  onShareAppMessage() {
    return {
      title: '我的帖子 - 社区邻里',
      path: '/pages/profile/myposts'
    }
  },
  
  // 调试测试方法
  onDebugTest() {
    this.setData({
      showDebug: !this.data.showDebug
    })

  },
  
  // 清除缓存
  onClearCache() {
    this.clearCache()
    wx.showToast({
      title: '缓存已清除',
      icon: 'success'
    })
  },
  
  // 强制刷新
  onForceRefresh() {
    this.refreshPosts()
  }
})
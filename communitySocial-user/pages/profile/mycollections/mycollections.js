// pages/profile/mycollections/mycollections.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    collections: [],
    loading: false,
    refreshing: false,
    hasMore: true,
    currentPage: 1,
    pageSize: 10,
    total: 0,
    cacheKey: 'my_collections_cache'
  },

  onLoad(options) {

    this.loadMyCollections()
  },

  onShow() {
    // 页面显示时检查是否需要刷新数据
    const lastRefreshTime = wx.getStorageSync('my_collections_last_refresh')
    const now = new Date().getTime()
    
    // 如果距离上次刷新超过 5 分钟，或者列表为空，则刷新数据
    if (!lastRefreshTime || now - lastRefreshTime > 5 * 60 * 1000 || this.data.collections.length === 0) {
      this.refreshCollections()
      wx.setStorageSync('my_collections_last_refresh', now)
    }
  },

  // 加载我的收藏
  async loadMyCollections() {if (this.data.loading) {

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
        const cachedData = this.getCachedCollections()

        if (cachedData && cachedData.collections.length > 0) {

          this.setData({
            collections: cachedData.collections,
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

      const response = await api.getUserCollections(userInfo.userId, params)

      if (response.code === 200) {
        const newCollections = response.data.records || response.data.list || []
        const total = response.data.total || 0

        // 处理收藏数据
        const processedCollections = newCollections.map(collection => ({
          ...collection,
          collectTime: this.formatTime(collection.collectTime),
          imageUrls: collection.imageUrls ? (Array.isArray(collection.imageUrls) ? collection.imageUrls : []) : []
        }))

        this.setData({
          collections: this.data.currentPage === 1 ? processedCollections : [...this.data.collections, ...processedCollections],
          total: total,
          hasMore: processedCollections.length === this.data.pageSize && this.data.collections.length + processedCollections.length < total,
          loading: false,
          refreshing: false
        })

        // 缓存第一页数据
        if (this.data.currentPage === 1) {
          this.cacheCollections(processedCollections, total)
        }
      } else {

        throw new Error(response.message || '获取收藏失败')
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

  // 缓存收藏数据
  cacheCollections(collections, total) {
    const cacheData = {
      collections: collections,
      total: total,
      hasMore: collections.length < total,
      timestamp: new Date().getTime()
    }
    wx.setStorageSync(this.data.cacheKey, cacheData)
  },
  
  // 获取缓存的收藏数据
  getCachedCollections() {
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
      
      const response = await api.getUserCollections(userInfo.userId, params)
      if (response.code === 200) {
        const newCollections = response.data.records || response.data.list || []
        const total = response.data.total || 0
        
        // 处理收藏数据
        const processedCollections = newCollections.map(collection => ({
          ...collection,
          collectTime: this.formatTime(collection.collectTime),
          imageUrls: collection.imageUrls ? (Array.isArray(collection.imageUrls) ? collection.imageUrls : []) : []
        }))
        
        // 更新缓存
        this.cacheCollections(processedCollections, total)
        
        // 如果当前显示的是第一页，更新界面
        if (this.data.currentPage === 1) {
          this.setData({
            collections: processedCollections,
            total: total,
            hasMore: processedCollections.length < total
          })
        }
      }
    } catch (error) {
    }
  },

  // 刷新收藏
  refreshCollections() {

    this.setData({
      currentPage: 1,
      collections: [],
      refreshing: true
    }, () => {
      this.loadMyCollections()
    })
  },

  // 滚动加载更多
  onScrollToLower() {

    if (this.data.hasMore && !this.data.loading) {
      this.setData({
        currentPage: this.data.currentPage + 1
      }, () => {
        this.loadMyCollections()
      })
    }
  },

  // 下拉刷新
  onRefresh() {

    this.refreshCollections()
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

  // 点击收藏项
  onCollectionTap(e) {
    const postId = e.currentTarget.dataset.postid
    // 跳转到帖子详情页
    wx.navigateTo({
      url: `/pages/post/detail?id=${postId}`
    })
  },

  // 取消收藏
  async onCancelCollect(e) {
    const postId = e.currentTarget.dataset.postid
    const index = e.currentTarget.dataset.index
    
    wx.showModal({
      title: '确认取消',
      content: '确定要取消收藏这个帖子吗？',
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
            
            const response = await api.cancelCollect(postId)
            if (response.code === 200) {
              wx.showToast({
                title: '已取消收藏',
                icon: 'success'
              })
              
              // 从列表中移除
              const newCollections = [...this.data.collections]
              newCollections.splice(index, 1)
              this.setData({
                collections: newCollections
              })
            } else {
              throw new Error(response.message || '取消收藏失败')
            }
          } catch (error) {
            wx.showToast({
              title: error.message || '取消失败',
              icon: 'none'
            })
          }
        }
      }
    })
  },

  // 查看详情
  onViewPost(e) {
    const postId = e.currentTarget.dataset.postid
    wx.navigateTo({
      url: `/pages/post/detail?id=${postId}`
    })
  },

  // 分享
  onShareAppMessage() {
    return {
      title: '我的收藏',
      path: '/pages/profile/mycollections/mycollections'
    }
  }
})

// pages/index/index.js
const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')
const { debounce, throttle, formatCount } = require('../../utils/util.js')

// 格式化时间显示
const formatTime = (timeString) => {
  if (!timeString) return ''
  
  const date = new Date(timeString)
  const now = new Date()
  const diff = now - date
  
  // 小于 1 分钟
  if (diff < 60000) {
    return '刚刚'
  }
  
  // 小于 1 小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  
  // 小于 1 天
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  
  // 小于 3 天
  if (diff < 259200000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  
  // 超过 3 天，显示具体日期
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
}

// 网络状态监听
const networkListener = {
  listener: null,
  
  start(callback) {
    // 监听网络状态变化
    this.listener = wx.onNetworkStatusChange((res) => {
      if (callback && typeof callback === 'function') {
        callback(res.isConnected, res.networkType)
      }
    })
    
    // 获取当前网络状态
    wx.getNetworkType({
      success: (res) => {
        if (callback && typeof callback === 'function') {
          callback(res.networkType !== 'none', res.networkType)
        }
      }
    })
  },
  
  stop() {
    if (this.listener) {
      wx.offNetworkStatusChange(this.listener)
      this.listener = null
    }
  }
}

Page({
  data: {
    // 分类筛选
    categories: [],
    selectedCategory: 'all',
    
    // 地理位置相关
    currentLocation: null,
    radius: 5, // 默认 5 公里范围
    
    // 帖子列表
    posts: [],
    page: 1,
    size: 10,
    hasMore: true,
    loading: false,
    
    // 下拉刷新和上拉加载
    refreshing: false,
    
    // 筛选条件
    filters: {
      sortBy: 'time' // time 时间，hot 热度，nearby 附近
    },
    
    // 防止重复操作
    liking: false,
    collecting: false,
    navigating: false,
    
    // 防抖定时器
    debounceTimers: {},
    
    // 筛选工具栏显示状态
    showFilterBar: true,
    lastScrollTop: 0,
    ignoreScrollHide: false  // 是否忽略滚动隐藏
  },

  onLoad() {
    // 启动网络状态监听
    networkListener.start((isConnected, networkType) => {
      if (!isConnected) {
        wx.showToast({
          title: '网络连接已断开',
          icon: 'none',
          duration: 2000
        })
      }
    })
    
    this.initPage()
  },
  
  onUnload() {
    // 停止网络监听
    networkListener.stop()
  },

  onShow() {
    // 每次显示页面时刷新数据
    if (auth.isLoggedIn()) {
      this.refreshData()
      // 同步用户操作状态
      this.syncUserActions()
    }
  },
  
  // 同步用户操作状态
  syncUserActions() {
    // 这里可以添加从服务器获取用户最新点赞、收藏状态的逻辑
    // 暂时保留本地状态
  },

  // 初始化页面
  async initPage() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    
    // 检查认证状态
    const authStatus = auth.checkAuthStatus()
    
    if (authStatus === 1) {
      // 待审核状态：清除token并跳转到登录页
      wx.removeStorageSync('user_token')
      wx.removeStorageSync('user_info')
      
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    } else if (authStatus === 0 || authStatus === 3) {
      // 未认证或认证失败：跳转到实名认证页
      wx.redirectTo({
        url: '/pages/profile/auth/auth'
      })
      return
    }
      
    await this.getLocation()
    await this.getCategories()
    await this.loadPosts()
  },

  // 获取当前位置
  getLocation() {
    return new Promise((resolve) => {
      wx.getLocation({
        type: 'gcj02',
        success: (res) => {
          this.setData({
            currentLocation: {
              longitude: res.longitude,
              latitude: res.latitude
            }
          })
          resolve(res)
        },
        fail: (error) => {
          // 获取位置失败时使用默认位置
          this.setData({
            currentLocation: {
              longitude: 116.404,
              latitude: 39.915
            }
          })
          // 显示温和的提示
          if (error.errMsg && !error.errMsg.includes('cancel')) {
            wx.showToast({
              title: '无法获取位置信息，将使用默认位置',
              icon: 'none',
              duration: 2000
            })
          }
          resolve()
        }
      })
    })
  },

  // 获取分类列表
  async getCategories() {
    try {
      const response = await api.getCategoryList()
      const categories = response.data || []
        
      // 添加"全部"选项（使用 categoryId 字段）
      const allCategories = [
        { categoryId: 'all', name: '全部' },
        ...categories
      ]
        
      this.setData({ categories: allCategories })
    } catch (error) {
      // 如果获取失败，只显示"全部"选项
      this.setData({ 
        categories: [
          { categoryId: 'all', name: '全部' }
        ] 
      })
      wx.showToast({
        title: '分类加载失败',
        icon: 'none'
      })
    }
  },

  // 加载帖子列表
  async loadPosts(refresh = false) {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      const page = refresh ? 1 : this.data.page
      
      const params = {
        page: parseInt(page) || 1,
        size: parseInt(this.data.size) || 10
      }
      
      // 添加筛选条件（只添加有效的 categoryId）
      if (this.data.selectedCategory !== 'all' && 
          this.data.selectedCategory !== null && 
          this.data.selectedCategory !== undefined) {
        const categoryId = parseInt(this.data.selectedCategory)
        if (!isNaN(categoryId)) {
          params.categoryId = categoryId
        }
      }
      
      // 根据排序方式调用不同的 API
      let response;
      switch (this.data.filters.sortBy) {
        case 'hot':
          // 热度排序：调用 /resident/post/list/hot
          response = await api.getHotPosts(params)
          break
        case 'nearby':
          // 附近排序：调用 /resident/post/list/nearby
          // 添加位置信息
          if (this.data.currentLocation) {
            params.longitude = this.data.currentLocation.longitude
            params.latitude = this.data.currentLocation.latitude
            params.radius = this.data.radius
            response = await api.getNearbyPosts(params)
          } else {
            // 如果没有位置信息，降级为时间排序
            response = await api.getPostList(params)
            wx.showToast({
              title: '请开启定位权限以查看附近帖子',
              icon: 'none',
              duration: 2000
            })
          }
          break
        case 'time':
        default:
          // 时间排序：调用 /resident/post/list/time
          response = await api.getPostsByTime(params)
          break
      }
      
      const newPosts = response.data.records || response.data.list || response.data || []
      const total = response.data.total || 0
      const hasMore = page * this.data.size < total
      
      // 处理帖子数据，添加默认值和格式化时间
      const processedPosts = newPosts.map((post, index) => {
        // 兼容多种置顶字段命名：isTop, is_top, isTopPost
        let isTopValue = 0
        if (post.isTop === 1 || post.isTop === true) {
          isTopValue = 1
        } else if (post.is_top === 1 || post.is_top === true) {
          isTopValue = 1
        } else if (post.isTopPost === 1 || post.isTopPost === true) {
          isTopValue = 1
        }
        
        return {
          ...post,
          avatarUrl: (post.userInfo && post.userInfo.avatarUrl) || '/images/default-avatar.png',
          nickname: (post.userInfo && post.userInfo.nickname) || '匿名用户',
          publishTime: formatTime(post.publishTime || post.createTime),
          likeCount: post.likeCount || 0,
          commentCount: post.commentCount || 0,
          collectCount: post.collectCount || 0,
          formattedLikeCount: formatCount(post.likeCount || 0),
          formattedCommentCount: formatCount(post.commentCount || 0),
          formattedCollectCount: formatCount(post.collectCount || 0),
          isLiked: post.isLiked || false,
          isCollected: post.isCollected || false,
          imageUrls: post.imageUrls || [],
          location: post.location || '',
          distance: post.distance || 0,
          // 置顶字段（统一为 isTop）
          isTop: isTopValue,
          // 热度排序时添加排名指示
          rank: this.data.filters.sortBy === 'hot' ? index + 1 : null,
          // 附近排序时格式化距离
          formattedDistance: post.distance ? `${post.distance.toFixed(1)}km` : '',
          // 截取内容预览（最多100个字符）
          contentPreview: this.truncateContent(post.content, 100)
        }
      })
      
      // 对已置顶的帖子优先显示（置顶帖子排在最前面）
      const sortedPosts = processedPosts.sort((a, b) => {
        // 如果 a 是置顶且 b 不是，a 排前面
        if (a.isTop === 1 && b.isTop !== 1) return -1
        // 如果 b 是置顶且 a 不是，b 排前面
        if (b.isTop === 1 && a.isTop !== 1) return 1
        // 否则保持原有顺序
        return 0
      })
      
      this.setData({
        posts: refresh ? sortedPosts : [...this.data.posts, ...sortedPosts],
        page: page + 1,
        hasMore: hasMore,
        refreshing: false
      })
      
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
      this.setData({ refreshing: false })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 刷新数据
  refreshData() {
    this.setData({
      page: 1,
      posts: [],
      hasMore: true,
      refreshing: true
    })
    this.loadPosts(true).then(() => {
      // 如果是页面级下拉刷新，需要手动停止
      wx.stopPullDownRefresh()
      setTimeout(() => {
        this.setData({ refreshing: false })
      }, 500)
    }).catch(() => {
      wx.stopPullDownRefresh()
      this.setData({ refreshing: false })
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.refreshData()
  },

  // scroll-view 下拉刷新
  onRefresh() {
    this.setData({ refreshing: true })
    this.loadPosts(true).then(() => {
      setTimeout(() => {
        this.setData({ refreshing: false })
      }, 500)
    }).catch(() => {
      this.setData({ refreshing: false })
    })
  },

  // 上拉加载更多
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadPosts()
    }
  },

  // 页面滚动监听
  onPageScroll(e) {
    const scrollTop = e.detail.scrollTop
    const lastScrollTop = this.data.lastScrollTop
    
    // 如果设置了忽略滚动隐藏，则不执行隐藏逻辑
    if (!this.data.ignoreScrollHide) {
      // 如果向下滚动（scrollTop 增加）且滚动距离超过 50px，隐藏筛选栏
      if (scrollTop > lastScrollTop && scrollTop > 50) {
        if (this.data.showFilterBar) {
          this.setData({ showFilterBar: false })
        }
      }
    }
    
    // 更新上次滚动位置
    this.setData({ lastScrollTop: scrollTop })
  },
  
  // 切换筛选工具栏显示/隐藏
  onToggleFilterBar() {
    const willShow = !this.data.showFilterBar
    
    this.setData({ showFilterBar: willShow })
    
    // 如果是显示筛选栏，设置忽略滚动隐藏标志
    if (willShow) {
      this.setData({ ignoreScrollHide: true })
      
      // 500ms 后恢复滚动监听
      setTimeout(() => {
        this.setData({ ignoreScrollHide: false })
      }, 500)
    }
  },

  // 切换分类
  onCategoryChange(e) {
    const categoryId = e.currentTarget.dataset.id
    // 验证分类 ID 有效性
    if (categoryId === undefined || categoryId === null) {
      return
    }
    
    this.setData({
      selectedCategory: categoryId,
      page: 1,
      posts: []
    })
    this.loadPosts(true)
  },

  // 排序方式变更
  onSortChange(e) {
    const sortBy = e.currentTarget.dataset.sort
    this.setData({
      'filters.sortBy': sortBy,
      page: 1,
      posts: []
    })
    this.loadPosts(true)
  },

  // 筛选类型变更（根据动态分类进行筛选）
  onFilterTypeChange(e) {
    const category = e.currentTarget.dataset.category
      
    // 如果选择的是"全部"
    if (category === 'all') {
      this.setData({
        selectedCategory: 'all',
        page: 1,
        posts: []
      }, () => {
        this.loadPosts(true)
      })
    } else {
      // 否则使用选中的分类 ID
      const categoryId = parseInt(category)
      if (!isNaN(categoryId)) {
        this.setData({
          selectedCategory: categoryId,
          page: 1,
          posts: []
        }, () => {
          this.loadPosts(true)
        })
      }
    }
  },

  // 排序方式变更
  onSortChange(e) {
    const sortBy = e.currentTarget.dataset.sort
    this.setData({
      'filters.sortBy': sortBy,
      page: 1,
      posts: []
    })
    this.loadPosts(true)
  },

  // 点赞帖子（带防抖）
  onLikePost: debounce(function(e) {
    const postId = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    const post = this.data.posts[index]
    
    // 防止重复点击
    if (this.data.liking) return
    
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再进行点赞',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: '/pages/login/login'
            })
          }
        }
      })
      return
    }
    
    this.performLikeOperation(postId, index, post)
  }, 500),
  
  // 执行点赞操作
  async performLikeOperation(postId, index, post) {
    try {
      this.setData({ liking: true })
      
      // 更新本地状态（先更新 UI，后调用 API）
      const updatedPosts = [...this.data.posts]
      const newLikeCount = post.isLiked ? post.likeCount - 1 : post.likeCount + 1
      updatedPosts[index] = {
        ...post,
        isLiked: !post.isLiked,
        likeCount: newLikeCount,
        formattedLikeCount: formatCount(newLikeCount)
      }
      
      this.setData({ posts: updatedPosts })
      
      // 调用 API
      await api.toggleLike({
        targetType: 'post',
        targetId: postId,
        isLike: !post.isLiked
      })
      
      wx.showToast({
        title: post.isLiked ? '已取消点赞' : '点赞成功',
        icon: 'success',
        duration: 1500
      })
      
    } catch (error) {
      // 恢复原状态
      const rollbackPosts = [...this.data.posts]
      rollbackPosts[index] = post
      this.setData({ posts: rollbackPosts })
      
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    } finally {
      // 延迟重置操作锁，让用户看到反馈
      setTimeout(() => {
        this.setData({ liking: false })
      }, 500)
    }
  },

  // 收藏帖子（带防抖）
  onCollectPost: debounce(function(e) {
    const postId = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    const post = this.data.posts[index]
    
    // 防止重复点击
    if (this.data.collecting) return
    
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再进行收藏',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: '/pages/login/login'
            })
          }
        }
      })
      return
    }
    
    this.performCollectOperation(postId, index, post)
  }, 500),
  
  // 执行收藏操作
  async performCollectOperation(postId, index, post) {
    try {
      this.setData({ collecting: true })
      
      // 更新本地状态（先更新 UI，后调用 API）
      const updatedPosts = [...this.data.posts]
      const newCollectCount = post.isCollected ? post.collectCount - 1 : post.collectCount + 1
      updatedPosts[index] = {
        ...post,
        isCollected: !post.isCollected,
        collectCount: newCollectCount,
        formattedCollectCount: formatCount(newCollectCount)
      }
      
      this.setData({ posts: updatedPosts })
      
      // 调用 API
      await api.toggleCollect({
        targetType: 'post',
        targetId: postId,
        isCollect: !post.isCollected
      })
      
      wx.showToast({
        title: post.isCollected ? '已取消收藏' : '收藏成功',
        icon: 'success',
        duration: 1500
      })
      
    } catch (error) {
      // 恢复原状态
      const rollbackPosts = [...this.data.posts]
      rollbackPosts[index] = post
      this.setData({ posts: rollbackPosts })
      
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    } finally {
      // 延迟重置操作锁，让用户看到反馈
      setTimeout(() => {
        this.setData({ collecting: false })
      }, 500)
    }
  },

  // 跳转到帖子详情
  goToPostDetail(e) {
    const postId = e.currentTarget.dataset.id
    
    // 防止重复跳转
    if (this.navigating) return
    
    this.navigating = true
    
    wx.navigateTo({
      url: `/pages/post/detail?postId=${postId}`,
      complete: () => {
        // 延迟重置导航状态
        setTimeout(() => {
          this.navigating = false
        }, 500)
      }
    })
  },
  
  // 跳转到用户主页
  goToUserProfile(e) {
    const userId = e.currentTarget.dataset.userid
    
    // 防止重复跳转
    if (this.navigating) return
    
    this.navigating = true
    
    wx.navigateTo({
      url: `/pages/profile/profile/profile?userId=${userId}`,
      complete: () => {
        // 延迟重置导航状态
        setTimeout(() => {
          this.navigating = false
        }, 500)
      }
    })
  },
  
  // 阻止事件冒泡（用于点赞和收藏按钮）
  stopPropagation() {
    // 空函数，仅用于阻止事件冒泡
  },
  
  // 截取内容预览
  truncateContent(content, maxLength) {
    if (!content) return ''
    if (content.length <= maxLength) return content
    return content.substring(0, maxLength) + '...'
  }
})
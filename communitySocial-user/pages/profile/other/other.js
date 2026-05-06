// pages/profile/other/other.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    userId: '', // 目标用户 ID
    userInfo: null, // 目标用户信息
    isSelf: false, // 是否是自己的主页
    lastVisitedUserId: '', // 记录上次访问的用户 ID，用于检测切换
    
    // 统计数据
    stats: {
      postCount: 0,
      followerCount: 0,
      followingCount: 0
    },
    
    // 关注状态
    isFollowed: false,
    
    // 标签切换
    currentTab: 0, // 0: 帖子，1: 关注，2: 粉丝
    
    // 帖子列表
    posts: [],
    postsPage: 1,
    postsSize: 5,
    postsHasMore: true,
    
    // 关注列表
    followingList: [],
    followingPage: 1,
    followingSize: 10,
    followingHasMore: true,
    
    // 粉丝列表
    followerList: [],
    followerPage: 1,
    followerSize: 10,
    followerHasMore: true,
    
    // 加载状态
    loading: false,
    refreshing: false
  },

  onLoad(options) {


    // 初始化当前用户信息
    const currentUserInfo = auth.getUserInfo()
    this.setData({ currentUserInfo })
    
    if (options.userId) {
      const targetUserId = options.userId
      this.setData({ 
        userId: targetUserId,
        lastVisitedUserId: targetUserId // 记录首次加载的 userId
      })
      
      // 判断是否是自己的主页
      const isSelf = currentUserInfo && currentUserInfo.userId === targetUserId
      this.setData({ isSelf })


      // 加载用户信息
      this.loadUserInfo()
    } else {
      wx.showToast({
        title: '用户 ID 缺失',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }
  },
  
  onShow() {


    // 检测是否切换到不同的用户
    if (this.data.userId && this.data.userId !== this.data.lastVisitedUserId) {

      this.refreshForNewUser()
    }
    
    // 页面显示时刷新关注状态
    if (!this.data.isSelf && this.data.userInfo) {
      this.checkFollowStatus()
    }
  },
  
  // 为新用户刷新数据
  async refreshForNewUser() {
    try {

      // 更新记录
      this.setData({
        lastVisitedUserId: this.data.userId,
        loading: true
      })
      
      // 重置所有数据
      this.setData({
        userInfo: null,
        posts: [],
        followingList: [],
        followerList: [],
        postsPage: 1,
        followingPage: 1,
        followerPage: 1,
        currentTab: 0
      })
      
      // 重新加载用户信息
      await this.loadUserInfo()

    } catch (error) {
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },
  
  // 加载用户信息
  async loadUserInfo() {
    try {
      this.setData({ loading: true })

      // 调用新的 API，一次性获取所有数据
      const response = await api.getUserProfileComplete(this.data.userId, 1, 5)
      const completeData = response.data

      if (completeData) {
        // 1. 用户基本信息
        const userInfo = completeData.userInfo || {}
        
        // 处理头像 URL
        if (userInfo.avatarUrl) {
          userInfo.avatarUrl = api.handleImageUrl(userInfo.avatarUrl)
        }
        
        // 2. 统计数据
        const statistics = completeData.statistics || {
          postCount: 0,
          followerCount: 0,
          followingCount: 0
        }
        
        // 3. 帖子列表
        const postsData = completeData.posts || { records: [], total: 0 }
        const posts = this.processPosts(postsData.records || [])
        const postsHasMore = (postsData.records || []).length === this.data.postsSize
        
        // 4. 关注列表
        const followingData = completeData.followingList || { records: [], total: 0 }
        const followingList = await this.processUserList(followingData.records || [])
        const followingHasMore = (followingData.records || []).length === this.data.followingSize
        
        // 5. 粉丝列表
        const followerData = completeData.followerList || { records: [], total: 0 }
        const followerList = await this.processUserList(followerData.records || [])
        const followerHasMore = (followerData.records || []).length === this.data.followerSize
        
        // 更新所有数据
        this.setData({
          userInfo,
          'stats.postCount': statistics.postCount || 0,
          'stats.followerCount': statistics.followerCount || 0,
          'stats.followingCount': statistics.followingCount || 0,
          posts,
          postsHasMore,
          followingList,
          followingHasMore,
          followerList,
          followerHasMore
        })


        // 如果不是自己的主页，检查关注状态
        if (!this.data.isSelf) {
          await this.checkFollowStatus()
        }
      }
      
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },
  
  // 加载所有数据
  async loadAllData() {
    try {

      // 并发加载帖子、关注列表、粉丝列表
      const [postsResult, followingResult, followerResult] = await Promise.allSettled([
        this.loadPosts(true),
        this.loadFollowingList(true),
        this.loadFollowerList(true)
      ])
      
      // 检查各个加载结果
      if (postsResult.status === 'fulfilled') {

      } else {
      }
      
      if (followingResult.status === 'fulfilled') {

      } else {
      }
      
      if (followerResult.status === 'fulfilled') {

      } else {
      }

    } catch (error) {
      // 不显示错误，因为个别数据加载失败不影响其他数据
    }
  },
  
  // 检查关注状态
  async checkFollowStatus() {
    try {
      const response = await api.getFollowStatus(this.data.userId)
      // 后端返回的是 { isFollowed: boolean } 格式
      const isFollowed = response.data.isFollowed || false
      this.setData({ isFollowed })

    } catch (error) {
      // 不显示错误提示，避免影响用户体验
    }
  },
  
  // 切换关注状态
  async onToggleFollow() {
    if (!auth.isLoggedIn()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
    
    try {
      const currentStatus = this.data.isFollowed
      
      if (currentStatus) {
        // 取消关注
        await api.unfollowUser(this.data.userId)
        this.setData({ isFollowed: false })
        wx.showToast({
          title: '已取消关注',
          icon: 'success'
        })
      } else {
        // 添加关注
        await api.followUser(this.data.userId)
        this.setData({ isFollowed: true })
        wx.showToast({
          title: '关注成功',
          icon: 'success'
        })
      }
      
      // 更新统计数据
      this.updateFollowerCount(!currentStatus)
      
      // 刷新关注列表和粉丝列表
      await this.loadFollowingList(true)
      await this.loadFollowerList(true)
      
    } catch (error) {
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  },
  
  // 更新粉丝数量
  updateFollowerCount(increase) {
    const currentCount = this.data.stats.followerCount
    this.setData({
      'stats.followerCount': increase ? currentCount + 1 : currentCount - 1
    })
  },
  
  // 切换标签
  onSwitchTab(e) {
    const tab = parseInt(e.currentTarget.dataset.tab)

    if (tab === this.data.currentTab) return
    
    this.setData({ 
      currentTab: tab,
      refreshing: false
    })
    
    // 根据标签加载数据
    switch(tab) {
      case 0:
        if (this.data.posts.length === 0) {
          this.loadPosts()
        }
        break
      case 1:
        if (this.data.followingList.length === 0) {
          this.loadFollowingList()
        }
        break
      case 2:
        if (this.data.followerList.length === 0) {
          this.loadFollowerList()
        }
        break
    }
  },
  
  // 加载帖子列表
  async loadPosts(refresh = false) {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      let page = refresh ? 1 : this.data.postsPage
      
      const params = {
        page: page,
        size: this.data.postsSize
      }
      
      const response = await api.getUserPosts(this.data.userId, params)
      const newPosts = response.data.records || response.data.list || []
      const total = response.data.total || 0

      // 处理帖子数据
      const processedPosts = newPosts.map(post => ({
        ...post,
        publishTime: this.formatTime(post.publishTime),
        imageUrls: post.imageUrls ? (Array.isArray(post.imageUrls) ? post.imageUrls : []) : [],
        formattedLikeCount: post.likeCount || 0,
        formattedCommentCount: post.commentCount || 0,
        formattedCollectCount: post.collectCount || 0
      }))
      
      const hasMore = processedPosts.length === this.data.postsSize && 
                     this.data.posts.length + processedPosts.length < total
      
      this.setData({
        posts: refresh ? processedPosts : [...this.data.posts, ...processedPosts],
        postsPage: page + 1,
        postsHasMore: hasMore,
        refreshing: false,
        // 更新统计数据
        'stats.postCount': total
      })
      
    } catch (error) {
      // 静默失败，不影响其他数据加载
    } finally {
      this.setData({ loading: false })
    }
  },
  
  // 加载关注列表
  async loadFollowingList(refresh = false) {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      let page = refresh ? 1 : this.data.followingPage
      
      const response = await api.getFollowingList(this.data.userId, {
        page: page,
        size: this.data.followingSize
      })
      
      const newList = response.data.records || response.data.list || []
      const total = response.data.total || 0

      // 处理数据
      const processedList = newList.map(user => ({
        ...user,
        avatarUrl: user.avatarUrl ? api.handleImageUrl(user.avatarUrl) : '/images/default-avatar.png',
        followerCount: user.followerCount || 0,
        followingCount: user.followingCount || 0,
        postCount: user.postCount || 0
      }))
      
      const hasMore = processedList.length === this.data.followingSize &&
                     this.data.followingList.length + processedList.length < total
      
      this.setData({
        followingList: refresh ? processedList : [...this.data.followingList, ...processedList],
        followingPage: page + 1,
        followingHasMore: hasMore,
        refreshing: false,
        // 更新统计数据
        'stats.followingCount': total
      })
      
    } catch (error) {
      // 静默失败，不影响其他数据加载
    } finally {
      this.setData({ loading: false })
    }
  },
  
  // 加载粉丝列表
  async loadFollowerList(refresh = false) {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      let page = refresh ? 1 : this.data.followerPage
      
      const response = await api.getFollowerList(this.data.userId, {
        page: page,
        size: this.data.followerSize
      })
      
      const newList = response.data.records || response.data.list || []
      const total = response.data.total || 0

      // 处理数据
      const processedList = newList.map(user => ({
        ...user,
        avatarUrl: user.avatarUrl ? api.handleImageUrl(user.avatarUrl) : '/images/default-avatar.png',
        followerCount: user.followerCount || 0,
        followingCount: user.followingCount || 0,
        postCount: user.postCount || 0
      }))
      
      const hasMore = processedList.length === this.data.followerSize &&
                     this.data.followerList.length + processedList.length < total
      
      this.setData({
        followerList: refresh ? processedList : [...this.data.followerList, ...processedList],
        followerPage: page + 1,
        followerHasMore: hasMore,
        refreshing: false,
        // 更新统计数据
        'stats.followerCount': total
      })
      
    } catch (error) {
      // 静默失败，不影响其他数据加载
    } finally {
      this.setData({ loading: false })
    }
  },
  
  // 下拉刷新
  async onRefresh() {

    this.setData({ refreshing: true })
    
    switch(this.data.currentTab) {
      case 0:
        await this.loadPosts(true)
        break
      case 1:
        await this.loadFollowingList(true)
        break
      case 2:
        await this.loadFollowerList(true)
        break
    }
  },
  
  // 上拉加载更多
  async onScrollToLower() {

    if (this.data.loading) return
    
    switch(this.data.currentTab) {
      case 0:
        if (this.data.postsHasMore) {
          await this.loadPosts()
        }
        break
      case 1:
        if (this.data.followingHasMore) {
          await this.loadFollowingList()
        }
        break
      case 2:
        if (this.data.followerHasMore) {
          await this.loadFollowerList()
        }
        break
    }
  },
  
  // 点击帖子
  onPostTap(e) {
    const postId = e.currentTarget.dataset.postid
    wx.navigateTo({
      url: `/pages/post/detail?postId=${postId}`
    })
  },
  
  // 点击用户
  onUserTap(e) {
    const userId = e.currentTarget.dataset.userid

    if (!userId) {
      return
    }
    
    // 如果点击的是当前用户，跳转到个人主页
    if (userId === this.data.userId) {

      wx.navigateTo({
        url: '/pages/profile/profile/profile'
      })
      return
    }

    // 使用 wx.navigateTo 打开新用户主页（会创建新的页面实例）
    wx.navigateTo({
      url: '/pages/profile/other/other?userId=' + userId,
      fail: (err) => {
        wx.showToast({
          title: '打开失败',
          icon: 'none'
        })
      }
    })
  },
  
  // 关注列表中切换关注状态
  async onToggleFollowUser(e) {
    if (!auth.isLoggedIn()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
    
    const userId = e.currentTarget.dataset.userid
    const index = e.currentTarget.dataset.index
    const currentTab = this.data.currentTab
    const listKey = currentTab === 1 ? 'followingList' : 'followerList'
    const list = this.data[listKey]
    const user = list[index]
    
    // 保存当前状态用于回滚
    const oldIsFollowed = user.isFollowed || false
    const newIsFollowed = !oldIsFollowed
    
    // 立即更新 UI（乐观更新）
    list[index].isFollowed = newIsFollowed
    this.setData({
      [listKey]: list
    })
    
    try {
      if (oldIsFollowed) {
        // 取消关注
        await api.unfollowUser(userId)

        wx.showToast({
          title: '已取消关注',
          icon: 'success'
        })
      } else {
        // 添加关注
        await api.followUser(userId)

        wx.showToast({
          title: '关注成功',
          icon: 'success'
        })
      }
      
      // 成功后刷新列表数据（确保数据准确性）
      await this.loadFollowingList(true)
      await this.loadFollowerList(true)
      
    } catch (error) {
      // 如果是"已关注该用户"的错误，说明用户已经关注了，不需要回滚
      const isAlreadyFollowedError = error.message && error.message.includes('已关注')
      
      if (isAlreadyFollowedError) {
        // 保持已关注状态，不回滚

        wx.showToast({
          title: '已关注',
          icon: 'none'
        })
      } else {
        // 其他错误才回滚状态
        list[index].isFollowed = oldIsFollowed
        this.setData({
          [listKey]: list
        })
        
        wx.showToast({
          title: error.message || '操作失败',
          icon: 'none'
        })
      }
    }
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
  
  // 处理帖子数据
  processPosts(posts) {
    return posts.map(post => {
      // 调试日志：查看帖子类型字段
      return {
        ...post,
        publishTime: this.formatTime(post.publishTime),
        imageUrls: post.imageUrls ? (Array.isArray(post.imageUrls) ? post.imageUrls : []) : [],
        formattedLikeCount: post.likeCount || 0,
        formattedCommentCount: post.commentCount || 0,
        formattedCollectCount: post.collectCount || 0,
        // 确保 type 字段存在，默认为 3（资讯）
        type: post.type !== undefined ? post.type : 3
      }
    })
  },
  
  // 处理用户列表数据（关注/粉丝）
  async processUserList(users) {
    const currentUserId = wx.getStorageSync('userId')
    
    // 先处理基本数据
    const processedUsers = users.map(user => ({
      ...user,
      avatarUrl: user.avatarUrl ? api.handleImageUrl(user.avatarUrl) : '/images/default-avatar.png',
      followerCount: user.followerCount || 0,
      followingCount: user.followingCount || 0,
      postCount: user.postCount || 0,
      isFollowed: user.userId === currentUserId ? true : false
    }))
    
    // 批量检查关注状态（排除自己）
    const usersToCheck = processedUsers.filter(user => user.userId !== currentUserId)
    
    if (usersToCheck.length > 0) {
      // 并发检查每个用户的关注状态
      const followStatusPromises = usersToCheck.map(async (user) => {
        try {
          const response = await api.getFollowStatus(user.userId)
          return {
            userId: user.userId,
            isFollowed: response.data || false
          }
        } catch (error) {
          return {
            userId: user.userId,
            isFollowed: false
          }
        }
      })
      
      const followStatuses = await Promise.all(followStatusPromises)
      
      // 更新关注状态
      followStatuses.forEach(status => {
        const user = processedUsers.find(u => u.userId === status.userId)
        if (user) {
          user.isFollowed = status.isFollowed
        }
      })
    }
    
    return processedUsers
  },
  
  // 分享功能
  onShareAppMessage() {
    return {
      title: `${this.data.userInfo?.nickname || '用户'}的主页`,
      path: `/pages/profile/other?userId=${this.data.userId}`
    }
  }
})

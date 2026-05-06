// pages/post/detail.js
const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')
const { debounce, throttle, formatCount, formatDate } = require('../../utils/util.js')
const tencentMap = require('../../utils/tencentMap.js')

Page({
  data: {
    postId: '',
    postDetail: null,
    comments: [],
    page: 1,
    size: 10,
    hasMore: true,
    loading: false,
    refreshing: false,
    
    // 用户信息
    userInfo: null,
    
    // 评论相关
    commentContent: '',
    isSubmittingComment: false,
    showCommentInput: false,
    replyToComment: null, // 回复的目标评论
    editingComment: null, // 正在编辑的评论
    
    // 评论展开控制
    expandedComments: {}, // 控制长评论的展开/收起
    
    // 举报相关
    showReportModal: false,
    reportReason: '',
    
    // 二手交易相关
    transactionModes: ['仅支持自提', '仅支持快递', '自提和快递均可']
  },

  onLoad(options) {
    // 初始化用户信息
    const userInfo = auth.getUserInfo()
    this.setData({ userInfo })
    
    // 检查登录状态，如果已登录但用户信息不完整，则尝试刷新
    if (auth.isLoggedIn() && (!userInfo || !userInfo.userId)) {

      this.refreshUserInfo()
    }
    
    if (options.postId) {
      this.setData({ postId: options.postId })
      // 获取用户位置并加载帖子详情
      this.loadPostDetailWithDistance()
      this.loadComments()
    }
  },
  
  // 加载帖子详情并计算距离
  async loadPostDetailWithDistance() {
    try {
      // 先获取用户当前位置
      const userLocation = await this.getUserLocation()
      
      // 加载帖子详情
      const response = await api.getPostDetail(this.data.postId)
      const postDetail = response.data


      // 处理头像 URL
      if (postDetail.userInfo && postDetail.userInfo.avatarUrl) {
        postDetail.avatarUrl = api.handleImageUrl(postDetail.userInfo.avatarUrl)
      }
      
      // 处理帖子图片 URL
      if (postDetail.imageUrls && Array.isArray(postDetail.imageUrls)) {
        postDetail.imageUrls = postDetail.imageUrls.map(url => api.handleImageUrl(url))
      }
              
      // 添加格式化数字
      postDetail.formattedLikeCount = formatCount(postDetail.likeCount || 0)
      postDetail.formattedCommentCount = formatCount(postDetail.commentCount || 0)
      postDetail.formattedCollectCount = formatCount(postDetail.collectCount || 0)
      
      // 格式化发布时间
      if (postDetail.publishTime) {
        postDetail.formattedPublishTime = formatDate(postDetail.publishTime)
      }
      
      // 计算距离（如果帖子有位置信息）
      if (userLocation && postDetail.longitude && postDetail.latitude) {
        const distance = tencentMap.calculateDistance(
          userLocation.longitude,
          userLocation.latitude,
          postDetail.longitude,
          postDetail.latitude
        )
        
        // 格式化距离显示
        let distanceText
        if (distance < 1000) {
          distanceText = `${distance}m`
        } else {
          distanceText = `${(distance / 1000).toFixed(1)}km`
        }
        
        postDetail.distanceText = distanceText
        postDetail.distance = distance
      }
      
      this.setData({ postDetail })

    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    }
  },
  
  // 获取用户当前位置
  getUserLocation() {
    return new Promise((resolve, reject) => {
      wx.getLocation({
        type: 'gcj02',
        success: (res) => {
          resolve({
            longitude: res.longitude,
            latitude: res.latitude
          })
        },
        fail: (error) => {
          // 不拒绝，返回 null 让调用方处理
          resolve(null)
        }
      })
    })
  },
  
  // 刷新用户信息
  async refreshUserInfo() {
    try {
      const refreshedUserInfo = await auth.refreshUserInfo()
      this.setData({ userInfo: refreshedUserInfo })

    } catch (error) {
      // 如果刷新失败，清除登录状态
      wx.removeStorageSync('user_token')
      wx.removeStorageSync('user_info')
      this.setData({ userInfo: null })
    }
  },

  // 加载帖子详情
  async loadPostDetail() {
    try {
      const response = await api.getPostDetail(this.data.postId)
      const postDetail = response.data
      
      // 处理头像URL
      if (postDetail.userInfo && postDetail.userInfo.avatarUrl) {
        postDetail.avatarUrl = api.handleImageUrl(postDetail.userInfo.avatarUrl)
      }
      
      // 处理帖子图片URL
      if (postDetail.imageUrls && Array.isArray(postDetail.imageUrls)) {
        postDetail.imageUrls = postDetail.imageUrls.map(url => api.handleImageUrl(url))
      }
              
      // 添加格式化数字
      postDetail.formattedLikeCount = formatCount(postDetail.likeCount || 0)
      postDetail.formattedCommentCount = formatCount(postDetail.commentCount || 0)
      postDetail.formattedCollectCount = formatCount(postDetail.collectCount || 0)
      
      // 格式化发布时间
      if (postDetail.publishTime) {
        postDetail.formattedPublishTime = formatDate(postDetail.publishTime)
      }
      
      this.setData({ postDetail })
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    }
  },

  // 模拟获取帖子详情
  mockGetPostDetail(postId) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          postId: postId,
          title: '这是一个求助帖子标题',
          content: '这里是帖子的详细内容，可能包含很多文字信息，用来描述具体的问题或需求...',
          userId: 'user_123',
          nickname: '热心邻居',
          avatarUrl: '',
          type: 1,
          imageUrls: [
            'https://example.com/image1.jpg',
            'https://example.com/image2.jpg'
          ],
          likeCount: 25,
          commentCount: 12,
          collectCount: 8,
          isLiked: false,
          isCollected: false,
          publishTime: new Date().toISOString(),
          location: '北京市朝阳区',
          distance: 2.5,
          isAnonymous: false
        })
      }, 500)
    })
  },

  // 加载评论列表
  async loadComments(refresh = false) {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      let page = refresh ? 1 : this.data.page
      const response = await api.getComments(this.data.postId, {
        page: page,
        size: this.data.size
      })
      
      const newComments = response.data.records || []
      const hasMore = page * this.data.size < response.data.total
      
      // 处理评论数据
      const processedComments = newComments.map(comment => ({
        ...comment,
        // 处理头像URL
        avatarUrl: comment.userInfo && comment.userInfo.avatarUrl ? 
          api.handleImageUrl(comment.userInfo.avatarUrl) : '',
        // 处理昵称（匿名处理）
        nickname: comment.isAnonymous ? '匿名用户' : 
                 (comment.userInfo ? comment.userInfo.nickname : '未知用户'),
        // 处理父评论
        parentComment: comment.parentComment ? {
          ...comment.parentComment,
          nickname: comment.parentComment.isAnonymous ? '匿名用户' : 
                   (comment.parentComment.userInfo ? comment.parentComment.userInfo.nickname : '未知用户')
        } : null,
        // 格式化评论时间
        formattedCreateTime: comment.createTime ? formatDate(comment.createTime) : ''
      }))
      
      this.setData({
        comments: refresh ? processedComments : [...this.data.comments, ...processedComments],
        page: page + 1,
        hasMore: hasMore,
        refreshing: false
      })
      
    } catch (error) {
      wx.showToast({
        title: error.message || '加载评论失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 模拟获取评论
  mockGetComments(postId, page, size) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const mockComments = []
        const total = 35
        
        for (let i = 0; i < Math.min(size, total - (page - 1) * size); i++) {
          const index = (page - 1) * size + i
          if (index >= total) break
          
          mockComments.push({
            commentId: 'comment_' + index,
            userId: 'user_' + (index % 5 + 1),
            nickname: '用户' + (index % 5 + 1),
            avatarUrl: '',
            content: `这是第${index + 1}条评论内容，可能是对帖子的回复或讨论...`,
            likeCount: Math.floor(Math.random() * 20),
            isLiked: Math.random() > 0.7,
            createTime: new Date(Date.now() - Math.random() * 86400000 * 3).toISOString(),
            parentId: null, // 这里可以是null或父评论ID
            parentComment: null // 如果有回复，这里存放父评论信息
          })
        }
        
        resolve({
          data: {
            records: mockComments,
            total: total,
            page: page,
            size: size
          }
        })
      }, 600)
    })
  },

  // 刷新数据
  refreshData() {
    this.setData({
      page: 1,
      comments: [],
      hasMore: true,
      refreshing: true
    })
    this.loadComments(true)
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.refreshData()
  },

  // 上拉加载更多
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadComments()
    }
  },

  // 点赞帖子（带防抖）
  onLikePost: debounce(async function() {
    if (!auth.isLoggedIn()) {
      this.navigateToLogin()
      return
    }
    
    try {
      const post = this.data.postDetail
      const updatedPost = {
        ...post,
        isLiked: !post.isLiked,
        likeCount: post.isLiked ? post.likeCount - 1 : post.likeCount + 1,
        formattedLikeCount: formatCount(post.isLiked ? post.likeCount - 1 : post.likeCount + 1)
      }
      
      this.setData({ postDetail: updatedPost })
      
      await api.toggleLike({
        targetType: 'post',
        targetId: this.data.postId,
        isLike: !post.isLiked
      })
      
      wx.showToast({
        title: post.isLiked ? '取消点赞' : '点赞成功',
        icon: 'success',
        duration: 1000
      })
      
    } catch (error) {
      // 恢复原状态
      const post = this.data.postDetail
      const rollbackPost = {
        ...post,
        isLiked: !post.isLiked,
        likeCount: post.isLiked ? post.likeCount + 1 : post.likeCount - 1
      }
      this.setData({ postDetail: rollbackPost })
      
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  }, 500),

  // 收藏帖子（带防抖）
  onCollectPost: debounce(async function() {
    if (!auth.isLoggedIn()) {
      this.navigateToLogin()
      return
    }
    
    try {
      const post = this.data.postDetail
      const updatedPost = {
        ...post,
        isCollected: !post.isCollected,
        collectCount: post.isCollected ? post.collectCount - 1 : post.collectCount + 1,
        formattedCollectCount: formatCount(post.isCollected ? post.collectCount - 1 : post.collectCount + 1)
      }
      
      this.setData({ postDetail: updatedPost })
      
      await api.toggleCollect({
        targetType: 'post',
        targetId: this.data.postId,
        isCollect: !post.isCollected
      })
      
      wx.showToast({
        title: post.isCollected ? '取消收藏' : '收藏成功',
        icon: 'success',
        duration: 1000
      })
      
    } catch (error) {
      // 恢复原状态
      const post = this.data.postDetail
      const rollbackPost = {
        ...post,
        isCollected: !post.isCollected,
        collectCount: post.isCollected ? post.collectCount + 1 : post.collectCount - 1
      }
      this.setData({ postDetail: rollbackPost })
      
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  }, 500),

  // 显示评论输入框
  onShowCommentInput(e) {


    if (!auth.isLoggedIn()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        this.navigateToLogin()
      }, 1000)
      return
    }
    
    const replyTo = e.currentTarget.dataset.comment || null
    this.setData({
      showCommentInput: true,
      replyToComment: replyTo,
      editingComment: null,
      commentContent: replyTo ? `@${replyTo.nickname} ` : ''
    })
  },

  // 隐藏评论输入框
  onHideCommentInput() {
    this.setData({
      showCommentInput: false,
      replyToComment: null,
      editingComment: null,
      commentContent: ''
    })
  },

  // 输入评论内容
  onCommentInput(e) {
    const content = e.detail.value


    this.setData({
      commentContent: content
    })
    
    // 调试：检查按钮状态

    // 实时字数统计提示
    if (content.length > 450) {
      wx.showToast({
        title: `还可输入${500 - content.length}个字符`,
        icon: 'none',
        duration: 1000
      })
    }
  },
  
  // 评论输入框获得焦点
  onCommentFocus(e) {

    // 可以在这里添加键盘弹起的处理逻辑
  },
  
  // 评论输入框失去焦点
  onCommentBlur(e) {

    // 可以在这里添加键盘收起的处理逻辑
  },
  
  // 清空评论内容
  onClearComment() {
    this.setData({
      commentContent: '',
      replyToComment: null,
      editingComment: null
    })
  },
  
  // 插入表情
  onInsertEmoji(e) {
    const emoji = e.currentTarget.dataset.emoji
    const currentContent = this.data.commentContent || ''
    this.setData({
      commentContent: currentContent + emoji
    })
  },

  // 提交评论
  async onSubmitComment() {


    // 调试：强制更新按钮状态
    this.setData({ isSubmittingComment: false })
    
    // 检查登录状态
    if (!auth.isLoggedIn()) {

      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        this.navigateToLogin()
      }, 1000)
      return
    }
    
    // 验证用户信息完整性
    const userInfo = auth.getUserInfo()

    if (!userInfo || !userInfo.userId) {

      wx.showToast({
        title: '用户信息异常，请重新登录',
        icon: 'none'
      })
      setTimeout(() => {
        this.navigateToLogin()
      }, 1000)
      return
    }
    
    if (!this.data.commentContent.trim()) {
      wx.showToast({
        title: '请输入评论内容',
        icon: 'none'
      })
      return
    }
    
    if (this.data.commentContent.trim().length > 500) {
      wx.showToast({
        title: '评论内容不能超过500个字符',
        icon: 'none'
      })
      return
    }
    
    this.setData({ isSubmittingComment: true })
    
    try {
      let response
      
      // 如果是编辑评论
      if (this.data.editingComment) {
        response = await api.updateComment({
          commentId: this.data.editingComment.commentId,
          content: this.data.commentContent.trim()
        })
        wx.showToast({
          title: '编辑成功',
          icon: 'success'
        })
      } else {
        // 新增评论
        const commentData = {
          postId: this.data.postId,
          content: this.data.commentContent.trim()
        }
        
        // 如果是回复评论，添加parentId
        if (this.data.replyToComment) {
          commentData.parentId = this.data.replyToComment.commentId
        }

        response = await api.createComment(commentData)
        wx.showToast({
          title: '评论成功',
          icon: 'success'
        })
      }

      // 清空输入框并隐藏
      this.setData({
        commentContent: '',
        showCommentInput: false,
        replyToComment: null,
        editingComment: null
      })
      
      // 刷新评论列表
      this.refreshData()
      
    } catch (error) {
      // 特殊错误处理
      if (error.code === 401) {
        // token过期或无效
        wx.showToast({
          title: '登录已过期，请重新登录',
          icon: 'none',
          duration: 2000
        })
        // 清除本地存储的登录信息
        wx.removeStorageSync('user_token')
        wx.removeStorageSync('user_info')
        // 延迟跳转到登录页
        setTimeout(() => {
          wx.redirectTo({
            url: '/pages/login/login'
          })
        }, 2000)
      } else if (error.code === 403) {
        wx.showToast({
          title: '权限不足，无法发表评论',
          icon: 'none'
        })
      } else if (error.code === 500) {
        wx.showToast({
          title: '服务器错误，请稍后再试',
          icon: 'none'
        })
      } else {
        wx.showToast({
          title: error.message || '评论失败，请重试',
          icon: 'none'
        })
      }
    } finally {
      this.setData({ isSubmittingComment: false })
    }
  },

  // 模拟创建评论
  mockCreateComment(commentData) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (Math.random() > 0.1) {
          resolve({ commentId: 'comment_' + Date.now() })
        } else {
          reject(new Error('评论失败'))
        }
      }, 1000)
    })
  },

  // 点赞评论
  async onLikeComment(e) {
    if (!auth.isLoggedIn()) {
      this.navigateToLogin()
      return
    }
    
    const commentId = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    const comment = this.data.comments[index]
    
    try {
      const updatedComments = [...this.data.comments]
      updatedComments[index] = {
        ...comment,
        isLiked: !comment.isLiked,
        likeCount: comment.isLiked ? comment.likeCount - 1 : comment.likeCount + 1
      }
      
      this.setData({ comments: updatedComments })
      
      if (comment.isLiked) {
        await api.unlikeComment(commentId)
      } else {
        await api.likeComment(commentId)
      }
      
      wx.showToast({
        title: comment.isLiked ? '取消点赞' : '点赞成功',
        icon: 'success',
        duration: 1000
      })
      
    } catch (error) {
      // 恢复原状态
      const rollbackComments = [...this.data.comments]
      rollbackComments[index] = comment
      this.setData({ comments: rollbackComments })
      
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  },

  // 编辑评论
  onEditComment(e) {
    if (!auth.isLoggedIn()) {
      this.navigateToLogin()
      return
    }
    
    const comment = e.currentTarget.dataset.comment
    const index = e.currentTarget.dataset.index
    
    // 检查权限
    if (comment.userId !== this.data.userInfo.userId) {
      wx.showToast({
        title: '只能编辑自己的评论',
        icon: 'none'
      })
      return
    }
    
    this.setData({
      showCommentInput: true,
      editingComment: comment,
      replyToComment: null,
      commentContent: comment.content
    })
  },
  
  // 删除评论
  onDeleteComment(e) {
    const commentId = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条评论吗？删除后无法恢复',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.deleteComment(commentId)
            
            // 从列表中移除
            const updatedComments = [...this.data.comments]
            updatedComments.splice(index, 1)
            this.setData({ comments: updatedComments })
            
            wx.showToast({
              title: '删除成功',
              icon: 'success'
            })
            
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
  
  // 切换评论展开状态
  toggleCommentExpand(e) {
    const commentId = e.currentTarget.dataset.id
    const currentExpanded = this.data.expandedComments[commentId] || false
    
    this.setData({
      [`expandedComments.${commentId}`]: !currentExpanded
    })
  },
  
  // 举报评论
  onReportComment(e) {
    if (!auth.isLoggedIn()) {
      this.navigateToLogin()
      return
    }
    
    const comment = e.currentTarget.dataset.comment
    
    // 显示举报选项
    wx.showActionSheet({
      itemList: ['广告营销', '色情低俗', '政治敏感', '人身攻击', '诈骗信息', '其他违规'],
      success: async (res) => {
        const reasons = ['广告营销', '色情低俗', '政治敏感', '人身攻击', '诈骗信息', '其他违规']
        const selectedReason = reasons[res.tapIndex]
        
        try {
          await api.submitReport({
            targetType: 2, // 评论类型
            targetId: comment.commentId,
            reason: selectedReason
          })
          
          wx.showToast({
            title: '举报成功',
            icon: 'success'
          })
        } catch (error) {
          wx.showToast({
            title: error.message || '举报失败',
            icon: 'none'
          })
        }
      }
    })
  },
  
  // 显示举报弹窗
  onShowReport(e) {
    this.setData({ showReportModal: true })
  },

  // 关闭举报弹窗
  onCloseReport() {
    this.setData({
      showReportModal: false,
      reportReason: ''
    })
  },

  // 输入举报原因
  onReportReasonInput(e) {
    this.setData({
      reportReason: e.detail.value
    })
  },

  // 提交举报
  async onSubmitReport() {
    if (!this.data.reportReason || this.data.reportReason.trim().length === 0) {
      wx.showToast({
        title: '请输入举报原因',
        icon: 'none'
      })
      return
    }
      
    // 验证举报原因长度
    if (this.data.reportReason.length > 200) {
      wx.showToast({
        title: '举报原因不能超过 200 字',
        icon: 'none'
      })
      return
    }
      
    try {
      await api.submitReport({
        targetType: 1,  // 1 表示帖子类型
        targetId: this.data.postId,
        reason: this.data.reportReason.trim()
      })
        
      wx.showToast({
        title: '举报成功',
        icon: 'success'
      })
        
      this.onCloseReport()
        
    } catch (error) {
      wx.showToast({
        title: error.message || '举报失败',
        icon: 'none'
      })
    }
  },

  // 举报弹窗点击（防止关闭）
  onModalTap() {
    // 空方法，防止点击弹窗内容时关闭
  },

  // 跳转到登录页
  navigateToLogin() {
    wx.showModal({
      title: '提示',
      content: '请先登录后再进行操作',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          wx.redirectTo({
            url: '/pages/login/login'
          })
        }
      }
    })
  },

  // 分享功能
  onShareAppMessage() {
    return {
      title: this.data.postDetail ? this.data.postDetail.title : '社区邻里',
      path: `/pages/post/detail?postId=${this.data.postId}`
    }
  },
  
  // 图片预览
  previewImage(e) {
    const urls = e.currentTarget.dataset.urls || []
    const current = e.currentTarget.dataset.url
    
    wx.previewImage({
      current: current,
      urls: urls
    })
  },
  
  // 复制帖子链接
  onCopyLink() {
    const link = `pages/post/detail?postId=${this.data.postId}`
    wx.setClipboardData({
      data: link,
      success: () => {
        wx.showToast({
          title: '链接已复制',
          icon: 'success'
        })
      }
    })
  },
  
  // 跳转到用户主页
  goToUserProfile(e) {
    const userId = e.currentTarget.dataset.userid

    // 验证 userId 有效性
    if (!userId || userId === '' || userId === 'null' || userId === 'undefined') {
      wx.showToast({
        title: '用户信息不可用',
        icon: 'none'
      })
      return
    }
    
    wx.navigateTo({
      url: `/pages/profile/other/other?userId=${userId}`,
      success: () => {

      },
      fail: (error) => {
        wx.showToast({
          title: '跳转失败：' + (error.errMsg || '未知错误'),
          icon: 'none'
        })
      }
    })
  },
  
  // 点击发消息按钮
  onSendMessageTap(e) {

    if (!auth.isLoggedIn()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        this.navigateToLogin()
      }, 1500)
      return
    }
    
    const userId = e.currentTarget.dataset.userid
    const nickname = e.currentTarget.dataset.nickname

    if (!userId || userId === 'undefined' || userId === 'null') {
      wx.showToast({
        title: '用户信息不可用',
        icon: 'none'
      })
      return
    }
    
    // 直接发起聊天
    this.startChat(userId, nickname)
  },
  
  // 点击发帖人头像（发起聊天或查看主页）
  onAuthorAvatarTap(e) {


    if (!auth.isLoggedIn()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        this.navigateToLogin()
      }, 1500)
      return
    }
    
    const userId = e.currentTarget.dataset.userid
    const nickname = e.currentTarget.dataset.nickname


    if (!userId || userId === 'undefined' || userId === 'null' || userId === '') {
      wx.showModal({
        title: '提示',
        content: '用户信息不可用，无法查看主页',
        showCancel: false
      })
      return
    }
    
    // 检查是否是自己的帖子
    const currentUserId = this.data.userInfo?.userId


    if (userId === currentUserId) {
      // 点击自己的头像，跳转到个人主页
      wx.navigateTo({
        url: '/pages/profile/profile'
      })
      return
    }
    
    // 显示操作选项
    try {
      wx.showActionSheet({
        itemList: ['发消息', '查看主页'],
        success: (res) => {


          if (res.tapIndex === 0) {
            // 发起聊天
            this.startChat(userId, nickname)
          } else if (res.tapIndex === 1) {
            // 查看主页

            // 验证 userId 有效性
            if (!userId || userId === '' || userId === 'null' || userId === 'undefined') {
              wx.showToast({
                title: '用户信息不可用',
                icon: 'none'
              })
              return
            }
            wx.navigateTo({
              url: '/pages/profile/other/other?userId=' + userId,
              success: () => {

              },
              fail: (error) => {
                wx.showToast({
                  title: '跳转失败：' + (error.errMsg || '未知错误'),
                  icon: 'none'
                })
              }
            })
          }
        },
        fail: (err) => {

          if (err.errMsg && err.errMsg.includes('cancel')) {

          } else {
            wx.showToast({
              title: '操作失败',
              icon: 'none'
            })
          }
        }
      })
    } catch (error) {
      wx.showToast({
        title: '无法打开菜单',
        icon: 'none'
      })
    }
  },
  
  // 发起聊天
  startChat(userId, nickname) {
    try {
      // 获取当前用户信息
      const userInfo = auth.getUserInfo()
      if (!userInfo || !userInfo.userId) {
        wx.showToast({
          title: '请先登录',
          icon: 'none'
        })
        setTimeout(() => {
          this.navigateToLogin()
        }, 1500)
        return
      }
      
      // 检查是否是自己的帖子
      if (userId === userInfo.userId) {
        wx.showToast({
          title: '不能给自己发消息',
          icon: 'none'
        })
        return
      }
      
      // 显示加载提示
      wx.showLoading({
        title: '正在打开聊天...',
        mask: true
      })

      // 直接使用 navigateTo 跳转到聊天页面
      wx.navigateTo({
        url: `/pages/message/chat/chat?userId=${userId}&nickname=${encodeURIComponent(nickname || '未知用户')}&avatar=`,
        success: () => {
          wx.hideLoading()
        },
        fail: (error) => {
          wx.hideLoading()
          
          // 如果是页面层级限制，尝试使用 redirectTo
          if (error.errMsg && error.errMsg.includes('maximum page layer')) {
            wx.redirectTo({
              url: `/pages/message/chat/chat?userId=${userId}&nickname=${encodeURIComponent(nickname || '未知用户')}&avatar=`,
              fail: (err) => {
                wx.showToast({
                  title: '无法打开聊天窗口',
                  icon: 'none'
                })
              }
            })
          } else {
            wx.showToast({
              title: '无法打开聊天窗口',
              icon: 'none',
              duration: 2000
            })
          }
        }
      })
      
    } catch (error) {
      wx.hideLoading()
      wx.showToast({
        title: '无法打开聊天窗口',
        icon: 'none',
        duration: 2000
      })
    }
  },
  
  // 点击评论者头像
  onCommentAvatarTap(e) {
    if (!auth.isLoggedIn()) {
      this.navigateToLogin()
      return
    }
    
    const userId = e.currentTarget.dataset.userid
    const nickname = e.currentTarget.dataset.nickname
    
    // 验证 userId 有效性
    if (!userId || userId === '' || userId === 'null' || userId === 'undefined') {
      wx.showToast({
        title: '用户信息不可用',
        icon: 'none'
      })
      return
    }
    
    // 检查是否是自己的评论
    const currentUserId = this.data.userInfo?.userId


    if (userId === currentUserId) {
      // 点击自己的头像，跳转到个人主页
      wx.navigateTo({
        url: '/pages/profile/profile'
      })
      return
    }
    
    // 显示操作选项
    wx.showActionSheet({
      itemList: ['发消息', '查看主页'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 发起聊天
          this.startChat(userId, nickname)
        } else if (res.tapIndex === 1) {
          // 查看主页
          // 验证 userId 有效性
          if (!userId || userId === '' || userId === 'null' || userId === 'undefined') {
            wx.showToast({
              title: '用户信息不可用',
              icon: 'none'
            })
            return
          }
          wx.navigateTo({
            url: '/pages/profile/other/other?userId=' + userId,
            success: () => {

            },
            fail: (error) => {
              wx.showToast({
                title: '跳转失败：' + (error.errMsg || '未知错误'),
                icon: 'none'
              })
            }
          })
        }
      },
      fail: (err) => {

      }
    })
  },
  
  // 格式化时间显示
  formatDate(dateString) {
    if (!dateString) return ''
    
    const date = new Date(dateString)
    const now = new Date()
    const diff = now - date
    
    // 小于1分钟
    if (diff < 60000) {
      return '刚刚'
    }
    
    // 小于1小时
    if (diff < 3600000) {
      return `${Math.floor(diff / 60000)}分钟前`
    }
    
    // 小于24小时
    if (diff < 86400000) {
      return `${Math.floor(diff / 3600000)}小时前`
    }
    
    // 小于30天
    if (diff < 2592000000) {
      return `${Math.floor(diff / 86400000)}天前`
    }
    
    // 超过30天，显示具体日期
    return date.toLocaleDateString('zh-CN')
  },
  
  // 检查用户权限
  checkUserPermission() {
    const userInfo = auth.getUserInfo()
    return userInfo && userInfo.userId
  },
  
  // 获取用户信息
  getUserInfo() {
    return auth.getUserInfo() || {}
  },
  
  // 跳转到登录页
  navigateToLogin() {
    wx.showModal({
      title: '提示',
      content: '请先登录后再进行操作',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          wx.redirectTo({
            url: '/pages/login/login'
          })
        }
      }
    })
  },
  
  // ========== 二手交易相关功能 ==========
  
  // 复制联系方式
  onCopyContact() {
    const contactInfo = this.data.postDetail.contactInfo
    if (!contactInfo) return
    
    wx.setClipboardData({
      data: contactInfo,
      success: () => {
        wx.showToast({
          title: '联系方式已复制',
          icon: 'success'
        })
      }
    })
  },
  
  // 立即购买
  async onBuyNow() {
    try {
      // 检查登录状态
      if (!auth.isLoggedIn()) {
        this.navigateToLogin()
        return
      }
      
      const post = this.data.postDetail
      
      // 检查是否是卖家本人
      if (post.userId === this.data.userInfo.userId) {
        wx.showToast({
          title: '不能购买自己的商品',
          icon: 'none'
        })
        return
      }
      
      // 显示购买确认对话框
      wx.showModal({
        title: '确认购买',
        content: `商品：${post.title}\n价格：￥${post.price}\n\n点击确定后，将创建订单并跳转到支付页面。`,
        confirmText: '确认购买',
        cancelText: '再看看',
        success: async (res) => {
          if (res.confirm) {
            // 创建订单
            await this.createTradeOrder(post)
          }
        }
      })
    } catch (error) {
      wx.showToast({
        title: '操作失败，请重试',
        icon: 'none'
      })
    }
  },
  
  // 复制联系方式
  onCopyContact() {
    const contactInfo = this.data.postDetail.contactInfo
    if (!contactInfo) {
      wx.showToast({
        title: '无联系方式',
        icon: 'none'
      })
      return
    }
    
    wx.setClipboardData({
      data: contactInfo,
      success: () => {
        wx.showToast({
          title: '已复制到剪贴板',
          icon: 'success',
          duration: 1500
        })
      },
      fail: () => {
        wx.showToast({
          title: '复制失败',
          icon: 'none'
        })
      }
    })
  },
  
  // 创建交易订单
  async createTradeOrder(post) {
    try {
      wx.showLoading({
        title: '创建订单中...',
        mask: true
      })
      
      // 调用 API 创建订单
      const orderData = {
        postId: post.postId,
        quantity: 1,
        transactionMode: 1, // 默认自提，用户可以在订单页面修改
        contactPhone: this.data.userInfo.phone || '',
        remark: ''
      }

      // TODO: 调用后端创建订单 API
      // const result = await api.createTradeOrder(orderData)
      
      wx.hideLoading()
      
      // 模拟成功（实际应该调用后端 API）
      wx.showModal({
        title: '订单创建成功',
        content: `订单号：T${Date.now()}

请尽快联系卖家完成交易。

卖家联系方式：${post.contactInfo}`,
        showCancel: false,
        confirmText: '我知道了'
      })
      
      // TODO: 跳转到订单详情页或订单列表页
      // wx.navigateTo({
      //   url: `/pages/order/detail?orderId=${result.data.orderId}`
      // })
      
    } catch (error) {
      wx.hideLoading()
      wx.showModal({
        title: '创建失败',
        content: error.message || '请稍后重试',
        showCancel: false
      })
    }
  },
  
  // 编辑帖子
  onEditPost() {
    const postId = this.data.postId
    wx.navigateTo({
      url: `/pages/post/create?postId=${postId}&mode=edit`
    })
  },
  
  // 删除帖子
  onDeletePost() {
    const postId = this.data.postId
    
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
            
            // 返回上一页
            setTimeout(() => {
              wx.navigateBack()
            }, 1500)
            
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
  }
})
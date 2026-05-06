// pages/postManage/postManage.js
const api = require('../../../../utils/api.js')
const auth = require('../../../../utils/auth.js')

Page({
  data: {
    postList: [],
    currentPage: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    hasMore: true,
    refreshing: false,  // 下拉刷新状态
    selectedPost: null,
    showReviewModal: false,
    reviewStatus: 2, // 2: 通过，3: 拒绝
    reviewRemark: '',
    activeTab: 'all', // all, pending, approved, rejected
    tabs: [
      { key: 'all', name: '全部' },
      { key: 'pending', name: '待审核' },
      { key: 'approved', name: '已通过' },
      { key: 'rejected', name: '已拒绝' }
    ],
    // 搜索和筛选相关
    searchKeyword: '',
    selectedCategory: '',
    selectedCategoryIndex: 0,
    categories: [], // 从后端获取的板块列表
    // 审核历史相关
    showReviewHistory: false,
    reviewHistory: [],
    selectedPostForHistory: null
  },

  onLoad() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    
    this.loadCategories()
    this.loadPostList()
  },

  onShow() {
    if (auth.isLoggedIn()) {
      this.loadPostList()
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({
      currentPage: 1,
      postList: [],
      hasMore: true
    })
    this.loadPostList().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // scroll-view 下拉刷新
  onRefresh() {

    this.setData({ refreshing: true })
    this.loadPostList().then(() => {
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
      this.loadMore()
    }
  },

  // 切换标签页
  switchTab(e) {
    const tabKey = e.currentTarget.dataset.tab
    this.setData({
      activeTab: tabKey,
      currentPage: 1,
      postList: []
    })
    this.loadPostList()
  },
  
  // 搜索输入
  onSearchInput(e) {
    this.setData({
      searchKeyword: e.detail.value
    })
  },
  
  // 执行搜索
  onSearch() {
    this.setData({
      currentPage: 1,
      postList: []
    })
    this.loadPostList()
  },
  
  // 加载板块列表
  async loadCategories() {
    try {
      const response = await api.getCategoryList()
      
      // 直接使用 response.data，因为 API 返回的 data 就是数组
      let categories = []
      if (response && response.code === 200 && response.data) {
        categories = response.data
      }
      
      // 只取启用的板块
      const enabledCategories = categories.filter(cat => cat.status === 1)
      
      this.setData({
        categories: [
          { categoryId: '', name: '全部板块' },
          ...enabledCategories
        ],
        selectedCategoryIndex: 0,
        selectedCategory: ''
      })
      
      if (enabledCategories.length === 0) {
        wx.showToast({
          title: '暂无可用板块',
          icon: 'none',
          duration: 3000
        })
      }
    } catch (error) {
      this.setData({
        categories: [],
        selectedCategoryIndex: 0,
        selectedCategory: ''
      })
      wx.showToast({
        title: '加载失败，请检查网络',
        icon: 'none',
        duration: 3000
      })
    }
  },
  
  // 选择帖子板块类型
  onTypeChange(e) {
    const index = e.detail.value
    
    // 安全检查
    if (!this.data.categories || !this.data.categories[index]) {
      wx.showToast({
        title: '板块数据未加载',
        icon: 'none'
      })
      return
    }
    
    const categoryId = this.data.categories[index].categoryId
    
    this.setData({
      selectedCategoryIndex: index,
      selectedCategory: categoryId,
      currentPage: 1,
      postList: []
    })
    this.loadPostList()
  },
  
  // 重置搜索
  resetSearch() {
    this.setData({
      searchKeyword: '',
      selectedCategory: '',
      selectedCategoryIndex: 0,
      currentPage: 1,
      postList: []
    })
    this.loadPostList()
  },

  // 加载帖子列表
  loadPostList() {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    const statusValue = this.getStatusByTab(this.data.activeTab)
    const categoryIdValue = this.data.selectedCategory
    
    const params = {
      page: parseInt(this.data.currentPage) || 1,
      size: parseInt(this.data.pageSize) || 10
    }
    
    // 只添加有效的参数，避免传递 null、undefined 或空字符串


    if (statusValue !== null && statusValue !== undefined && statusValue !== '') {
      params.status = statusValue

    } else {

    }
    
    if (this.data.searchKeyword && this.data.searchKeyword.trim() !== '') {
      params.keyword = this.data.searchKeyword.trim()

    } else {

    }
    
    if (categoryIdValue !== null && categoryIdValue !== undefined && categoryIdValue !== '' && categoryIdValue !== 'all') {
      const parsedCategoryId = parseInt(categoryIdValue)
      if (!isNaN(parsedCategoryId)) {
        params.categoryId = parsedCategoryId

      } else {

      }
    } else {

    }


    // 根据不同状态加载不同数据
    return api.getPostsByStatus(params).then(res => {

      const newList = res.data.records || res.data.list || []
      const total = res.data.total || 0


      // 处理帖子数据，添加板块名称等信息
      const processedList = newList.map(post => {


        return {
          ...post,
          categoryName: post.categoryName || '未知板块',
          formattedTime: this.formatTime(post.publishTime),
          statusText: this.getStatusText(post.status),
          // 使用后端返回的 isTop 字段（兼容 is_top）
          isTop: (post.isTop === 1 || post.is_top === 1) ? 1 : 0,
          // 截取内容预览（最多100个字符）
          contentPreview: this.truncateContent(post.content, 100)
        }
      })

      if (processedList.length > 0) {

      }
      
      this.setData({
        postList: this.data.currentPage === 1 ? processedList : [...this.data.postList, ...processedList],
        total: total,
        hasMore: processedList.length === this.data.pageSize,
        loading: false
      })

    }).catch(err => {
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
      this.setData({ loading: false })
    })
  },

  // 加载更多
  loadMore() {
    this.setData({
      currentPage: this.data.currentPage + 1
    })
    this.loadPostList()
  },

  // 根据标签页获取状态
  getStatusByTab(tab) {
    switch(tab) {
      case 'pending': return 1
      case 'approved': return 2
      case 'rejected': return 3
      case 'all': 
      default: return null // null 表示查询所有状态
    }
  },
  
  // 获取板块分类名称（已废弃，直接使用 categoryName）
  getPostTypeName(type) {
    const typeMap = {
      1: '普通帖子',
      2: '求助帖子',
      3: '二手帖子',
      4: '活动帖子'
    }
    return typeMap[type] || '未知类型'
  },
  
  // 获取状态文本
  getStatusText(status) {
    const statusMap = {
      1: '待审核',
      2: '已通过',
      3: '已拒绝'
    }
    return statusMap[status] || '未知状态'
  },
  
  // 格式化时间
  formatTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  },

  // 截取内容预览
  truncateContent(content, maxLength) {
    if (!content) return ''
    if (content.length <= maxLength) return content
    return content.substring(0, maxLength) + '...'
  },

  // 查看帖子详情（跳转到详情页）
  viewPostDetail(e) {
    const post = e.currentTarget.dataset.post


    if (!post || !post.postId) {
      wx.showToast({
        title: '帖子信息不完整',
        icon: 'none'
      })
      return
    }
    
    wx.navigateTo({
      url: `/pages/index/postManage/postDetail/postDetail?postId=${post.postId}`,
      fail: (err) => {
        wx.showToast({
          title: '跳转失败，请检查路径',
          icon: 'none'
        })
      }
    })
  },

  // 关闭审核模态框
  closeReviewModal() {
    this.setData({
      showReviewModal: false,
      selectedPost: null,
      reviewStatus: 2,
      reviewRemark: ''
    })
  },

  // 选择审核结果
  selectReviewStatus(e) {
    const status = parseInt(e.currentTarget.dataset.status)
    this.setData({
      reviewStatus: status
    })
  },

  // 输入审核备注
  onRemarkInput(e) {
    this.setData({
      reviewRemark: e.detail.value
    })
  },

  // 提交审核
  submitReview() {
    if (!this.data.selectedPost) return
    
    const reviewData = {
      targetId: this.data.selectedPost.postId,
      status: this.data.reviewStatus,
      remark: this.data.reviewRemark || (this.data.reviewStatus === 2 ? '审核通过' : '审核拒绝')
    }
    
    wx.showLoading({
      title: '提交中...'
    })
    
    api.reviewPost(reviewData).then(res => {
      wx.hideLoading()
      wx.showToast({
        title: '审核成功',
        icon: 'success'
      })
      
      this.closeReviewModal()
      
      // 重新加载列表
      this.setData({
        currentPage: 1,
        postList: []
      })
      this.loadPostList()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '审核失败',
        icon: 'none'
      })
    })
  },

  // 置顶/取消置顶帖子
  toggleTopPost(e) {
    const post = e.currentTarget.dataset.post
    const isTop = post.isTop || 0
    
    wx.showModal({
      title: '确认操作',
      content: isTop ? '确定要取消置顶此帖子吗？' : '确定要将此帖子置顶吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({
            title: isTop ? '取消置顶中...' : '置顶中...'
          })
          
          api.topPost(post.postId).then(res => {
            wx.hideLoading()
            wx.showToast({
              title: isTop ? '取消置顶成功' : '置顶成功',
              icon: 'success'
            })
            
            // 重新加载列表以确保显示最新的置顶状态
            this.setData({
              currentPage: 1,
              postList: []
            })
            this.loadPostList()
          }).catch(err => {
            wx.hideLoading()
            wx.showToast({
              title: err.message || '操作失败',
              icon: 'none'
            })
          })
        }
      }
    })
  },

  // 删除帖子
  deletePost(e) {
    const post = e.currentTarget.dataset.post
    wx.showModal({
      title: '确认删除',
      content: '确定要删除此帖子吗？此操作不可恢复！',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({
            title: '删除中...'
          })
          
          api.deletePost(post.postId).then(res => {
            wx.hideLoading()
            wx.showToast({
              title: '删除成功',
              icon: 'success'
            })
            
            // 从列表中移除该帖子
            const filteredList = this.data.postList.filter(item => item.postId !== post.postId)
            this.setData({
              postList: filteredList
            })
          }).catch(err => {
            wx.hideLoading()
            wx.showToast({
              title: err.message || '删除失败',
              icon: 'none'
            })
          })
        }
      }
    })
  },

  // 显示通过模态框
  showApproveModal(e) {
    const post = e.currentTarget.dataset.post
    this.setData({
      selectedPost: post,
      reviewStatus: 2, // 通过
      reviewRemark: '',
      showReviewModal: true
    })
  },

  // 显示拒绝模态框
  showRejectModal(e) {
    const post = e.currentTarget.dataset.post
    this.setData({
      selectedPost: post,
      reviewStatus: 3, // 拒绝
      reviewRemark: '',
      showReviewModal: true
    })
  },

  // 快速通过（保留该方法，但不再直接使用）
  quickApprove(e) {
    const post = e.currentTarget.dataset.post
    wx.showLoading({
      title: '快速通过中...'
    })
    
    const reviewData = {
      targetId: post.postId,
      status: 2, // 通过
      remark: '快速通过'
    }
    
    api.reviewPost(reviewData).then(res => {
      wx.hideLoading()
      wx.showToast({
        title: '快速通过成功',
        icon: 'success'
      })
      
      // 重新加载列表
      this.setData({
        currentPage: 1,
        postList: []
      })
      this.loadPostList()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '快速通过失败',
        icon: 'none'
      })
    })
  },

  // 快速拒绝（保留该方法，但不再直接使用）
  quickReject(e) {
    const post = e.currentTarget.dataset.post
    wx.showModal({
      title: '确认拒绝',
      content: '确定要拒绝该帖子吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({
            title: '快速拒绝中...'
          })
          
          const reviewData = {
            targetId: post.postId,
            status: 3, // 拒绝
            remark: '快速拒绝'
          }
          
          api.reviewPost(reviewData).then(res => {
            wx.hideLoading()
            wx.showToast({
              title: '快速拒绝成功',
              icon: 'success'
            })
            
            // 重新加载列表
            this.setData({
              currentPage: 1,
              postList: []
            })
            this.loadPostList()
          }).catch(err => {
            wx.hideLoading()
            wx.showToast({
              title: err.message || '快速拒绝失败',
              icon: 'none'
            })
          })
        }
      }
    })
  },

  // 预览图片
  previewImage(e) {
    const urls = e.currentTarget.dataset.urls
    const current = e.currentTarget.dataset.current
    wx.previewImage({
      current: current,
      urls: urls
    })
  },
  
  // 阻止事件冒泡
  stopPropagation(e) {
    // 空函数，仅用于阻止事件冒泡
  },
  
  // 选择帖子（用于批量操作）已删除
  
  // 全选/取消全选 已删除
  
  // 批量审核 已删除
  
  // 执行批量审核 已删除
  
  // 批量删除 已删除
  
  // 执行批量删除 已删除
  
  // 查看审核历史
  viewReviewHistory(e) {
    const post = e.currentTarget.dataset.post
    this.setData({
      selectedPostForHistory: post,
      showReviewHistory: true
    })
    
    // 加载审核历史
    this.loadReviewHistory(post.postId)
  },
  
  // 加载审核历史
  loadReviewHistory(postId) {
    api.getReviewHistory(postId).then(res => {
      this.setData({
        reviewHistory: res.data || []
      })
    }).catch(err => {
      wx.showToast({
        title: '加载审核历史失败',
        icon: 'none'
      })
    })
  },
  
  // 关闭审核历史
  closeReviewHistory() {
    this.setData({
      showReviewHistory: false,
      selectedPostForHistory: null,
      reviewHistory: []
    })
  }
})
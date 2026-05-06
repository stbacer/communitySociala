// pages/postManage/postDetail/postDetail.js
const api = require('../../../../utils/api.js')
const auth = require('../../../../utils/auth.js')
const { getAddressByLocation } = require('../../../../utils/tencentMap.js')

Page({
  data: {
    post: null,
    loading: true,
    showReviewModal: false,
    reviewStatus: 2,
    reviewRemark: '',
    showImagePreview: false,
    previewImages: [],
    currentPreviewIndex: 0,
    categories: [],
    addressText: '' // 存储转换后的地址文本
  },

  onLoad(options) {
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    
    // 从参数中获取帖子信息
    if (options.postId) {
      this.loadPostDetail(options.postId)
    } else {
      wx.showToast({
        title: '帖子 ID 不能为空',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }
    
    // 加载板块列表
    this.loadCategories()
  },

  // 加载帖子详情
  loadPostDetail(postId) {
    this.setData({ loading: true })
    
    const params = {
      page: 1,
      size: 100
    }
    
    api.getPendingPosts(params).then(res => {
      const postList = res.data.records || res.data.list || []
      const targetPost = postList.find(post => post.postId === postId)
       
      if (targetPost) {
        this.processPostDataAndSet(targetPost)
      } else {
        api.getPostsByStatus({ page: 1, size: 100 }).then(res => {
          const allPosts = res.data.records || res.data.list || []
          const foundPost = allPosts.find(post => post.postId === postId)
          
          if (foundPost) {
            this.processPostDataAndSet(foundPost)
          } else {
            this.showNotFound()
          }
        })
      }
    }).catch(err => {
      api.getPostsByStatus({ page: 1, size: 100 }).then(res => {
        const allPosts = res.data.records || res.data.list || []
        const foundPost = allPosts.find(post => post.postId === postId)
        
        if (foundPost) {
          this.processPostDataAndSet(foundPost)
        } else {
          this.showNotFound()
        }
      }).catch(err2 => {
        this.showNotFound()
      })
    })
  },
  
  // 处理帖子数据并设置
  async processPostDataAndSet(postData) {


    if (postData.userInfo) {


    }
    
    if (postData.imageUrls && typeof postData.imageUrls === 'string') {
      try {
        postData.imageUrlsArray = JSON.parse(postData.imageUrls)
      } catch (e) {
        postData.imageUrlsArray = []
      }
    } else if (Array.isArray(postData.imageUrls)) {
      postData.imageUrlsArray = postData.imageUrls
    } else {
      postData.imageUrlsArray = []
    }
    
    // 处理位置信息：将经纬度转换为实际地址
    let addressText = '未知位置'
    if (postData.longitude && postData.latitude) {
      try {
        const addressInfo = await getAddressByLocation(postData.longitude, postData.latitude)
        addressText = addressInfo.formattedAddress || addressInfo.address || `${postData.longitude.toFixed(6)}, ${postData.latitude.toFixed(6)}`
      } catch (err) {
        addressText = `${postData.longitude.toFixed(6)}, ${postData.latitude.toFixed(6)}`
      }
    }
    
    this.setData({
      post: postData,
      addressText: addressText,
      loading: false
    })


  },
  
  // 加载板块列表
  async loadCategories() {
    try {
      const response = await api.getCategoryList()
      
      if (response && response.code === 200 && response.data) {
        this.setData({
          categories: response.data
        })
      }
    } catch (error) {
      // 错误已在 request 拦截器中处理
    }
  },
  
  // 根据 categoryId 获取板块名称
  getCategoryName(categoryId) {
    if (!categoryId) return '未知板块'
     
    const category = this.data.categories.find(cat => cat.categoryId == categoryId)
    return category ? category.name : '未知板块'
  },
   
  // 判断是否为二手交易帖子（categoryId= 2）
  isSecondHandPost(categoryId) {
    return categoryId == 2
  },
   
  // 获取交易方式文本
  getTransactionModeText(mode) {
    switch (mode) {
      case 1:
        return '自提'
      case 2:
        return '快递'
      case 3:
        return '自提/快递均可'
      default:
        return '未设置'
    }
  },
  
  // 获取状态文本
  getStatusText(status) {
    switch (status) {
      case 0:
        return '已删除'
      case 1:
        return '待审核'
      case 2:
        return '已发布'
      default:
        return '未知'
    }
  },
  
  // 格式化时间
  formatTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  },
  
  // 格式化完整时间
  formatFullTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  },
  
  // 预览图片
  previewImages(e) {
    const { images, index } = e.currentTarget.dataset
    if (images && images.length > 0) {
      this.setData({
        showImagePreview: true,
        previewImages: images,
        currentPreviewIndex: index || 0
      })
    }
  },
  
  // 关闭图片预览
  closeImagePreview() {
    this.setData({
      showImagePreview: false,
      previewImages: [],
      currentPreviewIndex: 0
    })
  },
  
  // 下一张图片
  nextImage() {
    const nextIndex = (this.data.currentPreviewIndex + 1) % this.data.previewImages.length
    this.setData({
      currentPreviewIndex: nextIndex
    })
  },
  
  // 上一张图片
  prevImage() {
    const prevIndex = (this.data.currentPreviewIndex - 1 + this.data.previewImages.length) % this.data.previewImages.length
    this.setData({
      currentPreviewIndex: prevIndex
    })
  },
  
  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，用于阻止事件冒泡
  },
  
  // swiper 切换事件
  onSwiperChange(e) {
    this.setData({
      currentPreviewIndex: e.detail.current
    })
  },
  
  // 显示审核模态框
  showReviewModal() {
    this.setData({
      showReviewModal: true,
      reviewStatus: 2,
      reviewRemark: ''
    })
  },
  
  // 关闭审核模态框
  closeReviewModal() {
    this.setData({
      showReviewModal: false,
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
    if (!this.data.post) return
     
    const reviewData = {
      targetId: this.data.post.postId,
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
      
      // 延迟返回上一页
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '审核失败',
        icon: 'none'
      })
    })
  },
  
  // 显示通过模态框
  showApproveModal() {
    this.setData({
      reviewStatus: 2, // 通过
      reviewRemark: '',
      showReviewModal: true
    })
  },
  
  // 显示拒绝模态框
  showRejectModal() {
    wx.showModal({
      title: '确认拒绝',
      content: '确定要拒绝该帖子的发布申请吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            reviewStatus: 3, // 拒绝
            reviewRemark: '',
            showReviewModal: true
          })
        }
      }
    })
  },
  
  // 快速通过（保留该方法，但不再直接使用）
  quickApprove() {
    this.setData({
      reviewStatus: 2,
      reviewRemark: '快速通过'
    })
    this.submitReview()
  },
  
  // 快速拒绝（保留该方法，但不再直接使用）
  quickReject() {
    wx.showModal({
      title: '确认拒绝',
      content: '确定要拒绝该帖子的发布申请吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            reviewStatus: 3,
            reviewRemark: '快速拒绝'
          })
          this.submitReview()
        }
      }
    })
  },
  
  // 显示未找到提示并返回
  showNotFound() {
    wx.showToast({
      title: '未找到该帖子信息',
      icon: 'none'
    })
    setTimeout(() => {
      wx.navigateBack()
    }, 1500)
  },
  
  // 返回列表
  goBack() {
    wx.navigateBack()
  },
  
  // 置顶/取消置顶帖子
  toggleTopPost() {
    if (!this.data.post) return
    
    const isTop = this.data.post.isTop === 1 ? 1 : 0
    
    wx.showModal({
      title: '确认操作',
      content: isTop ? '确定要取消置顶此帖子吗？' : '确定要将此帖子置顶吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({
            title: isTop ? '取消置顶中...' : '置顶中...'
          })
          
          api.topPost(this.data.post.postId).then(res => {
            wx.hideLoading()
            wx.showToast({
              title: isTop ? '取消置顶成功' : '置顶成功',
              icon: 'success'
            })
            
            // 更新当前帖子的置顶状态
            this.setData({
              'post.isTop': isTop ? 0 : 1
            })
            
            // 延迟返回上一页
            setTimeout(() => {
              wx.navigateBack()
            }, 1500)
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
  
  // 查看用户主页
  viewUserProfile(e) {
    const userId = e.currentTarget.dataset.userId
    const userNickname = e.currentTarget.dataset.userNickname
    
    if (!userId) {
      wx.showToast({
        title: '用户 ID 不能为空',
        icon: 'none'
      })
      return
    }
    
    // 跳转到用户主页
    wx.navigateTo({
      url: `/pages/userProfile/userProfile?userId=${userId}&nickname=${userNickname}`
    })
  }
})

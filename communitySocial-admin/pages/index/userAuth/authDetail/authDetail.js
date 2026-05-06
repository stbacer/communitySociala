// pages/userAuth/authDetail/authDetail.js
const api = require('../../../../utils/api.js')
const auth = require('../../../../utils/auth.js')

Page({
  data: {
    authUser: null,
   loading: true,
    showReviewModal: false,
   reviewStatus: 2, // 2: 通过，3: 拒绝
   reviewRemark: '',
    showImagePreview: false,
   previewImages: [],
   currentPreviewIndex: 0
  },

  onLoad(options) {
    // 检查登录状态
   if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login/login'
      })
     return
    }
    
    // 从参数中获取用户信息
   if (options.userId) {
     this.loadAuthDetail(options.userId)
    } else {
      wx.showToast({
       title: '用户 ID 不能为空',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }
  },

  // 加载认证详情
  loadAuthDetail(userId) {
  this.setData({ loading: true })
    
    const params = {
     page: 1,
      size: 100
    }
    
    api.getPendingAuthUsers(params).then(res => {
     const userList = res.data.records || res.data.list || []
     const targetUser = userList.find(user => user.userId === userId)
      
    if (targetUser) {
      this.processUserDataAndSet(targetUser)
      } else {
       api.getUserInfo(userId).then(res => {
        if (res.data) {
          this.processUserDataAndSet(res.data)
          } else {
          this.showNotFound()
          }
        })
      }
    }).catch(err => {
     api.getUserInfo(userId).then(res => {
      if (res.data) {
        this.processUserDataAndSet(res.data)
        } else {
        this.showNotFound()
        }
      }).catch(err2 => {
      this.showNotFound()
      })
    })
  },
  
  // 处理用户数据并设置
  processUserDataAndSet(userData) {
    if (userData.identityImages) {
     try {
     if (typeof userData.identityImages === 'string') {
       // 按竖线拆分字符串
       userData.identityImagesArray = userData.identityImages.split('|').filter(url => url.trim() !== '')
        } else {
       userData.identityImagesArray = userData.identityImages
        }
      } catch (e) {
     userData.identityImagesArray = []
      }
    } else {
   userData.identityImagesArray = []
    }
    
    // 格式化时间字段
    if (userData.createTime) {
      userData.formattedCreateTime = this.formatFullTime(userData.createTime)
    }
    
  this.setData({
   authUser: userData,
   loading: false
    })
  },
  
  // 显示未找到提示并返回
  showNotFound() {
   wx.showToast({
     title: '未找到该用户的认证信息',
     icon: 'none'
    })
   setTimeout(() => {
     wx.navigateBack()
   }, 1500)
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
  
  // 预览身份证图片
  previewIdentityImages(e) {
   const { images, index } = e.currentTarget.dataset
   if (images && images.length> 0) {
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
  
  // 头像加载错误处理
  onAvatarLoadError(e) {
    // 静默处理头像加载错误
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
   if (!this.data.authUser) return
    
   const reviewData = {
     targetId: this.data.authUser.userId,
      status: this.data.reviewStatus,
     remark: this.data.reviewRemark || (this.data.reviewStatus === 2 ? '审核通过' : '审核拒绝')
    }
    wx.showLoading({
     title: '提交中...'
    })
  
    api.reviewAuth(reviewData).then(res => {
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
      content: '确定要拒绝该用户的认证申请吗？',
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
     content: '确定要拒绝该用户的认证申请吗？',
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
  
  // 返回列表页
  goBack() {
    wx.navigateBack()
  }
})

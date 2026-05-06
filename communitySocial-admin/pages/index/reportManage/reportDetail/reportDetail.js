// pages/reportManage/reportDetail/reportDetail.js
const api = require('../../../../utils/api.js')
const auth = require('../../../../utils/auth.js')

Page({
  data: {
   report: null,
   loading: true,
    showHandleModal: false,
    handleStatus: 1, // 1: 处理完成，2: 驳回举报
    handleResult: '',
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
    
    // 从参数中获取举报 ID
    if (options.reportId) {
     this.loadReportDetail(options.reportId)
    } else {
      wx.showToast({
        title: '举报 ID 不能为空',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }
  },

  // 加载举报详情
  loadReportDetail(reportId) {
  this.setData({ loading: true })
    
    api.getReportDetail(reportId).then(res => {
      const reportData = res.data


      // 格式化时间字段（在 JS 中处理，不在 WXML 中调用方法）
   if (reportData.reportTime) {
     reportData.formattedReportTime = this.formatTime(reportData.reportTime)
      }
   if (reportData.handleTime) {
     reportData.formattedHandleTime = this.formatTime(reportData.handleTime)
      }
    
    // 映射举报类型和内容类型（在 JS 中处理）
  reportData.targetTypeText = this.getTargetTypeText(reportData.targetType)
  reportData.statusText = this.getStatusText(reportData.status)
  reportData.statusClass = this.getStatusClass(reportData.status)

      // 处理图片数据（如果有）
    if (reportData.images) {
        try {
        if (typeof reportData.images === 'string') {
         reportData.imagesArray = JSON.parse(reportData.images)
           } else {
         reportData.imagesArray = reportData.images
           }
        } catch (e) {
      reportData.imagesArray = []
        }
      } else {
     reportData.imagesArray = []
      }
      
    this.setData({
      report: reportData,
      loading: false
      })
    }).catch(err => {
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    })
  },

  // 格式化时间
  formatTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
   return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  },

  // 获取举报类型文本
  getTargetTypeText(type) {
    const typeMap = {
      1: '帖子',
      2: '评论',
      3: '用户'
    }
   return typeMap[type] || '未知'
  },

  // 获取状态文本
  getStatusText(status) {
    const statusMap = {
      0: '待处理',
      1: '已处理',
      2: '已驳回'
    }
   return statusMap[status] || '未知'
  },

  // 获取状态样式类
  getStatusClass(status) {
    const classMap = {
      0: 'status-pending',
      1: 'status-approved',
      2: 'status-rejected'
    }
   return classMap[status] || ''
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

  // 跳转到被举报内容详情页
  goToTargetContent() {
    const report = this.data.report
    if (!report) return
    
    switch (report.targetType) {
      case 1: // 帖子
        wx.navigateTo({
          url: `/pages/index/postManage/postDetail/postDetail?postId=${report.targetId}`
        })
       break
      case 2: // 评论
        wx.showToast({
          title: '评论详情功能待完善',
          icon: 'none'
        })
       break
      case 3: // 用户
        wx.navigateTo({
          url: `/pages/index/userAuth/authDetail/authDetail?userId=${report.targetId}`
        })
       break
      default:
        wx.showToast({
          title: '不支持的类型',
          icon: 'none'
        })
    }
  },

  // 跳转到举报人主页
  goToReporterProfile() {
    const report = this.data.report
    if (!report || !report.reporterId) return
    
    wx.navigateTo({
      url: `/pages/index/userAuth/authDetail/authDetail?userId=${report.reporterId}`
    })
  },

  // 显示处理模态框
  showHandleModal() {
   this.setData({
      showHandleModal: true,
      handleStatus: 1,
      handleResult: ''
    })
  },

  // 关闭处理模态框
  closeHandleModal() {
   this.setData({
      showHandleModal: false,
      handleStatus: 1,
      handleResult: ''
    })
  },

  // 选择处理结果
  selectHandleStatus(e) {
    const status = parseInt(e.currentTarget.dataset.status)
   this.setData({
      handleStatus: status
    })
  },

  // 输入处理结果
  onResultInput(e) {
   this.setData({
      handleResult: e.detail.value
    })
  },

  // 提交处理
  submitHandle() {
    if (!this.data.report) return
    
    if (!this.data.handleResult.trim()) {
      wx.showToast({
        title: '请输入处理说明',
        icon: 'none'
      })
     return
    }
    
    const handleData = {
     reportId: this.data.report.reportId,
      status: this.data.handleStatus,
      handleResult: this.data.handleResult || (this.data.handleStatus === 1 ? '举报属实，已处理' : '举报不属实，已驳回')
    }
    
    wx.showLoading({
      title: '提交中...'
    })
    
    api.handleReport(handleData).then(res => {
      wx.hideLoading()
      wx.showToast({
        title: '处理成功',
        icon: 'success'
      })
      
     this.closeHandleModal()
      
      // 延迟返回上一页
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '处理失败',
        icon: 'none'
      })
    })
  },

  // 快速处理
  quickHandle() {
   this.setData({
      handleStatus: 1,
      handleResult: '快速处理：举报属实，已处理相关内容'
    })
   this.submitHandle()
  },

  // 快速驳回
  quickReject() {
    wx.showModal({
      title: '确认驳回',
      content: '确定要驳回该举报吗？',
      success: (res) => {
        if (res.confirm) {
         this.setData({
            handleStatus: 2,
            handleResult: '快速驳回：举报不属实或证据不足'
          })
         this.submitHandle()
        }
      }
    })
  },

  // 返回列表页
  goBack() {
    wx.navigateBack()
  }
})

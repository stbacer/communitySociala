// pages/reportManage/reportManage.js
const api = require('../../../../utils/api.js')
const auth = require('../../../../utils/auth.js')

Page({
  data: {
   reportList: [],
   currentPage: 1,
   pageSize: 10,
   total: 0,
   loading: false,
    hasMore: true,
    refreshing: false,  // 下拉刷新状态
    selectedReport: null,
   showHandleModal: false,
    handleStatus: 1, // 1: 处理完成，2: 驳回举报
    handleResult: '',
    activeTab: 'all', // all, pending, handled, rejected
    searchKeyword: '',
    tabs: [
      { key: 'all', name: '全部' },
      { key: 'pending', name: '待处理' },
      { key: 'handled', name: '已处理' },
      { key: 'rejected', name: '已驳回' }
    ],
  },

  onLoad() {
   if (!auth.isLoggedIn()) {
      wx.redirectTo({
       url: '/pages/login/login'
      })
     return
    }
    
   this.loadReportList()
  },

  onShow() {
   if (auth.isLoggedIn()) {
     this.loadReportList()
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
   this.setData({
     currentPage: 1,
     reportList: [],
      hasMore: true
    })
   this.loadReportList().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // scroll-view 下拉刷新
  onRefresh() {

    this.setData({ refreshing: true })
    this.loadReportList().then(() => {
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
  
  // 搜索输入
  onSearchInput(e) {
   this.setData({
      searchKeyword: e.detail.value
    })
  },
  
  // 执行搜索
  async onSearch() {
   if (!this.data.searchKeyword.trim()) {
      wx.showToast({
        title: '请输入搜索关键词',
        icon: 'none'
      })
     return
    }
    
   this.setData({
     currentPage: 1,
     reportList: [],
      activeTab: 'all' // 切换到全部标签
    })
    
    await this.loadReportList()
  },
  
  // 清空搜索
  clearSearch() {
   this.setData({
      searchKeyword: '',
     currentPage: 1,
     reportList: [],
      activeTab: 'pending'
    })
   this.loadReportList()
  },

  // 切换标签页
  switchTab(e) {
    const tabKey = e.currentTarget.dataset.tab
   this.setData({
      activeTab: tabKey,
     currentPage: 1,
     reportList: []
    })
   this.loadReportList()
  },

  // 加载举报列表
  async loadReportList() {
   if (this.data.loading) return
    
   this.setData({ loading: true })
    
    try {
      const params = {
       page: this.data.currentPage,
        size: this.data.pageSize
      }
      
      // 根据当前标签页筛选数据
      if (this.data.activeTab === 'pending') {
        params.status = 0
      } else if (this.data.activeTab === 'handled') {
        params.status = 1
      } else if (this.data.activeTab === 'rejected') {
        params.status = 2
      }
      // 'all' 或不传 status 参数，查询所有状态的举报
      
      const response = await api.getReportList(params)
      const reports = response.data.records || []
      const total = response.data.total || 0
      const hasMore = this.data.currentPage * this.data.pageSize < total
      
      // 处理举报数据，将类型数字转换为文本
      const processedReports = reports.map(report => ({
        ...report,
        targetTypeText: this.getTargetTypeText(report.targetType),
        statusText: this.getStatusText(report.status)
      }))
      
     this.setData({
       reportList: this.data.currentPage === 1 ? processedReports : [...this.data.reportList, ...processedReports],
       total: total,
        hasMore: hasMore,
       loading: false
      })
      
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
     this.setData({ loading: false })
    }
  },

  // 加载更多
  loadMore() {
   this.setData({
     currentPage: this.data.currentPage + 1
    })
   this.loadReportList()
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

  // 查看举报详情（打开处理模态框）
  async viewReportDetail(e) {
    const report = e.currentTarget.dataset.report
    
    wx.showLoading({
      title: '加载中...'
    })
    
    try {
      // 调用后端 API 获取完整的举报详情
      const response = await api.getReportDetail(report.reportId)
      const detailedReport = response.data
      
      // 处理举报数据，将类型数字转换为文本
      detailedReport.targetTypeText = this.getTargetTypeText(detailedReport.targetType)
      
     this.setData({
        selectedReport: detailedReport,
       showHandleModal: true,
        handleResult: ''
      })
      
      wx.hideLoading()
      
    } catch (error) {
      wx.hideLoading()
      wx.showToast({
        title: error.message || '获取详情失败',
        icon: 'none'
      })
      
      // 如果 API 调用失败，仍然显示列表中的基本信息
     const fallbackReport = { ...report }
     fallbackReport.targetTypeText = this.getTargetTypeText(report.targetType)
     
     this.setData({
        selectedReport: fallbackReport,
       showHandleModal: true,
        handleResult: ''
      })
    }
  },
  
  // 跳转到被举报内容详情页
  goToTargetContent(e) {
    const report = e.currentTarget.dataset.report
   if (!report) return
    
    switch (report.targetType) {
     case 1: // 帖子
        wx.navigateTo({
         url: `/pages/postManage/postDetail?postId=${report.targetId}`
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
         url: `/pages/userAuth/userDetail?userId=${report.targetId}`
        })
       break
     default:
        wx.showToast({
          title: '不支持的类型',
          icon: 'none'
        })
    }
  },
  
  // 跳转到举报详情页
  goToReportDetail(e) {
    const report = e.currentTarget.dataset.report


    if (!report || !report.reportId) {
      wx.showToast({
        title: '举报信息不完整',
        icon: 'none'
      })
      return
    }
    
    // 构建完整的页面路径
    const pagePath = '/pages/index/reportManage/reportDetail/reportDetail'
    const fullPath = `${pagePath}?reportId=${report.reportId}`

    wx.navigateTo({
      url: fullPath,
      fail: (err) => {
        wx.showModal({
          title: '跳转失败',
          content: `无法打开举报详情页，请重试`,
          showCancel: false
        })
      }
    })
  },

  // 关闭处理模态框
  closeHandleModal() {
   this.setData({
     showHandleModal: false,
      selectedReport: null,
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
  async submitHandle() {
   if (!this.data.selectedReport) return
    
   if (!this.data.handleResult.trim()) {
      wx.showToast({
        title: '请输入处理说明',
        icon: 'none'
      })
     return
    }
    
    const handleData = {
     reportId: this.data.selectedReport.reportId,
      status: this.data.handleStatus,
      handleResult: this.data.handleResult || (this.data.handleStatus === 1 ? '举报属实，已处理' : '举报不属实，已驳回')
    }
    
    wx.showLoading({
      title: '提交中...'
    })
    
    try {
      await api.handleReport(handleData)
      
      wx.hideLoading()
      wx.showToast({
        title: '处理成功',
        icon: 'success'
      })
      
     this.closeHandleModal()
      
      // 重新加载列表
     this.setData({
       currentPage: 1,
       reportList: []
      })
     this.loadReportList()
      
    } catch (error) {
      wx.hideLoading()
      wx.showToast({
        title: error.message || '处理失败',
        icon: 'none'
      })
    }
  },

  // 显示处理模态框
  showHandleModal(e) {
    const report = e.currentTarget.dataset.report
    this.setData({
      selectedReport: report,
      handleStatus: 1, // 处理完成
      handleResult: '',
      showHandleModal: true
    })
  },

  // 显示驳回模态框
  showRejectModal(e) {
    const report = e.currentTarget.dataset.report
    wx.showModal({
      title: '确认驳回',
      content: '确定要驳回该举报吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            selectedReport: report,
            handleStatus: 2, // 驳回举报
            handleResult: '',
            showHandleModal: true
          })
        }
      }
    })
  },

  // 快速处理（保留该方法，但不再直接使用）
  async quickHandle(e) {
    const report = e.currentTarget.dataset.report
    wx.showLoading({
      title: '处理中...'
    })
    
    try {
      const handleData = {
       reportId: report.reportId,
        status: 1,
        handleResult: '举报属实，已处理相关内容'
      }
      
      await api.handleReport(handleData)
      
      wx.hideLoading()
      wx.showToast({
        title: '处理成功',
        icon: 'success'
      })
      
      // 重新加载列表
     this.setData({
       currentPage: 1,
       reportList: []
      })
     this.loadReportList()
      
    } catch (error) {
      wx.hideLoading()
      wx.showToast({
        title: error.message || '处理失败',
        icon: 'none'
      })
    }
  },

  // 选择举报项（用于批量操作）已删除
  
  // 全选/取消全选 已删除
  
  // 批量处理举报 已删除
  
  // 导出举报数据 已删除
  
  // 快速驳回
  async quickReject(e) {
    const report = e.currentTarget.dataset.report
    wx.showModal({
      title: '确认驳回',
      content: '确定要驳回该举报吗？',
      success: async (res) => {
       if (res.confirm) {
          wx.showLoading({
            title: '驳回中...'
          })
          
          try {
            const handleData = {
             reportId: report.reportId,
              status: 2,
              handleResult: '举报不属实，已驳回'
            }
            
            await api.handleReport(handleData)
            
            wx.hideLoading()
            wx.showToast({
              title: '驳回成功',
              icon: 'success'
            })
            
            // 重新加载列表
           this.setData({
             currentPage: 1,
             reportList: []
            })
           this.loadReportList()
            
          } catch (error) {
            wx.hideLoading()
            wx.showToast({
              title: error.message || '驳回失败',
              icon: 'none'
            })
          }
        }
      }
    })
  }
})

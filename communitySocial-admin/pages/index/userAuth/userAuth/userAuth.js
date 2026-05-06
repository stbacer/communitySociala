// pages/userAuth/userAuth.js
const api = require('../../../../utils/api.js')
const auth = require('../../../../utils/auth.js')

Page({
  data: {
   userList: [],
   currentPage: 1,
   pageSize: 10,
   total: 0,
   loading: false,
    hasMore: true,
    searchKeyword: '',
    showImagePreview: false,
    previewImages: [],
   currentPreviewIndex: 0
  },

  onLoad() {
    // 检查登录状态
   if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login/login'
      })
     return
    }
    
   this.loadUserList()
  },

  onShow() {
    // 页面显示时刷新数据
   if (auth.isLoggedIn()) {
     this.loadUserList()
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
   this.setData({
     currentPage: 1,
     userList: [],
      hasMore: true
    })
   this.loadUserList().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 上拉加载更多
  onReachBottom() {
   if (this.data.hasMore && !this.data.loading) {
     this.loadMore()
    }
  },

  // 加载用户列表
  loadUserList() {
   if (this.data.loading) return
    
   this.setData({ loading: true })
    
    const params = {
     page: this.data.currentPage,
      size: this.data.pageSize
    }
    
    // 如果有搜索关键词，添加到参数中
   if (this.data.searchKeyword) {
     params.keyword = this.data.searchKeyword
    }
    
   return api.getPendingAuthUsers(params).then(res => {
      const newList = res.data.records || res.data.list || []
      const total = res.data.total || 0
      
      // 处理身份证图片数据
      const processedList = newList.map(user => {
       if (user.identityImages) {
          try {
            // 如果 identityImages 是 JSON 字符串，解析它
           if (typeof user.identityImages === 'string') {
             user.identityImagesArray = JSON.parse(user.identityImages)
            } else {
             user.identityImagesArray = user.identityImages
            }
          } catch (e) {
           user.identityImagesArray = []
          }
        } else {
         user.identityImagesArray = []
        }
        
        // 格式化时间字段
        if (user.createTime) {
          user.formattedCreateTime = this.formatFullTime(user.createTime)
        }
        
       return user
      })
      
     this.setData({
       userList: this.data.currentPage === 1 ? processedList: [...this.data.userList, ...processedList],
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
   this.loadUserList()
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
     userList: [],
      hasMore: true
    })
   this.loadUserList()
  },
  
  // 清空搜索
  onClearSearch() {
   this.setData({
      searchKeyword: '',
     currentPage: 1,
     userList: [],
      hasMore: true
    })
   this.loadUserList()
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
  
  // 查看认证详情（跳转到详情页）
  viewAuthDetail(e) {
    const user = e.currentTarget.dataset.user
    wx.navigateTo({
      url: `/pages/index/userAuth/authDetail/authDetail?userId=${user.userId}`
    })
  },

  // 头像加载错误处理
  onAvatarLoadError(e) {

  }
})

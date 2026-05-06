// pages/post/create.js
const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')
const tencentMap = require('../../utils/tencentMap.js')
const BASE_URL = 'http://localhost:8080'

Page({
  data: {
    mode: 'create',
    postId: '',
    categories: [],
    selectedCategoryIndex: -1,
    selectedCategoryId: '',
    formData: {
      title: '',
      content: '',
      imageUrls: [],
      isAnonymous: false,
      location: null
    },
    // 二手交易相关字段
    secondHandData: {
      price: '',
      transactionMode: 1, // 1-自提，2-快递，3 两者皆可
      contactInfo: ''
    },
    transactionModes: ['仅支持自提', '仅支持快递', '自提和快递均可'],
    currentLocation: null,
    maxImages: 9,
    uploading: false,
    isSubmitting: false,
    textareaHeight: '200rpx', // textarea 动态高度
    pageState: {
      isLoading: false,
      isError: false,
      errorMessage: ''
    }
  },

  onLoad(options) {
    // 优先检查是否从“我的帖子”页面跳转过来的编辑模式
    const editPostId = wx.getStorageSync('edit_post_id')
    const editMode = wx.getStorageSync('edit_post_mode')
    
    if (editMode === 'edit' && editPostId) {
      // 清除存储的编辑信息
      wx.removeStorageSync('edit_post_id')
      wx.removeStorageSync('edit_post_mode')
      
      this.setData({ mode: 'edit', postId: editPostId })
      // 编辑模式下，先加载板块列表，再加载帖子数据
      this.loadCategories().then(() => {
        this.loadPostData(editPostId)
      })
      return
    }
    
    // 其次检查 URL 参数（兼容旧的方式）
    if (options.mode === 'edit' && options.postId) {
      this.setData({ mode: 'edit', postId: options.postId })
      // 编辑模式下，先加载板块列表，再加载帖子数据
      this.loadCategories().then(() => {
        this.loadPostData(options.postId)
      })
      return
    }
    
    // 创建模式
    // 检查是否需要实名认证
    if (auth.needsAuthentication()) {
      wx.redirectTo({
        url: '/pages/profile/auth/auth'
      })
      return
    }
    
    this.checkAuthStatus()
    this.getCurrentLocation()
    this.loadDraft()
    this.loadCategories()
  },

  onShow() {
    // 检查是否有编辑信息（从 switchTab 跳转过来的情况）
    const editPostId = wx.getStorageSync('edit_post_id')
    const editMode = wx.getStorageSync('edit_post_mode')
    
    if (editMode === 'edit' && editPostId && this.data.mode !== 'edit') {
      // 清除存储的编辑信息
      wx.removeStorageSync('edit_post_id')
      wx.removeStorageSync('edit_post_mode')
      
      this.setData({ mode: 'edit', postId: editPostId })
      // 编辑模式下，先加载板块列表，再加载帖子数据
      this.loadCategories().then(() => {
        this.loadPostData(editPostId)
      })
    }
  },

  async loadPostData(postId) {
    try {
      wx.showLoading({ title: '加载中...' })
      const response = await api.getPostDetail(postId)
      const postData = response.data
      
      let categoryIndex = -1
      if (postData.categoryId) {
        categoryIndex = this.data.categories.findIndex(cat => cat.categoryId === postData.categoryId)
      }
      
      this.setData({
        selectedCategoryIndex: categoryIndex,
        selectedCategoryId: postData.categoryId || '',
        formData: {
          title: postData.title || '',
          content: postData.content || '',
          imageUrls: postData.imageUrls || [],
          isAnonymous: postData.isAnonymous === 1,
          location: postData.location ? {
            longitude: postData.longitude,
            latitude: postData.latitude,
            address: postData.location
          } : null
        },
        currentLocation: postData.location ? {
          longitude: postData.longitude,
          latitude: postData.latitude,
          address: postData.location
        } : null
      })
      
      // 加载完成后调整 textarea 高度
      if (postData.content) {
        setTimeout(() => {
          this.autoResizeTextarea(postData.content)
        }, 100)
      }
      
      wx.hideLoading()
      wx.setNavigationBarTitle({ title: '编辑帖子' })
    } catch (error) {
      wx.hideLoading()
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 1500)
    }
  },
  
  async loadCategories() {
    try {
      wx.showLoading({ title: '加载中...' })
      const response = await api.getCategoryList()


      let categories = []
      if (response && response.code === 200 && response.data) {
        categories = response.data
      }


      const enabledCategories = categories.filter(cat => cat.status === 1)


      this.setData({
        categories: enabledCategories,
        selectedCategoryIndex: -1,
        selectedCategoryId: ''
      })
      
      wx.hideLoading()
      
      if (enabledCategories.length === 0) {
        wx.showToast({ title: '暂无可用板块', icon: 'none', duration: 3000 })
      }
      
      return enabledCategories
    } catch (error) {
      wx.hideLoading()
      this.setData({ categories: [], selectedCategoryIndex: -1, selectedCategoryId: '' })
      wx.showToast({ title: '加载失败，请检查网络', icon: 'none', duration: 3000 })
      return []
    }
  },
  
  onSelectCategory(e) {
    const index = e.detail.value


    if (!this.data.categories || !this.data.categories[index]) {
      wx.showToast({ title: '板块数据未加载', icon: 'none' })
      return
    }
    
    const categoryId = this.data.categories[index].categoryId

    this.setData({ selectedCategoryIndex: index, selectedCategoryId: categoryId })
  },

  checkAuthStatus() {
    if (!auth.canPublishContent()) {
      wx.showModal({
        title: '提示',
        content: '请先完成实名认证才能发布内容',
        confirmText: '去认证',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/profile/auth/auth' })
          } else {
            wx.switchTab({ url: '/pages/index/index' })
          }
        }
      })
    }
  },

  getCurrentLocation() {
    this.setData({ 'pageState.isLoading': true, 'pageState.errorMessage': '' })
    
    wx.getSetting({
      success: (res) => {
        if (!res.authSetting['scope.userLocation']) {
          wx.authorize({
            scope: 'scope.userLocation',
            success: () => this.getLocation(),
            fail: () => this.handleLocationError('用户拒绝授权获取位置')
          })
        } else {
          this.getLocation()
        }
      },
      fail: (error) => {

        this.handleLocationError('获取位置权限设置失败')
      }
    })
  },
  
  getLocation() {
    // 使用微信原生获取位置
    wx.getLocation({
      type: 'gcj02',
      altitude: true,  // 获取高度信息
      success: (res) => {
        // 验证坐标合理性
        if (res.longitude < 73 || res.longitude > 135 || res.latitude < 3 || res.latitude > 54) {
          // 坐标异常警告（仅用于调试）
        }
        
        // 根据经纬度粗略判断城市（用于提示用户）
        let cityHint = '未知地区'
        const lng = res.longitude
        const lat = res.latitude
        
        if (lng >= 116.0 && lng <= 117.0 && lat >= 39.5 && lat <= 41.0) {
          cityHint = '北京市'
        } else if (lng >= 121.0 && lng <= 122.0 && lat >= 30.5 && lat <= 32.0) {
          cityHint = '上海市'
        } else if (lng >= 113.0 && lng <= 114.0 && lat >= 22.5 && lat <= 24.0) {
          cityHint = '广州市/深圳市'
        } else if (lng >= 106.0 && lng <= 107.0 && lat >= 29.0 && lat <= 30.0) {
          cityHint = '重庆市'
        } else if (lng >= 104.0 && lng <= 105.0 && lat >= 30.0 && lat <= 31.0) {
          cityHint = '成都市'
        } else if (lng >= 120.0 && lng <= 121.0 && lat >= 30.0 && lat <= 31.0) {
          cityHint = '杭州市'
        } else if (lng >= 118.5 && lng <= 119.5 && lat >= 31.5 && lat <= 32.5) {
          cityHint = '南京市'
        } else if (lng >= 114.0 && lng <= 115.0 && lat >= 30.0 && lat <= 31.0) {
          cityHint = '武汉市'
        } else if (lng >= 108.5 && lng <= 109.5 && lat >= 34.0 && lat <= 35.0) {
          cityHint = '西安市'
        }
        
        // 直接使用获取到的位置，不再弹出确认框
        this.setData({
          currentLocation: {
            longitude: res.longitude,
            latitude: res.latitude,
            address: '获取中...'
          },
          'pageState.isLoading': false
        })
        // 使用腾讯地图 API 进行逆地址解析
        this.getAddressFromCoordinates(res.longitude, res.latitude)
      },
      fail: (error) => {
        // 根据错误类型给出具体提示
        let errorMessage = '获取位置失败'
        if (error.errCode === 12001 || error.errMsg?.includes('auth deny')) {
          errorMessage = '用户拒绝授权，请在设置中开启位置权限'
        } else if (error.errCode === 12002 || error.errMsg?.includes('network timeout')) {
          errorMessage = '网络超时，请检查网络连接'
        } else if (error.errCode === 12003) {
          errorMessage = '定位失败，请检查GPS是否开启'
        }
        
        this.handleLocationError(errorMessage)
      }
    })
  },
  
  handleLocationError(errorMessage) {
    this.setData({ 'pageState.isLoading': false, 'pageState.errorMessage': errorMessage })
    wx.showToast({ title: errorMessage, icon: 'none', duration: 2000 })
  },
  
  async getAddressFromCoordinates(longitude, latitude) {
    try {
      wx.showLoading({ title: '获取地址中...' })
      // 调用腾讯地图 API 获取地址信息
      const addressInfo = await tencentMap.getAddressByLocation(longitude, latitude)
      wx.hideLoading()

      // 检查是否有降级警告
      let showToastMessage = '位置获取成功'
      if (addressInfo.warning) {
        showToastMessage = addressInfo.warning
      }
      
      this.setData({
        currentLocation: {
          longitude: longitude,
          latitude: latitude,
          address: addressInfo.formattedAddress || addressInfo.address,
          province: addressInfo.province,
          city: addressInfo.city,
          district: addressInfo.district
        },
        'formData.location': {
          longitude: longitude,
          latitude: latitude,
          address: addressInfo.formattedAddress || addressInfo.address
        }
      })
      
      wx.showToast({ 
        title: showToastMessage, 
        icon: addressInfo.warning ? 'none' : 'success',
        duration: 2000
      })
    } catch (error) {
      wx.hideLoading()
      
      // 如果腾讯地图 API 失败，使用默认地址
      this.setData({
        currentLocation: {
          longitude: longitude,
          latitude: latitude,
          address: `${longitude.toFixed(4)}, ${latitude.toFixed(4)}`
        },
        'formData.location': {
          longitude: longitude,
          latitude: latitude,
          address: `${longitude.toFixed(4)}, ${latitude.toFixed(4)}`
        }
      })
      
      wx.showToast({ 
        title: '获取地址失败，已显示坐标', 
        icon: 'none',
        duration: 2000
      })
    }
  },

  onTitleInput(e) { this.setData({ 'formData.title': e.detail.value }) },
  
  onContentInput(e) { 
    const content = e.detail.value
    this.setData({ 'formData.content': content })
    // 自动调整 textarea 高度
    this.autoResizeTextarea(content)
  },
  
  // 自动调整 textarea 高度
  autoResizeTextarea(content) {
    // 使用 wx.createSelectorQuery 获取 textarea 的实际高度
    const query = wx.createSelectorQuery()
    query.select('.form-textarea').boundingClientRect()
    query.exec((res) => {
      if (res && res[0]) {
        const scrollHeight = res[0].scrollHeight
        // 根据内容长度计算合适的高度（最小200rpx，最大600rpx）
        let newHeight = Math.max(200, Math.min(scrollHeight + 40, 600))
        // 更新 textarea 样式
        this.setData({ textareaHeight: newHeight + 'rpx' })
      }
    })
  },

  // 二手交易价格输入
  onPriceInput(e) {
    const price = e.detail.value
    // 验证价格格式（最多两位小数）
    if (price === '' || /^\d+(\.\d{0,2})?$/.test(price)) {
      this.setData({ 'secondHandData.price': price })
    } else {
      wx.showToast({ title: '请输入有效价格', icon: 'none' })
    }
  },

  // 选择交易方式
  onTransactionModeChange(e) {
    const index = e.detail.value
    const modes = [1, 2, 3] // 对应自提、快递、两者皆可
    this.setData({ 'secondHandData.transactionMode': modes[index] })
  },

  // 联系方式输入
  onContactInfoInput(e) {
    this.setData({ 'secondHandData.contactInfo': e.detail.value })
  },

  onChooseImage() {
    if (this.data.formData.imageUrls.length >= this.data.maxImages) {
      wx.showToast({ title: `最多只能上传${this.data.maxImages}张图片`, icon: 'none' })
      return
    }
    const count = this.data.maxImages - this.data.formData.imageUrls.length
    wx.chooseImage({
      count: count,
      sizeType: ['original', 'compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => this.uploadImages(res.tempFilePaths),
      fail: (err) =>    })
  },

  uploadImages(tempFilePaths) {
    if (this.data.uploading) return
    this.setData({ uploading: true })
    
    const uploadPromises = tempFilePaths.map(filePath => this.realUploadImage(filePath))
    
    Promise.all(uploadPromises)
      .then(results => {
        const newImageUrls = [...this.data.formData.imageUrls, ...results]
        this.setData({ 'formData.imageUrls': newImageUrls, uploading: false })
        wx.showToast({ title: `成功上传${results.length}张图片`, icon: 'success' })
      })
      .catch(error => {
        this.setData({ uploading: false })
        wx.showToast({ title: '图片上传失败', icon: 'none' })
      })
  },

  realUploadImage(filePath) {
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: BASE_URL + '/image/upload',
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': 'Bearer ' + wx.getStorageSync('user_token')
        },
        success: (res) => {


          try {
            const data = JSON.parse(res.data)

            if (data.code === 200) {
              let imageUrl = data.data

              // 处理返回的图片 URL，确保是完整 URL
              if (imageUrl && typeof imageUrl === 'string') {
                if (imageUrl.startsWith('/')) {
                  imageUrl = BASE_URL + imageUrl
                } else if (!imageUrl.startsWith('http://') && !imageUrl.startsWith('https://')) {
                  imageUrl = BASE_URL + '/' + imageUrl
                }
              }

              resolve(imageUrl)
            } else {
              const errorMsg = data.message || '上传失败'
              reject(new Error(errorMsg))
            }
          } catch (e) {
            reject(new Error('解析失败：' + e.message))
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  },

  onDeleteImage(e) {
    const index = e.currentTarget.dataset.index
    const imageUrls = this.data.formData.imageUrls.filter((_, i) => i !== index)
    this.setData({ 'formData.imageUrls': imageUrls })
  },

  onPreviewImage(e) {
    const index = e.currentTarget.dataset.index
    wx.previewImage({
      urls: this.data.formData.imageUrls,
      current: index
    })
  },

  onToggleAnonymous(e) {
    this.setData({ 'formData.isAnonymous': e.detail.value })
  },

  onSelectLocation() { 
    // 使用腾讯地图选择位置
    this.chooseLocationWithTencentMap()
  },
  
  async chooseLocationWithTencentMap() {
    try {
      wx.showLoading({ title: '打开地图中...' })
      // 调用腾讯地图工具类选择位置
      const locationInfo = await tencentMap.chooseLocation()
      wx.hideLoading()

      // 更新位置信息
      this.setData({
        currentLocation: {
          longitude: locationInfo.longitude,
          latitude: locationInfo.latitude,
          address: locationInfo.address || locationInfo.name || '未知位置'
        },
        'formData.location': {
          longitude: locationInfo.longitude,
          latitude: locationInfo.latitude,
          address: locationInfo.address || locationInfo.name || '未知位置'
        }
      })
      
      wx.showToast({ 
        title: '位置已选择', 
        icon: 'success',
        duration: 1500
      })
    } catch (error) {
      wx.hideLoading()
      wx.showToast({ 
        title: '选择位置失败，请重试', 
        icon: 'none',
        duration: 2000
      })
    }
  },
  
  onClearLocation() {
    this.setData({ currentLocation: null, 'formData.location': null })
  },

  async onSubmit() {
    // 验证必填字段
    if (!this.data.formData.title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' })
      return
    }
    if (!this.data.formData.content.trim()) {
      wx.showToast({ title: '请输入内容', icon: 'none' })
      return
    }
    if (!this.data.selectedCategoryId) {
      wx.showToast({ title: '请选择板块', icon: 'none' })
      return
    }

    // 如果是二手交易类型，验证价格和联系方式
    const isSecondHand = this.data.selectedCategoryId == 2
    if (isSecondHand) {
      if (!this.data.secondHandData.price || parseFloat(this.data.secondHandData.price) <= 0) {
        wx.showToast({ title: '请输入有效价格', icon: 'none' })
        return
      }
      if (!this.data.secondHandData.contactInfo || !this.data.secondHandData.contactInfo.trim()) {
        wx.showToast({ title: '请输入联系方式', icon: 'none' })
        return
      }
    }

    // 准备提交数据 - imageUrls 直接传递数组
    const postData = {
      postId: this.data.postId,  // 编辑模式下需要传递帖子ID
      title: this.data.formData.title.trim(),
      content: this.data.formData.content.trim(),
      imageUrls: this.data.formData.imageUrls,  // ✅ 直接传递数组
      longitude: this.data.currentLocation?.longitude || null,
      latitude: this.data.currentLocation?.latitude || null,
      categoryId: this.data.selectedCategoryId,  // 板块分类 ID
      isAnonymous: this.data.formData.isAnonymous ? 1 : 0
    }

    // 添加二手交易相关字段
    if (isSecondHand) {
      postData.price = parseFloat(this.data.secondHandData.price)
      postData.transactionMode = this.data.secondHandData.transactionMode
      postData.contactInfo = this.data.secondHandData.contactInfo.trim()
    }


    try {
      wx.showLoading({ title: this.data.mode === 'edit' ? '保存中...' : '发布中...', mask: true })
      
      let result
      if (this.data.mode === 'edit') {
        // 编辑模式：调用更新接口
        result = await api.updatePost(postData)
        wx.hideLoading()
        wx.showToast({ title: '保存成功', icon: 'success', duration: 1500 })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        // 创建模式：调用创建接口
        result = await api.createPost(postData)
        wx.hideLoading()
        wx.showToast({ title: '发布成功', icon: 'success', duration: 1500 })
        setTimeout(() => {
          wx.switchTab({ url: '/pages/index/index' })
        }, 1500)
      }
    } catch (error) {
      wx.hideLoading()
      wx.showToast({ title: error.message || (this.data.mode === 'edit' ? '保存失败' : '发布失败') + '，请稍后重试', icon: 'none', duration: 2000 })
    }
  },

  onCancel() {
    wx.showModal({
      title: '提示',
      content: '确定要取消发布吗？已输入的内容将不会保存',
      success: (res) => {
        if (res.confirm) wx.navigateBack()
      }
    })
  },

  onReset() {
    wx.showModal({
      title: '提示',
      content: '确定要重置所有输入吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            formData: { title: '', content: '', imageUrls: [], isAnonymous: false, location: null },
            selectedCategoryIndex: -1,
            selectedCategoryId: '',
            currentLocation: null
          })
        }
      }
    })
  },

  onSaveDraft() {
    wx.setStorageSync('post_draft', {
      title: this.data.formData.title,
      content: this.data.formData.content,
      imageUrls: this.data.formData.imageUrls
    })
    wx.showToast({ title: '草稿已保存', icon: 'success' })
  },

  loadDraft() {
    const draft = wx.getStorageSync('post_draft')
    if (draft) {
      this.setData({
        formData: { ...this.data.formData, ...draft }
      })
    }
  }
})

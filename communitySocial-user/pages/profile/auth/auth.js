// pages/profile/auth/auth.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    // 表单数据
    authForm: {
      realName: '',
      idCard: '',
      province: '',
      city: '',
      district: '',
      community: '',
      identityImages: []
    },
    
    // 页面状态
    submitting: false,
    loading: false,
    
    // 配置信息
    maxIdentityImages: 3,
    uploadingIdentity: false,
    
    // 省市区和社区选择器数据
    regionRange: [[], [], []],  // 省市区三级数据
    regionValue: [0, 0, 0],     // 当前选中索引 [省，市，区]
    selectedRegionText: '',      // 选中的省市区文本
    
    communityRange: [],         // 社区列表
    communityIndex: -1,         // 当前选中社区索引
    selectedCommunityText: '',   // 选中的社区文本
    
    // 是否为直辖市
    isDirectCity: false,
    
    // 缓存的社区管理员区域数据
    adminRegions: null,
    
    // 表单验证规则
    authRules: {
      realName: { required: true, minLength: 2, maxLength: 20 },
      idCard: { required: true, pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/ },
      community: { required: true, minLength: 2, maxLength: 50 }
    }
  },

  onLoad(options) {
    // 检查用户认证状态
    this.checkUserAuthStatus()
    
    // 加载社区管理员的区域数据
    this.loadAdminRegions()
    
    // 如果是从认证失败状态过来的，预填充信息
    if (options.from === 'retry' && options.userInfo) {
      try {
        const userInfo = JSON.parse(decodeURIComponent(options.userInfo))
        
        // 确保 identityImages 是数组
        let identityImages = []
        if (Array.isArray(userInfo.identityImages)) {
          identityImages = userInfo.identityImages
        } else if (userInfo.identityImages && typeof userInfo.identityImages === 'string') {
          try {
            // 按竖线拆分字符串
            const parsed = userInfo.identityImages.split('|').filter(url => url.trim() !== '')
            if (Array.isArray(parsed)) {
              identityImages = parsed
            }
          } catch (e) {
          }
        }
        
        this.setData({
          'authForm.realName': userInfo.realName || '',
          'authForm.idCard': userInfo.idCard || '',
          'authForm.province': userInfo.province || '',
          'authForm.city': userInfo.city || '',
          'authForm.district': userInfo.district || '',
          'authForm.community': userInfo.community || '',
          'authForm.identityImages': identityImages
        })
      } catch (e) {

      }
    }
  },
  
  // 检查用户认证状态
  checkUserAuthStatus() {
    const authStatus = auth.checkAuthStatus()
    
    // 如果已经认证成功，提示用户并跳转到首页
    if (authStatus === 2) {
      wx.showModal({
        title: '提示',
        content: '您已经完成实名认证，无需重复认证。',
        showCancel: false,
        confirmText: '确定',
        success: () => {
          wx.switchTab({
            url: '/pages/index/index'
          })
        }
      })
      return
    }
    
    // 如果认证审核中，显示等待提示
    if (authStatus === 1) {
      wx.showModal({
        title: '认证审核中',
        content: '您的实名认证正在审核中，请耐心等待管理员审核。审核通过后将自动解锁全部功能。',
        showCancel: false,
        confirmText: '我知道了',
        success: () => {
          // 不允许返回，继续停留在认证页面
          // 用户可以通过底部导航栏切换到其他页面，但会被 app.js 拦截
        }
      })
      
      // 设置页面为只读模式
      this.setData({
        submitting: true, // 禁用提交按钮
        loading: true     // 显示加载状态
      })
    }
    
    // 如果认证失败（status=3），允许重新提交
    // 如果未认证（status=0），正常填写表单
  },

  // 加载社区管理员的区域数据
  loadAdminRegions() {
    wx.showLoading({
      title: '加载中...',
      mask: true
    })
    
    api.getAdminRegions().then(res => {
      wx.hideLoading()
      
      if (res.code === 200 && res.data) {
        const { provinces, cities, districts, communities } = res.data
        
        // 构建省市区三级数据结构
        const regionRange = this.buildRegionRange(provinces, cities, districts)
        
        this.setData({
          adminRegions: res.data,
          regionRange: regionRange,
          communityRange: communities
        })} else {
        wx.showToast({
          title: '加载区域数据失败',
          icon: 'none'
        })
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      })
    })
  },
  
  // 构建省市区三级数据结构
  buildRegionRange(provinces, cities, districts) {
    // 第一级：省份
    const provinceLevel = provinces.map(p => ({ name: p.name, code: p.code }))
    
    // 第二级：城市（根据第一个省份初始化）
    let cityLevel = []
    if (provinceLevel.length > 0 && cities && cities.length > 0) {
      const firstProvince = provinceLevel[0].name
      cityLevel = cities
        .filter(c => c.province === firstProvince)
        .map(c => ({ name: c.name, code: c.code }))
    }
    
    // 第三级：区县（根据第一个城市初始化）
    let districtLevel = []
    if (cityLevel.length > 0 && districts && districts.length > 0) {
      const firstCity = cityLevel[0].name
      districtLevel = districts
        .filter(d => d.city === firstCity)
        .map(d => ({ name: d.name, code: d.code }))
    }
    
    return [provinceLevel, cityLevel, districtLevel]
  },
  
  // 省市区选择改变（确认选择）
  onRegionChange(e) {
    const { regionRange, adminRegions } = this.data
    const indexes = e.detail.value
    
    if (indexes.length !== 3) {
      return
    }
    
    const [provinceIndex, cityIndex, districtIndex] = indexes
    const selectedProvince = regionRange[0][provinceIndex]
    const selectedCity = regionRange[1][cityIndex]
    const selectedDistrict = regionRange[2][districtIndex]
    
    if (!selectedProvince || !selectedCity || !selectedDistrict) {
      return
    }
    
    const isDirectCity = ['北京市', '天津市', '上海市', '重庆市'].includes(selectedProvince.name)
    
    this.setData({
      regionValue: indexes,
      selectedRegionText: `${selectedProvince.name} ${selectedCity.name} ${selectedDistrict.name}`,
      isDirectCity: isDirectCity,
      'authForm.province': selectedProvince.name,
      'authForm.city': isDirectCity ? selectedProvince.name : selectedCity.name,
      'authForm.district': selectedDistrict.name
    })
    
    // 根据选择的省市区过滤社区列表
    this.filterCommunitiesByRegion(selectedProvince.name, selectedCity.name, selectedDistrict.name)
    
    // 更新社区选择器提示
    this.updateCommunityHint()
  },
  
  // 省市区列改变（联动逻辑）
  onColumnChange(e) {
    const { regionRange, adminRegions } = this.data
    const { column, value } = e.detail
    
    const newRegionRange = [...regionRange]
    
    if (column === 0) {
      // 第一列（省份）改变
      const selectedProvince = newRegionRange[0][value].name
      const isDirectCity = ['北京市', '天津市', '上海市', '重庆市'].includes(selectedProvince)
      
      // 更新第二列（城市）
      let cities = []
      if (adminRegions.cities) {
        cities = adminRegions.cities
          .filter(c => c.province === selectedProvince)
          .map(c => ({ name: c.name, code: c.code }))
      }
      newRegionRange[1] = cities
      
      // 更新第三列（区县）
      let districts = []
      if (cities.length > 0 && adminRegions.districts) {
        const firstCity = cities[0].name
        districts = adminRegions.districts
          .filter(d => d.city === firstCity)
          .map(d => ({ name: d.name, code: d.code }))
      }
      newRegionRange[2] = districts
      
      // 重置选中索引
      this.setData({
        regionRange: newRegionRange,
        regionValue: [value, 0, 0],
        isDirectCity: isDirectCity
      })
    } else if (column === 1) {
      // 第二列（城市）改变
      const selectedProvince = newRegionRange[0][this.data.regionValue[0]].name
      const selectedCity = newRegionRange[1][value].name
      
      // 更新第三列（区县）
      let districts = []
      if (adminRegions.districts) {
        districts = adminRegions.districts
          .filter(d => d.province === selectedProvince && d.city === selectedCity)
          .map(d => ({ name: d.name, code: d.code }))
      }
      newRegionRange[2] = districts
      
      // 重置选中索引
      this.setData({
        regionRange: newRegionRange,
        regionValue: [this.data.regionValue[0], value, 0]
      })
    }
  },
  
  // 根据省市区过滤社区列表
  filterCommunitiesByRegion(province, city, district) {
    const { adminRegions } = this.data
    
    if (!adminRegions || !adminRegions.communities) {
      return
    }
    
    // 过滤出该省市区的所有社区
    const filteredCommunities = adminRegions.communities
      .filter(community => 
        community.province === province && 
        community.city === city && 
        community.district === district
      )
      .map(community => community.name)

    this.setData({
      communityRange: filteredCommunities,
      communityIndex: -1,
      selectedCommunityText: '',
      'authForm.community': ''
    })
  },
  
  // 更新社区选择器提示
  updateCommunityHint() {
    // 这个方法可以根据需要动态加载对应区域的社区列表
    // 目前社区列表是从后端一次性获取的所有社区
    // 后续可以根据 selectedProvince 和 selectedCity 过滤社区
  },
  
  // 社区选择改变
  onCommunityChange(e) {
    const index = e.detail.value
    const { communityRange } = this.data
    
    if (!communityRange[index]) {
      return
    }
    
    const selectedCommunity = communityRange[index]
    
    this.setData({
      communityIndex: index,
      selectedCommunityText: selectedCommunity,
      'authForm.community': selectedCommunity
    })
  },

  onShow() {
    // 页面显示时检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
    }
  },

  // 输入真实姓名
  onRealNameInput(e) {
    this.setData({
      'authForm.realName': e.detail.value
    })
  },

  // 输入身份证号
  onIdCardInput(e) {
    this.setData({
      'authForm.idCard': e.detail.value
    })
  },

  // 选择身份证明图片
  onSelectIdentityImage() {
    if (this.data.authForm.identityImages.length >= this.data.maxIdentityImages) {
      wx.showToast({
        title: `最多只能上传${this.data.maxIdentityImages}张图片`,
        icon: 'none'
      })
      return
    }
    
    const that = this
    wx.showActionSheet({
      itemList: ['拍照', '从相册选择'],
      success: function(res) {
        if (res.tapIndex === 0) {
          that.chooseIdentityImage('camera')
        } else if (res.tapIndex === 1) {
          that.chooseIdentityImage('album')
        }
      }
    })
  },

  // 选择身份证明图片
  chooseIdentityImage(sourceType) {
    const that = this
    const count = this.data.maxIdentityImages - this.data.authForm.identityImages.length
    
    wx.chooseImage({
      count: count,
      sizeType: ['compressed'],
      sourceType: [sourceType],
      success: function(res) {
        that.uploadIdentityImages(res.tempFilePaths)
      },
      fail: function(err) {
        wx.showToast({
          title: '选择图片失败',
          icon: 'none'
        })
      }
    })
  },

  // 上传身份证明图片
  async uploadIdentityImages(tempFilePaths) {
    if (this.data.uploadingIdentity) return
    
    this.setData({ uploadingIdentity: true })
    
    try {
      const uploadPromises = tempFilePaths.map(filePath => {
        return api.uploadImage(filePath)
      })
      
      const results = await Promise.all(uploadPromises)
      const imageUrls = results.map(result => result.data)
      
      this.setData({
        'authForm.identityImages': [...this.data.authForm.identityImages, ...imageUrls],
        uploadingIdentity: false
      })
      
      wx.showToast({
        title: `成功上传${imageUrls.length}张图片`,
        icon: 'success'
      })
      
    } catch (error) {
      wx.showToast({
        title: '图片上传失败',
        icon: 'none'
      })
      this.setData({ uploadingIdentity: false })
    }
  },

  // 删除身份证明图片
  onDeleteIdentityImage(e) {
    const index = e.currentTarget.dataset.index
    const images = [...this.data.authForm.identityImages]
    images.splice(index, 1)
    
    this.setData({
      'authForm.identityImages': images
    })
  },

  // 预览身份证明图片
  onPreviewIdentityImage(e) {
    const urls = this.data.authForm.identityImages
    const current = e.currentTarget.dataset.url
    
    wx.previewImage({
      current: current,
      urls: urls
    })
  },

  // 表单验证
  validateAuthForm() {
    const { realName, idCard, province, city, district, community, identityImages } = this.data.authForm
    const rules = this.data.authRules
    
    // 验证真实姓名
    if (!realName || realName.trim().length === 0) {
      wx.showToast({
        title: '请输入真实姓名',
        icon: 'none'
      })
      return false
    }
    
    const trimmedRealName = realName.trim()
    if (trimmedRealName.length < rules.realName.minLength) {
      wx.showToast({
        title: `姓名至少${rules.realName.minLength}个字符`,
        icon: 'none'
      })
      return false
    }
    
    // 验证身份证号
    if (!idCard || idCard.trim().length === 0) {
      wx.showToast({
        title: '请输入身份证号',
        icon: 'none'
      })
      return false
    }
    
    const trimmedIdCard = idCard.trim()
    const idCardPattern = new RegExp('^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$');
    if (!idCardPattern.test(trimmedIdCard)) {
      wx.showToast({
        title: '身份证号格式不正确',
        icon: 'none'
      })
      return false
    }
    
    // 验证省市
    if (!province || province.trim().length === 0) {
      wx.showToast({
        title: '请选择所在省份',
        icon: 'none'
      })
      return false
    }
    
    // 验证城市（非直辖市）
    if (!this.data.isDirectCity && (!city || city.trim().length === 0)) {
      wx.showToast({
        title: '请选择所在城市',
        icon: 'none'
      })
      return false
    }
    
    // 验证区县
    if (!district || district.trim().length === 0) {
      wx.showToast({
        title: '请选择所在区县',
        icon: 'none'
      })
      return false
    }
    
    // 验证社区信息
    if (!community || community.trim().length === 0) {
      wx.showToast({
        title: '请选择所在社区',
        icon: 'none'
      })
      return false
    }
    
    // 验证身份证明图片
    if (!identityImages || identityImages.length === 0) {
      wx.showToast({
        title: '请上传至少一张社区身份证明图片',
        icon: 'none'
      })
      return false
    }
    
    if (identityImages.length > this.data.maxIdentityImages) {
      wx.showToast({
        title: `最多只能上传${this.data.maxIdentityImages}张图片`,
        icon: 'none'
      })
      return false
    }
    
    return true
  },

  // 提交认证
  async onSubmitAuth() {
    if (!this.validateAuthForm()) {
      return
    }
    
    if (this.data.submitting) return
    
    wx.showModal({
      title: '确认提交',
      content: '提交后需要等待管理员审核，确定要提交认证信息吗？',
      success: async (res) => {
        if (res.confirm) {
          this.setData({ submitting: true })
          
          wx.showLoading({
            title: '提交中...',
            mask: true
          })
          
          try {
            const authData = {
              realName: this.data.authForm.realName.trim(),
              idCard: this.data.authForm.idCard.trim(),
              province: this.data.authForm.province.trim(),
              city: this.data.authForm.city.trim(),
              district: this.data.authForm.district.trim(),
              community: this.data.authForm.community.trim(),
              identityImages: this.data.authForm.identityImages
            }
            
            const response = await api.submitAuth(authData)
                        
            wx.hideLoading()
                                    
            // 更新用户信息中的认证状态为“审核中”
            const userInfo = wx.getStorageSync('user_info')
            if (userInfo) {
              userInfo.authStatus = 1 // 1表示审核中
              auth.saveUserInfo(userInfo)
            }
                                    
            // 显示成功提示
            wx.showToast({
              title: '提交成功',
              icon: 'success',
              duration: 1500
            })
                                    
            // 1.5秒后跳转到登录页，让用户重新登录以刷新认证状态
            setTimeout(() => {
              // 清除token，强制用户重新登录
              wx.removeStorageSync('user_token')
              
              wx.redirectTo({
                url: '/pages/login/login',
                fail: (err) => {
                  // 如果跳转失败，尝试使用 switchTab 到首页
                  wx.switchTab({
                    url: '/pages/index/index'
                  })
                }
              })
            }, 1500)
            
          } catch (error) {
            wx.hideLoading()
            let errorMsg = '提交失败'
            if (error && error.message) {
              errorMsg = error.message
            }
            
            wx.showToast({
              title: errorMsg,
              icon: 'none',
              duration: 2000
            })
            
            this.setData({ submitting: false })
          }
        }
      }
    })
  },

  // 返回上一页（只有在认证失败时才允许返回）
  onGoBack() {
    const authStatus = auth.checkAuthStatus()
    
    // 如果正在审核中或已提交，不允许返回
    if (authStatus === 1 || authStatus === 2) {
      wx.showToast({
        title: '认证审核中，无法返回',
        icon: 'none'
      })
      return
    }
    
    // 认证失败或未认证时允许返回
    wx.navigateBack({
      delta: 1
    })
  }
})

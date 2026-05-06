// pages/userAuth/authSubmit/authSubmit.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    formData: {
      realName: '',
      idCard: '',
      province: '',
      city: '',
      district: '',
      community: ''
    },
    identityImages: [],
    loading: false,
    
    // 省市区数据 - multiSelector 格式
    regionRange: [[], [], []],  // [省份列表，城市列表，区县列表]
    regionValue: [0, 0, 0],     // 当前选中索引
    selectedRegionText: '',     // 选中的地区文本
    
    // 缓存数据
    allProvinces: [],          // 所有省份
    tempCities: [],            // 临时城市数据
    tempDistricts: []          // 临时区县数据
  },

  onLoad() {
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    
    // 加载省市区数据
    this.loadRegionData()
  },

  // 加载省市区数据（从 Redis 缓存）
  loadRegionData() {
    const that = this
    
    wx.showLoading({
      title: '加载中...',
      mask: true
    })

    // 从后端 API 获取省份列表
    api.getProvinces().then(res => {
      wx.hideLoading()
      
      if (res && res.data && res.data.length > 0) {
        const provincesData = res.data.map(province => ({
          name: province.name,
          code: province.code,
          pinyin: province.pinyin || []
        }))
        
        // 默认加载第一个省份的城市数据
        const firstProvince = provincesData[0]
        
        // 获取第一个省份的城市数据
        api.getCitiesByProvinceCode(firstProvince.code).then(cityRes => {
          const cities = cityRes.data || []
          
          if (cities.length > 0) {
            // 保存所有城市数据
            that.setData({
              allCities: cities
            })
            
            // 默认加载第一个城市的区县数据
            const firstCity = cities[0]
            api.getDistrictsByCityCode(firstCity.code).then(districtRes => {
              const districts = districtRes.data || []
              
              that.setData({
                allProvinces: provincesData,
                tempCities: cities,
                tempDistricts: districts,
                regionRange: [provincesData, cities, districts],
                regionValue: [0, 0, 0]
              })
            }).catch(err => {
              that.setData({
                allProvinces: provincesData,
                tempCities: cities,
                tempDistricts: [],
                regionRange: [provincesData, cities, []],
                regionValue: [0, 0, 0]
              })
            })
          } else {
            that.setData({
              allProvinces: provincesData,
              tempCities: cities,
              tempDistricts: [],
              regionRange: [provincesData, cities, []],
              regionValue: [0, 0, 0]
            })
          }
        }).catch(err => {
          wx.showToast({
            title: '加载失败，请重试',
            icon: 'none'
          })
        })
      } else {
        that.setData({
          allProvinces: [],
          tempCities: [],
          tempDistricts: [],
          regionRange: [[], [], []],
          regionValue: [0, 0, 0]
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
  
  // 列改变时触发
  onColumnChange(e) {
    const { column, value } = e.detail
    const { regionValue, allProvinces } = this.data
    
    // 更新当前列的选中值
    regionValue[column] = value
    
    if (column === 0) {
      // 省份改变，重新加载城市和区县
      const selectedProvince = allProvinces[value]
      
      // 从后端 API 获取城市数据
      api.getCitiesByProvinceCode(selectedProvince.code).then(cityRes => {
        const cities = cityRes.data || []
        
        if (cities.length > 0) {
          this.setData({
            allCities: cities
          })
          
          // 加载第一个城市的区县数据
          const firstCity = cities[0]
          api.getDistrictsByCityCode(firstCity.code).then(districtRes => {
            const districts = districtRes.data || []
            
            this.setData({
              tempCities: cities,
              tempDistricts: districts,
              regionRange: [allProvinces, cities, districts],
              regionValue: [value, 0, 0]
            })
          }).catch(err => {
            this.setData({
              tempCities: cities,
              tempDistricts: [],
              regionRange: [allProvinces, cities, []],
              regionValue: [value, 0, 0]
            })
          })
        } else {
          this.setData({
            tempCities: cities,
            tempDistricts: [],
            regionRange: [allProvinces, cities, []],
            regionValue: [value, 0, 0]
          })
        }
      }).catch(err => {
        wx.showToast({
          title: '加载失败',
          icon: 'none'
        })
      })
    } else if (column === 1) {
      // 城市改变，重新加载区县
      const selectedCity = this.data.tempCities[value]
      
      if (selectedCity && selectedCity.code) {
        api.getDistrictsByCityCode(selectedCity.code).then(districtRes => {
          const districts = districtRes.data || []
          
          this.setData({
            tempDistricts: districts,
            regionRange: [this.data.allProvinces, this.data.tempCities, districts],
            regionValue: [regionValue[0], value, 0]
          })
        }).catch(err => {
          this.setData({
            tempDistricts: [],
            regionRange: [this.data.allProvinces, this.data.tempCities, []],
            regionValue: [regionValue[0], value, 0]
          })
        })
      } else {
        this.setData({
          tempDistricts: [],
          regionRange: [this.data.allProvinces, this.data.tempCities, []],
          regionValue: [regionValue[0], value, 0]
        })
      }
    }
  },
  
  // 省市区选择完成
  onRegionChange(e) {
    const { regionValue, allProvinces, tempCities, tempDistricts } = this.data
    const selectedProvince = allProvinces[regionValue[0]]
    const selectedCity = tempCities[regionValue[1]]
    const selectedDistrict = tempDistricts[regionValue[2]]
    
    this.setData({
      'formData.province': selectedProvince?.name || '',
      'formData.city': selectedCity?.name || '',
      'formData.district': selectedDistrict?.name || '',
      selectedRegionText: `${selectedProvince?.name}${selectedCity?.name}${selectedDistrict?.name}`
    })
  },

  // 输入框变化处理
  onInput(e) {
    const { field } = e.currentTarget.dataset
    const value = e.detail.value
    this.setData({
      [`formData.${field}`]: value
    })
  },

  // 上传图片
  uploadImage() {
    const that = this
    
    // 显示加载提示
    wx.showLoading({
      title: '上传中...',
      mask: true
    })
    
    wx.chooseMedia({
      count: 4 - this.data.identityImages.length,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: res => {
        const tempFiles = res.tempFiles
        
        // 逐个上传图片
        const uploadPromises = tempFiles.map((file, index) => {
          return new Promise((resolve, reject) => {
            wx.uploadFile({
              url: getApp().globalData.baseUrl + '/image/upload',
              filePath: file.tempFilePath,
              name: 'file',
              header: {
                'Authorization': 'Bearer ' + wx.getStorageSync('admin_token')
              },
              formData: {
                type: 'auth'  // 认证图片类型
              },
              success: (uploadRes) => {
                try {
                  const result = JSON.parse(uploadRes.data)
                  if (result.code === 200) {
                    resolve(result.data)
                  } else {
                    reject(new Error(result.message || '上传失败'))
                  }
                } catch (e) {
                  reject(e)
                }
              },
              fail: (err) => {
                reject(err)
              }
            })
          })
        })

        Promise.all(uploadPromises).then(imageUrls => {
          wx.hideLoading()
          
          that.setData({
            identityImages: [...that.data.identityImages, ...imageUrls]
          })
          
          wx.showToast({
            title: `成功上传${imageUrls.length}张图片`,
            icon: 'success'
          })
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({
            title: '上传失败，请重试',
            icon: 'none',
            duration: 2000
          })
        })
      },
      fail: (err) => {
        wx.hideLoading()
      }
    })
  },

  // 预览图片
  previewImage(e) {
    const index = e.currentTarget.dataset.index
    wx.previewImage({
      current: this.data.identityImages[index],
      urls: this.data.identityImages
    })
  },

  // 删除图片
  deleteImage(e) {
    const index = e.currentTarget.dataset.index
    this.data.identityImages.splice(index, 1)
    this.setData({
      identityImages: this.data.identityImages
    })
  },

  // 表单验证
  validateForm() {
    const { formData, identityImages } = this.data

    // 真实姓名验证
    if (!formData.realName) {
      wx.showToast({ title: '请输入真实姓名', icon: 'none' })
      return false
    }

    // 身份证号验证
    if (!formData.idCard || !/^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/.test(formData.idCard)) {
      wx.showToast({ title: '请输入正确的身份证号', icon: 'none' })
      return false
    }

    // 省市区验证
    if (!formData.province || !formData.city || !formData.district) {
      wx.showToast({ title: '请选择所在省市区', icon: 'none' })
      return false
    }

    // 社区验证
    if (!formData.community) {
      wx.showToast({ title: '请输入所在社区', icon: 'none' })
      return false
    }

    // 图片验证
    if (identityImages.length === 0) {
      wx.showToast({ title: '请上传身份证明图片', icon: 'none' })
      return false
    }

    return true
  },

  // 提交认证
  async handleSubmit() {
    if (!this.validateForm()) {
      return
    }

    this.setData({ loading: true })

    try {
      const { formData, identityImages } = this.data
      
      // 调用提交实名认证 API
      await api.submitAuth({
        realName: formData.realName,
        idCard: formData.idCard,
        province: formData.province,
        city: formData.city,
        district: formData.district,
        community: formData.community,
        identityImages: identityImages
      })

      wx.showModal({
        title: '提交成功',
        content: '您的认证申请已提交，等待管理员审核\n\n审核通过后您将能够：\n• 管理社区帖子\n• 审核用户内容\n• 处理举报信息\n• 查看统计数据\n\n请耐心等待，审核结果将通过系统消息通知您。',
        showCancel: false,
        confirmText: '我知道了',
        success: () => {
          // 清除token，返回登录页重新登录
          wx.removeStorageSync('admin_token')
          wx.removeStorageSync('admin_phone')
          wx.redirectTo({
            url: '/pages/login/login'
          })
        }
      })
    } catch (err) {
      wx.showToast({ 
        title: err.response?.data?.message || '提交失败，请稍后重试', 
        icon: 'none',
        duration: 2000
      })
    } finally {
      this.setData({ loading: false })
    }
  }
})

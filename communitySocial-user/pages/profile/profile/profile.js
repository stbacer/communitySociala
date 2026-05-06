// pages/profile/profile.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    userInfo: null,
    loading: true,

    // 认证状态
    authStatus: 0, // 0未认证，1认证中，2已认证，3认证失败
    authStatusText: ['未认证', '认证中', '已认证', '认证失败'],
    authStatusColor: ['#999', '#ff9800', '#4caf50', '#f44336'],
    
    // 统计数据
    stats: {
      postCount: 0,
      likeCount: 0,
      followerCount: 0,
      followingCount: 0
    },
    
    // 功能菜单
    menuItems: [
      {
        id: 'my-posts',
        icon: '📝',
        title: '我的帖子',
        desc: '查看我发布的所有帖子',
        url: '/pages/profile/myposts/myposts'
      },
      {
        id: 'my-comments',
        icon: '💬',
        title: '我的评论',
        desc: '查看我的所有评论',
        url: '/pages/profile/mycomments/mycomments'
      },
      {
        id: 'my-collections',
        icon: '⭐',
        title: '我的收藏',
        desc: '查看我收藏的帖子',
        url: '/pages/profile/mycollections/mycollections'
      },
      {
        id: 'settings',
        icon: '⚙️',
        title: '个人信息编辑',
        desc: '修改密码、手机号、昵称等',
        url: '/pages/profile/account-settings/account-settings'
      }
    ],
    
    // 认证倒计时相关
    authCooldown: 0, // 认证冷却时间（秒）
    authCooldownTimer: null
  },

  onLoad() {
    this.checkLoginStatus()
    this.initAuthCooldown()
  },
  
  onUnload() {
    // 清除定时器
    if (this.data.authCooldownTimer) {
      clearInterval(this.data.authCooldownTimer)
    }
  },

  onShow() {
    if (auth.isLoggedIn()) {
      this.loadUserInfo()
    }
  },

  // 检查登录状态
  checkLoginStatus() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
    } else {
      this.loadUserInfo()
    }
  },

  // 加载用户信息
  async loadUserInfo() {
    const that = this
    this.setData({ loading: true })
    
    try {
      // 获取本地缓存的用户信息
      const localUserInfo = auth.getUserInfo()
      
      // 如果有本地信息，先显示
      if (localUserInfo) {
        this.setData({
          userInfo: localUserInfo,
          authStatus: localUserInfo.authStatus || 0
        })
      }
      
      // 调用 API 获取最新信息
      const response = await api.getCurrentUser()
      const userInfo = response.data


      // 处理头像 URL，确保是完整 URL，如果没有头像则使用默认头像
      if (userInfo.avatarUrl && userInfo.avatarUrl.trim() !== '') {
        // 检查是否已经是完整 URL
        if (!userInfo.avatarUrl.startsWith('http://') && !userInfo.avatarUrl.startsWith('https://')) {
          // 相对路径，需要拼接 BASE_URL
          userInfo.avatarUrl = api.handleImageUrl(userInfo.avatarUrl)
        }
        // 注意：不主动测试图片是否可以访问，因为微信开发者工具可能会阻止本地图片访问

      } else {
        // 使用默认头像
        userInfo.avatarUrl = '/images/default-avatar.png'

      }
      
      this.setData({
        userInfo: userInfo,
        authStatus: userInfo.authStatus || 0,
        loading: false,
      })


      // 更新本地缓存
      auth.saveUserInfo(userInfo)
      
      // 加载统计数据
      this.loadUserStats()
      
    } catch (error) {
      this.setData({ loading: false })
    }
  },

  // 加载用户统计数据
  async loadUserStats() {
    try {

      const response = await api.getUserStats();
      
      if (response.code === 200 && response.data) {
        const statsData = response.data;
        this.setData({
          stats: {
            postCount: statsData.postCount || 0,
            likeCount: statsData.likeCount || 0,
            followerCount: statsData.followerCount || 0,
            followingCount: statsData.followingCount || 0
          }
        });

      } else {
        // 如果API调用失败，使用默认值
        this.setData({
          stats: {
            postCount: 0,
            likeCount: 0,
            followerCount: 0,
            followingCount: 0
          }
        });
      }
    } catch (error) {
      // 出错时使用默认值
      this.setData({
        stats: {
          postCount: 0,
          likeCount: 0,
          followerCount: 0,
          followingCount: 0
        }
      });
    }
  },
  
  // 跳转到我的帖子页面
  onGoToMyPosts() {

    // 检查是否需要认证
    const authStatus = this.data.authStatus

    if (!auth.canPublishContent()) {

      wx.showModal({
        title: '提示',
        content: '请先完成实名认证才能使用此功能',
        confirmText: '去认证',
        success: (res) => {
          if (res.confirm) {
            this.onShowAuthModal()
          }
        }
      })
      return
    }
    
    // 跳转到我的帖子页面
    wx.navigateTo({
      url: '/pages/profile/myposts/myposts',
      success: () => {

      },
      fail: (error) => {
        wx.showToast({
          title: '跳转失败：' + (error.errMsg || '未知错误'),
          icon: 'none',
          duration: 3000
        })
      }
    })
  },
  
  // 头像图片加载失败处理
  onAvatarError(e) {
    // 使用默认头像
    this.setData({
      'userInfo.avatarUrl': '/images/default-avatar.png'
    })
  },

  // 退出登录
  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          auth.logout()
        }
      }
    })
  },

  // 认证操作统一入口
  onAuthActionTap(e) {


    const status = e.currentTarget.dataset.status
    const cooldown = e.currentTarget.dataset.cooldown


    if (status === 0 || status === 3) {
      // 未认证或认证失败，执行认证
      if (status === 0 && cooldown > 0) {
        wx.showToast({
          title: `${cooldown}秒后可重新认证`,
          icon: 'none'
        })
        return
      }
      this.onShowAuthModal()
    } else if (status === 1) {
      // 认证中
      wx.showToast({
        title: '认证审核中，请耐心等待',
        icon: 'none'
      })
    } else if (status === 2) {
      // 已认证
      this.onViewAuthDetail()
    }
  },

  // 跳转到认证页面
  onShowAuthModal() {


    if (this.data.authStatus === 2) {
      wx.showToast({
        title: '您已完成实名认证',
        icon: 'none'
      })

      return
    }
    
    // 跳转到独立的认证页面
    let url = '/pages/profile/auth/auth'

    // 如果是认证失败状态，传递用户信息用于预填充
    if (this.data.authStatus === 3 && this.data.userInfo) {
      const userInfo = encodeURIComponent(JSON.stringify({
        realName: this.data.userInfo.realName || '',
        idCard: this.data.userInfo.idCard || '',
        community: this.data.userInfo.community || '',
        identityImages: this.data.userInfo.identityImages || []
      }))
      url += `?from=retry&userInfo=${userInfo}`

    }

    wx.navigateTo({
      url: url,
      success: () => {

      },
      fail: (error) => {
        wx.showToast({
          title: '跳转失败: ' + (error.errMsg || '未知错误'),
          icon: 'none',
          duration: 3000
        })
      }
    })
  },

  // 初始化认证冷却时间
  initAuthCooldown() {
    const lastSubmitTime = wx.getStorageSync('auth_submit_time')
    if (lastSubmitTime) {
      const now = new Date().getTime()
      const timeDiff = now - lastSubmitTime
      const cooldownSeconds = 300 // 5分钟冷却时间
      
      if (timeDiff < cooldownSeconds * 1000) {
        const remainingSeconds = Math.ceil((cooldownSeconds * 1000 - timeDiff) / 1000)
        this.setData({
          authCooldown: remainingSeconds
        })
        
        // 启动倒计时
        this.startCooldownTimer()
      }
    }
  },
  
  // 启动冷却倒计时
  startCooldownTimer() {
    if (this.data.authCooldown <= 0) return
    
    const timer = setInterval(() => {
      this.setData({
        authCooldown: this.data.authCooldown - 1
      })
      
      if (this.data.authCooldown <= 0) {
        clearInterval(timer)
        this.setData({
          authCooldownTimer: null
        })
        wx.removeStorageSync('auth_submit_time')
      }
    }, 1000)
    
    this.setData({
      authCooldownTimer: timer
    })
  },

  // 菜单项点击
  onMenuItemTap(e) {
    const itemId = e.currentTarget.dataset.id
    const menuItem = this.data.menuItems.find(item => item.id === itemId)


    if (menuItem) {
      // 检查是否需要认证
      if (['my-posts', 'my-comments', 'my-collections'].includes(itemId)) {
        const authStatus = this.data.authStatus

        if (!auth.canPublishContent()) {

          wx.showModal({
            title: '提示',
            content: '请先完成实名认证才能使用此功能',
            confirmText: '去认证',
            success: (res) => {
              if (res.confirm) {
                this.onShowAuthModal()
              }
            }
          })
          return
        }
      }
      
      // 跳转到对应页面
      if (menuItem.url) {

        wx.navigateTo({
          url: menuItem.url,
          success: () => {

          },
          fail: (error) => {
            wx.showToast({
              title: '跳转失败：' + (error.errMsg || '未知错误'),
              icon: 'none',
              duration: 3000
            })
          }
        })
      } else {
      }
    } else {
    }
  },

  // 编辑个人信息
  onEditProfile() {
    wx.navigateTo({
      url: '/pages/profile/account-settings/account-settings'
    })
  },
  
  // 更改头像
  onChangeAvatar() {
    const that = this
    wx.showActionSheet({
      itemList: ['拍照', '从相册选择'],
      success: function(res) {
        if (res.tapIndex === 0) {
          // 拍照
          that.chooseImage('camera')
        } else if (res.tapIndex === 1) {
          // 从相册选择
          that.chooseImage('album')
        }
      }
    })
  },
  
  // 选择图片
  chooseImage(sourceType) {
    const that = this
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'], // 可以指定是原图还是压缩图，默认二者都有
      sourceType: [sourceType], // 可以指定来源是相册还是相机，默认二者都有
      success: function(res) {
        const tempFilePath = res.tempFilePaths[0]
        // 直接上传
        that.uploadDirectly(tempFilePath)
      },
      fail: function(err) {
        wx.showToast({
          title: '选择图片失败',
          icon: 'none'
        })
      }
    })
  },
  
  // 直接上传图片（省略裁剪步骤）
  uploadDirectly(filePath) {
    this.uploadAvatar(filePath)
  },
  
  // 上传头像
  async uploadAvatar(filePath) {
    wx.showLoading({
      title: '上传中...',
      mask: true
    })
    
    try {
      // 上传头像到服务器（会自动保存到数据库）
      const uploadResult = await api.uploadAvatar(filePath)
      const imageUrl = uploadResult.data
      
      wx.hideLoading()
      
      // 更新本地用户信息
      const updatedUserInfo = {
        ...this.data.userInfo,
        avatarUrl: api.handleImageUrl(imageUrl)
      }
      
      this.setData({
        userInfo: updatedUserInfo
      })
      
      // 更新本地存储
      auth.saveUserInfo(updatedUserInfo)
      
      wx.showToast({
        title: '头像更新成功',
        icon: 'success'
      })
      
    } catch (error) {
      wx.hideLoading()
      let errorMsg = '上传失败'
      if (error && error.message) {
        errorMsg = error.message
      }
      
      wx.showToast({
        title: errorMsg,
        icon: 'none'
      })
    }
  },

  // 查看认证详情
  onViewAuthDetail() {
    wx.navigateTo({
      url: '/pages/profile/auth-detail'
    })
  },

  // 分享功能
  onShareAppMessage() {
    return {
      title: '社区邻里 - 连接你我他',
      path: '/pages/index/index'
    }
  },

  // 联系客服
  onContactSupport() {
    wx.showModal({
      title: '联系客服',
      content: '客服微信：community_support\n工作时间：9:00-18:00',
      showCancel: false
    })
  }
})
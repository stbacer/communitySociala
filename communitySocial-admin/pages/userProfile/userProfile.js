// pages/dashboard/userProfile/userProfile.js
const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')
const BASE_URL = 'http://127.0.0.1:8080'

Page({
  data: {
    userInfo: null,
    loading: true,
    isEditing: false,
    // 头像 URL
    avatarUrl: '',
    // 表单数据
    formData: {
      nickname: '',
      phone: '',
      gender: 0, // 0:未知，1:男，2:女
      signature: ''
    },
    // 性别选项
    genderOptions: [
      { label: '未知', value: 0 },
      { label: '男', value: 1 },
      { label: '女', value: 2 }
    ],
    // 原始数据用于对比
    originalData: {},
    // 修改手机号弹窗
    showChangePhoneModal: false,
    phoneFormData: {
      newPhone: ''
    },
    // 修改密码弹窗
    showChangePasswordModal: false,
    passwordFormData: {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  },

  onLoad(options) {
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    
    // 如果是从首页跳转，传入 userId 参数
    const userId = options.userId
    if (userId) {
      this.loadUserInfo(userId)
    } else {
      // 否则加载当前登录用户信息
      this.loadCurrentUser()
    }
  },

  // 加载当前登录用户信息
  loadCurrentUser() {
    this.setData({ loading: true })
    
    api.getCurrentUser().then(res => {

      if (res.code === 200 && res.data) {


        this.processUserData(res.data)
      } else {
        throw new Error('获取用户信息失败')
      }
    }).catch(err => {
      this.setData({ loading: false })
      wx.showToast({
        title: '获取用户信息失败',
        icon: 'none'
      })
    })
  },

  // 加载指定用户信息（管理员查看其他用户）
  loadUserInfo(userId) {
    this.setData({ loading: true })
    
    api.getUserInfo(userId).then(res => {
      if (res.code === 200 && res.data) {
        this.processUserData(res.data)
      } else {
        throw new Error('获取用户信息失败')
      }
    }).catch(err => {
      this.setData({ loading: false })
      wx.showToast({
        title: '获取用户信息失败',
        icon: 'none'
      })
    })
  },

  // 处理用户数据
  processUserData(userData) {
    const formData = {
      nickname: userData.nickname || '',
      phone: userData.phone || '',
      gender: userData.gender || 0,
      signature: userData.signature || ''
    }
    
    // 处理时间格式（都使用完整时间格式）
    const formattedCreateTime = this.formatFullTime(userData.createTime)
    const formattedLastLoginTime = this.formatFullTime(userData.lastLoginTime)
    
    this.setData({
      userInfo: userData,
      formData: formData,
      originalData: { ...formData },
      avatarUrl: userData.avatarUrl || '',
      formattedCreateTime: formattedCreateTime,
      formattedLastLoginTime: formattedLastLoginTime,
      loading: false
    })
  },

  // 切换到编辑模式
  onEdit() {
    this.setData({
      isEditing: true
    })
  },

  // 取消编辑
  onCancelEdit() {
    this.setData({
      isEditing: false,
      formData: { ...this.data.originalData }
    })
  },

  // 保存修改
  onSave() {
    const { formData, userInfo } = this.data
    
    // 验证必填字段
    if (!formData.nickname) {
      wx.showToast({
        title: '昵称不能为空',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '保存中...'
    })

    // 准备更新的数据
    const updateData = {
      nickname: formData.nickname,
      phone: formData.phone,
      gender: formData.gender,
      signature: formData.signature
    }

    api.updateUserInfo({ userId: userInfo.userId, ...updateData }).then(res => {
      wx.hideLoading()
      
      if (res.code === 200) {
        wx.showToast({
          title: '保存成功',
          icon: 'success'
        })
        
        // 更新原始数据
        this.setData({
          originalData: { ...formData },
          isEditing: false
        })
        
        // 重新加载用户信息
        if (userInfo.userId) {
          this.loadUserInfo(userInfo.userId)
        } else {
          this.loadCurrentUser()
        }
      } else {
        throw new Error(res.message || '保存失败')
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '保存失败',
        icon: 'none'
      })
    })
  },

  // 表单输入处理
  onInput(e) {
    const { field } = e.currentTarget.dataset
    const value = e.detail.value
    
    this.setData({
      [`formData.${field}`]: value
    })
  },

  // 性别选择
  onGenderChange(e) {
    this.setData({
      'formData.gender': parseInt(e.detail.value)
    })
  },

  // 个性签名输入
  onSignatureInput(e) {
    this.setData({
      'formData.signature': e.detail.value
    })
  },

  // 选择头像
  onChooseAvatar() {
    if (!this.data.isEditing) {
      wx.showToast({
        title: '请先点击编辑',
        icon: 'none'
      })
      return
    }

    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      compressed: true,
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath

        // 先显示预览
        this.setData({
          avatarUrl: tempFilePath
        })
        
        // 上传到服务器
        this.uploadAvatar(tempFilePath)
      },
      fail: (err) => {
        if (err.errMsg !== 'chooseMedia:fail cancel') {
          wx.showToast({
            title: '选择失败',
            icon: 'none'
          })
        }
      }
    })
  },

  // 上传头像
  uploadAvatar(filePath) {
    wx.showLoading({
      title: '上传中...'
    })

    // 获取 token
    const token = wx.getStorageSync('admin_token')

    wx.uploadFile({
      url: `${BASE_URL}/image/avatar`,
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': `Bearer ${token}`
      },
      success: (res) => {
        wx.hideLoading()

        try {
          const data = JSON.parse(res.data)
          if (data.code === 200 && data.data) {
            const newAvatarUrl = data.data
            this.setData({
              avatarUrl: newAvatarUrl
            })
            
            // 调用后端更新用户信息，保存头像 URL
            this.updateUserAvatar(newAvatarUrl)
          } else {
            throw new Error(data.message || '上传失败')
          }
        } catch (e) {
          wx.showToast({
            title: '上传失败',
            icon: 'none'
          })
        }
      },
      fail: (err) => {
        wx.hideLoading()
        wx.showToast({
          title: '上传失败，请重试',
          icon: 'none'
        })
      }
    })
  },

  // 更新用户头像
  updateUserAvatar(avatarUrl) {
    const { userInfo } = this.data
    
    api.updateUserInfo({ 
      userId: userInfo.userId, 
      avatarUrl: avatarUrl 
    }).then(res => {
      if (res.code === 200) {
        wx.showToast({
          title: '头像已更新',
          icon: 'success'
        })
        
        // 重新加载用户信息
        this.loadCurrentUser()
      } else {
        throw new Error(res.message || '更新失败')
      }
    }).catch(err => {
      // 头像已经上传成功，只是数据库更新失败，所以还是显示成功提示
      wx.showToast({
        title: '头像已上传',
        icon: 'success'
      })
    })
  },

  // 格式化时间显示（年 - 月-日）
  formatTime(dateStr) {
    if (!dateStr) return ''
    try {
      // 处理 ISO 8601 格式：2026-03-27T16:52:59
      const date = new Date(dateStr.replace('T', ' '))
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    } catch (e) {
      return dateStr
    }
  },

  // 格式化完整时间（年 - 月-日 时：分）
  formatFullTime(dateStr) {
    if (!dateStr) return ''
    try {
      // 处理 ISO 8601 格式：2026-03-27T16:52:59
      const date = new Date(dateStr.replace('T', ' '))
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    } catch (e) {
      return dateStr
    }
  },

  // 退出登录
  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // 清除本地存储
          auth.logout()
          
          // 跳转到登录页
          wx.redirectTo({
            url: '/pages/login/login'
          })
        }
      }
    })
  },

  // 返回上一页
  onGoBack() {
    wx.navigateBack({
      delta: 1
    })
  },

  // ============ 修改手机号功能 ============
  
  // 打开修改手机号弹窗
  onChangePhone() {
    this.setData({
      showChangePhoneModal: true,
      phoneFormData: {
        newPhone: ''
      }
    })
  },

  // 关闭手机号弹窗
  onClosePhoneModal() {
    this.setData({
      showChangePhoneModal: false,
      phoneFormData: {
        newPhone: ''
      }
    })
  },

  // 手机号输入
  onPhoneInput(e) {
    this.setData({
      'phoneFormData.newPhone': e.detail.value
    })
  },

  // 确认修改手机号
  onConfirmChangePhone() {
    const { phoneFormData, userInfo } = this.data
    
    // 验证手机号格式
    if (!phoneFormData.newPhone) {
      wx.showToast({
        title: '请输入新手机号',
        icon: 'none'
      })
      return
    }
    
    // 验证手机号格式（1 开头，第二位 3-9，共 11 位数字）
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(phoneFormData.newPhone)) {
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      })
      return
    }
    
    // 检查是否与原手机号相同
    if (userInfo.phone === phoneFormData.newPhone) {
      wx.showToast({
        title: '新手机号与原手机号相同',
        icon: 'none'
      })
      return
    }
    
    wx.showLoading({
      title: '修改中...'
    })
    
    // 调用后端 API 修改手机号
    api.changePhone({
      newPhone: phoneFormData.newPhone
    }).then(res => {
      wx.hideLoading()
      
      if (res.code === 200) {
        wx.showToast({
          title: '修改成功',
          icon: 'success'
        })
        
        // 关闭弹窗
        this.onClosePhoneModal()
        
        // 重新加载用户信息
        this.loadCurrentUser()
      } else {
        throw new Error(res.message || '修改失败')
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '修改失败',
        icon: 'none'
      })
    })
  },

  // ============ 修改密码功能 ============
  
  // 打开修改密码弹窗
  onChangePassword() {
    this.setData({
      showChangePasswordModal: true,
      passwordFormData: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    })
  },

  // 关闭密码弹窗
  onClosePasswordModal() {
    this.setData({
      showChangePasswordModal: false,
      passwordFormData: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    })
  },

  // 密码输入
  onPasswordInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({
      [`passwordFormData.${field}`]: e.detail.value
    })
  },

  // 确认修改密码
  onConfirmChangePassword() {
    const { passwordFormData } = this.data
    
    // 验证原密码
    if (!passwordFormData.oldPassword) {
      wx.showToast({
        title: '请输入原密码',
        icon: 'none'
      })
      return
    }
    
    // 验证新密码
    if (!passwordFormData.newPassword) {
      wx.showToast({
        title: '请输入新密码',
        icon: 'none'
      })
      return
    }
    
    // 验证新密码长度
    if (passwordFormData.newPassword.length < 6 || passwordFormData.newPassword.length > 20) {
      wx.showToast({
        title: '密码长度必须在 6-20 个字符之间',
        icon: 'none'
      })
      return
    }
    
    // 验证确认密码
    if (!passwordFormData.confirmPassword) {
      wx.showToast({
        title: '请确认新密码',
        icon: 'none'
      })
      return
    }
    
    // 验证两次密码是否一致
    if (passwordFormData.newPassword !== passwordFormData.confirmPassword) {
      wx.showToast({
        title: '两次输入的密码不一致',
        icon: 'none'
      })
      return
    }
    
    // 验证新密码是否与原密码相同
    if (passwordFormData.newPassword === passwordFormData.oldPassword) {
      wx.showToast({
        title: '新密码不能与原密码相同',
        icon: 'none'
      })
      return
    }
    
    wx.showLoading({
      title: '修改中...'
    })
    
    // 调用后端 API 修改密码
    api.changePassword({
      oldPassword: passwordFormData.oldPassword,
      newPassword: passwordFormData.newPassword
    }).then(res => {
      wx.hideLoading()
      
      if (res.code === 200) {
        wx.showToast({
          title: '修改成功',
          icon: 'success'
        })
        
        // 关闭弹窗
        this.onClosePasswordModal()
      } else {
        throw new Error(res.message || '修改失败')
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '修改失败',
        icon: 'none'
      })
    })
  }
})

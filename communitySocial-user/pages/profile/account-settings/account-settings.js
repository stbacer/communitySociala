// pages/profile/account-settings/account-settings.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    userInfo: {},
    
    // 性别显示文本
    genderText: '未知',
    
    // 是否有密码
    hasPassword: false,
    
    // 手机号脱敏显示
    maskedPhone: '未绑定',
    
    // 昵称编辑
    showNicknameModal: false,
    nicknameInput: '',
    canSubmitNickname: false,
    
    // 性别编辑
    showGenderModal: false,
    genderInput: 0,
    
    // 个性签名编辑
    showSignatureModal: false,
    signatureInput: '',
    canSubmitSignature: false,
    
    // 手机号编辑
    showPhoneModal: false,
    currentPhoneInput: '', // 当前手机号输入
    phoneInput: '', // 新手机号输入
    canSubmitPhone: false,
    
    // 密码修改
    showPasswordModal: false,
    oldPasswordInput: '',
    newPasswordInput: '',
    confirmPasswordInput: '',
    canSubmitPassword: false
  },

  onLoad() {
    this.loadUserInfo()
  },

  // 加载用户信息
  async loadUserInfo() {
    try {
      const res = await api.getCurrentUser()
      if (res && res.data) {
        const userInfo = res.data

        // 处理性别数据，确保有效
        const gender = userInfo.gender !== undefined && userInfo.gender !== null ? userInfo.gender : 0
        
        // 直接使用后端返回的 hasPassword 字段
        const hasPassword = userInfo.hasPassword === true
        
        this.setData({
          userInfo,
          genderText: this.getGenderText(gender),
          hasPassword: hasPassword,
          maskedPhone: this.maskPhone(userInfo.phone),
          phone: userInfo.phone,
          userRole: userInfo.userRole
        })
      }
    } catch (error) {
    }
  },

  // 获取性别文本
  getGenderText(gender) {
    switch (gender) {
      case 1: return '男'
      case 2: return '女'
      default: return '未知'
    }
  },

  // 手机号脱敏
  maskPhone(phone) {
    if (!phone) return '未绑定'
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
  },

  // 更新手机号脱敏显示
  updateMaskedPhone() {
    this.setData({
      maskedPhone: this.maskPhone(this.data.userInfo.phone)
    })
  },

  // ==================== 昵称编辑 ====================
  
  onEditNickname() {
    this.setData({
      nicknameInput: this.data.userInfo.nickname || '',
      showNicknameModal: true,
      canSubmitNickname: false
    })
  },

  onNicknameInput(e) {
    const value = e.detail.value.trim()
    const canSubmit = value.length >= 2 && value.length <= 20
    this.setData({
      nicknameInput: value,
      canSubmitNickname: canSubmit
    })
  },

  closeNicknameModal() {
    this.setData({
      showNicknameModal: false
    })
  },

  async confirmEditNickname() {
    const { nicknameInput } = this.data
    
    if (!nicknameInput || nicknameInput.length < 2) {
      wx.showToast({
        title: '昵称至少 2 个字符',
        icon: 'none'
      })
      return
    }

    try {
      await api.updateUserProfile({
        nickname: nicknameInput
      })
      
      wx.showToast({
        title: '修改成功',
        icon: 'success'
      })
      
      this.closeNicknameModal()
      this.loadUserInfo()
    } catch (error) {
    }
  },

  // ==================== 性别编辑 ====================
  
  onEditGender() {
    this.setData({
      genderInput: this.data.userInfo.gender || 0,
      showGenderModal: true
    })
  },

  onSelectGender(e) {
    const gender = parseInt(e.currentTarget.dataset.gender)
    this.setData({
      genderInput: gender
    })
  },

  closeGenderModal() {
    this.setData({
      showGenderModal: false
    })
  },

  async confirmEditGender() {
    const { genderInput } = this.data

    try {
      await api.updateUserProfile({
        gender: genderInput
      })
      
      wx.showToast({
        title: '修改成功',
        icon: 'success'
      })
      
      this.closeGenderModal()
      this.loadUserInfo()
    } catch (error) {
    }
  },

  // ==================== 个性签名编辑 ====================
  
  onEditSignature() {
    this.setData({
      signatureInput: this.data.userInfo.signature || '',
      showSignatureModal: true,
      canSubmitSignature: false
    })
  },

  onSignatureInput(e) {
    const value = e.detail.value
    const canSubmit = value.length <= 100
    this.setData({
      signatureInput: value,
      canSubmitSignature: canSubmit
    })
  },

  closeSignatureModal() {
    this.setData({
      showSignatureModal: false
    })
  },

  async confirmEditSignature() {
    const { signatureInput } = this.data
    
    if (signatureInput && signatureInput.length > 100) {
      wx.showToast({
        title: '个性签名不能超过 100 个字符',
        icon: 'none'
      })
      return
    }

    try {
      await api.updateUserProfile({
        signature: signatureInput.trim()
      })
      
      wx.showToast({
        title: '修改成功',
        icon: 'success'
      })
      
      this.closeSignatureModal()
      this.loadUserInfo()
    } catch (error) {
    }
  },

  // ==================== 手机号编辑 ====================
  
  onEditPhone() {
    const { userInfo } = this.data
    const hasPhone = userInfo.phone && userInfo.phone.trim() !== ''
    
    this.setData({
      currentPhoneInput: '',
      phoneInput: '',
      canSubmitPhone: false,
      showPhoneModal: true,
      hasPhone: hasPhone  // 标记是否已绑定手机号
    })
  },

  onCurrentPhoneInput(e) {
    const value = e.detail.value.trim()
    this.setData({
      currentPhoneInput: value
    })
    this.validatePhoneForm()
  },

  onPhoneInput(e) {
    const value = e.detail.value.trim()
    this.setData({
      phoneInput: value
    })
    this.validatePhoneForm()
  },

  // 验证手机号表单
  validatePhoneForm() {
    const { currentPhoneInput, phoneInput, userInfo, hasPhone } = this.data
    const phoneRegex = /^1[3-9]\d{9}$/
    
    let canSubmit = false
    
    if (hasPhone) {
      // 已绑定手机号，需要验证当前手机号
      const isCurrentPhoneValid = phoneRegex.test(currentPhoneInput) && currentPhoneInput === userInfo.phone
      const isNewPhoneValid = phoneRegex.test(phoneInput)
      const isDifferent = currentPhoneInput !== phoneInput
      canSubmit = isCurrentPhoneValid && isNewPhoneValid && isDifferent
    } else {
      // 首次绑定，只需验证新手机号格式
      canSubmit = phoneRegex.test(phoneInput)
    }
    
    this.setData({
      canSubmitPhone: canSubmit
    })
  },

  closePhoneModal() {
    this.setData({
      showPhoneModal: false
    })
  },

  async confirmEditPhone() {
    const { currentPhoneInput, phoneInput, userInfo, hasPhone } = this.data
    
    // 验证新手机号
    if (!phoneInput) {
      wx.showToast({
        title: '请输入新手机号',
        icon: 'none'
      })
      return
    }
    
    // 如果不是首次绑定，需要验证当前手机号
    if (hasPhone) {
      if (!currentPhoneInput || currentPhoneInput !== userInfo.phone) {
        wx.showToast({
          title: '当前手机号不正确',
          icon: 'none'
        })
        return
      }
      
      // 验证两个手机号不能相同
      if (currentPhoneInput === phoneInput) {
        wx.showToast({
          title: '新手机号不能与当前手机号相同',
          icon: 'none'
        })
        return
      }
    }

    try {
      wx.showLoading({
        title: hasPhone ? '修改中...' : '绑定中...',
        mask: true
      })
      
      // 调用后端 API 修改手机号
      const params = {
        newPhone: phoneInput
      }
      
      // 如果不是首次绑定，需要传入当前手机号
      if (hasPhone) {
        params.currentPhone = currentPhoneInput
      }
      
      await api.changePhone(params)
      
      wx.hideLoading()
      
      wx.showToast({
        title: hasPhone ? '修改成功' : '绑定成功',
        icon: 'success'
      })
      
      this.closePhoneModal()
      this.loadUserInfo()
    } catch (error) {
      wx.hideLoading()
      wx.showToast({
        title: error.message || '修改失败',
        icon: 'none'
      })
    }
  },

  // ==================== 密码修改 ====================
  
  onChangePassword() {
    const { hasPassword } = this.data

    this.setData({
      oldPasswordInput: '',
      newPasswordInput: '',
      confirmPasswordInput: '',
      canSubmitPassword: false,
      showPasswordModal: true
    })
  },

  onOldPasswordInput(e) {
    const value = e.detail.value
    this.setData({
      oldPasswordInput: value
    }, () => {
      // 确保数据更新后再检查
      this.checkPassword(value, this.data.newPasswordInput, this.data.confirmPasswordInput)
    })
  },

  onNewPasswordInput(e) {
    const value = e.detail.value
    this.setData({
      newPasswordInput: value
    }, () => {
      // 确保数据更新后再检查
      this.checkPassword(this.data.oldPasswordInput, value, this.data.confirmPasswordInput)
    })
  },

  onConfirmPasswordInput(e) {
    const value = e.detail.value
    this.setData({
      confirmPasswordInput: value
    }, () => {
      // 确保数据更新后再检查
      this.checkPassword(this.data.oldPasswordInput, this.data.newPasswordInput, value)
    })
  },

  checkPassword(oldPwd, newPwd, confirmPwd) {
    let canSubmit = false
    
    if (this.data.hasPassword) {
      // 修改密码需要验证原密码
      canSubmit = oldPwd && oldPwd.length >= 6 && 
                  newPwd && newPwd.length >= 6 && 
                  newPwd.length <= 20 && 
                  newPwd === confirmPwd
    } else {
      // 设置密码只需要两次输入一致（不需要原密码）
      canSubmit = newPwd && newPwd.length >= 6 && 
                  newPwd.length <= 20 && 
                  newPwd === confirmPwd
    }

    this.setData({
      canSubmitPassword: canSubmit
    })
  },

  closePasswordModal() {
    this.setData({
      showPasswordModal: false
    })
  },

  async confirmChangePassword() {
    const { hasPassword, oldPasswordInput, newPasswordInput, confirmPasswordInput } = this.data
    
    if (newPasswordInput !== confirmPasswordInput) {
      wx.showToast({
        title: '两次密码输入不一致',
        icon: 'none'
      })
      return
    }

    if (newPasswordInput.length < 6 || newPasswordInput.length > 20) {
      wx.showToast({
        title: '密码长度 6-20 位',
        icon: 'none'
      })
      return
    }

    try {
      wx.showLoading({
        title: '修改中...',
        mask: true
      })
      
      if (hasPassword) {
        // 修改密码
        await api.changePassword({
          oldPassword: oldPasswordInput,
          newPassword: newPasswordInput
        })
      } else {
        // 首次设置密码
        await api.changePassword({
          newPassword: newPasswordInput
        })
      }
      
      wx.hideLoading()
      
      wx.showToast({
        title: '修改成功',
        icon: 'success'
      })
      
      this.closePasswordModal()
      this.setData({
        hasPassword: true
      })
      
      // 清空密码输入
      this.setData({
        oldPasswordInput: '',
        newPasswordInput: '',
        confirmPasswordInput: ''
      })
    } catch (error) {
      wx.hideLoading()
      let errorMsg = '修改失败'
      if (error && error.message) {
        // 处理特定的错误信息
        if (error.message.includes('原密码错误')) {
          errorMsg = '原密码错误'
        } else if (error.message.includes('用户不存在')) {
          errorMsg = '用户不存在'
        } else {
          errorMsg = error.message
        }
      }
      
      wx.showToast({
        title: errorMsg,
        icon: 'none'
      })
    }
  }
})

// pages/message/chat/chat.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')
const wsClient = require('../../../utils/websocket.js')

Page({
  data: {
    currentChat: null,
    messages: [],
    messagePage: 1,
    messageHasMore: true,
    messageLoading: false,
    
    // 输入相关
    messageContent: '',
    isSending: false,
    canSend: false,
    
    // 滚动控制
    scrollToView: '',
    scrollTop: 0,
    
    // 用户信息
    currentUserId: '',
    userAvatar: '',
    
    // 对方信息
    targetUserId: '',
    targetNickname: '',
    targetAvatar: '',
    
    // 是否为系统通知
    isSystemNotification: false
  },

  onLoad(options) {
    // 检查认证状态
    if (!auth.checkAuthAndGuide({
      onCancel: () => {
        wx.navigateBack()
      }
    })) {
      return
    }
    
    const userInfo = wx.getStorageSync('user_info')
    if (userInfo && userInfo.userId) {
      this.setData({
        currentUserId: userInfo.userId,
        userAvatar: userInfo.avatarUrl || '/images/default-avatar.png'
      })
      
      // 初始化 WebSocket 连接
      this.initWebSocket(userInfo.userId)
    }
    
    // 获取参数
    const { userId, nickname, avatar } = options
    
    // 判断是否为系统通知账号
    const isSystemNotification = (userId === 'SYSTEM')
    
    this.setData({
      targetUserId: userId,
      targetNickname: isSystemNotification ? '系统通知' : decodeURIComponent(nickname || '未知用户'),
      targetAvatar: decodeURIComponent(avatar || '/images/default-avatar.png'),
      isSystemNotification: isSystemNotification,
      currentChat: {
        userId: userId,
        nickname: isSystemNotification ? '系统通知' : decodeURIComponent(nickname || '未知用户'),
        avatarUrl: decodeURIComponent(avatar || '/images/default-avatar.png')
      }
    })
    
    // 设置导航栏标题
    wx.setNavigationBarTitle({
      title: this.data.targetNickname
    })
    
    // 注册 WebSocket 监听器
    this.setupWebSocketListener()
    
    // 加载历史消息
    this.loadMessages()
    
    // 标记消息为已读
    this.markMessagesRead()
  },

  onShow() {
    if (!auth.isAuthenticated()) {
      wx.navigateBack()
      return
    }
  },

  onUnload() {
    // 页面卸载时移除监听器
    wsClient.offMessage(this.onWebSocketMessage)
  },

  // 初始化 WebSocket 连接
  async initWebSocket(userId) {
    try {
      const wsClient = require('../../../utils/websocket.js')
      const status = wsClient.getConnectionStatus()
      // 如果未连接，则建立连接
      if (!status.isConnected) {
        // 延迟 500ms 再连接，确保页面完全加载
        await new Promise(resolve => setTimeout(resolve, 500))
        
        await wsClient.connect(userId)
        // 等待连接稳定
        await new Promise(resolve => setTimeout(resolve, 300))
      } else {
      }
    } catch (error) {
      // 不阻断页面加载，只是提示用户
      wx.showToast({
        title: '实时消息连接失败',
        icon: 'none',
        duration: 2000
      })
    }
  },

  // 设置 WebSocket 监听器
  setupWebSocketListener() {
    this.onWebSocketMessage = (message) => {
      if (message.type === 'new_message' && message.data) {
        const data = message.data
        
        // 只处理对方发来的消息，忽略自己发送的（已通过乐观更新显示）
        if (data.senderId === this.data.targetUserId) {
          this.handleNewMessage(data)
        }
      }
    }
    
    wsClient.onMessage(this.onWebSocketMessage)
  },

  // 处理新消息
  handleNewMessage(data) {
    const { messageId, senderId, receiverId, content, messageType, sendTime, senderNickname, senderAvatarUrl } = data
    
    // 格式化时间
    const formattedTime = this.formatMessageTime(sendTime)
    
    const newMessage = {
      messageId: messageId || `temp_${Date.now()}`,
      senderId: senderId,
      receiverId: receiverId,
      content: content,
      messageType: messageType || 1,
      sendTime: sendTime,
      formattedTime: formattedTime,
      senderNickname: senderNickname || (senderId === this.data.currentUserId ? '我' : this.data.targetNickname),
      senderAvatarUrl: senderAvatarUrl || (senderId === this.data.currentUserId ? this.data.userAvatar : this.data.targetAvatar),
      isSystemMessage: senderId === 'SYSTEM'
    }
    
    // 追加消息到末尾
    const messages = [...this.data.messages, newMessage]
    
    this.setData({
      messages: messages
    })
    
    // 滚动到底部
    setTimeout(() => {
      this.scrollToBottom()
    }, 100)
    
    // 标记为已读
    this.markSingleMessageRead(messageId)
  },

  // 加载历史消息
  async loadMessages() {
    if (this.data.messageLoading || !this.data.messageHasMore) return
    
    this.setData({ messageLoading: true })
    
    try {
      const params = {
        toUserId: this.data.targetUserId,
        page: this.data.messagePage,
        size: 20
      }
      
      // 如果有上一页的消息，传入 before 时间戳
      if (this.data.messages.length > 0) {
        params.before = this.data.messages[0].sendTime
      }
      
      const response = await api.getMessageRecords(this.data.targetUserId, params)
      const records = response.data.records || []
      
      // 格式化消息
      const formattedMessages = records.map(msg => ({
        ...msg,
        formattedTime: this.formatMessageTime(msg.sendTime),
        senderNickname: msg.senderNickname || (msg.senderId === this.data.currentUserId ? '我' : this.data.targetNickname),
        senderAvatarUrl: msg.senderAvatarUrl || (msg.senderId === this.data.currentUserId ? this.data.userAvatar : this.data.targetAvatar),
        isSystemMessage: msg.senderId === 'SYSTEM'
      }))
      
      // 后端已返回升序（旧的在前，新的在后），直接使用
      const newMessages = formattedMessages
      
      // 将新消息插入到前面（因为是分页加载历史消息）
      const messages = [...newMessages, ...this.data.messages]
      
      this.setData({
        messages: messages,
        messagePage: this.data.messagePage + 1,
        messageHasMore: records.length === 20,
        messageLoading: false
      })
      
      // 如果是首次加载，滚动到底部
      if (this.data.messagePage === 2) {
        setTimeout(() => {
          this.scrollToBottom()
        }, 200)
      }
      
    } catch (error) {
      wx.showToast({
        title: '加载消息失败',
        icon: 'none'
      })
      this.setData({ messageLoading: false })
    }
  },

  // 上拉加载更多
  onScrollToUpper() {
    if (this.data.messageHasMore && !this.data.messageLoading) {
      this.loadMessages()
    }
  },

  // 标记消息为已读
  async markMessagesRead() {
    try {
      // 使用按会话标记已读的接口
      await api.markMessageRead(this.data.targetUserId)
      
      // 更新本地消息状态
      const messages = this.data.messages.map(msg => {
        if (msg.senderId !== this.data.currentUserId) {
          return { ...msg, isRead: true }
        }
        return msg
      })
      
      this.setData({ messages })
    } catch (error) {
    }
  },

  // 标记单条消息为已读
  async markSingleMessageRead(messageId) {
    // 单条消息也使用按会话标记已读，确保会话级别的未读数正确
    try {
      await api.markMessageRead(this.data.targetUserId)
    } catch (error) {
    }
  },

  // 输入消息
  onMessageInput(e) {
    const content = e.detail.value.trim()
    this.setData({
      messageContent: content,
      canSend: content.length > 0
    })
  },

  // 发送文本消息
  async sendTextMessage() {
    if (!this.data.canSend || this.data.isSending) return
    
    const content = this.data.messageContent.trim()
    if (!content) return
    
    this.setData({ isSending: true })
    
    try {
      // 确保 WebSocket 已连接
      const wsClient = require('../../../utils/websocket.js')
      const status = wsClient.getConnectionStatus()
      
      if (!status.isConnected) {
        await this.initWebSocket(this.data.currentUserId)
        // 等待连接稳定
        await new Promise(resolve => setTimeout(resolve, 500))
      }
      
      // 构造消息对象
      const messageData = {
        toUserId: this.data.targetUserId,
        type: 'text',
        content: content,
        timestamp: Date.now()
      }
      
      // 通过 WebSocket 发送
      const sendSuccess = wsClient.send(messageData)
      
      if (!sendSuccess) {
        throw new Error('消息发送失败')
      }
      
      // 乐观更新 UI
      const tempMessage = {
        messageId: `temp_${Date.now()}`,
        senderId: this.data.currentUserId,
        receiverId: this.data.targetUserId,
        content: content,
        messageType: 1,
        sendTime: new Date().toISOString(),
        formattedTime: this.formatMessageTime(new Date().toISOString()),
        senderNickname: '我',
        senderAvatarUrl: this.data.userAvatar,
        isSystemMessage: false
      }


      const messages = [...this.data.messages, tempMessage]


      this.setData({
        messages: messages,
        messageContent: '',
        canSend: false,
        isSending: false
      })
      
      // 滚动到底部
      setTimeout(() => {
        this.scrollToBottom()
      }, 100)
      
    } catch (error) {
      wx.showToast({
        title: '发送失败',
        icon: 'none'
      })
      this.setData({ isSending: false })
    }
  },

  // 选择图片
  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath
        this.uploadAndSendImage(tempFilePath)
      },
      fail: (err) => {
      }
    })
  },

  // 上传图片并发送
  async uploadAndSendImage(filePath) {
    this.setData({ isSending: true })
    
    try {
      // 确保 WebSocket 已连接
      const wsClient = require('../../../utils/websocket.js')
      const status = wsClient.getConnectionStatus()
      
      if (!status.isConnected) {
        await this.initWebSocket(this.data.currentUserId)
        // 等待连接稳定
        await new Promise(resolve => setTimeout(resolve, 500))
      }
      
      // 上传图片
      const uploadRes = await api.uploadImage(filePath)
      const imageUrl = uploadRes.data.url
      
      // 构造消息对象
      const messageData = {
        toUserId: this.data.targetUserId,
        type: 'image',
        content: imageUrl,
        timestamp: Date.now()
      }
      
      // 通过 WebSocket 发送
      const sendSuccess = wsClient.send(messageData)
      
      if (!sendSuccess) {
        throw new Error('消息发送失败')
      }
      
      // 乐观更新 UI
      const tempMessage = {
        messageId: `temp_${Date.now()}`,
        senderId: this.data.currentUserId,
        receiverId: this.data.targetUserId,
        content: imageUrl,
        messageType: 2, // 图片消息
        sendTime: new Date().toISOString(),
        formattedTime: this.formatMessageTime(new Date().toISOString()),
        senderNickname: '我',
        senderAvatarUrl: this.data.userAvatar,
        isSystemMessage: false
      }
      
      const messages = [...this.data.messages, tempMessage]
      
      this.setData({
        messages: messages,
        isSending: false
      })
      
      // 滚动到底部
      setTimeout(() => {
        this.scrollToBottom()
      }, 100)
      
    } catch (error) {
      wx.showToast({
        title: '发送图片失败',
        icon: 'none'
      })
      this.setData({ isSending: false })
    }
  },

  // 预览图片
  previewImage(e) {
    const url = e.currentTarget.dataset.url
    wx.previewImage({
      urls: [url],
      current: url
    })
  },

  // 格式化消息时间
  formatMessageTime(dateString) {
    if (!dateString) return ''
    
    try {
      const date = new Date(dateString)
      const now = new Date()
      
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      
      // 当天只显示时间
      if (date.toDateString() === now.toDateString()) {
        return `${hours}:${minutes}`
      }
      
      // 非当天显示日期和时间
      return `${month}/${day} ${hours}:${minutes}`
      
    } catch (error) {
      return dateString
    }
  },

  // 滚动到底部
  scrollToBottom() {

    if (this.data.messages.length === 0) return
    
    const lastIndex = this.data.messages.length - 1
    
    // 使用 scroll-into-view 滚动到最后一消息
    this.setData({
      scrollToView: `msg-${lastIndex}`
    }, () => {

    })
  }
})

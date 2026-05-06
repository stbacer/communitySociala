// pages/message/conversation-list/conversation-list.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')
const { formatDate } = require('../../../utils/util.js')
const app = getApp()

Page({
  data: {
    conversations: [],
    loading: false,
    refreshing: false,
    searchKeyword: '',
    showSearch: false,
    currentUserId: ''
  },

  onLoad() {
    // 检查认证状态
    if (!auth.checkAuthAndGuide({
      onCancel: () => {
        wx.switchTab({ url: '/pages/index/index' })
      }
    })) {
      return
    }
    
    const userInfo = wx.getStorageSync('user_info')
    if (userInfo && userInfo.userId) {
      this.setData({ currentUserId: userInfo.userId })
      
      // 初始化 WebSocket 连接
      this.initWebSocket(userInfo.userId)
    }
    
    // 注册 WebSocket 消息监听器
    this.setupWebSocketListener()
  },

  onShow() {
    if (!auth.isAuthenticated()) {
      wx.switchTab({ url: '/pages/index/index' })
      return
    }
    
    if (auth.isLoggedIn()) {
      this.loadConversations()
    }
  },

  onPullDownRefresh() {
    this.onRefresh()
  },

  // 初始化 WebSocket 连接
  async initWebSocket(userId) {
    try {
      const wsClient = require('../../../utils/websocket.js')
      const status = wsClient.getConnectionStatus()
      // 如果未连接，则建立连接
      if (!status.isConnected) {
        await wsClient.connect(userId)
      } else {
      }
    } catch (error) {
    }
  },

  // 设置 WebSocket 监听器
  setupWebSocketListener() {
    const wsClient = require('../../../utils/websocket.js')
    wsClient.onMessage((message) => {

      if (message.type === 'new_message' && message.data) {
        this.handleNewMessage(message.data)
      }
    })
  },

  // 处理新消息
  handleNewMessage(data) {
    const { senderId, receiverId, content, messageType, sendTime } = data
    
    // 判断是否是当前用户收到的消息
    const isReceived = receiverId === this.data.currentUserId
    if (!isReceived) return
    
    const otherUserId = senderId === 'SYSTEM' ? 'SYSTEM' : senderId
    
    // 查找是否已存在该会话
    const index = this.data.conversations.findIndex(conv => conv.userId === otherUserId)
    
    let lastMessageContent = content || ''
    if (messageType === 2) {
      lastMessageContent = '[图片]'
    } else if (messageType === 3) {
      lastMessageContent = '撤回了一条消息'
    }
    
    const updatedConversation = {
      userId: otherUserId,
      nickname: data.senderNickname || (otherUserId === 'SYSTEM' ? '系统通知' : '未知用户'),
      avatarUrl: data.senderAvatarUrl || '/images/default-avatar.png',
      content: lastMessageContent,
      lastMessageContent: lastMessageContent,
      sendTime: sendTime || new Date().toISOString(),
      formattedSendTime: sendTime ? formatDate(sendTime) : formatDate(new Date().toISOString()),
      unreadCount: (this.data.conversations[index]?.unreadCount || 0) + 1,
      isSystemMessage: otherUserId === 'SYSTEM',
      messageType: messageType
    }
    
    let conversations = [...this.data.conversations]
    
    if (index > -1) {
      // 更新现有会话
      conversations[index] = updatedConversation
    } else {
      // 添加新会话
      conversations.unshift(updatedConversation)
    }
    
    // 按时间排序（最新的在前）
    conversations.sort((a, b) => new Date(b.sendTime) - new Date(a.sendTime))
    
    this.setData({ conversations })
  },

  // 加载会话列表
  async loadConversations(refresh = false) {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      const params = {
        page: 1,
        size: 100,
        keyword: this.data.searchKeyword || null
      }
      
      const response = await api.getConversations(params)
      const newConversations = response.data.records || []
      
      // 处理会话数据
      const processedConversations = newConversations.map(conv => {
        const isSystemMessage = conv.senderId === 'SYSTEM'
        const otherPartyId = conv.contactId || (conv.senderId === this.data.currentUserId ? conv.receiverId : conv.senderId)
        
        let lastMessageContent = conv.content || ''
        if (conv.messageType === 2) {
          lastMessageContent = '[图片]'
        } else if (conv.messageType === 3) {
          lastMessageContent = '撤回了一条消息'
        }
        
        return {
          ...conv,
          isSystemMessage: isSystemMessage,
          nickname: isSystemMessage ? '系统通知' : (conv.contactNickname || conv.nickname || '未知用户'),
          avatarUrl: conv.contactAvatarUrl || conv.senderAvatarUrl || '/images/default-avatar.png',
          userId: isSystemMessage ? 'SYSTEM' : otherPartyId,
          unreadCount: conv.unreadCount || 0,
          lastMessageContent: lastMessageContent,
          // 格式化时间
          formattedSendTime: conv.sendTime ? formatDate(conv.sendTime) : ''
        }
      })
      
      // 按最后一条消息时间倒序排列（最新的在前）
      processedConversations.sort((a, b) => {
        const timeA = a.sendTime ? new Date(a.sendTime).getTime() : 0
        const timeB = b.sendTime ? new Date(b.sendTime).getTime() : 0
        return timeB - timeA
      })
      
      // 按最后一条消息时间倒序排列（最新的在前）
      processedConversations.sort((a, b) => {
        const timeA = a.sendTime ? new Date(a.sendTime).getTime() : 0
        const timeB = b.sendTime ? new Date(b.sendTime).getTime() : 0
        return timeB - timeA
      })
      
      this.setData({
        conversations: processedConversations,
        refreshing: false
      })
      
    } catch (error) {
      wx.showToast({
        title: '加载会话失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 下拉刷新
  onRefresh() {
    this.setData({ refreshing: true })
    this.loadConversations(true)
  },

  // 搜索相关
  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value })
  },

  onSearch() {
    this.loadConversations()
  },

  clearSearch() {
    this.setData({ searchKeyword: '' })
    this.loadConversations()
  },

  // 进入聊天
  enterChat(e) {
    const { userId, nickname, avatar } = e.currentTarget.dataset
    
    wx.navigateTo({
      url: `/pages/message/chat/chat?userId=${userId}&nickname=${encodeURIComponent(nickname)}&avatar=${encodeURIComponent(avatar)}`
    })
  }
})

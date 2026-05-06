// WebSocket 工具类
const BASE_URL = 'ws://127.0.0.1:8080'  
// const BASE_URL = 'wss://21ee023c.r8.cpolar.cn' 

class WebSocketClient {
  constructor() {
    this.socketTask = null
    this.isConnected = false
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.messageHandlers = [] 
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectInterval = 3000 
    this.heartbeatInterval = 30000 
  }

  /**
   * 连接 WebSocket 服务器
   */
  connect(userId) {
    if (this.socketTask && this.isConnected) {

      return Promise.resolve()
    }

    return new Promise((resolve, reject) => {
      try {

        const wsUrl = `${BASE_URL}/ws/message?userId=${userId}`

        this.socketTask = wx.connectSocket({
          url: wsUrl,
          protocols: ['websocket'],
          success: () => {
          },
          fail: (err) => {
            reject(err)
          }
        })

        this.socketTask.onOpen(() => {
          this.isConnected = true
          this.reconnectAttempts = 0
   
          this.startHeartbeat()
          
          resolve()
        })

        this.socketTask.onMessage((res) => {
          this.handleMessage(res.data)
        })

        this.socketTask.onClose((res) => {
          this.isConnected = false
          
          this.stopHeartbeat()
          
          this.scheduleReconnect(userId)
        })

        this.socketTask.onError((err) => {
          this.isConnected = false
          this.stopHeartbeat()
          reject(err)
        })

      } catch (error) {
        reject(error)
      }
    })
  }

  /**
   * 处理接收到的消息
   */
  handleMessage(data) {
    try {
      const message = JSON.parse(data)

      this.messageHandlers.forEach(handler => {
        if (typeof handler === 'function') {
          handler(message)
        }
      })
    } catch (error) {
      // 解析消息失败
    }
  }

  /**
   * 注册消息处理器
   */
  onMessage(handler) {
    if (typeof handler === 'function') {
      this.messageHandlers.push(handler)
    }
  }

  /**
   * 移除消息处理器
   */
  offMessage(handler) {
    if (handler && typeof handler === 'function') {
      const index = this.messageHandlers.indexOf(handler)
      if (index > -1) {
        this.messageHandlers.splice(index, 1)
      }
    }
  }

  /**
   * 发送消息到服务器
   */
  send(data) {
    if (!this.isConnected || !this.socketTask) {
      return false
    }

    try {
      const messageStr = typeof data === 'string' ? data : JSON.stringify(data)
      this.socketTask.send({
        data: messageStr,
        success: () => {
        },
        fail: (err) => {
          // 消息发送失败
        }
      })
      return true
    } catch (error) {
      return false
    }
  }

  /**
   * 发送心跳消息
   */
  sendHeartbeat() {
    if (this.isConnected) {
      const success = this.send({
        type: 'heartbeat',
        timestamp: Date.now()
      })
      if (!success) {
        // 心跳发送失败
      }
    }
  }

  /**
   * 启动心跳机制
   */
  startHeartbeat() {
    this.stopHeartbeat()
    
    this.heartbeatTimer = setInterval(() => {
      this.sendHeartbeat()
    }, this.heartbeatInterval)
  }

  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 安排重连
   */
  scheduleReconnect(userId) {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      return
    }

    this.reconnectAttempts++

    this.reconnectTimer = setTimeout(() => {
      this.connect(userId)
    }, this.reconnectInterval)
  }

  /**
   * 取消重连
   */
  cancelReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.cancelReconnect()
    this.stopHeartbeat()
    
    if (this.socketTask) {
      this.socketTask.close({
        code: 1000,
        reason: '用户主动关闭',
        success: () => {
        }
      })
      this.socketTask = null
    }
    
    this.isConnected = false
    this.messageHandlers = []
  }

  /**
   * 获取连接状态
   */
  getConnectionStatus() {
    return {
      isConnected: this.isConnected,
      reconnectAttempts: this.reconnectAttempts,
      handlersCount: this.messageHandlers.length
    }
  }
}

// 创建单例实例
const wsClient = new WebSocketClient()

module.exports = wsClient

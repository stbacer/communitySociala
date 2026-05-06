// pages/statistics/statistics.js
const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({

  /**
   * 页面的初始数据
   */
  data: {
    statistics: {},
    userRankings: [],
    hotCategories: [],
    communityName: '',
    loading: false,
    selectedRange: 'week', // today, week, month
    canvasWidth: 300,
    canvasHeight: 300,
    dauTrendData: [], // 近 7 日活跃趋势数据
    Math: Math // 引入 Math 对象用于模板计算
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login/login'
      })
      return
    }
    
    this.loadCurrentUser()
    this.loadStatistics()
    this.loadUserRankings()
    this.loadHotCategories()
    this.loadDauTrend()
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    if (auth.isLoggedIn()) {
      this.loadStatistics()
      this.loadUserRankings()
      this.loadHotCategories()
      this.loadDauTrend()
    }
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.loadStatistics().then(() => {
      this.loadUserRankings()
      this.loadHotCategories()
      this.loadDauTrend()
      wx.stopPullDownRefresh()
    })
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  },

  // 加载当前用户信息
  async loadCurrentUser() {
    try {
      const response = await api.getCurrentUser()
      if (response && response.data) {
        this.setData({
          communityName: response.data.community || '未知社区'
        })
      }
    } catch (error) {
      // 错误已在 request 拦截器中处理
    }
  },

  // 加载统计数据
  async loadStatistics() {
    this.setData({ loading: true })
    
    try {
      const response = await api.getCommunityStatistics()
      if (response && response.code === 200 && response.data) {
        this.setData({
          statistics: response.data
        })
      }
    } catch (error) {
      wx.showToast({
        title: '加载统计数据失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 加载用户发帖排名
  async loadUserRankings() {
    try {
      const response = await api.getUserPostRankings({ limit: 10 })
      if (response && response.code === 200 && response.data) {
        this.setData({
          userRankings: response.data
        })
      }
    } catch (error) {
      // 错误已在 request 拦截器中处理
    }
  },

  // 加载热门板块数据
  async loadHotCategories() {
    const timeRange = this.data.selectedRange
    try {
      const response = await api.getCategoryStatistics(timeRange)
      if (response && response.code === 200 && response.data && response.data.categoryStats) {
        let categories = response.data.categoryStats
        
        // 按帖子数量排序
        categories.sort((a, b) => b.postCount - a.postCount)
        
        // 取前 8 名
        const topCategories = categories.slice(0, 8)
        
        // 计算前 8 个板块的总帖子数（用于百分比计算）
        const top8Total = topCategories.reduce((sum, item) => sum + item.postCount, 0)
        
        // 格式化数据并计算百分比
        const formattedCategories = topCategories.map(item => {
          // 基于前 8 个板块的总数计算百分比，确保总和接近 100%
          const percentage = top8Total > 0 ? ((item.postCount / top8Total) * 100).toFixed(1) : 0
          return {
            ...item,
            percentage: percentage
          }
        })
        
        // 如果有超过 8 个板块，添加"其他"类别
        if (categories.length > 8) {
          const otherCategories = categories.slice(8)
          const otherTotal = otherCategories.reduce((sum, item) => sum + item.postCount, 0)
          const otherPercentage = top8Total + otherTotal > 0 ? ((otherTotal / (top8Total + otherTotal)) * 100).toFixed(1) : 0
          
          formattedCategories.push({
            categoryId: -1,
            categoryName: '其他板块',
            postCount: otherTotal,
            percentage: otherPercentage,
            color: '#9ca3af' // 灰色表示其他
          })
        }
        
        // 分配颜色：前三个板块使用红黄蓝，其他使用灰色
        formattedCategories.forEach((item, index) => {
          if (index === 0) {
            item.color = '#FF4444' // 红色 - 第一名
          } else if (index === 1) {
            item.color = '#FFBB33' // 黄色 - 第二名
          } else if (index === 2) {
            item.color = '#0099CC' // 蓝色 - 第三名
          } else {
            item.color = '#CCCCCC' // 灰色 - 其他
          }
        })
        
        this.setData({
          hotCategories: formattedCategories
        }, () => {
          // 数据加载完成后绘制扇形图
          this.drawPieChart()
        })
      }
    } catch (error) {
      this.setData({
        hotCategories: []
      })
    }
  },

  // 切换时间范围
  changeTimeRange(e) {
    const range = e.currentTarget.dataset.range
    this.setData({
      selectedRange: range
    }, () => {
      this.loadHotCategories()
    })
    
    wx.showToast({
      title: '已切换到' + (range === 'today' ? '今日' : range === 'week' ? '一周' : '一月'),
      icon: 'success'
    })
  },

  // 绘制扇形图
  drawPieChart() {
    const query = wx.createSelectorQuery()
    query.select('#pieChart')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0]) {
          return
        }
        
        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        // 使用 wx.getWindowInfo() 替代废弃的 wx.getSystemInfoSync()
        const windowInfo = wx.getWindowInfo()
        const dpr = windowInfo.pixelRatio
        
        // 设置 canvas 尺寸
        const width = res[0].width * dpr
        const height = res[0].height * dpr
        canvas.width = width
        canvas.height = height
        ctx.scale(dpr, dpr)
        
        const centerX = width / 2 / dpr
        const centerY = height / 2 / dpr
        const radius = Math.min(width, height) / 2 / dpr * 0.8
        
        // 清空画布
        ctx.clearRect(0, 0, width, height)
        
        const categories = this.data.hotCategories
        if (categories.length === 0) return
        
        // 计算总帖子数
        const total = categories.reduce((sum, item) => sum + item.postCount, 0)
        
        let startAngle = -Math.PI / 2 // 从顶部开始
        
        // 绘制每个扇形
        categories.forEach((item) => {
          const sliceAngle = (item.postCount / total) * 2 * Math.PI
          
          ctx.beginPath()
          ctx.moveTo(centerX, centerY)
          ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle)
          ctx.closePath()
          
          ctx.fillStyle = item.color
          ctx.fill()
          
          // 绘制边框
          ctx.strokeStyle = '#ffffff'
          ctx.lineWidth = 2
          ctx.stroke()
          
          startAngle += sliceAngle
        })
        
        // 绘制中心圆（形成环形图效果）
        ctx.beginPath()
        ctx.arc(centerX, centerY, radius * 0.5, 0, 2 * Math.PI)
        ctx.fillStyle = '#ffffff'
        ctx.fill()
        
        // 绘制中心文字
        ctx.fillStyle = '#333333'
        ctx.font = 'bold 24px sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText(total.toString(), centerX, centerY - 10)
        
        ctx.font = '14px sans-serif'
        ctx.fillStyle = '#666666'
        ctx.fillText('总帖子数', centerX, centerY + 15)
      })
  },

  // 加载近 7 日活跃趋势数据
  async loadDauTrend() {
    try {
      const response = await api.getDauTrend({ days: 7 })
      if (response && response.code === 200 && response.data) {
        this.setData({
          dauTrendData: response.data
        }, () => {
          // 数据加载完成后绘制折线图
          this.drawDauTrendChart()
        })
      }
    } catch (error) {
      this.setData({
        dauTrendData: []
      })
    }
  },

  // 绘制 DAU 趋势折线图
  drawDauTrendChart() {
    const query = wx.createSelectorQuery()
    query.select('#dauTrendCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0]) {
          return
        }
        
        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        // 使用 wx.getWindowInfo() 替代废弃的 wx.getSystemInfoSync()
        const windowInfo = wx.getWindowInfo()
        const dpr = windowInfo.pixelRatio
        
        // 设置 canvas 尺寸
        const width = res[0].width * dpr
        const height = res[0].height * dpr
        canvas.width = width
        canvas.height = height
        ctx.scale(dpr, dpr)
        
        const data = this.data.dauTrendData
        if (!data || data.length === 0) return
        
        // 图表边距
        const padding = {
          top: 40,
          bottom: 50,
          left: 60,
          right: 30
        }
        
        const chartWidth = width / dpr - padding.left - padding.right
        const chartHeight = height / dpr - padding.top - padding.bottom
        
        // 清空画布
        ctx.clearRect(0, 0, width / dpr, height / dpr)
        
        // 背景色
        ctx.fillStyle = '#fafafa'
        ctx.fillRect(padding.left, padding.top, chartWidth, chartHeight)
        
        // 找到最大值和最小值
        const values = data.map(item => item.activeUsers)
        const maxValue = Math.max(...values)
        const minValue = Math.min(...values)
        const valueRange = maxValue - minValue || 1
        
        // 绘制网格线
        ctx.strokeStyle = '#e0e0e0'
        ctx.lineWidth = 1
        const gridLines = 5
        for (let i = 0; i <= gridLines; i++) {
          const y = padding.top + (chartHeight / gridLines) * i
          ctx.beginPath()
          ctx.moveTo(padding.left, y)
          ctx.lineTo(padding.left + chartWidth, y)
          ctx.stroke()
          
          // Y 轴刻度值
          const value = maxValue - (valueRange / gridLines) * i
          ctx.fillStyle = '#999'
          ctx.font = '12px sans-serif'
          ctx.textAlign = 'right'
          ctx.textBaseline = 'middle'
          ctx.fillText(Math.round(value).toString(), padding.left - 10, y)
        }
        
        // 绘制折线
        const points = []
        const stepX = chartWidth / (data.length - 1)
        
        // 计算每个点的坐标
        data.forEach((item, index) => {
          const x = padding.left + stepX * index
          const y = padding.top + chartHeight - ((item.activeUsers - minValue) / valueRange) * chartHeight
          points.push({ x, y, ...item })
        })
        
        // 绘制折线
        ctx.beginPath()
        ctx.moveTo(points[0].x, points[0].y)
        
        // 使用曲线连接点
        for (let i = 1; i < points.length; i++) {
          const prev = points[i - 1]
          const curr = points[i]
          const cpX = (prev.x + curr.x) / 2
          ctx.quadraticCurveTo(prev.x, prev.y, cpX, (prev.y + curr.y) / 2)
        }
        ctx.lineTo(points[points.length - 1].x, points[points.length - 1].y)
        
        ctx.strokeStyle = '#667eea'
        ctx.lineWidth = 3
        ctx.lineCap = 'round'
        ctx.lineJoin = 'round'
        ctx.stroke()
        
        // 绘制渐变填充
        const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + chartHeight)
        gradient.addColorStop(0, 'rgba(102, 126, 234, 0.3)')
        gradient.addColorStop(1, 'rgba(102, 126, 234, 0.05)')
        
        ctx.beginPath()
        ctx.moveTo(points[0].x, padding.top + chartHeight)
        points.forEach((point, index) => {
          if (index === 0) {
            ctx.lineTo(point.x, point.y)
          } else {
            const prev = points[index - 1]
            const cpX = (prev.x + point.x) / 2
            ctx.quadraticCurveTo(prev.x, prev.y, cpX, (prev.y + point.y) / 2)
          }
        })
        ctx.lineTo(points[points.length - 1].x, padding.top + chartHeight)
        ctx.closePath()
        ctx.fillStyle = gradient
        ctx.fill()
        
        // 绘制数据点
        points.forEach((point) => {
          // 外圆
          ctx.beginPath()
          ctx.arc(point.x, point.y, 8, 0, 2 * Math.PI)
          ctx.fillStyle = '#ffffff'
          ctx.fill()
          ctx.strokeStyle = '#667eea'
          ctx.lineWidth = 3
          ctx.stroke()
          
          // 内圆
          ctx.beginPath()
          ctx.arc(point.x, point.y, 4, 0, 2 * Math.PI)
          ctx.fillStyle = '#667eea'
          ctx.fill()
        })
        
        // 绘制 X 轴标签（日期）
        ctx.fillStyle = '#666'
        ctx.font = '12px sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'top'
        points.forEach((point, index) => {
          const dateLabel = point.date.substring(5) // 显示 MM-DD
          ctx.fillText(dateLabel, point.x, padding.top + chartHeight + 10)
        })
        
        // 绘制标题
        ctx.fillStyle = '#333'
        ctx.font = 'bold 14px sans-serif'
        ctx.textAlign = 'left'
        ctx.textBaseline = 'top'
        ctx.fillText('活跃用户数', padding.left, 10)
        
        // 在数据点上方显示具体数值
        ctx.fillStyle = '#667eea'
        ctx.font = 'bold 12px sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'bottom'
        points.forEach((point) => {
          ctx.fillText(point.activeUsers.toString(), point.x, point.y - 12)
        })
      })
  }
})
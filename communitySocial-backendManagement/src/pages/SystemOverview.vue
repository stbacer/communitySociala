<template>
  <div class="dashboard-container">
    <h2 class="page-title">系统统计</h2>
    
    <!-- 核心数据卡片 -->
    <div class="stats-overview">
      <div class="stat-card primary">
        <div class="stat-icon">👥</div>
        <div class="stat-content">
          <div class="stat-value">{{ formatNumber((stats.totalUsers || 0) - 1) }}</div>
          <div class="stat-label">总用户数</div>
          <div class="stat-detail">
            <span class="detail-item">居民：{{ formatNumber(residentCount) }}</span>
            <span class="detail-item">管理员：{{ formatNumber(adminCount) }}</span>
          </div>
        </div>
      </div>
      
      <div class="stat-card success">
        <div class="stat-icon">🏘️</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.communityCount || 0 }}</div>
          <div class="stat-label">社区数量</div>
          <div class="stat-detail">覆盖全部区域</div>
        </div>
      </div>
      
      <div class="stat-card warning">
        <div class="stat-icon">📝</div>
        <div class="stat-content">
          <div class="stat-value">{{ formatNumber(stats.totalPosts || 0) }}</div>
          <div class="stat-label">总帖子数</div>
          <div class="stat-detail">今日新增：{{ formatNumber(stats.todayPosts || 0) }}</div>
        </div>
      </div>
    </div>
    
    <!-- 详细统计数据 -->
    <div class="stats-detail">
      <!-- 社区统计 -->
      <div class="stats-section full-width">
        <div class="section-header">
          <h3>🏘️ 社区统计</h3>
          <button @click="loadCommunitiesStatistics" class="refresh-btn" :disabled="loading.communities">
            {{ loading.communities ? '加载中...' : '刷新' }}
          </button>
        </div>
        
        <div v-if="communitiesStats && communitiesStats.length > 0" class="stats-content">
          <div class="community-table">
            <table>
              <thead>
                <tr>
                  <th>排名</th>
                  <th>社区名称</th>
                  <th>用户总数</th>
                  <th>帖子总数</th>
                  <th>今日新增用户</th>
                  <th>今日新增帖子</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in communitiesStats" :key="item.community">
                  <td>{{ index + 1 }}</td>
                  <td class="community-name clickable" @click="showCommunityDetail(item.community)">{{ item.community }}</td>
                  <td class="number">{{ formatNumber(item.userCount || 0) }}</td>
                  <td class="number">{{ formatNumber(item.postCount || 0) }}</td>
                  <td class="number positive">{{ formatNumber(item.todayNewUsers || 0) }}</td>
                  <td class="number positive">{{ formatNumber(item.todayNewPosts || 0) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        
        <div v-else-if="!loading.communities" class="empty-state">
          <p>暂无社区数据</p>
        </div>
      </div>
    </div>

    <!-- 社区详情弹窗 -->
    <div v-if="showDetailDialog" class="dialog-overlay" @click="closeCommunityDetail">
      <div class="detail-dialog" @click.stop>
        <div class="dialog-header">
          <h3>🏘️ {{ selectedCommunity }} - 详情</h3>
          <button class="close-btn" @click="closeCommunityDetail">×</button>
        </div>
        
        <div v-if="loading.communityDetail" class="loading-state">
          <p>加载中...</p>
        </div>
        
        <div v-else-if="communityDetail" class="dialog-content">
          <!-- 基础数据 -->
          <div class="detail-overview">
            <div class="overview-item">
              <div class="item-label">总用户数</div>
              <div class="item-value">{{ formatNumber(communityDetail.totalUsers || 0) }}</div>
            </div>
            <div class="overview-item">
              <div class="item-label">总发帖量</div>
              <div class="item-value">{{ formatNumber(communityDetail.totalPosts || 0) }}</div>
            </div>
          </div>

          <!-- 发帖排行榜 -->
          <div class="detail-section">
            <h4>🏆 发帖排行榜</h4>
            <div class="ranking-list" v-if="communityDetail.top10Users && communityDetail.top10Users.length > 0">
              <div class="ranking-item" v-for="(user, index) in communityDetail.top10Users" :key="user.userId">
                <span class="ranking-num" :class="getRankingClass(index)">{{ index + 1 }}</span>
                <img :src="user.avatarUrl || '/default-avatar.png'" class="ranking-avatar" />
                <span class="ranking-name">{{ user.nickname }}</span>
                <span class="ranking-count">{{ formatNumber(user.postCount) }} 帖</span>
              </div>
            </div>
            <div v-else class="empty-tip">暂无发帖数据</div>
          </div>

          <!-- 板块分布 -->
          <div class="detail-section">
            <h4>📊 板块分布</h4>
            <div v-if="communityDetail.categoryDistribution && communityDetail.categoryDistribution.length > 0">
              <!-- ECharts 饼图容器 -->
              <div ref="pieChartRef" style="width: 100%; height: 350px;"></div>
              
              <!-- 图例 -->
              <div class="pie-legend">
                <div class="legend-item" v-for="(cat, index) in communityDetail.categoryDistribution" :key="cat.categoryId">
                  <span class="legend-color" :style="{ backgroundColor: getPieColor(index) }"></span>
                  <span class="legend-name">{{ cat.categoryName }}</span>
                  <span class="legend-value">{{ formatNumber(cat.postCount) }} ({{ calculatePercentage(cat.postCount, totalPostsForPie) }}%)</span>
                </div>
              </div>
            </div>
            <div v-else class="empty-tip">暂无板块数据</div>
          </div>

          <!-- 活跃用户统计 -->
          <div class="detail-section">
            <h4>👥 活跃用户</h4>
            <div v-if="communityDetail.activeUsers" class="active-users-overview">
              <div class="active-stat-item">
                <div class="active-stat-label">今日活跃用户</div>
                <div class="active-stat-value">{{ formatNumber(communityDetail.activeUsers.todayActive || 0) }}</div>
              </div>
              <div class="active-stat-item">
                <div class="active-stat-label">近 7 日活跃用户</div>
                <div class="active-stat-value">{{ formatNumber(communityDetail.activeUsers.weekActive || 0) }}</div>
              </div>
              <div class="active-stat-item">
                <div class="active-stat-label">月活跃用户</div>
                <div class="active-stat-value">{{ formatNumber(communityDetail.activeUsers.monthActive || 0) }}</div>
              </div>
            </div>
            <div v-else class="empty-tip">暂无活跃用户数据</div>
          </div>

          <!-- 近 7 日活跃趋势 -->
          <div class="detail-section">
            <h4>📈 近 7 日活跃趋势</h4>
            <div v-if="communityDetail.sevenDayTrend && communityDetail.sevenDayTrend.length > 0">
              <!-- ECharts 趋势图容器 -->
              <div ref="trendChartRef" style="width: 100%; height: 300px;"></div>
            </div>
            <div v-else class="empty-tip">暂无趋势数据</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getSystemStatistics, getCommunitiesStatistics, getCommunityDetail } from '@/api/system'

export default {
  name: 'SystemOverview',
  setup() {
    const stats = ref({
      totalUsers: 0,
      communityCount: 0,
      totalPosts: 0,
      totalComments: 0,
      onlineUsers: 0,
      todayPosts: 0,
      todayComments: 0
    })
    
    const communitiesStats = ref([])
    const communityDetail = ref(null)
    const showDetailDialog = ref(false)
    const selectedCommunity = ref('')
    
    const loading = ref({
      overview: false,
      communities: false,
      communityDetail: false
    })
    
    // ECharts 实例引用
    const pieChartRef = ref(null)
    const trendChartRef = ref(null)
    let pieChartInstance = null
    let trendChartInstance = null
    
    // 计算饼图总帖子数（用于图例百分比计算）
    const totalPostsForPie = computed(() => {
      if (!communityDetail.value?.categoryDistribution) return 0
      return communityDetail.value.categoryDistribution.reduce(
        (sum, cat) => sum + (cat.postCount || 0), 0
      )
    })
    
    // 计算饼图数据
    const pieSlices = computed(() => {
      if (!communityDetail.value?.categoryDistribution) return []
      
      const slices = []
      let currentAngle = -90 // 从顶部开始
      
      const totalPosts = totalPostsForPie.value
      
      communityDetail.value.categoryDistribution.forEach((cat, index, arr) => {
        const startAngle = currentAngle
        
        // 如果是最后一个板块，使用剩余角度，确保总和为 360 度
        let endAngle
        if (index === arr.length - 1) {
          endAngle = 270 // 最后回到起点，形成完整圆形
        } else {
          // 根据实际帖子数计算角度
          const percentage = totalPosts > 0 ? (cat.postCount / totalPosts * 100) : 0
          endAngle = currentAngle + (percentage / 100 * 360)
        }
        
        slices.push({
          categoryId: cat.categoryId,
          startAngle,
          endAngle,
          postCount: cat.postCount,
          categoryName: cat.categoryName
        })
        
        currentAngle = endAngle
      })
      
      return slices
    })
    
    // 饼图颜色配置
    const pieColors = [
      '#1e88e5', '#42a5f5', '#64b5f6', '#90caf9',
      '#4caf50', '#66bb6a', '#81c784', '#a5d6a7',
      '#ff9800', '#ffb74d', '#ffcc80', '#ffe0b2',
      '#ab47bc', '#ba68c8', '#ce93d8', '#e1bee7'
    ]
    
    const getPieColor = (index) => {
      return pieColors[index % pieColors.length]
    }
    
    // 计算饼图路径
    const getPiePath = (startAngle, endAngle) => {
      const startRad = (startAngle * Math.PI) / 180
      const endRad = (endAngle * Math.PI) / 180
      
      const x1 = 50 + 40 * Math.cos(startRad)
      const y1 = 50 + 40 * Math.sin(startRad)
      const x2 = 50 + 40 * Math.cos(endRad)
      const y2 = 50 + 40 * Math.sin(endRad)
      
      const largeArcFlag = endAngle - startAngle <= 180 ? 0 : 1
      
      return `M 50 50 L ${x1} ${y1} A 40 40 0 ${largeArcFlag} 1 ${x2} ${y2} Z`
    }
    
    // 计算居民用户和管理员数量（暂时保留，用于核心数据卡片）
    const residentCount = computed(() => {
      return 0 // 简化处理，后续可以从 stats 中获取
    })
    
    const adminCount = computed(() => {
      return 0 // 简化处理，后续可以从 stats 中获取
    })
    
    // 格式化数字（千分位）
    const formatNumber = (num) => {
      return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    }
    
    // 获取百分比（保留一位小数）
    const calculatePercentage = (part, total) => {
      if (total === 0) return '0.0'
      const percentage = (part / total * 100)
      return percentage.toFixed(1)
    }
    
    // 获取趋势图最大活跃数
    const maxActiveCount = computed(() => {
      if (!communityDetail.value?.sevenDayTrend) return 0
      const counts = communityDetail.value.sevenDayTrend.map(day => day.activeCount || 0)
      return Math.max(...counts, 1) // 至少为 1，避免除以 0
    })
    
    // 获取趋势条高度（百分比）
    const getTrendBarHeight = (count, maxCount) => {
      if (maxCount === 0) return '0%'
      const height = (count / maxCount * 100)
      return `${Math.max(height, 5)}%` // 最小 5%，保证可见性
    }
    
    // 加载平台总览数据
    const loadOverview = async () => {
      loading.value.overview = true
      try {
        const res = await getSystemStatistics()
        if (res.data.code === 200 && res.data.data) {
          const data = res.data.data
          stats.value = {
            totalUsers: data.totalUsers || 0,
            communityCount: data.communityCount || 0,
            totalPosts: data.totalPosts || 0,
            totalComments: data.totalComments || 0,
            onlineUsers: data.onlineUsers || 0,
            todayPosts: data.todayPosts || 0,
            todayComments: data.todayComments || 0
          }
        }
      } catch (error) {
        console.error('加载总览数据失败:', error)
      } finally {
        loading.value.overview = false
      }
    }
    
    // 加载社区统计数据
    const loadCommunitiesStatistics = async () => {
      loading.value.communities = true
      try {
        const res = await getCommunitiesStatistics()
        if (res.data.code === 200 && res.data.data) {
          communitiesStats.value = res.data.data
        }
      } catch (error) {
        console.error('加载社区统计数据失败:', error)
      } finally {
        loading.value.communities = false
      }
    }
    
    // 显示社区详情
    const showCommunityDetail = async (community) => {
      selectedCommunity.value = community
      showDetailDialog.value = true
      loading.value.communityDetail = true
      
      try {
        const res = await getCommunityDetail(community)
        if (res.data.code === 200 && res.data.data) {
          communityDetail.value = res.data.data
          // 等待 DOM 更新后初始化 ECharts
          // 使用双重 nextTick + setTimeout 确保 v-if 条件满足且 DOM 完全渲染
          await nextTick()
          await nextTick()
          
          // 额外延迟 100ms，确保弹窗动画完成且容器有有效尺寸
          setTimeout(() => {
            console.log('开始初始化图表')
            console.log('饼图ref:', pieChartRef.value, '趋势图ref:', trendChartRef.value)
            console.log('板块分布数据:', communityDetail.value?.categoryDistribution)
            console.log('七日趋势数据:', communityDetail.value?.sevenDayTrend)
            
            initPieChart()
            initTrendChart()
          }, 100)
        }
      } catch (error) {
        console.error('加载社区详情失败:', error)
      } finally {
        loading.value.communityDetail = false
      }
    }
    
    // 关闭社区详情弹窗
    const closeCommunityDetail = () => {
      showDetailDialog.value = false
      communityDetail.value = null
      selectedCommunity.value = ''
      
      // 销毁 ECharts 实例
      if (pieChartInstance) {
        pieChartInstance.dispose()
        pieChartInstance = null
      }
      if (trendChartInstance) {
        trendChartInstance.dispose()
        trendChartInstance = null
      }
    }
    
    // 获取排名样式
    const getRankingClass = (index) => {
      if (index === 0) return 'rank-1'
      if (index === 1) return 'rank-2'
      if (index === 2) return 'rank-3'
      return ''
    }
    
    // 初始化饼图
    const initPieChart = () => {
      if (!pieChartRef.value || !communityDetail.value?.categoryDistribution) {
        console.warn('饼图容器或数据未就绪')
        return
      }
      
      try {
        // 如果实例已存在，先销毁
        if (pieChartInstance) {
          pieChartInstance.dispose()
        }
        
        // 确保容器有有效尺寸
        const containerWidth = pieChartRef.value.offsetWidth
        const containerHeight = pieChartRef.value.offsetHeight
        
        if (containerWidth === 0 || containerHeight === 0) {
          console.error('饼图容器尺寸为 0，无法初始化')
          return
        }
        
        pieChartInstance = echarts.init(pieChartRef.value)
        
        const data = communityDetail.value.categoryDistribution.map((cat, index) => ({
          value: cat.postCount,
          name: cat.categoryName,
          itemStyle: { color: getPieColor(index) }
        }))
        
        const option = {
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
          },
          legend: {
            orient: 'vertical',
            right: 10,
            top: 'center'
          },
          series: [
            {
              type: 'pie',
              radius: ['40%', '70%'],
              center: ['35%', '50%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2
              },
              label: {
                show: false,
                position: 'center'
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 16,
                  fontWeight: 'bold'
                }
              },
              labelLine: {
                show: false
              },
              data: data
            }
          ]
        }
        
        pieChartInstance.setOption(option)
        console.log('饼图初始化成功')
      } catch (error) {
        console.error('饼图初始化失败:', error)
      }
    }
    
    // 初始化趋势图
    const initTrendChart = () => {
      if (!trendChartRef.value || !communityDetail.value?.sevenDayTrend) {
        console.warn('趋势图容器或数据未就绪')
        return
      }
      
      try {
        // 如果实例已存在，先销毁
        if (trendChartInstance) {
          trendChartInstance.dispose()
        }
        
        // 确保容器有有效尺寸
        const containerWidth = trendChartRef.value.offsetWidth
        const containerHeight = trendChartRef.value.offsetHeight
        
        if (containerWidth === 0 || containerHeight === 0) {
          console.error('趋势图容器尺寸为 0，无法初始化')
          return
        }
        
        trendChartInstance = echarts.init(trendChartRef.value)
        
        const dates = communityDetail.value.sevenDayTrend.map(day => day.date)
        const counts = communityDetail.value.sevenDayTrend.map(day => day.activeCount)
        
        const option = {
          tooltip: {
            trigger: 'axis'
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: dates
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              name: '活跃用户',
              type: 'line',
              smooth: true,
              data: counts,
              itemStyle: {
                color: '#1e88e5'
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(30, 136, 229, 0.3)' },
                  { offset: 1, color: 'rgba(30, 136, 229, 0.1)' }
                ])
              }
            }
          ]
        }
        
        trendChartInstance.setOption(option)
        console.log('趋势图初始化成功')
      } catch (error) {
        console.error('趋势图初始化失败:', error)
      }
    }
    
    onMounted(() => {
      loadOverview()
      loadCommunitiesStatistics()
      
      // 添加窗口大小变化监听
      window.addEventListener('resize', handleResize)
    })
    
    // 组件卸载时清理
    onUnmounted(() => {
      // 移除窗口大小变化监听
      window.removeEventListener('resize', handleResize)
      
      // 销毁 ECharts 实例
      if (pieChartInstance) {
        pieChartInstance.dispose()
        pieChartInstance = null
      }
      if (trendChartInstance) {
        trendChartInstance.dispose()
        trendChartInstance = null
      }
    })
    
    // 处理窗口大小变化
    const handleResize = () => {
      if (pieChartInstance) {
        pieChartInstance.resize()
      }
      if (trendChartInstance) {
        trendChartInstance.resize()
      }
    }
    
    return {
      stats,
      communitiesStats,
      communityDetail,
      showDetailDialog,
      selectedCommunity,
      loading,
      pieChartRef,
      trendChartRef,
      residentCount,
      adminCount,
      totalPostsForPie,
      maxActiveCount,
      formatNumber,
      calculatePercentage,
      getTrendBarHeight,
      pieSlices,
      getPiePath,
      getPieColor,
      loadCommunitiesStatistics,
      showCommunityDetail,
      closeCommunityDetail,
      getRankingClass
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}

/* 核心数据卡片 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  font-size: 48px;
  line-height: 1;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #1e88e5;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-detail {
  font-size: 12px;
  color: #999;
}

.detail-item {
  margin-right: 12px;
}

/* 卡片颜色变体 */
.stat-card.primary .stat-value { color: #1e88e5; }
.stat-card.success .stat-value { color: #4caf50; }
.stat-card.warning .stat-value { color: #ff9800; }
.stat-card.info .stat-value { color: #00bcd4; }

/* 详细统计 */
.stats-detail {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stats-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.stats-section.full-width {
  grid-column: 1 / -1;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.section-header h3 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.refresh-btn {
  padding: 6px 12px;
  font-size: 13px;
  color: #1e88e5;
  background: #e3f2fd;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  background: #bbdefb;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stats-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.stat-item .label {
  font-size: 12px;
  color: #666;
}

.stat-item .value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.stat-item .value.positive {
  color: #4caf50;
}

.stat-item .value.negative {
  color: #f44336;
}

.stat-item .value.success {
  color: #4caf50;
}

.stat-item .value.warning {
  color: #ff9800;
}

.stat-item .value.danger {
  color: #f44336;
}

/* 分布图表 */
.distribution-chart {
  margin-top: 8px;
}

.distribution-chart h4 {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
}

.role-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.role-bar {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bar-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.role-name {
  color: #666;
}

.role-count {
  font-weight: bold;
  color: #333;
}

.bar-bg {
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #1e88e5, #42a5f5);
  border-radius: 4px;
  transition: width 0.3s;
}

.status-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.status-tag {
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
}

.status-normal {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-disabled {
  background: #ffebee;
  color: #c62828;
}

.status-pending {
  background: #fff3e0;
  color: #ef6c00;
}

.type-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-tag {
  padding: 6px 12px;
  background: #e3f2fd;
  color: #1565c0;
  border-radius: 16px;
  font-size: 13px;
}

/* 社区表格 */
.community-table {
  width: 100%;
  overflow-x: auto;
}

.community-table table {
  width: 100%;
  border-collapse: collapse;
}

.community-table th,
.community-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.community-table th {
  background: #f5f5f5;
  font-weight: 600;
  color: #333;
  font-size: 14px;
  white-space: nowrap;
}

.community-table tbody tr:hover {
  background: #f9f9f9;
}

.community-table .community-name {
  font-weight: 500;
  color: #1e88e5;
}

.community-table .number {
  font-weight: 600;
  color: #333;
}

.community-table .number.positive {
  color: #4caf50;
}

.community-name.clickable {
  cursor: pointer;
  transition: all 0.2s;
}

.community-name.clickable:hover {
  color: #1565c0;
  text-decoration: underline;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

/* 弹窗样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.detail-dialog {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 800px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.dialog-header h3 {
  font-size: 20px;
  color: #333;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #333;
}

.dialog-content {
  padding: 20px;
}

.loading-state,
.empty-tip {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.detail-overview {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.overview-item {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.item-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.item-value {
  font-size: 28px;
  font-weight: bold;
  color: #1e88e5;
}

.detail-section {
  margin-bottom: 30px;
}

.detail-section h4 {
  font-size: 16px;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #f0f0f0;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  transition: transform 0.2s;
}

.ranking-item:hover {
  transform: translateX(4px);
  background: #f0f0f0;
}

.ranking-num {
  font-size: 16px;
  font-weight: bold;
  color: #666;
  width: 24px;
  text-align: center;
}

.ranking-num.rank-1 {
  color: #ffd700;
  font-size: 20px;
}

.ranking-num.rank-2 {
  color: #c0c0c0;
  font-size: 18px;
}

.ranking-num.rank-3 {
  color: #cd7f32;
  font-size: 18px;
}

.ranking-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.ranking-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.ranking-count {
  font-size: 14px;
  color: #1e88e5;
  font-weight: 600;
}

.category-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.category-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-info {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.category-name {
  font-weight: 500;
  color: #333;
}

.category-count {
  color: #666;
}

.category-bar-bg {
  height: 10px;
  background: #e0e0e0;
  border-radius: 5px;
  overflow: hidden;
}

.category-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #1e88e5, #42a5f5);
  border-radius: 5px;
  transition: width 0.3s;
}

.category-percentage {
  font-size: 13px;
  color: #1e88e5;
  font-weight: 600;
}

/* 饼图样式 */
.pie-chart-container {
  position: relative;
  width: 240px;
  height: 240px;
  margin: 20px auto;
}

.pie-chart {
  width: 100%;
  height: 100%;
  transform: rotate(0deg);
}

.pie-slice {
  stroke: #fff;
  stroke-width: 1;
  transition: opacity 0.2s;
}

.pie-slice:hover {
  opacity: 0.8;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.1);
}

.center-total {
  font-size: 24px;
  font-weight: bold;
  color: #1e88e5;
}

.center-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.pie-legend {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #f9f9f9;
  border-radius: 6px;
  transition: transform 0.2s;
}

.legend-item:hover {
  transform: translateX(4px);
  background: #f0f0f0;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  flex-shrink: 0;
}

.legend-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.legend-value {
  font-size: 13px;
  color: #1e88e5;
  font-weight: 600;
}

/* 活跃用户统计 */
.active-users-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 16px;
}

.active-stat-item {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: transform 0.2s;
}

.active-stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.active-stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 500;
}

.active-stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1e88e5;
}

/* 趋势图表 */
.trend-chart {
  margin-top: 16px;
}

.trend-bars {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 8px;
  padding: 20px 10px 10px;
  height: 200px;
  background: linear-gradient(to bottom, #f9fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.trend-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.trend-bar-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.trend-bar {
  width: 100%;
  min-height: 5px;
  background: linear-gradient(to top, #1e88e5, #42a5f5);
  border-radius: 4px 4px 0 0;
  transition: all 0.3s ease;
  position: relative;
}

.trend-bar:hover {
  background: linear-gradient(to top, #1565c0, #1e88e5);
  transform: scaleY(1.05);
}

.trend-date {
  font-size: 12px;
  color: #666;
  font-weight: 500;
  white-space: nowrap;
}

.trend-count {
  font-size: 13px;
  color: #1e88e5;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }
  
  .stats-detail {
    grid-template-columns: 1fr;
  }
  
  .stat-value {
    font-size: 24px !important;
  }
}
</style>

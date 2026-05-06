<template>
  <div class="log-detail-page">
    <div class="page-header">
      <button @click="goBack" class="back-btn">← 返回列表</button>
      <h2>日志详情</h2>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 详情内容 -->
    <div v-else-if="logDetail" class="detail-content">
      <!-- 用户基本信息卡片 (仅当查看登录/注册日志时显示) -->
      <div v-if="isLoginOrRegisterLog && userInfo" class="info-card">
        <h3>用户基本信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <label>用户 ID:</label>
            <span>{{ userInfo.userId }}</span>
          </div>
          <div class="info-item">
            <label>用户名:</label>
            <span>{{ userInfo.username }}</span>
          </div>
          <div class="info-item">
            <label>昵称:</label>
            <span>{{ userInfo.nickname }}</span>
          </div>
          <div class="info-item">
            <label>手机号:</label>
            <span>{{ userInfo.phone || '-' }}</span>
          </div>
          <div class="info-item">
            <label>邮箱:</label>
            <span>{{ userInfo.phone || '-' }}</span>
          </div>
          <div class="info-item">
            <label>角色:</label>
            <span :class="['role-badge', getRoleClass(userInfo.userRole)]">
              {{ getRoleText(userInfo.userRole) }}
            </span>
          </div>
          <div class="info-item">
            <label>状态:</label>
            <span :class="['status-badge', userInfo.status === 1 ? 'success' : 'disabled']">
              {{ userInfo.status === 1 ? '正常' : '禁用' }}
            </span>
          </div>
          <div class="info-item">
            <label>注册时间:</label>
            <span>{{ formatDate(userInfo.createTime) }}</span>
          </div>
        </div>
      </div>
      <!-- 基本信息卡片 -->
      <div class="info-card">
        <h3>基本信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <label>日志 ID:</label>
            <span>{{ logDetail.logId }}</span>
          </div>
          <div class="info-item">
            <label>操作用户:</label>
            <span>{{ formatOperator(logDetail.userId, logDetail.operatorName) }}</span>
          </div>
        </div>
      </div>

      <!-- 操作信息卡片 -->
      <div class="info-card">
        <h3>操作信息</h3>
        <div class="info-grid">
          <div class="info-item full-width">
            <label>操作类型:</label>
            <span :class="['operation-badge', getOperationClass(logDetail.operation, logDetail.module)]">
              {{ getOperationText(logDetail.operation, logDetail.module) }}
            </span>
          </div>
          <div class="info-item full-width">
            <label>操作内容:</label>
            <span class="content-text">{{ logDetail.content || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 操作时间 -->
      <div class="info-card">
        <h3>操作时间</h3>
        <div class="info-item">
          <span class="time-text">{{ formatDate(logDetail.createTime) }}</span>
        </div>
      </div>

      <!-- 该用户的登录和注册日志 (仅当查看登录/注册日志时显示) -->
      <div v-if="isLoginOrRegisterLog && allUserLogs.length > 0" class="info-card">
        <h3>该用户所有登录和注册记录</h3>
        <div class="table-container">
          <table class="log-table">
            <thead>
              <tr>
                <th>日志 ID</th>
                <th>操作用户</th>
                <th>操作类型</th>
                <th class="content-column">操作详情</th>
                <th>操作时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in allUserLogs" :key="log.logId">
                <td>{{ log.logId }}</td>
                <td>{{ formatOperator(log.userId, log.operatorName) }}</td>
                <td>
                  <span :class="['operation-badge', getOperationClass(log.operation, log.module)]">
                    {{ getOperationText(log.operation, log.module) }}
                  </span>
                </td>
                <td class="content-column">{{ log.content || '-' }}</td>
                <td>{{ formatDate(log.createTime) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 帖子的历史操作日志 (仅当操作目标为帖子时显示) -->
      <div v-if="isPostRelatedLog && postHistoryLogs.length > 0" class="info-card">
        <h3>该帖子的历史操作记录</h3>
        <div class="table-container">
          <table class="log-table">
            <thead>
              <tr>
                <th>日志 ID</th>
                <th>操作用户</th>
                <th>操作类型</th>
                <th class="content-column">操作详情</th>
                <th>客户端</th>
                <th>操作时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in postHistoryLogs" :key="log.logId">
                <td>{{ log.logId }}</td>
                <td>{{ formatOperator(log.userId, log.operatorName) }}</td>
                <td>
                  <span :class="['operation-badge', getOperationClass(log.operation, log.module)]">
                    {{ getOperationText(log.operation, log.module) }}
                  </span>
                </td>
                <td class="content-column">{{ log.content || '-' }}</td>
                <td>
                  <span :class="['client-type-badge', getClientTypeClass(log.clientType)]">
                    {{ getClientTypeText(log.clientType) }}
                  </span>
                </td>
                <td>{{ formatDate(log.createTime) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button @click="deleteCurrentLog" class="delete-btn">删除该日志</button>
        <button @click="goBack" class="back-normal-btn">返回</button>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-state">
      <p>日志不存在或加载失败</p>
      <button @click="goBack" class="back-btn">返回列表</button>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import apiClient from '@/api'

export default {
  name: 'LogDetailPage',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const logDetail = ref(null)
    const userInfo = ref(null)
    const allUserLogs = ref([])
    const loading = ref(true)
    const loginLogs = ref([])
    const isLoginOrRegisterLog = ref(false)
    const isPostRelatedLog = ref(false)
    const postHistoryLogs = ref([])

    // 判断是否为登录或注册操作
    const checkIsLoginOrRegisterLog = (log) => {
      if (!log) return false
      // 登录操作：operation='LOGIN' 或 subModule='LOGIN'
      // 注册操作：operation='CREATE' 且 subModule='REGISTER'
      return log.operation === 'LOGIN' || 
             log.subModule === 'LOGIN' || 
             (log.operation === 'CREATE' && log.subModule === 'REGISTER')
    }

    // 判断是否为帖子相关操作
    const checkIsPostRelatedLog = (log) => {
      if (!log) return false
      // 帖子相关操作：module='POST' 或 content 中包含"帖子"
      return log.module === 'POST' || 
             (log.content && log.content.includes('帖子'))
    }

    // 获取日志详情
    const fetchLogDetail = async () => {
      loading.value = true
      
      try {
        const logId = route.params.logId
        
        // 调用后端 API 获取日志详情
        const response = await apiClient.get(`/sadmin/log/${logId}`)
        
        if (response.data.code === 200 && response.data.data) {
          logDetail.value = response.data.data
          
          // 判断是否为登录/注册日志
          isLoginOrRegisterLog.value = checkIsLoginOrRegisterLog(logDetail.value)
          // 判断是否为帖子相关日志
          isPostRelatedLog.value = checkIsPostRelatedLog(logDetail.value)
          
          // 如果是登录/注册日志，获取用户基本信息和所有相关日志
          if (isLoginOrRegisterLog.value) {
            await fetchUserInfo(logDetail.value.userId)
            await fetchAllUserLogs(logDetail.value.userId)
          } else if (isPostRelatedLog.value) {
            // 如果是帖子相关日志，获取该帖子的历史操作日志
            await fetchPostHistoryLogs(logDetail.value)
          } else {
            // 否则获取该用户的普通日志列表
            await fetchUserLogs(logDetail.value.userId)
          }
        } else {
          logDetail.value = null
          loginLogs.value = []
        }
      } catch (error) {
        logDetail.value = null
        loginLogs.value = []
      } finally {
        loading.value = false
      }
    }

    // 获取用户基本信息
    const fetchUserInfo = async (userId) => {
      try {
        const response = await apiClient.get(`/sadmin/user/detail/${userId}`)
        if (response.data.code === 200 && response.data.data) {
          userInfo.value = response.data.data
        } else {
          userInfo.value = null
        }
      } catch (error) {
        userInfo.value = null
      }
    }

    // 获取用户的所有登录和注册日志
    const fetchAllUserLogs = async (userId) => {
      try {
        const response = await apiClient.get(`/sadmin/log/user/${userId}/login-register`, {
          params: { page: 1, size: 50 }
        })
        if (response.data.code === 200 && response.data.data) {
          allUserLogs.value = response.data.data.records || []
        } else {
          allUserLogs.value = []
        }
      } catch (error) {
        allUserLogs.value = []
      }
    }

    // 获取用户的普通日志列表（用于非登录/注册日志）
    const fetchUserLogs = async (userId) => {
      try {
        const response = await apiClient.get(`/sadmin/log/user/${userId}`, {
          params: { page: 1, size: 10 }
        })
        if (response.data.code === 200 && response.data.data) {
          loginLogs.value = response.data.data || []
        } else {
          loginLogs.value = []
        }
      } catch (error) {
        loginLogs.value = []
      }
    }

    // 从内容中提取帖子 ID
    const extractPostId = (content) => {
      if (!content) return null
      
      // 支持两种格式的帖子 ID：
      // 格式 1: id:post_xxx (带 post_前缀)
      // 格式 2: id:xxx (纯 UUID，无 post_前缀)
      
      // 先尝试匹配带 post_前缀的格式
      let match = content.match(/id:(post_[a-zA-Z0-9_]+)/i)
      if (match) {
        return match[1]
      }
      
      // 再尝试匹配帖子相关的 ID（在"帖子"二字后面）
      if (content.includes('帖子')) {
        // 找到"帖子"后面的 id:xxx 格式
        const postIndex = content.indexOf('帖子')
        const contentAfterPost = content.substring(postIndex)
        match = contentAfterPost.match(/id:([a-zA-Z0-9]+)/i)
        if (match && !match[1].startsWith('user_') && !match[1].startsWith('admin_')) {
          return match[1]
        }
      }
      
      // 最后尝试匹配通用的 id:xxx 格式（排除 user_和 admin_开头的）
      match = content.match(/id:([a-zA-Z0-9]+)/gi)
      if (match) {
        // 遍历所有匹配项，找到不是用户 ID 的那个
        for (const m of match) {
          const idValue = m.replace('id:', '')
          if (!idValue.startsWith('user_') && !idValue.startsWith('admin_')) {
            return idValue
          }
        }
      }
      
      return null
    }

    // 获取帖子的历史操作日志
    const fetchPostHistoryLogs = async (log) => {
      try {
        // 从当前日志的内容中提取帖子 ID
        const postId = extractPostId(log.content)
        if (!postId) {
          return
        }
        
        // 调用后端 API 获取该帖子的所有操作日志
        const response = await apiClient.get(`/sadmin/log/post/${postId}`, {
          params: { page: 1, size: 50 }
        })
        if (response.data.code === 200 && response.data.data) {
          postHistoryLogs.value = response.data.data.records || []
        } else {
          postHistoryLogs.value = []
        }
      } catch (error) {
        postHistoryLogs.value = []
      }
    }

    // 生成模拟详情数据
    const generateMockLogDetail = (logId) => {
      // 定义不同的操作类型和对应的 content 格式
      const logTemplates = [
        {
          operation: 'CREATE',
          module: 'USER',
          subModule: 'REGISTER',
          content: '张三注册了 user_789 账户 (id:user_789)',
          userId: 'user_789',
          operatorName: '张三'
        },
        {
          operation: 'QUERY',
          module: 'USER',
          subModule: 'LOGIN',
          content: '李四（id:user_456）登录了超级管理员端',
          userId: 'user_456',
          operatorName: '李四'
        },
        {
          operation: 'CREATE',
          module: 'USER',
          subModule: 'AUTH_SUBMIT',
          content: '王五（id:user_123）提交了实名认证',
          userId: 'user_123',
          operatorName: '王五'
        },
        {
          operation: 'UPDATE',
          module: 'USER',
          subModule: 'AUTH_APPROVE',
          content: '张管理员（id:admin_001）审核通过了王五（id:user_123）的实名认证',
          userId: 'admin_001',
          operatorName: '张管理员'
        },
        {
          operation: 'CREATE',
          module: 'POST',
          subModule: 'PUBLISH',
          content: '小明（id:user_789）提交发布了帖子出售二手电脑（id:post_20260313_001）',
          userId: 'user_789',
          operatorName: '小明'
        },
        {
          operation: 'UPDATE',
          module: 'POST',
          subModule: 'REVIEW_APPROVE',
          content: '张管理员（id:admin_001）审核通过了小明（id:user_789）的帖子出售二手电脑（id:post_20260313_001）',
          userId: 'admin_001',
          operatorName: '张管理员'
        },
        {
          operation: 'CREATE',
          module: 'POST',
          subModule: 'LIKE',
          content: '小刚（id:user_111）点赞了小明（id:user_789）的帖子出售二手电脑（id:post_20260313_001）',
          userId: 'user_111',
          operatorName: '小刚'
        },
        {
          operation: 'CREATE',
          module: 'POST',
          subModule: 'COLLECT',
          content: '小芳（id:user_222）收藏了小明（id:user_789）的帖子出售二手电脑（id:post_20260313_001）',
          userId: 'user_222',
          operatorName: '小芳'
        },
        {
          operation: 'CREATE',
          module: 'POST',
          subModule: 'COMMENT',
          content: '小丽（id:user_333）评论了小明（id:user_789）的帖子出售二手电脑（id:post_20260313_001）',
          userId: 'user_333',
          operatorName: '小丽'
        }
      ]
      
      // 随机选择一个模板
      const template = logTemplates[Math.floor(Math.random() * logTemplates.length)]
      
      return {
        logId: logId || `log_${Date.now()}`,
        userId: template.userId,
        operatorName: template.operatorName,
        operation: template.operation,
        content: template.content,
        createTime: new Date(Date.now() - Math.floor(Math.random() * 7 * 24 * 60 * 60 * 1000)).toISOString()
      }
    }

    // 生成模拟用户信息
    const generateMockUserInfo = (userId) => {
      const roles = [
        { value: 1, text: '普通用户' },
        { value: 2, text: '社区管理员' },
        { value: 3, text: '超级管理员' }
      ]
      const role = roles[Math.floor(Math.random() * roles.length)]
      
      return {
        userId: userId,
        username: `user_${Math.floor(Math.random() * 1000)}`,
        nickname: `用户${Math.floor(Math.random() * 1000)}`,
        phone: `1${Math.floor(Math.random() * 9)}${Math.random().toString().slice(2, 11)}`,
        userRole: role.value,
        status: Math.random() > 0.1 ? 1 : 0, // 90% 概率正常
        avatarUrl: null,
        createTime: new Date(Date.now() - Math.floor(Math.random() * 365 * 24 * 60 * 60 * 1000)).toISOString()
      }
    }

    // 生成模拟登录和注册日志
    const generateMockLoginAndRegisterLogs = (userId) => {
      const logs = []
      
      // 生成 1 条注册记录
      const registerTime = new Date(Date.now() - Math.floor(Math.random() * 365 * 24 * 60 * 60 * 1000))
      logs.push({
        logId: `register_log_${userId}`,
        userId: userId,
        operation: 'CREATE',
        subModule: 'REGISTER',
        content: `${userId} 注册了账户`,
        createTime: registerTime.toISOString(),
        clientType: Math.floor(Math.random() * 3) + 1,
        ip: '192.168.1.1',
        duration: Math.floor(Math.random() * 500)
      })
      
      // 生成 5-10 条登录记录
      const count = Math.floor(Math.random() * 6) + 5
      for (let i = 0; i < count; i++) {
        const daysAgo = Math.floor(Math.random() * 30)
        const hoursAgo = Math.floor(Math.random() * 24)
        const loginTime = new Date(Date.now() - (daysAgo * 24 + hoursAgo) * 60 * 60 * 1000)
        
        const loginTypes = ['账号密码登录', '微信授权登录']
        const loginType = loginTypes[Math.floor(Math.random() * loginTypes.length)]
        
        logs.push({
          logId: `login_log_${Date.now()}_${i}`,
          userId: userId,
          operation: 'LOGIN',
          subModule: 'LOGIN',
          content: `${userId} 通过${loginType}方式登录`,
          createTime: loginTime.toISOString(),
          clientType: Math.floor(Math.random() * 3) + 1,
          ip: `192.168.1.${Math.floor(Math.random() * 255)}`,
          duration: Math.floor(Math.random() * 300)
        })
      }
      
      // 按时间倒序排序
      return logs.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    }

    // 格式化操作用户信息
    const formatOperator = (userId, operatorName) => {
      if (!userId && !operatorName) return '-'
      const id = userId || '未知 ID'
      const name = operatorName || '匿名用户'
      return `${name}(${id})`
    }

    // 格式化 JSON
    const formatJson = (jsonStr) => {
      if (!jsonStr) return '-'
      try {
        if (typeof jsonStr === 'string') {
          const obj = JSON.parse(jsonStr)
          return JSON.stringify(obj, null, 2)
        }
        return JSON.stringify(jsonStr, null, 2)
      } catch (e) {
        return jsonStr
      }
    }

    // 获取操作类型文本
    const getOperationText = (operation, module) => {
      // 特殊处理：评论模块的 CREATE 操作显示为"评论"
      if (module === 'COMMENT' && operation === 'CREATE') {
        return '评论'
      }
      
      const operationMap = {
        'LOGIN': '登录',
        'APPROVE_AUTH': '管理员审核',
        'COLLECT': '收藏',
        'LIKE': '点赞',
        '实名审核': '实名审核',
        'REPORT': '举报',
        'DELETE': '删除',
        '帖子': '帖子',
        'CREATE': '创建',
        'UNCOLLECT': '取消收藏',
        'UNLIKE': '取消点赞'
      }
      return operationMap[operation] || operation
    }

    // 获取操作类型样式类
    const getOperationClass = (operation, module) => {
      // 特殊处理：评论模块的 CREATE 操作使用 comment 样式
      if (module === 'COMMENT' && operation === 'CREATE') {
        return 'comment'
      }
      
      const classMap = {
        'LOGIN': 'login',
        'APPROVE_AUTH': 'admin-approve',
        'COLLECT': 'collect',
        'LIKE': 'like',
        '实名审核': 'auth-review',
        'REPORT': 'report',
        'DELETE': 'delete',
        '帖子': 'post',
        'CREATE': 'create',
        'UNCOLLECT': 'uncollect',
        'UNLIKE': 'unlike'
      }
      return classMap[operation] || 'default'
    }

    // 获取客户端类型文本
    const getClientTypeText = (clientType) => {
      const typeMap = {
        '1': '超级管理员端',
        '2': '社区管理员端',
        '3': '居民端'
      }
      return typeMap[clientType] || '未知'
    }

    // 获取客户端类型样式类
    const getClientTypeClass = (clientType) => {
      const classMap = {
        '1': 'super-admin',
        '2': 'admin',
        '3': 'user'
      }
      return classMap[clientType] || 'default'
    }

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN', { 
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      })
    }

    // 删除当前日志
    const deleteCurrentLog = async () => {
      if (confirm('确认删除该日志？')) {
        // 模拟删除
        await new Promise(resolve => setTimeout(resolve, 200))
        alert('删除成功')
        router.push('/logs')
      }
    }

    // 返回列表页
    const goBack = () => {
      router.push('/logs')
    }

    // 获取角色文本
    const getRoleText = (userRole) => {
      const roleMap = {
        '1': '普通用户',
        '2': '社区管理员',
        '3': '超级管理员'
      }
      return roleMap[userRole] || '未知'
    }

    // 获取角色样式类
    const getRoleClass = (userRole) => {
      const classMap = {
        '1': 'user',
        '2': 'admin',
        '3': 'super-admin'
      }
      return classMap[userRole] || 'default'
    }

    // 获取登录方式文本
    const getLoginTypeText = (content) => {
      if (!content) return '-'
      if (content.includes('账号密码')) return '账号密码'
      if (content.includes('微信')) return '微信授权'
      return '未知'
    }

    // 获取登录方式样式类
    const getLoginTypeClass = (content) => {
      if (!content) return 'default'
      if (content.includes('账号密码')) return 'password'
      if (content.includes('微信')) return 'wechat'
      return 'default'
    }

    onMounted(() => {
      fetchLogDetail()
    })

    return {
      logDetail,
      userInfo,
      allUserLogs,
      loading,
      loginLogs,
      isLoginOrRegisterLog,
      isPostRelatedLog,
      postHistoryLogs,
      formatOperator,
      formatJson,
      getOperationText,
      getOperationClass,
      getClientTypeText,
      getClientTypeClass,
      getRoleText,
      getRoleClass,
      getLoginTypeText,
      getLoginTypeClass,
      formatDate,
      deleteCurrentLog,
      goBack
    }
  }
}
</script>

<style scoped>
.log-detail-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.back-btn {
  padding: 8px 16px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background-color: #5a6268;
}

.page-header h2 {
  margin: 0;
  color: #333;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.info-card h3 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 18px;
  border-bottom: 2px solid #4CAF50;
  padding-bottom: 10px;
  display: inline-block;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item label {
  font-weight: 600;
  color: #555;
  font-size: 14px;
}

.info-item span {
  color: #333;
  word-break: break-all;
}

.operation-badge {
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  display: inline-block;
  width: fit-content;
}

.operation-badge.query {
  background-color: #d1ecf1;
  color: #0c5460;
}

.operation-badge.create {
  background-color: #d4edda;
  color: #155724;
}

.operation-badge.update {
  background-color: #fff3cd;
  color: #856404;
}

.operation-badge.delete {
  background-color: #f8d7da;
  color: #721c24;
}

.operation-badge.like {
  background-color: #ffc107;
  color: #856404;
}

.operation-badge.unlike {
  background-color: #e0e0e0;
  color: #666666;
}

.operation-badge.collect {
  background-color: #ff9800;
  color: #ffffff;
}

.operation-badge.uncollect {
  background-color: #bdbdbd;
  color: #666666;
}

.operation-badge.report {
  background-color: #e53935;
  color: #ffffff;
}

.operation-badge.comment {
  background-color: #9c27b0;
  color: #ffffff;
}

.operation-badge.auth-review {
  background-color: #2196F3;
  color: #ffffff;
}

.operation-badge.admin-approve {
  background-color: #4CAF50;
  color: #ffffff;
}

.operation-badge.post {
  background-color: #673AB7;
  color: #ffffff;
}

.content-text {
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.time-text {
  font-family: monospace;
  color: #333;
}

.action-buttons {
  display: flex;
  gap: 10px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.delete-btn {
  padding: 10px 20px;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  background-color: #c82333;
}

.back-normal-btn {
  padding: 10px 20px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.back-normal-btn:hover {
  background-color: #5a6268;
}

.error-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.error-state p {
  margin-bottom: 20px;
  font-size: 16px;
}

/* 登录日志表格样式 */
.login-logs-table {
  margin-top: 15px;
}

.login-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.login-table th,
.login-table td {
  padding: 10px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.login-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
}

.login-table tbody tr:hover {
  background-color: #f5f5f5;
}

.client-type-badge,
.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.client-type-badge.super-admin {
  background-color: #9c27b0;
  color: white;
}

.client-type-badge.admin {
  background-color: #2196f3;
  color: white;
}

.client-type-badge.user {
  background-color: #4caf50;
  color: white;
}

.role-badge.super-admin {
  background-color: #9c27b0;
  color: white;
}

.role-badge.admin {
  background-color: #2196f3;
  color: white;
}

.role-badge.user {
  background-color: #4caf50;
  color: white;
}

.login-type-badge.password {
  background-color: #ff9800;
  color: white;
}

.login-type-badge.wechat {
  background-color: #07c160;
  color: white;
}

.status-badge.success {
  background-color: #d4edda;
  color: #155724;
}

.status-badge.failed {
  background-color: #f8d7da;
  color: #721c24;
}

.status-badge.disabled {
  background-color: #e0e0e0;
  color: #666;
}

/* 表格样式 - 与 LogManagement 页面保持一致 */
.table-container {
  overflow-x: auto;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin-bottom: 20px;
  background: white;
}

.log-table {
  width: 100%;
  border-collapse: collapse;
}

.log-table th, .log-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.log-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
}

.content-column {
  min-width: 300px;
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

@media (max-width: 768px) {
  .log-detail-page {
    padding: 10px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
  }

  .delete-btn, .back-normal-btn {
    width: 100%;
  }
}
</style>

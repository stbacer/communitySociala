// 操作日志 API
import apiClient from './index'

/**
 * 分页查询操作日志列表
 */
export function getLogList(params) {
  return apiClient.get('/sadmin/log/list', { params })
}

/**
 * 查询日志详情
 */
export function getLogDetail(logId) {
  return apiClient.get(`/sadmin/log/${logId}`)
}

/**
 * 删除单条日志
 */
export function deleteLog(logId) {
  return apiClient.delete(`/sadmin/log/${logId}`)
}

/**
 * 批量删除日志
 */
export function batchDeleteLogs(logIds) {
  return apiClient.post('/sadmin/log/batch-delete', logIds)
}

/**
 * 清理旧日志
 */
export function cleanOldLogs() {
  // TODO: 后端实现后再对接
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          code: 200,
          message: `清理成功，共清理 0 条旧日志`
        }
      })
    }, 500)
  })
}

/**
 * 获取日志统计信息
 */
export function getLogStatistics() {
  // TODO: 后端实现后再对接
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          code: 200,
          message: 'success',
          data: []
        }
      })
    }, 300)
  })
}

/**
 * 查询用户的操作日志
 */
export function getUserLogs(userId, params) {
  return apiClient.get(`/sadmin/log/user/${userId}`, { params })
}

/**
 * 查询指定模块的操作日志
 */
export function getModuleLogs(module, date, params) {
  return apiClient.get(`/sadmin/log/module/${module}`, { params: { date, ...params } })
}

/**
 * 获取今日统计数据
 */
export function getTodayStats() {
  return apiClient.get('/sadmin/log/stats/today')
}

/**
 * 获取所有已记录的操作类型（从 Redis Set 中读取）
 */
export function getOperationTypes() {
  return apiClient.get('/sadmin/log/operation-types')
}

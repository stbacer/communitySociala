import apiClient from './index'

export function getSystemConfig() {
  return apiClient.get('/sadmin/system/config')
}

export function updateSystemConfig(data) {
  return apiClient.put('/sadmin/system/config', data)
}

/**
 * 获取平台总览统计数据（包含社区数量、用户数量等）
 */
export function getSystemStatistics() {
  return apiClient.get('/sadmin/statistics/overview')
}

/**
 * 获取用户统计数据
 */
export function getUserStatistics() {
  return apiClient.get('/sadmin/statistics/users')
}

/**
 * 获取内容统计数据
 */
export function getContentStatistics() {
  return apiClient.get('/sadmin/statistics/content')
}

/**
 * 获取互动统计数据
 */
export function getInteractionStatistics() {
  return apiClient.get('/sadmin/statistics/interaction')
}

/**
 * 获取所有社区的统计数据列表
 */
export function getCommunitiesStatistics() {
  return apiClient.get('/sadmin/statistics/communities')
}

/**
 * 获取指定社区的详细统计信息
 */
export function getCommunityDetail(community) {
  return apiClient.get('/sadmin/statistics/community/detail', {
    params: { community }
  })
}

export function clearCache() {
  return apiClient.delete('/sadmin/system/cache')
}
import apiClient from './index'

// 获取用户列表
export const getUserList = (params) => {
  return apiClient.get('/admin/user/list', { params })
}

// 获取所有社区列表
export const getAllCommunities = () => {
  return apiClient.get('/admin/user/communities')
}

// 创建用户
export const createUser = (data) => {
  return apiClient.post('/admin/user/create', data)
}

// 更新用户信息
export const updateUser = (userId, data) => {
  return apiClient.put(`/admin/user/update/${userId}`, data)
}

// 删除用户
export const deleteUser = (userId) => {
  return apiClient.delete(`/admin/user/delete/${userId}`)
}

// 更新用户状态
export const updateUserStatus = (userId, status) => {
  return apiClient.put(`/admin/user/status/${userId}`, null, {
    params: { status }
  })
}

// 获取用户详情
export const getUserDetail = (userId) => {
  return apiClient.get(`/admin/user/detail/${userId}`)
}

// 重置用户密码
export const resetUserPassword = (userId, newPassword) => {
  return apiClient.put(`/admin/user/reset-password/${userId}`, { newPassword })
}

// 获取社区管理员所在的省市区列表（从 MySQL 数据库，用于普通用户选择）
export const getAdminRegions = () => {
  return apiClient.get('/sadmin/user/admin-regions')
}

// 获取完整的省市区数据（从 Redis 缓存，用于社区管理员选择）
export const getAllRegions = () => {
  return apiClient.get('/sadmin/user/regions')
}

// 获取省份列表（懒加载）
export const getProvinces = () => {
  return apiClient.get('/sadmin/user/provinces')
}

// 根据省份代码获取城市列表
export const getCitiesByProvinceCode = (provinceCode) => {
  return apiClient.get(`/sadmin/user/cities/${provinceCode}`)
}

// 根据城市代码获取区县列表
export const getDistrictsByCityCode = (cityCode) => {
  return apiClient.get(`/sadmin/user/districts/${cityCode}`)
}

// ========== 管理员实名认证审核相关接口 ==========

// 获取待审核管理员实名认证列表
export const getPendingAdminAuth = (params) => {
  return apiClient.get('/admin/management/pending-auth', { params })
}

// 通过管理员实名认证
export const approveAdminAuth = (data) => {
  return apiClient.put('/admin/management/approve-auth', data)
}

// 驳回管理员实名认证
export const rejectAdminAuth = (data) => {
  return apiClient.put('/admin/management/reject-auth', data)
}
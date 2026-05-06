import apiClient from './index'

export function getConfigList() {
  return apiClient.get('/sadmin/config/list')
}

export function updateConfig(data) {
  return apiClient.put('/sadmin/config/update', data)
}

export function resetConfig(configKey) {
  return apiClient.put(`/sadmin/config/reset/${configKey}`)
}
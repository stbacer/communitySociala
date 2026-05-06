import apiClient from './index'

export function getPendingContent(params) {
  return apiClient.get('/sadmin/content/pending-list', { params })
}

export function reviewContent(data) {
  return apiClient.put('/sadmin/content/review', data)
}

export function batchReviewContent(data) {
  return apiClient.put('/sadmin/content/batch-review', data)
}

export function getReviewStatistics() {
  return apiClient.get('/sadmin/content/statistics')
}
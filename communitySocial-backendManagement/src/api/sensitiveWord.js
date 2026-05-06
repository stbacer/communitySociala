import apiClient from './index'

export function getSensitiveWordList(params) {
  return apiClient.get('/sadmin/sensitive-word/list', { params })
}

export function createSensitiveWord(data) {
  return apiClient.post('/sadmin/sensitive-word/create', data)
}

export function updateSensitiveWord(wordId, data) {
  return apiClient.put(`/sadmin/sensitive-word/update/${wordId}`, data)
}

export function deleteSensitiveWord(wordId) {
  return apiClient.delete(`/sadmin/sensitive-word/delete/${wordId}`)
}

export function batchDeleteSensitiveWords(ids) {
  return apiClient.delete('/sadmin/sensitive-word/batch-delete', { data: ids })
}

export function hardDeleteSensitiveWord(wordId) {
  return apiClient.delete(`/sadmin/sensitive-word/hard-delete/${wordId}`)
}

export function batchHardDeleteSensitiveWords(ids) {
  return apiClient.post('/sadmin/sensitive-word/batch-hard-delete', ids)
}
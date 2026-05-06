import apiClient from './index'

export function getCategoryList() {
  return apiClient.get('/sadmin/category/list')
}

export function createCategory(data) {
  return apiClient.post('/sadmin/category/create', data)
}

export function updateCategory(categoryId, data) {
  return apiClient.put(`/sadmin/category/update/${categoryId}`, data)
}

export function deleteCategory(categoryId) {
  return apiClient.delete(`/sadmin/category/delete/${categoryId}`)
}
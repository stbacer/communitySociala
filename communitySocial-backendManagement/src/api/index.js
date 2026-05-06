import axios from 'axios'

// 根据环境判断 API 基础路径
const isProduction = import.meta.env.PROD

const apiClient = axios.create({
  baseURL: isProduction ? '/admin/api' : 'http://127.0.0.1:8080',
  headers: {
    'Content-Type': 'application/json'
  }
})

// attach token if present
apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

export default apiClient

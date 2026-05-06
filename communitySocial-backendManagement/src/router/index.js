import { createRouter, createWebHistory } from 'vue-router'

// lazy-loaded page components
const Login = () => import('@/pages/Login.vue')
const Dashboard = () => import('@/pages/Dashboard.vue')
const SystemOverview = () => import('@/pages/SystemOverview.vue')
const UserManagement = () => import('@/pages/UserManagement.vue')
const CategoryManagement = () => import('@/pages/CategoryManagement.vue')
const SensitiveWordManagement = () => import('@/pages/SensitiveWordManagement.vue')


const AdminReview = () => import('@/pages/AdminReview.vue')
const SystemMonitor = () => import('@/pages/SystemMonitor.vue')
const LogManagement = () => import('@/pages/LogManagement.vue')
const LogDetailPage = () => import('@/pages/LogDetailPage.vue')


const routes = [
  { path: '/login', name: 'Login', component: Login },
  {
    path: '/',
    component: Dashboard,
    children: [
      { path: '', redirect: '/overview' },
      { path: 'overview', name: 'SystemOverview', component: SystemOverview },
      { path: 'users', name: 'UserManagement', component: UserManagement },
      { path: 'admin-review', name: 'AdminReview', component: AdminReview, meta: { hideInMenu: true } },
      { path: 'categories', name: 'CategoryManagement', component: CategoryManagement },
      { path: 'sensitive-words', name: 'SensitiveWordManagement', component: SensitiveWordManagement },

      { path: 'monitor', name: 'SystemMonitor', component: SystemMonitor },
      { path: 'logs', name: 'LogManagement', component: LogManagement },
      { path: 'logs/:logId', name: 'LogDetailPage', component: LogDetailPage },

    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// enhanced auth guard with role checking
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfoStr = localStorage.getItem('userInfo')
  
  // 如果没有token，重定向到登录页
  if (to.name !== 'Login' && !token) {
    next({ name: 'Login' })
    return
  }
  
  // 如果有token，检查用户角色权限
  if (token && userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr)
      
      // 只允许超级管理员(userRole === 3)访问
      if (userInfo.userRole !== 3) {
        // 非超级管理员尝试访问，清除登录信息并跳转到登录页
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        alert('权限不足：仅超级管理员可访问后端管理系统')
        next({ name: 'Login' })
        return
      }
    } catch (e) {
      // 用户信息解析失败，清除登录信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      next({ name: 'Login' })
      return
    }
  }
  
  // 正常放行
  next()
})

export default router

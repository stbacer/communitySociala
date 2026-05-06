<template>
  <div class="page-container">
    <h2>用户管理</h2>
    
    <!-- 搜索和筛选区域 -->
    <div class="search-section">
      <div class="search-row">
        <input 
          v-model="searchForm.phone" 
          placeholder="手机号" 
          class="search-input"
        />
        <input 
          v-model="searchForm.nickname" 
          placeholder="昵称" 
          class="search-input"
        />
        <select v-model="searchForm.community" class="search-select">
          <option value="">全部社区</option>
          <option v-for="comm in communities" :key="comm" :value="comm">
            {{ comm }}
          </option>
        </select>
        <select v-model="searchForm.userRole" class="search-select">
          <option value="">全部角色</option>
          <option value="1">普通用户</option>
          <option value="2">社区管理员</option>
          <option value="3">超级管理员</option>
        </select>
        <button @click="searchUsers" class="search-btn">搜索</button>
        <button @click="resetSearch" class="reset-btn">重置</button>
        <button @click="showAddModal = true" class="add-btn">添加用户</button>
        <button @click="showAuthReviewModal = true" class="auth-review-btn">管理员认证审核</button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 用户表格 -->
    <div class="table-container">
      <table class="user-table">
        <thead>
          <tr>
            <th>用户 ID</th>
            <th>昵称</th>
            <th>用户角色</th>
            <th>用户状态</th>
            <th>所在社区</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.userId">
            <td>{{ user.userId }}</td>
            <td>{{ user.nickname }}</td>
            <td>{{ getUserRoleText(user.userRole) }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(user.status)]">
                {{ getStatusText(user.status) }}
              </span>
            </td>
            <td>{{ user.community || '未设置' }}</td>
            <td class="action-cell">
              <button @click="viewUser(user)" class="view-btn">查看</button>
              <button @click="editUser(user)" class="edit-btn">编辑</button>
              <button 
                @click="toggleUserStatus(user)" 
                :class="['status-toggle-btn', user.status === 1 ? 'disable-btn' : 'enable-btn']"
              >
                {{ user.status === 1 ? '禁用' : '启用' }}
              </button>
              <button @click="resetPassword(user)" class="reset-pwd-btn">重置密码</button>
              <button @click="deleteUser(user)" class="delete-btn">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 空状态 -->
      <div v-if="users.length === 0 && !loading" class="empty-state">
        <p>暂无用户数据</p>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1 || total > 0" class="pagination">
      <div class="page-size-selector">
        <span>每页显示：</span>
        <select v-model="pageSize" @change="onPageSizeChange" class="page-size-select">
          <option :value="10">10 条</option>
          <option :value="20">20 条</option>
          <option :value="50">50 条</option>
          <option :value="100">100 条</option>
        </select>
      </div>
      <button 
        @click="changePage(1)" 
        :disabled="currentPage === 1"
        class="page-btn"
        title="首页"
      >
        首页
      </button>
      <button 
        @click="changePage(currentPage - 1)" 
        :disabled="currentPage === 1"
        class="page-btn"
        title="上一页"
      >
        上一页
      </button>
      <span class="page-info">
        第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ total }} 条
      </span>
      <button 
        @click="changePage(currentPage + 1)" 
        :disabled="currentPage >= totalPages"
        class="page-btn"
        title="下一页"
      >
        下一页
      </button>
      <button 
        @click="changePage(totalPages)" 
        :disabled="currentPage >= totalPages"
        class="page-btn"
        title="末页"
      >
        末页
      </button>
    </div>

    <!-- 查看用户详情模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="closeViewModal">
      <div class="modal-content" @click.stop>
        <h3>用户详情</h3>
        <div v-if="currentUser" class="user-detail">
          <div class="detail-grid">
            <div class="detail-section">
              <h4>基本信息</h4>
              <div class="detail-row">
                <label>用户ID:</label>
                <span>{{ currentUser.userId }}</span>
              </div>
              <div class="detail-row">
                <label>手机号:</label>
                <span>{{ currentUser.phone || '未设置' }}</span>
              </div>
              <div class="detail-row">
                <label>昵称:</label>
                <span>{{ currentUser.nickname }}</span>
              </div>
              <div class="detail-row">
                <label>头像:</label>
                <div>
                  <img 
                    v-if="currentUser.avatarUrl" 
                    :src="currentUser.avatarUrl" 
                    :alt="currentUser.nickname" 
                    class="avatar-large"
                    @error="handleImageError"
                  />
                  <span v-else class="no-avatar-large">无头像</span>
                </div>
              </div>
            </div>
            
            <div class="detail-section">
              <h4>联系方式</h4>
              <div class="detail-row">
                <label>手机号:</label>
                <span>{{ currentUser.phone || '未设置' }}</span>
              </div>
              <div class="detail-row" v-if="currentUser.openid">
                <label>微信OpenID:</label>
                <span class="openid-text">{{ currentUser.openid }}</span>
              </div>
            </div>
            
            <div class="detail-section">
              <h4>个人资料</h4>
              <div class="detail-row">
                <label>性别:</label>
                <span>{{ getGenderText(currentUser.gender) }}</span>
              </div>
              <div class="detail-row">
                <label>真实姓名:</label>
                <span>{{ currentUser.realName || '未认证' }}</span>
              </div>
              <div class="detail-row">
                <label>身份证号:</label>
                <span>{{ currentUser.idCard || '未认证' }}</span>
              </div>
              <div class="detail-row">
                <label>个性签名:</label>
                <span>{{ currentUser.signature || '未设置' }}</span>
              </div>
              <div class="detail-row">
                <label>所在社区:</label>
                <span>{{ currentUser.community || '未设置' }}</span>
              </div>
            </div>
            
            <div class="detail-section">
              <h4>账户信息</h4>
              <div class="detail-row">
                <label>用户角色:</label>
                <span>{{ getUserRoleText(currentUser.userRole) }}</span>
              </div>
              <div class="detail-row">
                <label>用户状态:</label>
                <span :class="['status-badge', getStatusClass(currentUser.status)]">
                  {{ getStatusText(currentUser.status) }}
                </span>
              </div>
              <div class="detail-row">
                <label>认证状态:</label>
                <span>{{ getAuthStatusText(currentUser.authStatus) }}</span>
              </div>
              <div class="detail-row">
                <label>创建时间:</label>
                <span>{{ formatDate(currentUser.createTime) }}</span>
              </div>
              <div class="detail-row">
                <label>最后登录时间:</label>
                <span>{{ currentUser.lastLoginTime ? formatDate(currentUser.lastLoginTime) : '从未登录' }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="closeViewModal" class="close-btn">关闭</button>
        </div>
      </div>
    </div>

    <!-- 编辑用户模态框 -->
    <div v-if="showEditModal" class="modal-overlay" @click="closeEditModal">
      <div class="modal-content" @click.stop>
        <h3>{{ isAdding ? '添加用户' : '编辑用户' }}</h3>
        <form @submit.prevent="saveUser" class="user-form">
          <div class="form-row">
            <label>用户角色:</label>
            <select v-model.number="editForm.userRole" @change="onUserRoleChange">
              <option :value="1">普通用户</option>
              <option :value="2">社区管理员</option>
              <option :value="3">超级管理员</option>
            </select>
          </div>
          <div class="form-row">
            <label>手机号:</label>
            <input 
              v-model="editForm.phone" 
              type="tel" 
              required 
              :disabled="!isAdding"
              placeholder="请输入手机号"
            />
          </div>
          <div class="form-row">
            <label>昵称:</label>
            <input 
              v-model="editForm.nickname" 
              type="text" 
              required 
              placeholder="请输入昵称"
            />
          </div>
          <div v-if="isAdding" class="form-row">
            <label>密码:</label>
            <input 
              v-model="editForm.password" 
              type="password" 
              required 
              placeholder="请输入密码"
            />
          </div>
          <div class="form-row">
            <label>头像:</label>
            <div class="avatar-upload-container">
              <!-- 当前头像预览 -->
              <div class="current-avatar-wrapper">
                <img 
                  v-if="editForm.avatarUrl" 
                  :src="editForm.avatarUrl" 
                  alt="头像预览"
                  class="avatar-preview"
                  @error="handleImageError"
                />
                <div v-else class="avatar-placeholder">无头像</div>
              </div>
              <!-- 上传区域 -->
              <div class="upload-section">
                <input 
                  type="file" 
                  ref="avatarFileInput"
                  accept="image/*"
                  @change="handleAvatarChange"
                  style="display: none;"
                />
                <button type="button" @click="triggerAvatarUpload" class="upload-btn">
                  {{ editForm.avatarUrl ? '更换头像' : '上传头像' }}
                </button>
                <div v-if="uploadingAvatar" class="upload-status">上传中...</div>
                <small class="form-hint">支持 JPG、PNG、GIF 格式，文件大小不超过 5MB</small>
              </div>
            </div>
          </div>
          <div class="form-row">
            <label>性别:</label>
            <select v-model="editForm.gender">
              <option value="0">未知</option>
              <option value="1">男</option>
              <option value="2">女</option>
            </select>
          </div>
          <div class="form-row">
            <label>真实姓名:</label>
            <input 
              v-model="editForm.realName" 
              type="text" 
              placeholder="请输入真实姓名"
            />
          </div>
          <div class="form-row">
            <label>身份证号:</label>
            <input 
              v-model="editForm.idCard" 
              type="text" 
              placeholder="请输入身份证号"
            />
          </div>
          <div class="form-row">
            <label>个性签名:</label>
            <textarea 
              v-model="editForm.signature" 
              placeholder="请输入个性签名"
            ></textarea>
          </div>
          <div class="form-row-group">
            <div class="form-row">
              <label>省份:</label>
              <select v-model="selectedProvince" @change="onProvinceChange">
                <option value="">请选择省份</option>
                <option v-for="province in getAvailableProvinces()" 
                        :key="province?.code || Math.random()" 
                        :value="province?.code || ''">
                  {{ province?.name || '' }}
                </option>
              </select>
            </div>
            <div class="form-row">
              <label>城市:</label>
              <select v-model="selectedCity" @change="onCityChange">
                <option value="">请选择城市</option>
                <option v-for="city in cities" 
                        :key="city?.code || Math.random()" 
                        :value="city?.code || ''">
                  {{ city?.name || '' }}
                </option>
              </select>
            </div>
            <div class="form-row">
              <label>区县:</label>
              <select v-model="selectedDistrict">
                <option value="">请选择区县</option>
                <option v-for="district in districts" 
                        :key="district?.code || Math.random()" 
                        :value="district?.name || ''">
                  {{ district?.name || '' }}
                </option>
              </select>
            </div>
            <div class="form-row" v-if="editForm && Number(editForm.userRole) === 2 && !nationalRegionsLoaded.value">
              <button @click="loadAllRegionsForSAdmin" class="other-city-btn" type="button">
                其他城市
              </button>
            </div>
          </div>
          <div class="form-row">
            <label>所在社区:</label>
            <!-- 所有角色都可以手动输入社区 -->
            <input 
              v-model="editForm.community" 
              type="text" 
              placeholder="请输入所在社区"
            />
          </div>
          <div class="form-row">
            <label>状态:</label>
            <select v-model="editForm.status">
              <option value="0">禁用</option>
              <option value="1">正常</option>
              <option value="2">待审核</option>
            </select>
          </div>
          <div class="modal-actions">
            <button type="submit" class="save-btn">保存</button>
            <button type="button" @click="closeEditModal" class="cancel-btn">取消</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 管理员认证审核模态框 -->
    <div v-if="showAuthReviewModal" class="modal-overlay" @click="closeAuthReviewModal">
      <div class="modal-content modal-content-large" @click.stop>
        <h3>社区管理员实名认证审核</h3>
        
        <!-- 搜索和筛选 -->
        <div class="auth-search-section">
          <input 
            v-model="authReviewKeyword" 
            placeholder="搜索姓名、手机号或昵称" 
            class="auth-search-input"
          />
          <button @click="loadPendingAdminAuths" class="search-btn">搜索</button>
          <button @click="resetAuthSearch" class="reset-btn">重置</button>
        </div>

        <!-- 加载状态 -->
        <div v-if="authReviewLoading" class="loading-overlay">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <!-- 待审核列表 -->
        <div class="auth-review-list">
          <div 
            v-for="user in pendingAdminAuths" 
            :key="user.userId" 
            class="auth-review-item"
          >
            <div class="auth-item-header">
              <div class="auth-user-info">
                <img 
                  v-if="user.avatarUrl" 
                  :src="user.avatarUrl" 
                  :alt="user.nickname" 
                  class="auth-avatar"
                  @error="handleImageError"
                />
                <span v-else class="auth-avatar-placeholder">{{ user.nickname?.charAt(0) || '用' }}</span>
                <div class="auth-user-details">
                  <div class="auth-user-name">{{ user.nickname }}</div>
                  <div class="auth-user-phone">{{ user.phone || '未绑定手机号' }}</div>
                </div>
              </div>
              <div class="auth-status-badge status-pending">审核中</div>
            </div>

            <div class="auth-item-body">
              <div class="auth-detail-row">
                <label>真实姓名:</label>
                <span>{{ user.realName || '未填写' }}</span>
              </div>
              <div class="auth-detail-row">
                <label>身份证号:</label>
                <span>{{ user.idCard || '未填写' }}</span>
              </div>
              <div class="auth-detail-row">
                <label>地址:</label>
                <span>{{ user.province || '' }} {{ user.city || '' }} {{ user.district || '' }}</span>
              </div>
              <div class="auth-detail-row">
                <label>所在社区:</label>
                <span>{{ user.community || '未填写' }}</span>
              </div>
              <div class="auth-detail-row">
                <label>身份证明图片:</label>
                <div class="auth-images-container">
                  <div 
                    v-for="(img, index) in (user.identityImages || [])" 
                    :key="index" 
                    class="auth-image-item"
                  >
                    <a :href="img" target="_blank" class="auth-image-link">
                      <img :src="img" :alt="'身份证明' + (index + 1)" @error="handleImageError" />
                    </a>
                  </div>
                </div>
              </div>
              <div class="auth-detail-row">
                <label>申请时间:</label>
                <span>{{ formatDate(user.createTime) }}</span>
              </div>
            </div>

            <div class="auth-item-actions">
              <button @click="approveAuth(user)" class="approve-btn">通过</button>
              <button @click="showRejectModal(user)" class="reject-btn">驳回</button>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="pendingAdminAuths.length === 0 && !authReviewLoading" class="empty-state">
            <p>暂无待审核的管理员认证申请</p>
          </div>
        </div>

        <div class="modal-actions">
          <button @click="closeAuthReviewModal" class="cancel-btn">关闭</button>
        </div>
      </div>
    </div>

    <!-- 驳回原因模态框 -->
    <div v-if="showRejectReasonModal" class="modal-overlay" @click="closeRejectModal">
      <div class="modal-content" @click.stop>
        <h3>填写驳回原因</h3>
        <textarea 
          v-model="rejectReason" 
          placeholder="请输入驳回原因（必填）" 
          class="reject-reason-textarea"
          rows="4"
        ></textarea>
        <div class="modal-actions">
          <button @click="submitReject" class="confirm-btn">确认驳回</button>
          <button @click="closeRejectModal" class="cancel-btn">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, watch } from 'vue'
import { getUserList, createUser, updateUser, deleteUser, updateUserStatus, resetUserPassword, getAdminRegions, getProvinces, getCitiesByProvinceCode, getDistrictsByCityCode, getPendingAdminAuth, approveAdminAuth, rejectAdminAuth, getAllCommunities } from '@/api/user'
// 不再使用 region.js，直接使用后端返回的管理员区域数据

export default {
  name: 'UserManagement',
  setup() {
    // 数据响应式变量
    const users = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const size = ref(10)
    const total = ref(0)
    const totalPages = ref(0)
    const loading = ref(false)
    
    // 搜索表单
    const searchForm = reactive({
      phone: '',
      nickname: '',
      community: '',
      userRole: ''
    })
    
    // 编辑表单
    const editForm = reactive({
      userId: '',
      phone: '',
      nickname: '',
      password: '',
      avatarUrl: '',
      gender: 0,
      realName: '',
      idCard: '',
      signature: '',
      community: '',
      userRole: 1,
      status: 1
    })
    
    // 当前用户详情
    const currentUser = ref(null)
    
    // 省市区数据（从后端获取）
    const allProvinces = ref([])
    const allCities = ref([])
    const allDistricts = ref([])
    const cities = ref([])  // 当前选中省份的城市列表（懒加载）
    const districts = ref([])  // 当前选中城市的区县列表（懒加载）
    const selectedProvince = ref('')
    const selectedCity = ref('')
    const selectedDistrict = ref('')
    
    // 标记是否已加载全国省份数据
    const nationalRegionsLoaded = ref(false)
    
    // 管理员省市区数据（用于普通用户）
    const adminProvinces = ref([])
    const adminCities = ref([])
    const adminDistricts = ref([])
    const adminCommunities = ref([])
    const allAdminRegionsLoaded = ref(false)
    
    // 所有社区列表（用于筛选）
    const communities = ref([])
    
    // 模态框状态
    const showViewModal = ref(false)
    const showEditModal = ref(false)
    const showAddModal = ref(false)
    const isAdding = ref(false)
    
    // 头像上传相关状态
    const uploadingAvatar = ref(false)
    const avatarFileInput = ref(null)
    
    // 管理员认证审核相关状态
    const showAuthReviewModal = ref(false)
    const showRejectReasonModal = ref(false)
    const pendingAdminAuths = ref([])
    const authReviewTotal = ref(0)
    const authReviewLoading = ref(false)
    const authReviewKeyword = ref('')
    const currentRejectUser = ref(null)
    const rejectReason = ref('')
    
    // 获取用户列表
    const fetchUsers = async (page = 1) => {
      loading.value = true
      try {
        const params = {
          page: page,
          size: pageSize.value,
          phone: searchForm.phone,
          nickname: searchForm.nickname,
          community: searchForm.community || undefined,
          userRole: searchForm.userRole || undefined
        }
            
        const res = await getUserList(params)
        if (res.data.code === 200) {
          users.value = res.data.data.records || []
          currentPage.value = res.data.data.page || page
          total.value = res.data.data.total || 0
          totalPages.value = Math.ceil(total.value / pageSize.value) || 0
        }
      } catch (err) {
        console.error('获取用户列表失败:', err)
        alert('获取用户列表失败：' + (err.response?.data?.message || err.message))
      } finally {
        loading.value = false
      }
    }
    
    // 搜索用户
    const searchUsers = () => {
      currentPage.value = 1
      fetchUsers(1)
    }
    
    // 重置搜索
    const resetSearch = () => {
      searchForm.phone = ''
      searchForm.nickname = ''
      searchForm.community = ''
      searchForm.userRole = ''
      currentPage.value = 1
      fetchUsers(1)
    }
    
    // 查看用户详情
    const viewUser = (user) => {
      currentUser.value = user
      showViewModal.value = true
    }
    
    // 关闭查看详情模态框
    const closeViewModal = () => {
      showViewModal.value = false
      currentUser.value = null
    }
    
    // 编辑用户
    const editUser = (user) => {
      isAdding.value = false
      Object.assign(editForm, {
        userId: user.userId,
        phone: user.phone || '',
        nickname: user.nickname,
        password: '',
        avatarUrl: user.avatarUrl || '',
        gender: user.gender || 0,
        realName: user.realName || '',
        idCard: user.idCard || '',
        signature: user.signature || '',
        community: user.community || '',
        userRole: user.userRole || 1,
        status: user.status || 1
      })
      
      // 解析并设置省市区
      if (user.province) {
        selectedProvince.value = user.province
        selectedCity.value = user.city || ''
        selectedDistrict.value = user.district || ''
      }
      
      showEditModal.value = true
    }
    
    // 添加用户
    const addUser = async () => {
      isAdding.value = true
      Object.assign(editForm, {
        userId: '',
        phone: '',
        nickname: '',
        password: '',
        avatarUrl: '',
        gender: 0,
        realName: '',
        idCard: '',
        signature: '',
        community: '',
        userRole: 1,
        status: 1
      })
      // 重置省市区选择
      selectedProvince.value = ''
      selectedCity.value = ''
      selectedDistrict.value = ''
      cities.value = []
      districts.value = []
      
      // 预加载管理员区域数据（因为默认是普通用户）
      if (!allAdminRegionsLoaded.value) {
        await loadAdminRegions()
      }
      
      showEditModal.value = true
    }
    
    // 关闭编辑模态框
    const closeEditModal = () => {
      showEditModal.value = false
      showAddModal.value = false
    }
    
    // 加载管理员区域数据
    const loadAdminRegions = async () => {
      try {
        console.log('开始加载管理员区域数据...')
        const res = await getAdminRegions()
        console.log('管理员区域数据响应:', res)
        
        if (res.data && res.data.code === 200) {
          const regionData = res.data.data
          console.log('解析后的区域数据:', regionData)
          
          // 直接使用后端返回的扁平化数据
          adminProvinces.value = (regionData.provinces || []).map(province => ({
            name: province.name,
            code: province.code,
            pinyin: []
          }))
          
          adminCities.value = (regionData.cities || []).map(city => ({
            name: city.name,
            code: city.code,
            province: city.province,
            pinyin: []
          }))
          
          adminDistricts.value = (regionData.districts || []).map(district => ({
            name: district.name,
            code: district.code,
            city: district.city
          }))
          
          adminCommunities.value = regionData.communities || []
          allAdminRegionsLoaded.value = true
          
          console.log('加载完成 - 省份:', adminProvinces.value.length, 
                     '城市:', adminCities.value.length,
                     '区县:', adminDistricts.value.length,
                     '社区:', adminCommunities.value.length)
          return true
        } else {
          console.error('获取管理员区域数据失败:', res.data)
          return false
        }
      } catch (error) {
        console.error('加载管理员区域数据失败:', error)
        return false
      }
    }
    
    // 加载所有社区列表（用于筛选）
    const loadCommunities = async () => {
      try {
        const res = await getAllCommunities()
        if (res.data && res.data.code === 200) {
          communities.value = res.data.data || []}
      } catch (error) {
        console.error('加载社区列表失败:', error)
      }
    }
    const loadAllRegionsForSAdmin = async () => {
      // 检查是否已经加载过全国省份数据
      if (nationalRegionsLoaded.value) {
        return
      }
          
      try {
        loading.value = true
        console.log('开始加载全国省份数据...')
        // 调用后端接口获取省份列表
        const res = await getProvinces()
        console.log('全国省份数据响应:', res)
        
        if (res.data && res.data.code === 200) {
          allProvinces.value = res.data.data || []
          console.log('解析后的省份数据:', allProvinces.value)
          nationalRegionsLoaded.value = true
          // 重置当前选择，让用户重新选择
          selectedProvince.value = ''
          selectedCity.value = ''
          selectedDistrict.value = ''
          cities.value = []
          districts.value = []
          console.log('加载完成 - 省份数量:', allProvinces.value.length)
        } else {
          console.error('加载省份数据失败:', res.data)
          alert('加载失败，请稍后重试')
        }
      } catch (error) {
        console.error('加载失败:', error)
        alert('加载失败：' + (error.message || '网络错误'))
      } finally {
        loading.value = false
      }
    }
    
    // 获取当前可选的省份列表
    const getAvailableProvinces = () => {
      // 确保 editForm.userRole 有值
      const userRole = editForm?.userRole || 1
      
      if (userRole === 2 || userRole === 3) {
        // 社区管理员和超级管理员：如果已加载全国数据则显示所有省份，否则显示管理员区域省份
        return nationalRegionsLoaded.value ? (allProvinces.value || []) : (adminProvinces.value || [])
      } else {
        // 普通用户：只显示管理员所在的省份
        return adminProvinces.value || []
      }
    }
    
    // 省份变化处理
    const onProvinceChange = async () => {
      if (selectedProvince.value) {
        // 清空城市和区县选择
        selectedCity.value = ''
        selectedDistrict.value = ''
        cities.value = []
        districts.value = []
        
        // 判断是否使用懒加载（仅社区管理员且已加载全国数据时使用）
        const shouldUseLazyLoad = (editForm.userRole === 2 || editForm.userRole === 3) && nationalRegionsLoaded.value
        
        if (shouldUseLazyLoad) {
          // 懒加载该省份的城市数据（从 Redis 缓存）
          try {
            const res = await getCitiesByProvinceCode(selectedProvince.value)
            if (res.data && res.data.code === 200) {
              cities.value = res.data.data || []
            }
          } catch (error) {
            console.error('加载城市失败:', error)
            cities.value = []
          }
        } else {
          // 普通用户或社区管理员未加载全国数据时，从 adminCities 中过滤
          cities.value = (adminCities.value || []).filter(city => city.province === selectedProvince.value)
        }
      } else {
        selectedCity.value = ''
        selectedDistrict.value = ''
        cities.value = []
        districts.value = []
      }
    }
    
    // 城市变化处理
    const onCityChange = async () => {
      if (selectedCity.value) {
        // 清空区县选择
        selectedDistrict.value = ''
        districts.value = []
        
        // 判断是否使用懒加载（仅社区管理员且已加载全国数据时使用）
        const shouldUseLazyLoad = (editForm.userRole === 2 || editForm.userRole === 3) && nationalRegionsLoaded.value
        
        if (shouldUseLazyLoad) {
          // 懒加载该城市的区县数据（从 Redis 缓存）
          try {
            const res = await getDistrictsByCityCode(selectedCity.value)
            if (res.data && res.data.code === 200) {
              districts.value = res.data.data || []
            }
          } catch (error) {
            console.error('加载区县失败:', error)
            districts.value = []
          }
        } else {
          // 普通用户或社区管理员未加载全国数据时，从 adminDistricts 中过滤
          districts.value = (adminDistricts.value || []).filter(district => district.city === selectedCity.value)
        }
      } else {
        selectedDistrict.value = ''
        districts.value = []
      }
    }
    
    // 用户角色变化处理
    const onUserRoleChange = () => {
      console.log('[用户角色变化] editForm.userRole:', editForm.userRole, '类型:', typeof editForm.userRole)
      
      // 重置省市区选择
      selectedProvince.value = ''
      selectedCity.value = ''
      selectedDistrict.value = ''
      cities.value = []
      districts.value = []
      // 重置社区选择
      editForm.community = ''
      
      // 根据角色加载对应的区域数据
      if (editForm.userRole === 2 || editForm.userRole === 3) {
        console.log('[用户角色变化] 角色为社区管理员或超级管理员')
        // 社区管理员或超级管理员：先加载管理员区域数据（如果未加载）
        if (!allAdminRegionsLoaded.value) {
          loadAdminRegions()
        }
        // 默认使用管理员区域数据，点击“其他城市”后才加载全国数据
        allProvinces.value = [...adminProvinces.value]
        allCities.value = [...adminCities.value]
        allDistricts.value = [...adminDistricts.value]
        // 重置全国数据加载标记
        nationalRegionsLoaded.value = false
        console.log('[用户角色变化] nationalRegionsLoaded:', nationalRegionsLoaded.value)
      } else if (editForm.userRole === 1) {
        console.log('[用户角色变化] 角色为普通用户')
        // 普通用户：只使用管理员区域数据，不加载全国数据
        if (!allAdminRegionsLoaded.value) {
          loadAdminRegions()
        }
        // 使用管理员区域数据
        allProvinces.value = [...adminProvinces.value]
        allCities.value = [...adminCities.value]
        allDistricts.value = [...adminDistricts.value]
        // 重置全国数据加载标记
        nationalRegionsLoaded.value = false
        console.log('[用户角色变化] nationalRegionsLoaded:', nationalRegionsLoaded.value)
      }
    }
    
    // 监听用户角色变化（用于按钮显示）
    watch(() => editForm.userRole, (newRole) => {// Vue 3 会自动响应这个变化并更新 v-if 条件
    })
    
    // 表单验证
    const validateForm = () => {
      if (isAdding.value) {
        // 添加用户时的验证
        if (!editForm.phone.trim()) {
          alert('手机号不能为空')
          return false
        }
        // 验证手机号格式
        if (!/^1[3-9]\d{9}$/.test(editForm.phone)) {
          alert('手机号格式不正确')
          return false
        }
      }
          
      if (!editForm.nickname.trim()) {
        alert('昵称不能为空')
        return false
      }
      
      if (editForm.nickname.length > 50) {
        alert('昵称长度不能超过50个字符')
        return false
      }
      
      // 验证手机号格式
      if (editForm.phone && !/^1[3-9]\d{9}$/.test(editForm.phone)) {
        alert('手机号格式不正确')
        return false
      }
      
      // 验证身份证号格式
      if (editForm.idCard && !/^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/.test(editForm.idCard)) {
        alert('身份证号格式不正确')
        return false
      }
      
      return true
    }
    
    // 保存用户
    const saveUser = async () => {
      // 表单验证
      if (!validateForm()) {
        return
      }
      
      // 普通用户需要验证社区是否存在于所选省市区
      if (isAdding.value && editForm.userRole === 1) {
        if (!editForm.community || !editForm.community.trim()) {
          alert('请输入所在社区')
          return
        }
        
        // 检查该社区是否在所选省市区内存在社区管理员
        const communityExists = adminCommunities.value.some(community => {
          // 这里需要根据实际情况判断，目前 adminCommunities 只包含社区名称
          // 如果需要更精确的匹配，需要后端提供省市区+社区的完整数据
          return community === editForm.community.trim()
        })
        
        if (!communityExists) {
          alert(`社区 "${editForm.community}" 在所选区域内不存在，请检查或联系管理员`)
          return
        }
      }
      
      loading.value = true
      try {
        // 构建省市区数据
        const regionData = {}
        if (selectedProvince.value) {
          const province = allProvinces.value.find(p => p.code === selectedProvince.value)
          if (province) {
            regionData.provinceCode = province.code
            regionData.provinceName = province.name
          }
        }
        if (selectedCity.value) {
          const city = allCities.value.find(c => c.code === selectedCity.value)
          if (city) {
            regionData.cityCode = city.code
            regionData.cityName = city.name
          }
        }
        if (selectedDistrict.value) {
          regionData.districtCode = selectedDistrict.value
          regionData.districtName = selectedDistrict.value
        }
        
        if (isAdding.value) {
          // 创建用户
          await createUser({
            phone: editForm.phone.trim(),
            nickname: editForm.nickname.trim(),
            password: editForm.password,
            avatarUrl: editForm.avatarUrl.trim(),
            gender: editForm.gender,
            realName: editForm.realName.trim(),
            idCard: editForm.idCard.trim(),
            signature: editForm.signature.trim(),
            community: editForm.community.trim(),
            userRole: editForm.userRole,
            status: editForm.status,
            ...regionData
          })
          alert('用户创建成功')
        } else {
          // 更新用户
          await updateUser(editForm.userId, {
            nickname: editForm.nickname.trim(),
            avatarUrl: editForm.avatarUrl.trim(),
            gender: editForm.gender,
            realName: editForm.realName.trim(),
            idCard: editForm.idCard.trim(),
            signature: editForm.signature.trim(),
            community: editForm.community.trim(),
            userRole: editForm.userRole,
            status: editForm.status,
            ...regionData
          })
          alert('用户信息更新成功')
        }
        closeEditModal()
        fetchUsers(currentPage.value)
      } catch (err) {
        console.error('保存用户失败:', err)
        alert('保存失败：' + (err.response?.data?.message || err.message))
      } finally {
        loading.value = false
      }
    }
    
    // 删除用户
    const deleteUserConfirm = async (user) => {
      if (confirm(`确定要删除用户 "${user.phone || user.nickname}" 吗？此操作不可恢复！`)) {
        try {
          await deleteUser(user.userId)
          alert('用户删除成功')
          fetchUsers(currentPage.value)
        } catch (err) {
          console.error('删除用户失败:', err)
          alert('删除失败：' + (err.response?.data?.message || err.message))
        }
      }
    }
    
    // 切换用户状态
    const toggleUserStatus = async (user) => {
      const newStatus = user.status === 1 ? 0 : 1
      const action = newStatus === 1 ? '启用' : '禁用'
      
      if (confirm(`确定要${action}用户 "${user.phone || user.nickname}" 吗？`)) {
        try {
          await updateUserStatus(user.userId, newStatus)
          alert(`用户${action}成功`)
          fetchUsers(currentPage.value)
        } catch (err) {
          console.error(`${action}用户失败:`, err)
          alert(`${action}失败：` + (err.response?.data?.message || err.message))
        }
      }
    }
    
    // 重置密码
    const resetPassword = async (user) => {
      const newPassword = prompt('请输入新密码:')
      if (newPassword) {
        if (newPassword.length < 6) {
          alert('密码长度不能少于 6 位')
          return
        }
                
        if (confirm(`确定要重置用户 "${user.phone || user.nickname}" 的密码吗？`)) {
          try {
            await resetUserPassword(user.userId, newPassword)
            alert('密码重置成功')
          } catch (err) {
            console.error('重置密码失败:', err)
            alert('重置密码失败：' + (err.response?.data?.message || err.message))
          }
        }
      }
    }
        
    // ========== 管理员认证审核相关方法 ==========
        
    // 加载待审核管理员列表（不分页，一次性加载所有）
    const loadPendingAdminAuths = async () => {
      authReviewLoading.value = true
      try {
        // 使用大尺寸一次性加载所有数据
        const params = {
          page: 1,
          size: 10000,  // 足够大的数字，确保获取所有数据
          keyword: authReviewKeyword.value || undefined
        }
            
        const res = await getPendingAdminAuth(params)
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          
          // 处理 identityImages 字段：将竖线分隔字符串转换为数组
          pendingAdminAuths.value = records.map(user => {
            if (user.identityImages) {
              try {
                // 如果 identityImages 是字符串，按竖线拆分
                if (typeof user.identityImages === 'string') {
                  user.identityImages = user.identityImages.split('|').filter(url => url.trim() !== '')
                }
                // 确保是数组
                if (!Array.isArray(user.identityImages)) {
                  user.identityImages = []
                }
              } catch (e) {
                console.warn('解析 identityImages 失败:', e)
                user.identityImages = []
              }
            } else {
              user.identityImages = []
            }
            return user
          })
          
          authReviewTotal.value = res.data.data.total || 0
        }
      } catch (err) {
        console.error('加载待审核管理员列表失败:', err)
        alert('加载失败：' + (err.response?.data?.message || err.message))
      } finally {
        authReviewLoading.value = false
      }
    }
        
    // 重置认证审核搜索
    const resetAuthSearch = () => {
      authReviewKeyword.value = ''
      loadPendingAdminAuths()
    }
        
    // 通过实名认证
    const approveAuth = async (user) => {
      if (confirm(`确定要通过用户 "${user.nickname || user.realName}" 的管理员实名认证吗？`)) {
        try {
          await approveAdminAuth({ userId: String(user.userId) })
          alert('已通过实名认证')
          // 重新加载列表
          loadPendingAdminAuths()
        } catch (err) {
          console.error('通过认证失败:', err)
          alert('通过失败：' + (err.response?.data?.message || err.message))
        }
      }
    }
        
    // 显示驳回模态框
    const showRejectModal = (user) => {
      currentRejectUser.value = user
      rejectReason.value = ''
      showRejectReasonModal.value = true
    }
        
    // 关闭驳回模态框
    const closeRejectModal = () => {
      showRejectReasonModal.value = false
      currentRejectUser.value = null
      rejectReason.value = ''
    }
        
    // 提交驳回
    const submitReject = async () => {
      if (!rejectReason.value.trim()) {
        alert('请填写驳回原因')
        return
      }
          
      try {
        await rejectAdminAuth({ 
          userId: String(currentRejectUser.value.userId),
          reason: rejectReason.value.trim()
        })
        alert('已驳回实名认证')
        closeRejectModal()
        // 重新加载列表
        loadPendingAdminAuths()
      } catch (err) {
        console.error('驳回认证失败:', err)
        alert('驳回失败：' + (err.response?.data?.message || err.message))
      }
    }
        
    // 关闭认证审核模态框
    const closeAuthReviewModal = () => {
      showAuthReviewModal.value = false
      // 关闭时刷新用户列表
      fetchUsers(currentPage.value)
    }
    
    // 改变页面
    const changePage = (newPage) => {
      if (newPage >= 1 && newPage <= totalPages.value) {
        currentPage.value = newPage
        fetchUsers(newPage)
      }
    }
    
    // 改变每页显示数量
    const onPageSizeChange = () => {
      currentPage.value = 1
      fetchUsers(1)
    }
    
    // 工具函数
    const getGenderText = (gender) => {
      const map = { 0: '未知', 1: '男', 2: '女' }
      return map[gender] || '未知'
    }
    
    const getUserRoleText = (role) => {
      const map = { 1: '普通用户', 2: '管理员', 3: '超级管理员' }
      return map[role] || '未知'
    }
    
    const getAuthStatusText = (status) => {
      const map = { 0: '未认证', 1: '认证中', 2: '已认证', 3: '认证失败' }
      return map[status] || '未知'
    }
    
    const getStatusText = (status) => {
      // 确保status是数字类型
      const statusNum = Number(status)
      const map = { 0: '禁用', 1: '正常', 2: '待审核' }
      return map[statusNum] || '未知'
    }
    
    const getStatusClass = (status) => {
      // 确保status是数字类型
      const statusNum = Number(status)
      const classMap = { 
        0: 'status-inactive', 
        1: 'status-active', 
        2: 'status-pending' 
      }
      return classMap[statusNum] || 'status-unknown'
    }
    
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN')
    }
    
    const handleImageError = (event) => {
      // 对于身份证明图片，显示一个占位符而不是默认头像
      event.target.style.display = 'none'
      
      // 创建一个占位符元素
      const placeholder = document.createElement('div')
      placeholder.className = 'image-load-error'
      placeholder.textContent = '图片加载失败'
      placeholder.style.cssText = `
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f5f5f5;
        color: #999;
        font-size: 12px;
        text-align: center;
        padding: 10px;
      `
      
      if (event.target.parentNode) {
        event.target.parentNode.appendChild(placeholder)
      }
    }
    
    // ========== 头像上传相关方法 ==========
    
    // 触发文件选择
    const triggerAvatarUpload = () => {
      if (avatarFileInput.value) {
        avatarFileInput.value.click()
      }
    }
    
    // 处理头像文件选择
    const handleAvatarChange = async (event) => {
      const file = event.target.files[0]
      if (!file) return
      
      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        alert('只支持图片文件上传')
        return
      }
      
      // 验证文件大小（5MB）
      const maxSize = 5 * 1024 * 1024
      if (file.size > maxSize) {
        alert('文件大小不能超过 5MB')
        return
      }
      
      // 上传文件
      uploadingAvatar.value = true
      try {
        const formData = new FormData()
        formData.append('file', file)
        
        // 获取当前环境的 API 基础路径
        const isProduction = import.meta.env.PROD
        const baseURL = isProduction ? '/admin/api' : 'http://127.0.0.1:8080'
        
        // 调用后端上传接口
        const response = await fetch(`${baseURL}/image/upload?type=avatar`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: formData
        })
        
        const result = await response.json()
        
        if (result.code === 200 && result.data) {
          editForm.avatarUrl = result.data
          alert('头像上传成功')
        } else {
          alert('上传失败：' + (result.message || '未知错误'))
        }
      } catch (error) {
        console.error('头像上传失败:', error)
        alert('上传失败：' + error.message)
      } finally {
        uploadingAvatar.value = false
        // 清空文件输入，允许重复选择同一文件
        event.target.value = ''
      }
    }
    
    // 监听showAddModal的变化
    watch(showAddModal, (newValue) => {
      if (newValue) {
        addUser()
        showAddModal.value = false
      }
    })
    
    // 监听认证审核模态框的打开，自动加载数据
    watch(showAuthReviewModal, (newValue) => {
      if (newValue) {
        loadPendingAdminAuths()
      }
    })
    
    // 组件挂载时获取数据
    onMounted(() => {
      fetchUsers()
      // 加载管理员区域数据
      loadAdminRegions()
      // 加载所有社区列表
      loadCommunities()
    })
    
    return {
      // 数据
      users,
      currentPage,
      pageSize,
      total,
      totalPages,
      loading,
      searchForm,
      editForm,
      currentUser,
      showViewModal,
      showEditModal,
      showAddModal,
      isAdding,
      uploadingAvatar,
      avatarFileInput,
      provinces: allProvinces,
      cities,  // 懒加载的城市列表
      districts,  // 懒加载的区县列表
      selectedProvince,
      selectedCity,
      selectedDistrict,
      nationalRegionsLoaded,  // 标记是否已加载全国省份数据
      adminProvinces,
      adminCities,
      adminDistricts,
      adminCommunities,
      communities,
      
      // 方法
      fetchUsers,
      searchUsers,
      resetSearch,
      viewUser,
      closeViewModal,
      editUser,
      addUser,
      closeEditModal,
      saveUser,
      deleteUser: deleteUserConfirm,
      toggleUserStatus,
      resetPassword,
      changePage,
      onPageSizeChange,
      getGenderText,
      getUserRoleText,
      getAuthStatusText,
      getStatusText,
      getStatusClass,
      formatDate,
      handleImageError,
      triggerAvatarUpload,
      handleAvatarChange,
      loadAdminRegions,
      getAvailableProvinces,
      onProvinceChange,
      onCityChange,
      onUserRoleChange,
      loadAllRegionsForSAdmin,
      // 管理员认证审核相关方法
      showAuthReviewModal,
      pendingAdminAuths,
      authReviewTotal,
      authReviewLoading,
      authReviewKeyword,
      loadPendingAdminAuths,
      resetAuthSearch,
      approveAuth,
      showRejectModal,
      closeRejectModal,
      submitReject,
      closeAuthReviewModal,
      showRejectReasonModal,
      rejectReason
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.search-section {
  margin-bottom: 20px;
  background: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
}

.search-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input, .search-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

/* 专门针对搜索筛选下拉框的宽度控制 */
.search-select {
  width: 120px; /* 设置下拉框固定宽度 */
  min-width: 100px;
  max-width: 150px;
}

/* 输入框宽度控制 */
.search-input {
  width: 150px;
  min-width: 120px;
  max-width: 200px;
}

.search-btn, .reset-btn, .add-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.search-btn {
  background: #007bff;
  color: white;
}

.reset-btn {
  background: #6c757d;
  color: white;
}

.add-btn {
  background: #28a745;
  color: white;
  margin-left: auto;
}

.auth-review-btn {
  background: #17a2b8;
  color: white;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.table-container {
  overflow-x: auto;
  margin-bottom: 20px;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.user-table th, .user-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.user-table th {
  background: #f8f9fa;
  font-weight: 600;
  position: sticky;
  top: 0;
}

.avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.no-avatar, .empty-text {
  color: #999;
  font-style: italic;
}

.openid-text, .signature-text {
  font-family: monospace;
  font-size: 12px;
  color: #666;
  word-break: break-all;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: #d4edda;
  color: #155724;
}

.status-inactive {
  background: #f8d7da;
  color: #721c24;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-unknown {
  background: #d1d1d1;
  color: #666;
}

.action-cell {
  white-space: nowrap;
}

.view-btn, .edit-btn, .status-toggle-btn, .reset-pwd-btn, .delete-btn {
  padding: 4px 8px;
  margin: 0 2px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.view-btn {
  background: #17a2b8;
  color: white;
}

.edit-btn {
  background: #ffc107;
  color: #212529;
}

.edit-btn:hover {
  background: #e0a800;
  color: #212529;
}

.disable-btn {
  background: #dc3545;
  color: white;
}

.enable-btn {
  background: #28a745;
  color: white;
}

.reset-pwd-btn {
  background: #6f42c1;
  color: white;
}

.delete-btn {
  background: #dc3545;
  color: white;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.page-size-select {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.page-size-select:hover {
  border-color: #4CAF50;
}

.page-size-select:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  color: #333;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
  font-size: 14px;
}

.page-btn:hover:not(:disabled) {
  background-color: #f8f9fa;
  border-color: #c6c6c6;
}

.page-btn:active:not(:disabled) {
  background-color: #e9ecef;
  border-color: #adb5bd;
  transform: scale(0.98);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background-color: #f8f9fa;
}

.page-info {
  font-size: 14px;
  color: #666;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  width: 90%;
}

.modal-content h3 {
  margin-top: 0;
  margin-bottom: 20px;
  text-align: center;
}

.user-detail {
  margin-bottom: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.detail-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.detail-section h4 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #495057;
  border-bottom: 2px solid #007bff;
  padding-bottom: 5px;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
  align-items: flex-start;
}

.detail-row label {
  width: 100px;
  font-weight: 600;
  color: #495057;
  flex-shrink: 0;
}

.detail-row span {
  flex: 1;
  word-break: break-word;
}

.no-avatar-large {
  color: #999;
  font-style: italic;
  display: inline-block;
  padding: 20px;
  background: #f1f1f1;
  border-radius: 50%;
  width: 80px;
  height: 80px;
  text-align: center;
  line-height: 40px;
}

.user-form {
  max-height: 60vh;
  overflow-y: auto;
}

.form-row {
  margin-bottom: 15px;
}

.form-row-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 15px;
}

.form-row-group .form-row {
  margin-bottom: 0;
}

.other-city-btn {
  width: 100%;
  padding: 8px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
}

.other-city-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

.other-city-btn:active {
  transform: translateY(0);
}

.form-row label {
  display: block;
  margin-bottom: 5px;
  font-weight: 600;
  color: #333;
}

.form-row input, .form-row select, .form-row textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  color: #333;  /* 明确设置文字颜色 */
  background-color: white;  /* 明确设置背景色为白色 */
}

.form-row textarea {
  min-height: 80px;
  resize: vertical;
}

/* 头像上传容器样式 */
.avatar-upload-container {
  display: flex;
  align-items: center;
  gap: 20px;
}

.current-avatar-wrapper {
  flex-shrink: 0;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ddd;
}

.avatar-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #f5f5f5;
  border: 2px dashed #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}

.upload-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-btn {
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  align-self: flex-start;
}

.upload-btn:hover {
  background: #0056b3;
}

.upload-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.upload-status {
  font-size: 12px;
  color: #007bff;
}

.form-hint {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.form-row input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.modal-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}

.save-btn, .cancel-btn, .close-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.save-btn {
  background: #28a745;
  color: white;
}

.cancel-btn, .close-btn {
  background: #6c757d;
  color: white;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #666;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

@media (max-width: 768px) {
  .search-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input, .search-select {
    width: 100%;
  }
  
  .add-btn {
    margin-left: 0;
    width: 100%;
  }
  
  .user-table {
    font-size: 12px;
  }
  
  .user-table th, .user-table td {
    padding: 8px 4px;
  }
  
  .action-cell button {
    display: block;
    width: 100%;
    margin: 2px 0;
  }
}

/* 中等屏幕适配 */
@media (min-width: 769px) and (max-width: 1024px) {
  .search-select {
    width: 100px;
    min-width: 90px;
    max-width: 120px;
  }
  
  .search-input {
    width: 130px;
    min-width: 110px;
    max-width: 180px;
  }
}

/* 大屏幕优化 */
@media (min-width: 1025px) {
  .search-select {
    width: 120px;
  }
  
  .search-input {
    width: 150px;
  }
}

/* 管理员认证审核模态框样式 */
.modal-content-large {
  max-width: 900px;
  width: 95%;
  max-height: 90vh;
  overflow-y: auto;
}

.auth-search-section {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.auth-search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.auth-review-list {
  margin-bottom: 20px;
}

.auth-review-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.auth-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.auth-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.auth-avatar-placeholder {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #007bff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}

.auth-user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-user-name {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.auth-user-phone {
  font-size: 14px;
  color: #666;
}

.auth-status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.auth-item-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
  margin-bottom: 15px;
}

.auth-detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-detail-row label {
  font-weight: 600;
  font-size: 12px;
  color: #666;
}

.auth-detail-row span {
  font-size: 14px;
  color: #333;
}

.auth-images-container {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.auth-image-item {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #ddd;
}

.auth-image-link {
  display: block;
  width: 100%;
  height: 100%;
}

.auth-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.auth-item-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.approve-btn {
  background: #28a745;
  color: white;
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.reject-btn {
  background: #dc3545;
  color: white;
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.reject-reason-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  margin-bottom: 15px;
}

.confirm-btn {
  background: #dc3545;
  color: white;
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}
</style>
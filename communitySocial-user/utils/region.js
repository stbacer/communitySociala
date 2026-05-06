// utils/region.js
// 省市区数据工具类 - 使用腾讯地图 API

// 腾讯地图 API Key
const TENCENT_MAP_KEY = '2OBBZ-PJALT-SR2X7-LYJRP-ICYGV-22FPI'

/**
 * 获取所有省份（使用腾讯地图 API）
 */
function getProvinces(callback) {
  // 先尝试从本地存储获取
  const cachedProvinces = wx.getStorageSync('tencent_provinces')
  if (cachedProvinces) {
   if (callback) callback(null, JSON.parse(cachedProvinces))
 return JSON.parse(cachedProvinces)
  }
  
  // 调用腾讯地图 API 获取省份数据
  wx.request({
   url: 'https://apis.map.qq.com/ws/district/v1/list',
  data: {
     key: TENCENT_MAP_KEY,
     output: 'json'
   },
   success(res) {
     if (res.data && res.data.status === 0) {
     const provincesData = res.data.result[0].map(item => ({
         name: item.name,
       code: item.id,
         pinyin: item.pinyin
       }))
       
       // 缓存到本地存储
       wx.setStorageSync('tencent_provinces', JSON.stringify(provincesData))
       
       if (callback) callback(null, provincesData)
     } else {
       if (callback) callback(new Error('获取省份数据失败'))
     }
   },
   fail(err) {
     if (callback) callback(err)
   }
  })
  
 return null
}

/**
 * 根据省份代码获取城市列表（使用腾讯地图 API）
 * @param {string} provinceCode - 省份代码
 * @param {function} callback - 回调函数
 */
function getCitiesByProvince(provinceCode, callback) {
  // 先尝试从本地存储获取
  const cacheKey = `tencent_cities_${provinceCode}`
  const cachedCities = wx.getStorageSync(cacheKey)
  if (cachedCities) {
   if (callback) callback(null, JSON.parse(cachedCities))
 return JSON.parse(cachedCities)
  }
  
  // 调用腾讯地图 API 获取城市数据
 wx.request({
  url: 'https://apis.map.qq.com/ws/district/v1/getchildren',
 data: {
   key: TENCENT_MAP_KEY,
   id: provinceCode,  // 使用 id 参数指定省份代码
   output: 'json'
  },
  success(res) {
   if (res.data && res.data.status === 0) {

     // getchildren 接口返回的 result 是二维数组，城市数据在 result[0][0]
     if (res.data.result && res.data.result[0] && Array.isArray(res.data.result[0])) {
    const citiesData = res.data.result[0].map(city => ({
         name: city.name,
      code: city.id,
         pinyin: city.pinyin,
       fullname: city.fullname || city.name  // 保留 fullname 字段，用于直辖市判断
       }))

       // 缓存到本地存储
       wx.setStorageSync(cacheKey, JSON.stringify(citiesData))
       
       if (callback) callback(null, citiesData)
     } else {
       if (callback) callback(null, [])
     }
   } else {
     if (callback) callback(new Error('获取城市数据失败'))
   }
  },
  fail(err) {
   if (callback) callback(err)
  }
 })
  
 return null
}

/**
 * 根据城市代码获取区县列表（使用腾讯地图 API）
 * @param {string} cityCode - 城市代码
 * @param {function} callback - 回调函数
 */
function getDistrictsByCity(cityCode, callback) {
  // 先尝试从本地存储获取
  const cacheKey = `tencent_districts_${cityCode}`
  const cachedDistricts = wx.getStorageSync(cacheKey)
  if (cachedDistricts) {

  const parsed = JSON.parse(cachedDistricts)

  
  if (callback) callback(null, parsed)
 return parsed
 }
  
  // 调用腾讯地图 API 获取区县数据
 wx.request({
  url: 'https://apis.map.qq.com/ws/district/v1/getchildren',
 data: {
   key: TENCENT_MAP_KEY,
   id: cityCode,  // 使用 id 参数指定城市代码
   output: 'json'
  },
  success(res) {
   if (res.data && res.data.status === 0) {
    
    // getchildren 接口返回的 result 是二维数组，区县数据在 result[0][0]
    if (res.data.result && res.data.result[0] && Array.isArray(res.data.result[0])) {
   const districtsData = res.data.result[0].map(district => {

    const processed = {
       name: district.name || district.fullname || '未知',
    code: district.id,
       pinyin: district.pinyin || []
     }

     return processed
   })
     
     // 缓存到本地存储
     wx.setStorageSync(cacheKey, JSON.stringify(districtsData))
     
     if (callback) callback(null, districtsData)
    } else {
     if (callback) callback(null, [])
    }
   } else {
    if (callback) callback(new Error('获取区县数据失败'))
   }
  },
  fail(err) {
   if (callback) callback(err)
  }
 })
  
 return null
}

module.exports = {
  getProvinces,
  getCitiesByProvince,
  getDistrictsByCity,
  TENCENT_MAP_KEY
}

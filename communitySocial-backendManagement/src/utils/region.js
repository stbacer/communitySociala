// utils/region.js
// 省市区数据工具类 - 使用腾讯地图 API

import axios from 'axios'

// 腾讯地图 API Key
const TENCENT_MAP_KEY = '2OBBZ-PJALT-SR2X7-LYJRP-ICYGV-22FPI'
const BASE_URL = 'https://apis.map.qq.com/ws/district/v1'

/**
 * 获取所有省份（使用腾讯地图 API）
 */
export async function getProvinces() {
  // 先尝试从本地存储获取
  const cachedProvinces = localStorage.getItem('tencent_provinces')
  if (cachedProvinces) {
    return JSON.parse(cachedProvinces)
  }
  
  try {
    // 调用腾讯地图 API 获取省份数据
    const response = await axios.get(`${BASE_URL}/list`, {
      params: {
        key: TENCENT_MAP_KEY,
        output: 'json'
      }
    })
    
    if (response.data && response.data.status === 0) {
      const provincesData = response.data.result[0].map(item => ({
        name: item.name,
        code: item.id,
        pinyin: item.pinyin
      }))
      
      // 缓存到本地存储
      localStorage.setItem('tencent_provinces', JSON.stringify(provincesData))
      
      return provincesData
    } else {
      throw new Error('获取省份数据失败')
    }
  } catch (error) {
    throw error
  }
}

/**
 * 根据省份代码获取城市列表（使用腾讯地图 API）
 * @param {string} provinceCode - 省份代码
 */
export async function getCitiesByProvince(provinceCode) {
  // 先尝试从本地存储获取
  const cacheKey = `tencent_cities_${provinceCode}`
  const cachedCities = localStorage.getItem(cacheKey)
  if (cachedCities) {
    return JSON.parse(cachedCities)
  }
  
  try {
    // 调用腾讯地图 API 获取城市数据
    const response = await axios.get(`${BASE_URL}/getchildren`, {
      params: {
        key: TENCENT_MAP_KEY,
        id: provinceCode,
        output: 'json'
      }
    })
    
    if (response.data && response.data.status === 0) {

      // getchildren 接口返回的 result 是二维数组，城市数据在 result[0][0]
      if (response.data.result && response.data.result[0] && Array.isArray(response.data.result[0])) {
        const citiesData = response.data.result[0].map(city => ({
          name: city.name,
          code: city.id,
          pinyin: city.pinyin,
          fullname: city.fullname || city.name
        }))

        // 缓存到本地存储
        localStorage.setItem(cacheKey, JSON.stringify(citiesData))
        
        return citiesData
      } else {
        return []
      }
    } else {
      throw new Error('获取城市数据失败')
    }
  } catch (error) {
    throw error
  }
}

/**
 * 根据城市代码获取区县列表（使用腾讯地图 API）
 * @param {string} cityCode - 城市代码
 */
export async function getDistrictsByCity(cityCode) {
  // 先尝试从本地存储获取
  const cacheKey = `tencent_districts_${cityCode}`
  const cachedDistricts = localStorage.getItem(cacheKey)
  if (cachedDistricts) {
    return JSON.parse(cachedDistricts)
  }
  
  try {
    // 调用腾讯地图 API 获取区县数据
    const response = await axios.get(`${BASE_URL}/getchildren`, {
      params: {
        key: TENCENT_MAP_KEY,
        id: cityCode,
        output: 'json'
      }
    })
    
    if (response.data && response.data.status === 0) {
      // getchildren 接口返回的 result 是二维数组，区县数据在 result[0][0]
      if (response.data.result && response.data.result[0] && Array.isArray(response.data.result[0])) {
        const districtsData = response.data.result[0].map(district => {
          const processed = {
            name: district.name || district.fullname || '未知',
            code: district.id,
            pinyin: district.pinyin || []
          }
          return processed
        })
        
        // 缓存到本地存储
        localStorage.setItem(cacheKey, JSON.stringify(districtsData))
        
        return districtsData
      } else {
        return []
      }
    } else {
      throw new Error('获取区县数据失败')
    }
  } catch (error) {
    throw error
  }
}

export default {
  getProvinces,
  getCitiesByProvince,
  getDistrictsByCity,
  TENCENT_MAP_KEY
}

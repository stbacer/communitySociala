// 腾讯地图位置服务工具类
// 文档：https://lbs.qq.com/dev/console/minicode

const TENCENT_MAP_KEY = '2OBBZ-PJALT-SR2X7-LYJRP-ICYGV-22FPI'
const GEOCODER_URL = 'https://apis.map.qq.com/ws/geocoder/v1/'

/**
 * 根据经纬度获取地址信息（逆地址解析）
 * @param {Number} longitude - 经度
 * @param {Number} latitude - 纬度
 * @returns {Promise} 返回地址信息
 */
const getAddressByLocation = (longitude, latitude) => {
  return new Promise((resolve, reject) => {
    // 验证参数
    if (!longitude || !latitude) {
      reject(new Error('无效的经纬度参数'))
      return
    }

    const requestUrl = `${GEOCODER_URL}?location=${latitude},${longitude}&key=${TENCENT_MAP_KEY}&get_poi=1`

    wx.request({
      url: GEOCODER_URL,
      data: {
        location: `${latitude},${longitude}`,  // 注意：腾讯地图要求格式为 纬度,经度
        key: TENCENT_MAP_KEY,
        get_poi: 1  // 是否返回周边 POI
      },
      success: (res) => {
        if (res.data.status === 0) {
          const result = res.data.result
          
          resolve({
            address: result.address,  // 完整地址
            formattedAddress: result.formatted_addresses?.recommend || result.address,  // 推荐地址
            province: result.ad_info?.province || '',  // 省份
            city: result.ad_info?.city || '',  // 城市
            district: result.ad_info?.district || '',  // 区县
            street: result.ad_info?.street || '',  // 街道
            streetNumber: result.ad_info?.street_number || '',  // 门牌号
            poi: result.poi || null  // 周边 POI 信息
          })
        } else {
          // 如果是 WebserviceAPI 未开启，使用备用方案
          if (res.data.message && (res.data.message.includes('未开启') || res.data.message.includes('WebServiceAPI'))) {
            resolve({
              address: `经纬度：${longitude.toFixed(6)}, ${latitude.toFixed(6)}`,
              formattedAddress: `经纬度：${longitude.toFixed(6)}, ${latitude.toFixed(6)}`,
              province: '',
              city: '',
              district: '',
              street: '',
              streetNumber: '',
              poi: null,
              warning: '腾讯地图 WebServiceAPI 未开启，请在控制台启用该服务'
            })
          } else if (res.data.message && res.data.message.includes('key')) {
            reject(new Error(`腾讯地图 Key 错误: ${res.data.message}`))
          } else {
            reject(new Error(res.data.message || '地址解析失败'))
          }
        }
      },
      fail: (err) => {
        // 网络错误时降级处理
        resolve({
          address: `经纬度：${longitude.toFixed(6)}, ${latitude.toFixed(6)}`,
          formattedAddress: `经纬度：${longitude.toFixed(6)}, ${latitude.toFixed(6)}`,
          province: '',
          city: '',
          district: '',
          street: '',
          streetNumber: '',
          poi: null,
          warning: '网络请求失败，已降级显示坐标'
        })
      }
    })
  })
}

/**
 * 根据地址获取经纬度（地理编码）
 * @param {String} address - 地址
 * @returns {Promise} 返回经纬度信息
 */
const getLocationByAddress = (address) => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: 'https://apis.map.qq.com/ws/geocoder/v1/',
      data: {
        address: address,
        key: TENCENT_MAP_KEY
      },
      success: (res) => {
        if (res.data.status === 0) {
          const result = res.data.result
          resolve({
            longitude: result.location.lng,
            latitude: result.location.lat,
            address: address
          })
        } else {
          reject(new Error(res.data.message || '地理编码失败'))
        }
      },
      fail: (err) => {
        reject(new Error('网络请求失败'))
      }
    })
  })
}

/**
 * 计算两点之间的距离
 * @param {Number} lng1 - 第一点经度
 * @param {Number} lat1 - 第一点纬度
 * @param {Number} lng2 - 第二点经度
 * @param {Number} lat2 - 第二点纬度
 * @returns {Number} 距离（米）
 */
const calculateDistance = (lng1, lat1, lng2, lat2) => {
  const EARTH_RADIUS = 6378137.0
  const radLat1 = lat1 * Math.PI / 180.0
  const radLat2 = lat2 * Math.PI / 180.0
  const a = radLat1 - radLat2
  const b = (lng1 * Math.PI / 180.0) - (lng2 * Math.PI / 180.0)
  let s = 2 * Math.asin(Math.sqrt(
    Math.pow(Math.sin(a / 2), 2) + 
    Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)
  ))
  return Math.round(s * EARTH_RADIUS)
}

/**
 * 打开腾讯地图选择位置
 * @param {Object} options - 选项
 * @param {Function} success - 成功回调
 * @param {Function} fail - 失败回调
 */
const chooseLocation = (options = {}) => {
  return new Promise((resolve, reject) => {
    // 直接使用微信原生的 chooseLocation API
    wx.chooseLocation({
      success: (res) => {
        resolve({
          longitude: res.longitude,
          latitude: res.latitude,
          name: res.name,
          address: res.address
        })
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

/**
 * 根据关键词搜索行政区划
 * @param {String} keyword - 关键词（如省份名称）
 * @returns {Promise} 返回匹配的行政区划信息
 */
const getRegionByKeyword = (keyword) => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: 'https://apis.map.qq.com/ws/district/v1/search',
      data: {
        keyword: keyword,
        key: TENCENT_MAP_KEY
      },
      success: (res) => {
        if (res.data.status === 0 && res.data.result && res.data.result.length > 0) {
          resolve(res.data.result[0])
        } else {
          reject(new Error('未找到匹配的行政区划'))
        }
      },
      fail: (err) => {
        reject(new Error('网络请求失败'))
      }
    })
  })
}

/**
 * 获取指定行政区的下级行政区
 * @param {String} id - 行政区 ID（如省级行政区的 id）
 * @param {String} level - 要获取的下级行政区级别（'district'表示区县级）
 * @returns {Promise} 返回下级行政区列表
 */
const getSubRegions = (id, level = 'district') => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: 'https://apis.map.qq.com/ws/district/v1/getchildren',
      data: {
        id: id,
        level: level,
        key: TENCENT_MAP_KEY
      },
      success: (res) => {
        if (res.data.status === 0 && res.data.result) {
          resolve(res.data.result)
        } else {
          reject(new Error(res.data.message || '获取下级行政区失败'))
        }
      },
      fail: (err) => {
        reject(new Error('网络请求失败'))
      }
    })
  })
}

module.exports = {
  getAddressByLocation,
  getLocationByAddress,
  calculateDistance,
  chooseLocation,
  getRegionByKeyword,
  getSubRegions,
  TENCENT_MAP_KEY
}

const TENCENT_MAP_KEY = '2OBBZ-PJALT-SR2X7-LYJRP-ICYGV-22FPI'
const GEOCODER_URL = 'https://apis.map.qq.com/ws/geocoder/v1/'

/**
 * 根据经纬度获取地址信息（逆地址解析）
 */
const getAddressByLocation = (longitude, latitude) => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: GEOCODER_URL,
      data: {
        location: `${latitude},${longitude}`,
        key: TENCENT_MAP_KEY,
        get_poi: 0
      },
      success: (res) => {
        if (res.data.status === 0) {
          const result = res.data.result
          resolve({
            address: result.address,
            formattedAddress: result.formatted_addresses.recommend,
            province: result.ad_info.province,
            city: result.ad_info.city,
            district: result.ad_info.district,
            street: result.ad_info.street,
            streetNumber: result.ad_info.street_number
          })
        } else {
          // 如果是 WebserviceAPI 未开启，使用备用方案
          if (res.data.message && res.data.message.includes('未开启')) {
            resolve({
              address: `经纬度：${longitude.toFixed(6)}, ${latitude.toFixed(6)}`,
              formattedAddress: `经纬度：${longitude.toFixed(6)}, ${latitude.toFixed(6)}`,
              province: '',
              city: '',
              district: '',
              street: '',
              streetNumber: '',
              warning: '腾讯地图 WebserviceAPI 未开启，已降级显示坐标'
            })
          } else {
            reject(new Error(res.data.message || '地址解析失败'))
          }
        }
      },
      fail: (err) => {
        resolve({
          address: `经纬度：${longitude.toFixed(6)}, ${latitude.toFixed(6)}`,
          formattedAddress: `经纬度：${longitude.toFixed(6)}, ${latitude.toFixed(6)}`,
          province: '',
          city: '',
          district: '',
          street: '',
          streetNumber: '',
          warning: '网络请求失败，已降级显示坐标'
        })
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
 * 格式化距离显示
 * @param {Number} distance - 距离（米）
 * @returns {String} 格式化后的距离文本
 */
const formatDistance = (distance) => {
  if (distance < 1000) {
    return `${Math.round(distance)}m`
  } else {
    return `${(distance / 1000).toFixed(1)}km`
  }
}

module.exports = {
  getAddressByLocation,
  calculateDistance,
  formatDistance,
  TENCENT_MAP_KEY
}

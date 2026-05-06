const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return `${[year, month, day].map(formatNumber).join('/')} ${[hour, minute, second].map(formatNumber).join(':')}`
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : `0${n}`
}

// 防抖函数
const debounce = (func, delay = 300) => {
  let timeoutId
  return function(...args) {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => func.apply(this, args), delay)
  }
}

// 节流函数
const throttle = (func, delay = 500) => {
  let lastExecTime = 0
  return function(...args) {
    const now = Date.now()
    if (now - lastExecTime >= delay) {
      lastExecTime = now
      return func.apply(this, args)
    }
  }
}

// 格式化数字
const formatCount = (count) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + '千'
  }
  return count.toString()
}

// 格式化日期时间（用于帖子详情页）
const formatDate = (dateString) => {
  if (!dateString) return ''
  
  try {
    const date = new Date(dateString)
    const now = new Date()
    
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    
    // 当天只显示时间
    if (date.toDateString() === now.toDateString()) {
      return `${hours}:${minutes}`
    }
    
    // 昨天
    const yesterday = new Date(now)
    yesterday.setDate(yesterday.getDate() - 1)
    if (date.toDateString() === yesterday.toDateString()) {
      return `昨天 ${hours}:${minutes}`
    }
    
    // 今年内显示月/日 时:分
    if (year === now.getFullYear()) {
      return `${month}/${day} ${hours}:${minutes}`
    }
    
    // 其他年份显示 年/月/日 时:分
    return `${year}/${month}/${day} ${hours}:${minutes}`
    
  } catch (error) {
    return dateString
  }
}

module.exports = {
  formatTime,
  debounce,
  throttle,
  formatCount,
  formatDate
}

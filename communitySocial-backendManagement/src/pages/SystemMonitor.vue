<template>
  <div class="page-container">
    <h2>系统监控</h2>
    <button @click="refresh">刷新统计</button>
    <button @click="clearCache">清理缓存</button>
    <pre>{{ stats }}</pre>
  </div>
</template>

<script>
import { ref } from 'vue'
import { getSystemStatistics, clearCache } from '@/api/system'

export default {
  name: 'SystemMonitor',
  setup() {
    const stats = ref({})
    const refresh = async () => {
      const res = await getSystemStatistics()
      stats.value = res.data.data || {}
    }
    const clearCache = async () => {
      if (confirm('确认清理系统缓存？')) {
        await clearCache()
        alert('缓存已清理')
      }
    }
    refresh()
    return { stats, refresh, clearCache }
  }
}
</script>
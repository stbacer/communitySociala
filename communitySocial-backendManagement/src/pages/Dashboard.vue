<template>
  <!-- Dashboard page simply renders the shared admin layout; actual views appear in the layout's router-view -->
  <AdminLayout />
</template>

<script>
import AdminLayout from '@/components/AdminLayout.vue'

export default {
  name: 'Dashboard',
  components: {
    AdminLayout
  }
}
</script>

<style scoped>
/* no additional styles; layout moved to AdminLayout.vue */
</style>

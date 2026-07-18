<template>
  <view class="page"
    ><view class="title">已发布健康报告</view
    ><PageState :loading="loading" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card"
        ><view class="row"
          ><view class="section-title">{{ item.title }}</view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle">{{ item.summary }}</view
        ><view class="muted">医生意见：{{ item.doctorOpinion || '无' }}</view></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyHealthReports } from '@/api/health-report'
import type { HealthReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<HealthReport[]>([]),
  loading = ref(true)
onShow(async () => {
  try {
    items.value = await getMyHealthReports()
  } finally {
    loading.value = false
  }
})
</script>

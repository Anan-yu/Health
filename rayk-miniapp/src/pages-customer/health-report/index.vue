<template>
  <view class="page elder-page"
    ><view class="title">已发布健康报告</view
    ><PageState :loading="loading" :error="error" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card report-card" @click="detail(item.id)"
        ><view class="row"
          ><view class="section-title">{{ item.title }}</view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle">{{ item.summary }}</view
        ><view class="muted report-date">报告日期：{{ formatReportDate(item.publishedAt) }}</view
        ><view class="muted"
          ><template v-if="hasDoctorOpinion(item.doctorOpinion)">
            医生意见：{{ item.doctorOpinion }} ·
          </template>
          查看报告 ›</view
        ></view
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
  loading = ref(true),
  error = ref('')
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    items.value = await getMyHealthReports()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '健康报告加载失败'
  } finally {
    loading.value = false
  }
})
const detail = (id: string) =>
  uni.navigateTo({ url: `/pages-customer/health-report/detail?id=${id}` })
const hasDoctorOpinion = (opinion?: string) => {
  const value = opinion?.trim()
  return Boolean(value && value !== '无' && value.toLowerCase() !== 'none')
}
const formatReportDate = (value?: string) => (value ? value.replace('T', ' ').slice(0, 10) : '待确认')
</script>
<style scoped>
.report-card {
  margin-bottom: 20rpx;
}
.report-date {
  margin-top: 10rpx;
}
</style>

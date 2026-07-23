<template>
  <view class="page">
    <view class="eyebrow">HEALTH REPORTS</view>
    <view class="title">{{ patientName ? `${patientName}的健康评估报告` : '健康评估报告' }}</view>
    <view class="subtitle">查看并下载本院已授权体检者的已发布报告</view>

    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view
        v-for="item in items"
        :key="item.id"
        class="card report-card"
        @click="detail(item.id)"
      >
        <view class="report-head">
          <view class="section-title">{{ item.title }}</view>
          <StatusTag :status="item.status" />
        </view>
        <view class="subtitle report-summary">{{ item.summary }}</view>
        <view class="report-meta">
          <text>{{ formatTime(item.publishedAt) }}</text>
          <text>查看与下载 ›</text>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getHealthReports } from '@/api/health-report'
import type { HealthReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const patientId = ref('')
const patientName = ref('')
const items = ref<HealthReport[]>([])
const loading = ref(true)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    items.value = (await getHealthReports(patientId.value || undefined)).records
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '健康评估报告加载失败'
  } finally {
    loading.value = false
  }
}

onLoad((options) => {
  patientId.value = String(options?.patientId || '')
  patientName.value = decodeURIComponent(String(options?.name || ''))
  void load()
})

const detail = (id: string) =>
  uni.navigateTo({ url: `/pages-customer/health-report/detail?id=${id}` })
const formatTime = (value?: string) =>
  value ? value.replace('T', ' ').slice(0, 16) : '发布时间待确认'
</script>

<style scoped>
.title { margin-top: 8rpx; }
.subtitle { margin-top: 10rpx; }
.report-card { margin-top: 22rpx; padding: 28rpx; }
.report-head,
.report-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.report-summary { margin-top: 18rpx; line-height: 1.65; }
.report-meta {
  margin-top: 22rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #edf2f0;
  color: #81918b;
  font-size: 21rpx;
}
.report-meta text:last-child { color: #0f7a62; font-weight: 650; }
</style>

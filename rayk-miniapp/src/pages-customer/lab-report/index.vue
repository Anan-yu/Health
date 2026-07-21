<template>
  <view class="page report-page">
    <view class="report-header">
      <view>
        <view class="eyebrow">LAB REPORTS</view>
        <view class="title">我的检验报告</view>
        <view class="subtitle">集中管理历次检验数据与确认进度</view>
      </view>
      <view class="upload-button" @click="upload">＋ 上传</view>
    </view>

    <view class="summary-strip">
      <view
        ><text>{{ reports.length }}</text
        ><text>全部报告</text></view
      >
      <view
        ><text>{{ confirmedCount }}</text
        ><text>已确认</text></view
      >
      <view
        ><text>{{ pendingCount }}</text
        ><text>待处理</text></view
      >
    </view>

    <view class="filter-row">
      <view
        v-for="item in filters"
        :key="item.code"
        class="filter"
        :class="{ active: activeFilter === item.code }"
        @click="activeFilter = item.code"
        >{{ item.label }}</view
      >
    </view>

    <PageState :loading="loading" :error="error" :empty="filteredReports.length === 0">
      <view
        v-for="item in filteredReports"
        :key="item.id"
        class="card report-card"
        @click="detail(item.id)"
      >
        <view class="report-icon">报</view>
        <view class="report-content">
          <view class="report-top">
            <view class="report-name">{{ item.reportName }}</view>
            <StatusTag :status="item.status" />
          </view>
          <view class="report-date">{{ item.reportDate }}</view>
          <view class="report-meta">
            <text>{{ item.indicators.length }} 项检验指标</text>
            <text>查看详情 ›</text>
          </view>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyLabReports } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const reports = ref<LabReport[]>([]),
  loading = ref(true),
  error = ref(''),
  activeFilter = ref<'ALL' | 'PENDING' | 'COMPLETED'>('ALL')
const filters = [
  { code: 'ALL' as const, label: '全部' },
  { code: 'PENDING' as const, label: '待确认' },
  { code: 'COMPLETED' as const, label: '已完成' },
]
const completedStatuses = ['CONFIRMED', 'AI_PROCESSING', 'AI_COMPLETED', 'COMPLETED', 'PUBLISHED']
const filteredReports = computed(() => {
  if (activeFilter.value === 'ALL') return reports.value
  return reports.value.filter((item) =>
    activeFilter.value === 'COMPLETED'
      ? completedStatuses.includes(item.status)
      : !completedStatuses.includes(item.status),
  )
})
const confirmedCount = computed(
  () => reports.value.filter((item) => completedStatuses.includes(item.status)).length,
)
const pendingCount = computed(() => Math.max(reports.value.length - confirmedCount.value, 0))

onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    reports.value = await getMyLabReports()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '检验报告加载失败'
  } finally {
    loading.value = false
  }
})
const upload = () => uni.navigateTo({ url: '/pages-customer/lab-report/upload' })
const detail = (id: string) => uni.navigateTo({ url: `/pages-customer/lab-report/detail?id=${id}` })
</script>

<style scoped>
.report-page {
  padding-top: 34rpx;
}
.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 4rpx 30rpx;
}
.report-header .title {
  margin-top: 8rpx;
}
.upload-button {
  flex: 0 0 auto;
  margin-left: 20rpx;
  padding: 16rpx 22rpx;
  border-radius: 999rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 23rpx;
  font-weight: 650;
  box-shadow: 0 8rpx 18rpx rgba(15, 122, 98, 0.18);
}
.summary-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-bottom: 26rpx;
  padding: 25rpx 12rpx;
  border: 1rpx solid #dfeae6;
  border-radius: 26rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(33, 77, 65, 0.05);
}
.summary-strip view {
  display: flex;
  flex-direction: column;
  text-align: center;
}
.summary-strip text:first-child {
  color: #173e34;
  font-size: 34rpx;
  font-weight: 750;
}
.summary-strip text:last-child {
  margin-top: 6rpx;
  color: #83918c;
  font-size: 20rpx;
}
.filter-row {
  display: flex;
  gap: 14rpx;
  margin-bottom: 22rpx;
}
.filter {
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: #eaf0ee;
  color: #74837e;
  font-size: 22rpx;
}
.filter.active {
  background: #dff3ec;
  color: #0d745c;
  font-weight: 650;
}
.report-card {
  display: flex;
  align-items: center;
  padding: 26rpx;
}
.report-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 82rpx;
  height: 82rpx;
  border-radius: 26rpx;
  background: linear-gradient(145deg, #e1f5ee, #ccecdf);
  color: #0e735c;
  font-size: 25rpx;
  font-weight: 750;
}
.report-content {
  flex: 1;
  min-width: 0;
  margin-left: 22rpx;
}
.report-top,
.report-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.report-name {
  overflow: hidden;
  font-size: 28rpx;
  font-weight: 680;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.report-date {
  margin-top: 8rpx;
  color: #82908b;
  font-size: 22rpx;
}
.report-meta {
  margin-top: 16rpx;
  color: #8c9995;
  font-size: 20rpx;
}
.report-meta text:last-child {
  color: #0f7a62;
}
</style>

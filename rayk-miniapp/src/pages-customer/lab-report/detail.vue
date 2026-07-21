<template>
  <view class="page report-detail-page">
    <PageState :loading="loading" :error="error" :empty="!report">
      <view class="report-hero">
        <view class="report-symbol">报</view>
        <view class="report-head-content">
          <view class="hero-name">{{ report?.reportName }}</view>
          <view class="hero-date">报告日期 · {{ report?.reportDate }}</view>
        </view>
        <StatusTag :status="report?.status || ''" />
      </view>
      <view class="indicator-summary">
        <view
          ><text>{{ report?.indicators.length || 0 }}</text
          ><text>检验指标</text></view
        >
        <view
          ><text>{{ abnormalCount }}</text
          ><text>需关注</text></view
        >
        <view
          ><text>{{ normalCount }}</text
          ><text>正常范围</text></view
        >
      </view>
      <button v-if="canConfirm" class="primary workflow-button" @click="openConfirmation">
        {{ isProcessing ? '查看识别进度' : '校对并确认指标' }}
      </button>
      <view class="section-head"
        ><view
          ><view class="eyebrow">INDICATORS</view><view class="section-title">检验指标</view></view
        ></view
      >
      <view v-for="item in report?.indicators" :key="item.id" class="card indicator-card">
        <view class="indicator-main">
          <view
            ><view class="indicator-name">{{ item.name }}</view
            ><view class="indicator-ref"
              >参考 {{ item.referenceLow ?? '-' }} ~ {{ item.referenceHigh ?? '-' }}
              {{ item.unit }}</view
            ></view
          >
          <StatusTag :status="item.abnormalFlag || 'NORMAL'" />
        </view>
        <view class="indicator-value"
          ><text>{{ item.value }}</text
          ><text>{{ item.unit }}</text></view
        >
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getLabReport } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const report = ref<LabReport>(),
  loading = ref(true),
  error = ref('')
const abnormalCount = computed(
  () =>
    report.value?.indicators.filter((item) => item.abnormalFlag && item.abnormalFlag !== 'NORMAL')
      .length || 0,
)
const normalCount = computed(() =>
  Math.max((report.value?.indicators.length || 0) - abnormalCount.value, 0),
)
const canConfirm = computed(() =>
  ['UPLOADED', 'OCR_PENDING', 'OCR_PROCESSING', 'OCR_FAILED', 'WAITING_CONFIRMATION'].includes(
    report.value?.status || '',
  ),
)
const isProcessing = computed(() =>
  ['UPLOADED', 'OCR_PENDING', 'OCR_PROCESSING'].includes(report.value?.status || ''),
)
const reportId = ref('')
onLoad((q) => {
  reportId.value = String(q?.id || '')
})
onShow(async () => {
  if (!reportId.value) {
    error.value = '缺少检验报告编号'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    report.value = await getLabReport(reportId.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '检验报告加载失败'
  } finally {
    loading.value = false
  }
})
const openConfirmation = () =>
  uni.navigateTo({ url: `/pages-customer/lab-report/confirm?id=${reportId.value}` })
</script>

<style scoped>
.report-detail-page {
  padding-top: 24rpx;
}
.report-hero {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-radius: 32rpx;
  background: linear-gradient(140deg, #e3f6ef, #f6fbf9);
  border: 1rpx solid #d4e9e1;
}
.report-symbol {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 82rpx;
  height: 82rpx;
  border-radius: 26rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 25rpx;
  font-weight: 750;
}
.report-head-content {
  flex: 1;
  min-width: 0;
  margin: 0 20rpx;
}
.hero-name {
  overflow: hidden;
  font-size: 31rpx;
  font-weight: 710;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hero-date {
  margin-top: 8rpx;
  color: #788b84;
  font-size: 22rpx;
}
.indicator-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 22rpx;
  padding: 24rpx 12rpx;
  border: 1rpx solid #e2ebe8;
  border-radius: 25rpx;
  background: #fff;
}
.workflow-button {
  margin-top: 22rpx;
}
.indicator-summary view {
  display: flex;
  flex-direction: column;
  text-align: center;
}
.indicator-summary text:first-child {
  color: #174539;
  font-size: 32rpx;
  font-weight: 740;
}
.indicator-summary text:last-child {
  margin-top: 5rpx;
  color: #82908b;
  font-size: 20rpx;
}
.indicator-card {
  padding: 27rpx;
}
.indicator-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}
.indicator-name {
  font-size: 28rpx;
  font-weight: 680;
}
.indicator-ref {
  margin-top: 8rpx;
  color: #87948f;
  font-size: 21rpx;
}
.indicator-value {
  display: flex;
  align-items: baseline;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #edf1f0;
}
.indicator-value text:first-child {
  color: #0f7a62;
  font-size: 44rpx;
  font-weight: 760;
}
.indicator-value text:last-child {
  margin-left: 9rpx;
  color: #7d8b86;
  font-size: 22rpx;
}
</style>

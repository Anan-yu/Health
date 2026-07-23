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
      <view v-if="isProcessing" class="processing-note"
        >系统正在自动识别并生成健康评估，无需停留本页，可退出后等待。完成后可在“我的报告”查看结果。</view
      >
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
import { computed, onUnmounted, ref } from 'vue'
import { onHide, onLoad, onShow } from '@dcloudio/uni-app'
import { getLabReport, getOcrTask } from '@/api/lab-report'
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
const isProcessing = computed(() =>
  ['UPLOADED', 'OCR_PENDING', 'OCR_PROCESSING', 'CONFIRMED', 'AI_PROCESSING'].includes(
    report.value?.status || '',
  ),
)
const reportId = ref('')
const autoReturn = ref(false)
let pollTimer: ReturnType<typeof globalThis.setTimeout> | undefined
onLoad((q) => {
  reportId.value = String(q?.id || '')
  autoReturn.value = String(q?.autoReturn || '') === '1'
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
    scheduleOcrPoll()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '检验报告加载失败'
  } finally {
    loading.value = false
  }
})
onHide(stopOcrPoll)
onUnmounted(stopOcrPoll)

function stopOcrPoll() {
  if (pollTimer) {
    globalThis.clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

function scheduleOcrPoll() {
  stopOcrPoll()
  if (!autoReturn.value) return
  pollTimer = globalThis.setTimeout(async () => {
    try {
      const task = await getOcrTask(reportId.value)
      if (task.status === 'SUCCESS') {
        stopOcrPoll()
        uni.showToast({ title: '识别完成' })
        globalThis.setTimeout(
          () => uni.redirectTo({ url: '/pages-customer/lab-report/index' }),
          500,
        )
        return
      }
      if (task.status === 'FAILED') {
        stopOcrPoll()
        report.value = await getLabReport(reportId.value)
        return
      }
      report.value = await getLabReport(reportId.value)
      scheduleOcrPoll()
    } catch {
      scheduleOcrPoll()
    }
  }, 1800)
}
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
.processing-note {
  margin-top: 22rpx;
  padding: 20rpx 24rpx;
  border-radius: 18rpx;
  background: #eaf7f2;
  color: #28715e;
  font-size: 23rpx;
  line-height: 1.6;
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

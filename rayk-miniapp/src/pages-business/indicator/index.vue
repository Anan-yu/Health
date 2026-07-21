<template>
  <view class="page editor-page">
    <OcrIndicatorEditor
      v-if="reportId"
      :report-id="reportId"
      assessment-route="/pages-business/assessment/index"
    />
    <view v-else class="selector-page">
      <view class="selector-hero">
        <view class="eyebrow-light">OCR REVIEW</view>
        <view class="hero-title">选择待校对报告</view>
        <view class="hero-copy">只有 OCR 已完成、等待人工确认的报告会显示在这里。</view>
      </view>
      <PageState :loading="loading" :error="error" :empty="items.length === 0">
        <view v-for="item in items" :key="item.id" class="card report-card" @click="open(item.id)">
          <view class="report-icon">OCR</view>
          <view class="report-content">
            <view class="section-title">{{ item.reportName }}</view>
            <view class="subtitle">客户 ID {{ item.patientId }} · {{ item.reportDate }}</view>
            <view class="muted">已识别 {{ item.indicators.length }} 项指标，点击开始人工校对</view>
          </view>
          <view class="arrow">›</view>
        </view>
      </PageState>
      <button class="task-button" @click="goTasks">查看全部报告任务</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getLabReports } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import OcrIndicatorEditor from '@/components/OcrIndicatorEditor.vue'
import PageState from '@/components/PageState.vue'

const reportId = ref('')
const items = ref<LabReport[]>([])
const loading = ref(true)
const error = ref('')

const load = async () => {
  if (reportId.value) return
  loading.value = true
  error.value = ''
  try {
    const reports = (await getLabReports()).records
    items.value = reports.filter((item) => item.status === 'WAITING_CONFIRMATION')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '待校对报告加载失败'
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  reportId.value = String(query?.id || '')
})
onShow(load)

const open = (id: string) => {
  reportId.value = id
}
const goTasks = () => uni.navigateTo({ url: '/pages-business/lab-report/index' })
</script>

<style scoped>
.editor-page {
  padding-top: 24rpx;
}
.selector-page {
  padding: 0 0 24rpx;
}
.selector-hero {
  padding: 34rpx;
  border-radius: 34rpx;
  background: linear-gradient(140deg, #173f37, #0b715a);
  color: #fff;
  box-shadow: 0 18rpx 40rpx rgba(15, 94, 75, 0.18);
}
.eyebrow-light {
  color: #91d9c4;
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}
.hero-title {
  margin-top: 12rpx;
  font-size: 40rpx;
  font-weight: 750;
}
.hero-copy {
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.66);
  font-size: 21rpx;
}
.report-card {
  display: flex;
  align-items: center;
  margin-top: 24rpx;
  padding: 26rpx;
}
.report-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 74rpx;
  height: 74rpx;
  border-radius: 22rpx;
  background: #e1f5ee;
  color: #0f765f;
  font-size: 19rpx;
  font-weight: 750;
}
.report-content {
  flex: 1;
  min-width: 0;
  margin-left: 20rpx;
}
.report-content .muted {
  margin-top: 10rpx;
}
.arrow {
  color: #0f7a62;
  font-size: 42rpx;
}
.task-button {
  margin-top: 26rpx;
  border: 1rpx solid #bcded3;
  border-radius: 16rpx;
  background: #fff;
  color: #0f7a62;
  font-size: 26rpx;
}
</style>

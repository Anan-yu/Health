<template>
  <view class="page editor-page">
    <view class="selector-page">
      <view class="selector-hero">
        <view class="eyebrow-light">OCR REVIEW</view>
        <view class="hero-title">报告处理进度</view>
        <view class="hero-copy">报告上传后由系统自动识别并进入健康评估，本工作台仅查看处理进度。</view>
      </view>
      <PageState :loading="loading" :error="error" :empty="items.length === 0">
        <view v-for="item in items" :key="item.id" class="card report-card">
          <view class="report-icon">OCR</view>
          <view class="report-content">
            <view class="section-title">{{ item.reportName }}</view>
            <view class="subtitle">客户 ID {{ item.patientId }} · {{ item.reportDate }}</view>
            <view class="muted">已识别 {{ item.indicators.length }} 项指标，系统正在生成健康评估</view>
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
import { onShow } from '@dcloudio/uni-app'
import { getLabReports } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'

const items = ref<LabReport[]>([])
const loading = ref(true)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const reports = (await getLabReports()).records
    items.value = reports.filter((item) =>
      ['OCR_PENDING', 'OCR_PROCESSING', 'CONFIRMED', 'AI_PROCESSING'].includes(item.status),
    )
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '报告处理进度加载失败'
  } finally {
    loading.value = false
  }
}

onShow(load)

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

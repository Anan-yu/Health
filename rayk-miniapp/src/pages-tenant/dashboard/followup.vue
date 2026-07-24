<template>
  <view class="page followup-page">
    <view class="page-heading">
      <view>
        <view class="eyebrow">HEALTH FOLLOW-UP</view>
        <view class="title">健康随访动态</view>
        <view class="subtitle">查看累计任务、客户反馈与完成情况</view>
      </view>
      <view class="refresh" @click="load">刷新</view>
    </view>
    <PageState :loading="loading" :error="error" :empty="!followups.length">
      <view
        v-for="item in followups"
        :key="item.id"
        class="card followup-card"
        hover-class="followup-card-active"
        @click="openDetail(item.id)"
      >
        <view class="followup-top">
          <view>
            <view class="section-title">{{ item.patientName }}</view>
            <view class="muted">{{ item.title }} · 截止 {{ item.dueDate }}</view>
          </view>
          <StatusTag :status="statusText(item)" />
        </view>
        <view v-if="item.feedback" class="feedback">客户反馈：{{ item.feedback }}</view>
        <view v-else class="muted pending-copy">客户尚未提交反馈</view>
        <view class="card-footer">
          <view v-if="item.completedAt" class="completed-time"
            >完成时间：{{ formatDateTime(item.completedAt) }}</view
          >
          <view v-else />
          <view class="detail-link">查看任务详情 ›</view>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPlatformOverview } from '@/api/admin'
import type { PlatformFollowup } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const followups = ref<PlatformFollowup[]>([])
const loading = ref(true)
const error = ref('')
const statusText = (item: PlatformFollowup) =>
  item.status === 'COMPLETED' ? '已完成' : item.feedback ? '有反馈' : '待完成'
const formatDateTime = (value: string) => value.replace('T', ' ').slice(0, 16)
const openDetail = (id: string) =>
  uni.navigateTo({ url: `/pages-tenant/dashboard/followup-detail?id=${id}` })
async function load() {
  loading.value = true
  error.value = ''
  try {
    followups.value = (await getPlatformOverview()).followups || []
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '健康随访动态加载失败'
  } finally {
    loading.value = false
  }
}
onShow(load)
</script>

<style scoped>
.followup-page {
  padding-top: 34rpx;
}
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 0 6rpx 30rpx;
  gap: 18rpx;
}
.page-heading .title {
  margin-top: 8rpx;
}
.refresh {
  flex: 0 0 auto;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: #e4f6f0;
  color: #0d765e;
  font-size: 22rpx;
}
.followup-card {
  margin-bottom: 18rpx;
  padding: 28rpx;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}
.followup-card-active {
  transform: scale(0.985);
  box-shadow: 0 8rpx 18rpx rgba(11, 96, 75, 0.1);
}
.followup-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}
.feedback {
  margin-top: 20rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #edf8f4;
  color: #256f5d;
  font-size: 23rpx;
  line-height: 1.6;
}
.pending-copy {
  margin-top: 20rpx;
  font-size: 22rpx;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
}
.completed-time {
  color: #8a9993;
  font-size: 21rpx;
}
.detail-link {
  flex: 0 0 auto;
  color: #0d765e;
  font-size: 22rpx;
  font-weight: 650;
}
</style>

<template>
  <view class="page message-page">
    <view class="page-heading">
      <view
        ><view class="eyebrow">消息通知</view><view class="title">消息中心</view
        ><view class="subtitle">已发布报告和用户健康随访会在这里汇总</view></view
      >
      <view class="message-count">{{ items.length }}</view>
    </view>
    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view v-for="item in items" :key="item.id" class="card notification" @click="open(item)">
        <view
          class="notification-icon"
          :class="item.kind === 'report' ? 'report-icon' : 'followup-icon'"
          >{{ item.kind === 'report' ? '报' : '访' }}</view
        >
        <view class="notification-content"
          ><view class="notification-title">{{ item.title }}</view
          ><view class="notification-copy">{{ item.content }}</view
          ><view class="notification-time">{{ item.time }}</view></view
        >
        <view class="chevron">›</view>
      </view>
    </PageState>
    <view v-if="items.length" class="message-footer">仅展示与你相关的业务动态</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyFollowups } from '@/api/followup'
import { getMyHealthReports } from '@/api/health-report'
import PageState from '@/components/PageState.vue'
import { useAuthStore } from '@/stores/auth'

type Notification = {
  id: string
  kind: 'report' | 'followup'
  title: string
  content: string
  time: string
}
const reports = ref<Awaited<ReturnType<typeof getMyHealthReports>>>([])
const followups = ref<Awaited<ReturnType<typeof getMyFollowups>>>([])
const loading = ref(true)
const error = ref('')
const auth = useAuthStore()
const isDoctor = computed(() => auth.currentWorkbench === 'DOCTOR')
const items = computed<Notification[]>(() => [
  ...reports.value.map((report) => ({
    id: report.id,
    kind: 'report' as const,
    title: '健康报告已发布',
    content: report.title,
    time: report.publishedAt || '刚刚',
  })),
  ...followups.value
    .filter((followup) => isDoctor.value || followup.status !== 'COMPLETED')
    .map((followup) => ({
      id: followup.id,
      kind: 'followup' as const,
      title: isDoctor.value ? '用户健康随访任务' : '待完成健康随访',
      content: isDoctor.value
        ? `${followup.title} · ${followup.status === 'COMPLETED' ? '已完成' : '进行中'}`
        : followup.title,
      time: `截止日期：${followup.dueDate}`,
    })),
])
const open = (item: Notification) => {
  if (isDoctor.value) {
    uni.navigateTo({
      url:
        item.kind === 'report'
          ? '/pages-business/lab-report/index'
          : '/pages-business/patient/index',
    })
    return
  }
  const url =
    item.kind === 'report'
      ? `/pages-customer/health-report/detail?id=${item.id}`
      : `/pages-customer/followup/feedback?id=${item.id}`
  uni.navigateTo({ url })
}
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    ;[reports.value, followups.value] = await Promise.all([getMyHealthReports(), getMyFollowups()])
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '消息加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.message-page {
  padding-top: 34rpx;
}
.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 6rpx 32rpx;
}
.page-heading .title {
  margin-top: 8rpx;
}
.message-count {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  border-radius: 22rpx;
  background: #e4f6f0;
  color: #0d755d;
  font-size: 27rpx;
  font-weight: 750;
}
.notification {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
  padding: 28rpx 24rpx;
}
.notification-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  font-size: 24rpx;
  font-weight: 750;
}
.report-icon {
  background: #e1f5ee;
  color: #0e765e;
}
.followup-icon {
  background: #fff1e9;
  color: #c76a32;
}
.notification-content {
  flex: 1;
  min-width: 0;
  margin-left: 22rpx;
}
.notification-title {
  font-size: 28rpx;
  font-weight: 680;
}
.notification-copy {
  overflow: hidden;
  margin-top: 8rpx;
  color: #70817b;
  font-size: 23rpx;
  line-height: 1.55;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notification-time {
  margin-top: 12rpx;
  color: #a0aaa6;
  font-size: 20rpx;
}
.chevron {
  margin-left: 12rpx;
  color: #a4b0ac;
  font-size: 38rpx;
}
.message-footer {
  padding: 30rpx;
  color: #9aa6a2;
  text-align: center;
  font-size: 21rpx;
}
</style>

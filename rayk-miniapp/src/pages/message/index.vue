<template>
  <view class="page message-page">
    <view class="page-heading">
      <view
        ><view class="title">消息中心</view
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
    <view v-if="items.length" class="message-footer">{{ scopeHint }}</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getFollowups, getMyFollowups } from '@/api/followup'
import { getHealthReports, getMyHealthReports } from '@/api/health-report'
import PageState from '@/components/PageState.vue'
import { useAuthStore } from '@/stores/auth'
import type { Followup, HealthReport } from '@/types/api'

type Notification = {
  id: string
  kind: 'report' | 'followup'
  patientId?: string
  status?: string
  title: string
  content: string
  time: string
}
const reports = ref<HealthReport[]>([])
const followups = ref<Followup[]>([])
const loading = ref(true)
const error = ref('')
const auth = useAuthStore()
const isCustomer = computed(() => auth.currentWorkbench === 'CUSTOMER')
const isPlatform = computed(() => auth.currentWorkbench === 'PLATFORM_ADMIN')
const scopeHint = computed(() =>
  isCustomer.value ? '仅展示本人的健康动态' : '展示全平台用户的健康动态',
)
const followupStatusText = (status: string) => {
  const labels: Record<string, string> = {
    PENDING: '进行中',
    COMPLETED: '已完成',
    PAUSED: '已暂停',
    CANCELLED: '已结束',
  }
  return labels[status] || status
}
const followupReminderTitle = (followup: Followup) => {
  if (followup.status === 'PAUSED') return '健康随访已暂停'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const due = new Date(`${followup.dueDate}T00:00:00`)
  const days = Math.round((due.getTime() - today.getTime()) / 86400000)
  if (days < 0) return `健康随访已逾期 ${Math.abs(days)} 天`
  if (days === 0) return '健康随访今天到期'
  if (days === 1) return '健康随访明天到期'
  return '待完成健康随访'
}
const items = computed<Notification[]>(() => [
  ...reports.value.map((report) => ({
    id: report.id,
    patientId: report.patientId,
    kind: 'report' as const,
    title: isCustomer.value ? '健康报告已发布' : `${report.patientName || '用户'}的健康报告已发布`,
    content: report.title,
    time: report.publishedAt || '刚刚',
  })),
  ...followups.value
    .filter(
      (followup) =>
        !isCustomer.value || ['PENDING', 'PAUSED'].includes(followup.status),
    )
    .map((followup) => ({
      id: followup.id,
      patientId: followup.patientId,
      status: followup.status,
      kind: 'followup' as const,
      title: isCustomer.value
        ? followupReminderTitle(followup)
        : `${followup.patientName || '用户'}的健康随访`,
      content: !isCustomer.value
        ? `${followup.title} · ${followupStatusText(followup.status)}`
        : followup.title,
      time: `截止日期：${followup.dueDate}`,
    })),
])
const open = (item: Notification) => {
  if (!isCustomer.value) {
    uni.navigateTo({
      url:
        item.kind === 'report'
          ? `/pages-customer/health-report/detail?id=${item.id}`
          : isPlatform.value
            ? '/pages-tenant/dashboard/followup'
            : `/pages-business/patient/detail?id=${item.patientId || ''}`,
    })
    return
  }
  const url =
    item.kind === 'report'
      ? `/pages-customer/health-report/detail?id=${item.id}`
      : item.status === 'PAUSED'
        ? '/pages-customer/followup/index'
      : `/pages-customer/followup/feedback?id=${item.id}`
  uni.navigateTo({ url })
}
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    if (isCustomer.value) {
      ;[reports.value, followups.value] = await Promise.all([
        getMyHealthReports(),
        getMyFollowups(),
      ])
    } else {
      const [reportPage, followupPage] = await Promise.all([getHealthReports(), getFollowups()])
      reports.value = reportPage.records
      followups.value = followupPage.records
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '消息加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.message-page {
  padding-top: 24rpx;
}
.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 28rpx;
  padding: 30rpx;
  border: 1rpx solid rgba(210, 230, 223, 0.9);
  border-radius: 32rpx;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(231, 248, 242, 0.96));
  box-shadow: 0 14rpx 34rpx rgba(24, 92, 73, 0.08);
}
.page-heading .title {
  margin: 0 0 8rpx;
}
.message-count {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 68rpx;
  height: 68rpx;
  padding: 0 10rpx;
  border: 1rpx solid rgba(151, 210, 191, 0.55);
  border-radius: 24rpx;
  background: linear-gradient(145deg, #e5f7f1, #d7f1e8);
  color: #0b6f58;
  font-size: 28rpx;
  font-weight: 780;
  font-variant-numeric: tabular-nums;
}
.notification {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
  padding: 28rpx;
  border-color: rgba(214, 229, 223, 0.95);
  box-shadow: 0 12rpx 32rpx rgba(27, 82, 67, 0.07);
}
.notification-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 80rpx;
  height: 80rpx;
  border-radius: 26rpx;
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
  color: #153d33;
  font-size: 29rpx;
  font-weight: 720;
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
  color: #93a29d;
  font-size: 20rpx;
}
.chevron {
  margin-left: 12rpx;
  color: #a4b0ac;
  font-size: 38rpx;
}
.message-footer {
  padding: 28rpx 30rpx 10rpx;
  color: #9aa6a2;
  text-align: center;
  font-size: 21rpx;
}
</style>

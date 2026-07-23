<template>
  <view class="page home-page">
    <PageState :loading="loading" :error="error">
      <view class="hero-card">
        <view class="hero-glow" />
        <view class="hero-top">
          <view>
            <view class="hero-date">{{ dateText }}</view>
            <view class="hero-greeting">{{
              summary?.greeting || `你好，${auth.user?.displayName}`
            }}</view>
            <view class="hero-caption">{{ heroCaption }}</view>
          </view>
          <view class="avatar">{{ avatarText }}</view>
        </view>
        <view class="hero-bottom">
          <view class="workbench-pill"> <text class="online-dot" />{{ roleLabel }} </view>
          <view class="switch-link" @click="goSwitch">切换工作台 ›</view>
        </view>
      </view>

      <view class="section-head">
        <view>
          <view class="eyebrow">OVERVIEW</view>
          <view class="section-title">今日概览</view>
        </view>
        <view class="section-tip refresh-tip" @click="refresh(true)">{{ refreshLabel }}</view>
      </view>
      <view class="metric-grid">
        <view
          v-for="(item, index) in summary?.metrics"
          :key="item.code"
          class="metric-card"
          :class="`tone-${index % 4}`"
          @click="open(item.route)"
        >
          <view class="metric-head">
            <view class="metric-icon">{{ metricIcons[index % metricIcons.length] }}</view>
            <text class="metric-arrow">↗</text>
          </view>
          <view class="metric">{{ item.value }}</view>
          <view class="metric-label">{{ item.label }}</view>
          <view class="metric-hint">点击查看详情</view>
        </view>
      </view>

      <view class="section-head">
        <view>
          <view class="eyebrow">SERVICES</view>
          <view class="section-title">常用服务</view>
        </view>
        <view class="section-tip" @click="goWorkbench">全部服务 ›</view>
      </view>
      <view class="card service-card">
        <view
          v-for="(item, index) in quickMenus"
          :key="item.route"
          class="service-item"
          @click="open(item.route)"
        >
          <view class="service-icon" :class="`service-tone-${index % 4}`">{{ item.icon }}</view>
          <view class="service-name">{{ item.title }}</view>
        </view>
      </view>

      <view v-if="isCustomer" class="insight-card" @click="open(insightRoute)">
        <view class="insight-icon">{{ isCustomer ? '✓' : '效' }}</view>
        <view class="insight-content">
          <view class="insight-label">{{ insightLabel }}</view>
          <view class="insight-title">{{ insightTitle }}</view>
          <view v-if="isCustomer" class="progress-track"
            ><view class="progress-value" :style="{ width: `${insightProgress}%` }"
          /></view>
        </view>
        <view class="insight-arrow">›</view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import PageState from '@/components/PageState.vue'
import { getHomeSummary } from '@/api/workbench'
import { menusFor } from '@/constants/menus'
import { useAuthStore } from '@/stores/auth'
import type { HomeSummary, Role } from '@/types/api'

const auth = useAuthStore()
const summary = ref<HomeSummary>()
const loading = ref(true),
  error = ref('')
const lastUpdatedAt = ref<Date | null>(null)
let refreshTimer: ReturnType<typeof globalThis.setInterval> | undefined
const metricIcons = ['待', '报', '评', '访']
const roleNames: Record<Role, string> = {
  PLATFORM_ADMIN: '平台管理工作台',
  DOCTOR: '医生工作台',
  CUSTOMER: '个人健康中心',
}

const isCustomer = computed(() => auth.currentWorkbench === 'CUSTOMER')
const roleLabel = computed(() =>
  auth.currentWorkbench ? roleNames[auth.currentWorkbench] : '工作台',
)
const avatarText = computed(() => auth.user?.displayName?.slice(0, 1) || 'R')
const quickMenus = computed(() =>
  menusFor(auth.currentWorkbench)
    .filter((item) => !item.permission || auth.permissions.includes(item.permission))
    .slice(0, 4),
)
const heroCaption = computed(() =>
  isCustomer.value ? '关注趋势，完成今天的健康管理任务' : '关键任务已为你整理，及时处理更高效',
)
const profileCompleteness = computed(() => {
  const value = summary.value?.metrics.find((item) => item.code === 'PROFILE')?.value ?? 0
  return Math.min(100, Math.max(0, Number(value) || 0))
})
const insightTitle = computed(() =>
  `健康档案已完善 ${profileCompleteness.value}%`,
)
const insightLabel = computed(() => '健康管理进度')
const insightProgress = computed(() => profileCompleteness.value)
const insightRoute = computed(() => '/pages-customer/profile/index')
const dateText = computed(() => {
  const now = new Date()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${now.getMonth() + 1}月${now.getDate()}日 · ${weekdays[now.getDay()]}`
})
const refreshLabel = computed(() =>
  lastUpdatedAt.value
    ? `更新于 ${lastUpdatedAt.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })} · 刷新`
    : '正在更新',
)

async function refresh(silent = false) {
  if (!silent) loading.value = true
  error.value = ''
  try {
    summary.value = await getHomeSummary()
    lastUpdatedAt.value = new Date()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onShow(() => {
  void refresh()
  if (refreshTimer) globalThis.clearInterval(refreshTimer)
  refreshTimer = globalThis.setInterval(() => void refresh(true), 20_000)
})
onHide(() => {
  if (refreshTimer) globalThis.clearInterval(refreshTimer)
  refreshTimer = undefined
})
onPullDownRefresh(async () => {
  await refresh(true)
  uni.stopPullDownRefresh()
})

const open = (url: string) => uni.navigateTo({ url })
const goSwitch = () => uni.navigateTo({ url: '/pages/switch-workbench/index' })
const goWorkbench = () => uni.switchTab({ url: '/pages/workbench/index' })
</script>

<style scoped>
.home-page {
  padding-top: 24rpx;
}
.hero-card {
  position: relative;
  overflow: hidden;
  padding: 38rpx 36rpx 32rpx;
  border-radius: 38rpx;
  background: linear-gradient(140deg, #075744 0%, #0e7c63 56%, #31aa89 100%);
  color: #fff;
  box-shadow: 0 22rpx 48rpx rgba(8, 100, 79, 0.22);
}
.hero-glow {
  position: absolute;
  top: -90rpx;
  right: -65rpx;
  width: 300rpx;
  height: 300rpx;
  border: 44rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 50%;
}
.hero-top,
.hero-bottom {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hero-date {
  color: rgba(255, 255, 255, 0.7);
  font-size: 23rpx;
}
.hero-greeting {
  max-width: 510rpx;
  margin-top: 14rpx;
  font-size: 42rpx;
  line-height: 1.3;
  font-weight: 750;
}
.hero-caption {
  max-width: 490rpx;
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 24rpx;
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 86rpx;
  height: 86rpx;
  margin-left: 18rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.28);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.15);
  font-size: 34rpx;
  font-weight: 760;
}
.hero-bottom {
  margin-top: 40rpx;
  padding-top: 26rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.16);
}
.workbench-pill {
  display: flex;
  align-items: center;
  font-size: 23rpx;
}
.online-dot {
  width: 12rpx;
  height: 12rpx;
  margin-right: 10rpx;
  border: 4rpx solid rgba(172, 244, 221, 0.2);
  border-radius: 50%;
  background: #aaf1d9;
}
.switch-link {
  color: rgba(255, 255, 255, 0.82);
  font-size: 23rpx;
}
.section-tip {
  color: #84938e;
  font-size: 22rpx;
}
.refresh-tip {
  color: #0f7a62;
}
.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}
.metric-card {
  padding: 24rpx;
  border: 1rpx solid #e1ece8;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(33, 77, 65, 0.06);
}
.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 26rpx;
}
.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
  border-radius: 18rpx;
  background: #e6f7f1;
  color: #0f7a62;
  font-size: 22rpx;
  font-weight: 750;
}
.metric-arrow {
  color: #9aaba5;
  font-size: 24rpx;
}
.tone-1 .metric-icon {
  background: #eaf1ff;
  color: #4472bc;
}
.tone-2 .metric-icon {
  background: #fff2da;
  color: #a96c0b;
}
.tone-3 .metric-icon {
  background: #f1eaff;
  color: #7653b5;
}
.metric-label {
  margin-top: 8rpx;
  font-size: 25rpx;
  font-weight: 650;
}
.metric-hint {
  margin-top: 8rpx;
  color: #9aa7a2;
  font-size: 20rpx;
}
.service-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  padding: 30rpx 18rpx 26rpx;
}
.service-item {
  min-width: 0;
  text-align: center;
}
.service-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 78rpx;
  height: 78rpx;
  margin: 0 auto 14rpx;
  border-radius: 26rpx;
  background: #e5f7f1;
  color: #0e735c;
  font-size: 24rpx;
  font-weight: 760;
}
.service-tone-1 {
  background: #eaf1ff;
  color: #3e6eb9;
}
.service-tone-2 {
  background: #fff2dc;
  color: #a86908;
}
.service-tone-3 {
  background: #f2ebff;
  color: #7452b1;
}
.service-name {
  overflow: hidden;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.insight-card {
  display: flex;
  align-items: center;
  padding: 28rpx;
  border: 1rpx solid #f3e5c2;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #fffaf0, #fff5df);
}
.insight-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: #ffe7ad;
  color: #8c6105;
  font-size: 27rpx;
  font-weight: 750;
}
.insight-content {
  flex: 1;
  min-width: 0;
  margin: 0 22rpx;
}
.insight-label {
  color: #9b7731;
  font-size: 21rpx;
}
.insight-title {
  overflow: hidden;
  margin-top: 6rpx;
  font-size: 25rpx;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.progress-track {
  height: 8rpx;
  margin-top: 15rpx;
  overflow: hidden;
  border-radius: 999rpx;
  background: #f2dfb1;
}
.progress-value {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #efb33a, #d78b13);
  transition: width 0.25s ease;
}
.insight-arrow {
  color: #b58d42;
  font-size: 42rpx;
}
</style>

<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view>
        <view class="eyebrow">PLATFORM OVERVIEW</view>
        <view class="title">平台运营概览</view>
        <view class="subtitle">跨机构聚合数据，不展示客户敏感明细</view>
      </view>
      <view class="refresh" @click="load">刷新</view>
    </view>
    <PageState :loading="loading" :error="error" :empty="!overview">
      <template v-if="overview">
        <view class="metric-grid">
          <view v-for="item in metrics" :key="item.label" class="card metric-card">
            <view class="metric-value">{{ item.value }}</view>
            <view class="metric-label">{{ item.label }}</view>
          </view>
        </view>
        <view class="section-heading">
          <view class="section-title">入驻机构</view>
          <view class="muted"
            >正常 {{ overview.activeTenantCount }} / {{ overview.tenantCount }}</view
          >
        </view>
        <view v-for="tenant in overview.tenants" :key="tenant.id" class="card tenant-card">
          <view class="tenant-icon">机</view>
          <view class="tenant-main">
            <view class="section-title">{{ tenant.name }}</view>
            <view class="muted"
              >{{ tenant.code }} · {{ tenant.servicePlan }} · {{ tenant.userCount }} 位用户</view
            >
          </view>
          <StatusTag :status="tenant.status" />
        </view>
      </template>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPlatformOverview } from '@/api/admin'
import type { PlatformOverview } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const overview = ref<PlatformOverview>()
const loading = ref(true)
const error = ref('')
const metrics = computed(() => {
  const value = overview.value
  if (!value) return []
  return [
    { label: '入驻机构', value: value.tenantCount },
    { label: '机构用户', value: value.userCount },
    { label: '健康客户', value: value.patientCount },
    { label: '待医生审核', value: value.pendingReviewCount },
    { label: '待完成随访', value: value.pendingFollowupCount },
  ]
})

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    overview.value = await getPlatformOverview()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '平台概览加载失败'
  } finally {
    loading.value = false
  }
}
onShow(load)
</script>

<style scoped>
.admin-page {
  padding-top: 34rpx;
}
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 0 6rpx 28rpx;
}
.page-heading .title {
  margin-top: 8rpx;
}
.refresh {
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background: #e4f6f0;
  color: #0d765e;
  font-size: 22rpx;
}
.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}
.metric-card {
  padding: 27rpx 25rpx;
}
.metric-card:last-child {
  grid-column: 1 / -1;
}
.metric-value {
  color: #0d765e;
  font-size: 46rpx;
  font-weight: 760;
}
.metric-label {
  margin-top: 5rpx;
  color: #657b74;
  font-size: 22rpx;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  margin: 34rpx 6rpx 18rpx;
}
.tenant-card {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
  padding: 25rpx;
}
.tenant-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68rpx;
  height: 68rpx;
  border-radius: 21rpx;
  background: #dff3ec;
  color: #0d7059;
  font-weight: 750;
}
.tenant-main {
  flex: 1;
  min-width: 0;
  margin: 0 18rpx;
}
</style>

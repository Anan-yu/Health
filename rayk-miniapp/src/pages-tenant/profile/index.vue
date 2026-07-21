<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view class="eyebrow">ORGANIZATION</view>
      <view class="title">机构信息</view>
      <view class="subtitle">当前账号所属机构的真实注册资料</view>
    </view>
    <PageState :loading="loading" :error="error" :empty="!profile">
      <view v-if="profile" class="card profile-card">
        <view class="institution-head">
          <view class="institution-icon">机</view>
          <view>
            <view class="section-title">{{ profile.name }}</view>
            <view class="muted">机构编号 {{ profile.id }}</view>
          </view>
          <StatusTag :status="profile.status" />
        </view>
        <view class="detail-row"
          ><text>服务套餐</text><text>{{ profile.servicePlan }}</text></view
        >
        <view class="detail-row"
          ><text>运行状态</text><text>{{ statusText }}</text></view
        >
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getTenantProfile } from '@/api/admin'
import type { TenantProfile } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const profile = ref<TenantProfile>()
const loading = ref(true)
const error = ref('')
const statusText = computed(() => (profile.value?.status === 'ACTIVE' ? '正常服务' : '已停用'))

onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    profile.value = await getTenantProfile()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '机构信息加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.admin-page {
  padding-top: 34rpx;
}
.page-heading {
  margin: 0 6rpx 28rpx;
}
.page-heading .title {
  margin-top: 8rpx;
}
.profile-card {
  padding: 30rpx;
}
.institution-head {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding-bottom: 26rpx;
}
.institution-head > view:nth-child(2) {
  flex: 1;
  min-width: 0;
}
.institution-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80rpx;
  height: 80rpx;
  border-radius: 25rpx;
  background: #dff3ec;
  color: #0d7059;
  font-weight: 750;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 23rpx 0;
  border-top: 1rpx solid #edf2f0;
  font-size: 24rpx;
}
.detail-row text:first-child {
  color: #798b85;
}
.detail-row text:last-child {
  color: #24463d;
  font-weight: 650;
}
</style>

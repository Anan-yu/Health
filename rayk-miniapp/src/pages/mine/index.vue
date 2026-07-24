<template>
  <view class="page mine-page">
    <view class="profile-card" :class="{ clickable: canSwitch }" @click="canSwitch && goSwitch()">
      <view class="profile-pattern" />
      <view class="profile-top">
        <view class="avatar">{{ avatarText }}</view>
        <view class="profile-content">
          <view class="profile-name">{{ auth.user?.displayName || '致宇用户' }}</view>
          <view class="profile-meta">{{ auth.user?.tenantName }}</view>
          <view class="role-pill">{{ workbenchName }}</view>
        </view>
        <view v-if="canSwitch" class="profile-arrow">›</view>
      </view>
      <view class="profile-stats">
        <view
          ><text>{{ auth.roles.length }}</text
          ><text>账号角色</text></view
        >
        <view
          ><text>{{ auth.user?.availableWorkbenches.length || 0 }}</text
          ><text>可用工作台</text></view
        >
      </view>
    </view>

    <view v-if="!isPlatform" class="section-head">
      <view>
        <view class="section-title">账号与服务</view>
      </view>
    </view>
    <view v-if="!isPlatform" class="card settings-card">
      <view class="setting last" @click="goSupport">
        <view class="setting-icon purple">帮</view>
        <view class="setting-content">
          <view class="setting-title">帮助与反馈</view>
          <view class="muted">使用指南与问题反馈</view>
        </view>
        <view class="setting-arrow">›</view>
      </view>
    </view>

    <button class="logout" @click="signOut">退出当前账号</button>
    <view class="version">致宇健康 · Version 0.1.0</view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types/api'

const auth = useAuthStore()
const roleNames: Record<Role, string> = {
  PLATFORM_ADMIN: '平台管理员',
  DOCTOR: '医生工作台',
  CUSTOMER: '个人健康中心',
}
const avatarText = computed(() => auth.user?.displayName?.slice(0, 1) || 'R')
const workbenchName = computed(() =>
  auth.currentWorkbench ? roleNames[auth.currentWorkbench] : '当前工作台',
)
const canSwitch = computed(() => (auth.user?.availableWorkbenches.length || 0) > 1)
const goSwitch = () => uni.navigateTo({ url: '/pages/switch-workbench/index' })
const goSupport = () => uni.navigateTo({ url: '/pages/support/index' })
const isPlatform = computed(() => auth.currentWorkbench === 'PLATFORM_ADMIN')
async function signOut() {
  await auth.signOut()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style scoped>
.mine-page {
  padding-top: 24rpx;
}
.profile-card {
  position: relative;
  overflow: hidden;
  padding: 38rpx 34rpx 30rpx;
  border-radius: 38rpx;
  background: linear-gradient(145deg, #123d34, #0b735c);
  color: #fff;
  box-shadow: 0 22rpx 48rpx rgba(10, 91, 72, 0.21);
}
.profile-card.clickable {
  cursor: pointer;
}
.profile-pattern {
  position: absolute;
  right: -80rpx;
  bottom: -100rpx;
  width: 280rpx;
  height: 280rpx;
  border: 46rpx solid rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}
.profile-top {
  position: relative;
  display: flex;
  align-items: center;
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 106rpx;
  height: 106rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.24);
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.15);
  font-size: 40rpx;
  font-weight: 780;
}
.profile-content {
  flex: 1;
  min-width: 0;
  margin-left: 24rpx;
}
.profile-name {
  font-size: 36rpx;
  font-weight: 750;
}
.profile-meta {
  margin-top: 5rpx;
  color: rgba(255, 255, 255, 0.65);
  font-size: 22rpx;
}
.role-pill {
  display: inline-block;
  margin-top: 12rpx;
  padding: 7rpx 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.1);
  color: #c8f3e5;
  font-size: 20rpx;
}
.profile-arrow {
  color: rgba(255, 255, 255, 0.62);
  font-size: 44rpx;
}
.profile-stats {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  margin-top: 34rpx;
  padding-top: 27rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.13);
}
.profile-stats view {
  display: flex;
  flex-direction: column;
  text-align: center;
}
.profile-stats text:first-child {
  font-size: 32rpx;
  font-weight: 750;
}
.profile-stats text:last-child {
  margin-top: 5rpx;
  color: rgba(255, 255, 255, 0.6);
  font-size: 20rpx;
}
.settings-card {
  padding: 0 28rpx;
}
.setting {
  display: flex;
  align-items: center;
  padding: 27rpx 0;
  border-bottom: 1rpx solid #edf1f0;
}
.setting.last {
  border-bottom: 0;
}
.setting-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 66rpx;
  height: 66rpx;
  border-radius: 21rpx;
  font-size: 22rpx;
  font-weight: 730;
}
.green {
  background: #e1f5ee;
  color: #0f765f;
}
.blue {
  background: #eaf1ff;
  color: #476fac;
}
.amber {
  background: #fff1d8;
  color: #9a6409;
}
.purple {
  background: #f1eaff;
  color: #7453ae;
}
.setting-content {
  flex: 1;
  margin-left: 20rpx;
}
.setting-title {
  margin-bottom: 4rpx;
  font-size: 27rpx;
  font-weight: 650;
}
.setting-arrow {
  color: #a2afaa;
  font-size: 36rpx;
}
.logout {
  margin-top: 20rpx;
  border: 1rpx solid #f1d6d2;
  border-radius: 20rpx;
  background: #fff;
  color: #b7473a;
}
.version {
  padding: 32rpx;
  color: #a0aca8;
  text-align: center;
  font-size: 20rpx;
}
</style>

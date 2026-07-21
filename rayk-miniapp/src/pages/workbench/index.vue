<template>
  <view class="page workbench-page">
    <view class="workbench-hero">
      <view class="hero-pattern" />
      <view class="eyebrow-light">MY WORKBENCH</view>
      <view class="hero-title">{{ workbenchName }}</view>
      <view class="hero-copy">{{ workbenchDescription }}</view>
      <view class="hero-stats">
        <view
          ><text>{{ visibleMenus.length }}</text
          ><text>项可用服务</text></view
        >
        <view class="divider" />
        <view
          ><text>{{ auth.permissions.length }}</text
          ><text>项授权能力</text></view
        >
      </view>
    </view>

    <view class="section-head">
      <view>
        <view class="eyebrow">APPLICATIONS</view>
        <view class="section-title">全部功能</view>
      </view>
      <view class="soft-button" @click="goSwitch">切换身份</view>
    </view>

    <view class="menu-grid">
      <view
        v-for="(item, index) in visibleMenus"
        :key="item.route"
        class="menu-card"
        @click="open(item.route)"
      >
        <view class="menu-icon" :class="`tone-${index % 4}`">{{ item.icon }}</view>
        <view class="menu-title">{{ item.title }}</view>
        <view class="menu-copy">{{ item.description }}</view>
        <view class="menu-footer"><text>立即进入</text><text class="arrow">›</text></view>
      </view>
    </view>

    <view class="help-card" @click="goSupport">
      <view class="help-icon">?</view>
      <view class="help-content">
        <view class="help-title">找不到需要的功能？</view>
        <view class="muted">查看使用指南或提交问题反馈</view>
      </view>
      <view class="help-arrow">›</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { menusFor } from '@/constants/menus'
import type { Role } from '@/types/api'

const auth = useAuthStore()
const roleNames: Record<Role, string> = {
  PLATFORM_ADMIN: '平台管理工作台',
  TENANT_ADMIN: '机构管理工作台',
  DOCTOR: '医生工作台',
  HEALTH_MANAGER: '健康管理工作台',
  CUSTOMER: '个人健康中心',
}
const roleDescriptions: Record<Role, string> = {
  PLATFORM_ADMIN: '查看平台运营、机构与关键审计信息',
  TENANT_ADMIN: '统筹机构客户、员工与健康服务流程',
  DOCTOR: '集中处理评估审核与健康报告发布',
  HEALTH_MANAGER: '高效管理客户报告、指标与随访任务',
  CUSTOMER: '管理个人档案、报告、评估与随访反馈',
}
const workbenchName = computed(() =>
  auth.currentWorkbench ? roleNames[auth.currentWorkbench] : '我的工作台',
)
const workbenchDescription = computed(() =>
  auth.currentWorkbench
    ? roleDescriptions[auth.currentWorkbench]
    : '功能入口随角色、权限和当前工作台变化',
)
const visibleMenus = computed(() =>
  menusFor(auth.currentWorkbench).filter(
    (item) => !item.permission || auth.permissions.includes(item.permission),
  ),
)
const open = (url: string) => uni.navigateTo({ url })
const goSwitch = () => uni.navigateTo({ url: '/pages/switch-workbench/index' })
const goSupport = () => uni.navigateTo({ url: '/pages/support/index' })
</script>

<style scoped>
.workbench-page {
  padding-top: 24rpx;
}
.workbench-hero {
  position: relative;
  overflow: hidden;
  padding: 38rpx 36rpx;
  border-radius: 38rpx;
  background: linear-gradient(145deg, #173f37 0%, #0c6c57 100%);
  color: #fff;
  box-shadow: 0 20rpx 44rpx rgba(16, 78, 64, 0.2);
}
.hero-pattern {
  position: absolute;
  top: -90rpx;
  right: -70rpx;
  width: 280rpx;
  height: 280rpx;
  border: 50rpx solid rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}
.eyebrow-light {
  position: relative;
  color: #91ddc7;
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}
.hero-title {
  position: relative;
  margin-top: 16rpx;
  font-size: 44rpx;
  font-weight: 760;
}
.hero-copy {
  position: relative;
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.7);
  font-size: 24rpx;
}
.hero-stats {
  position: relative;
  display: flex;
  align-items: center;
  margin-top: 34rpx;
  padding-top: 26rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.14);
}
.hero-stats > view:not(.divider) {
  display: flex;
  align-items: baseline;
}
.hero-stats text:first-child {
  margin-right: 9rpx;
  font-size: 34rpx;
  font-weight: 750;
}
.hero-stats text:last-child {
  color: rgba(255, 255, 255, 0.68);
  font-size: 21rpx;
}
.divider {
  width: 1rpx;
  height: 30rpx;
  margin: 0 28rpx;
  background: rgba(255, 255, 255, 0.22);
}
.menu-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}
.menu-card {
  display: flex;
  min-height: 264rpx;
  box-sizing: border-box;
  flex-direction: column;
  padding: 26rpx;
  border: 1rpx solid #e1ebe8;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(35, 78, 67, 0.06);
}
.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 66rpx;
  height: 66rpx;
  border-radius: 22rpx;
  background: #e3f6f0;
  color: #0f765f;
  font-size: 23rpx;
  font-weight: 760;
}
.menu-icon.tone-1 {
  background: #eaf1ff;
  color: #416fba;
}
.menu-icon.tone-2 {
  background: #fff1d7;
  color: #a36806;
}
.menu-icon.tone-3 {
  background: #f1eaff;
  color: #7450b4;
}
.menu-title {
  margin-top: 22rpx;
  font-size: 29rpx;
  font-weight: 700;
}
.menu-copy {
  flex: 1;
  margin-top: 8rpx;
  color: #7a8984;
  font-size: 22rpx;
  line-height: 1.5;
}
.menu-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
  color: #8a9893;
  font-size: 20rpx;
}
.arrow {
  color: #0f7a62;
  font-size: 34rpx;
}
.help-card {
  display: flex;
  align-items: center;
  margin-top: 26rpx;
  padding: 25rpx 28rpx;
  border: 1rpx dashed #cbdad5;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.55);
}
.help-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58rpx;
  height: 58rpx;
  margin-right: 20rpx;
  border-radius: 19rpx;
  background: #e9f3f0;
  color: #5e776f;
  font-weight: 750;
}
.help-title {
  margin-bottom: 4rpx;
  font-size: 25rpx;
  font-weight: 650;
}
.help-content {
  flex: 1;
}
.help-arrow {
  color: #7f918b;
  font-size: 38rpx;
}
</style>

<template>
  <view class="page mine-page" :class="{ 'elder-page': isCustomer || isGuest }">
    <view class="profile-card" :class="{ clickable: canSwitch }" @click="canSwitch && goSwitch()">
      <view class="profile-pattern" />
      <view class="profile-top">
        <view class="avatar">{{ avatarText }}</view>
        <view class="profile-content">
          <view class="profile-name">{{ profileDisplayName }}</view>
          <view class="profile-meta">{{ tenantDisplayName }}</view>
          <view class="role-pill">{{ workbenchName }}</view>
        </view>
        <view v-if="canSwitch" class="profile-arrow">›</view>
      </view>
      <view v-if="!isGuest" class="profile-stats">
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

    <view v-if="isGuest" class="card guest-account-card">
      <view class="guest-account-title">登录后管理个人健康服务</view>
      <view class="guest-account-copy">登录后可保存健康档案、上传检验报告，并查看本人的评估和随访进度。</view>
      <button class="guest-account-button" hover-class="guest-account-button-hover" @click="goLogin">
        选择登录
      </button>
    </view>

    <view v-if="!isPlatform && !isGuest" class="section-head">
      <view>
        <view class="section-title">账号与服务</view>
      </view>
    </view>
    <view v-if="!isPlatform && !isGuest" class="card settings-card">
      <view v-if="isCustomer" class="setting" @click="goAssistant">
        <view class="setting-icon cyan">助</view>
        <view class="setting-content">
          <view class="setting-title">健康助手</view>
          <view class="muted">结合你的健康资料进行问答</view>
        </view>
        <view class="setting-arrow">›</view>
      </view>
      <view v-if="isCustomer" class="setting" @click="goMembership">
        <view class="setting-icon green">会</view>
        <view class="setting-content">
          <view class="setting-title">健康会员</view>
          <view class="muted">查看会员权益与使用记录</view>
        </view>
        <view class="setting-arrow">›</view>
      </view>
      <view class="setting" @click="goMessages">
        <view class="setting-icon blue">信</view>
        <view class="setting-content">
          <view class="setting-title">我的消息</view>
          <view class="muted">查看报告发布、随访提醒和健康动态</view>
        </view>
        <view class="setting-arrow">›</view>
      </view>
      <view v-if="isCustomer && goldBeanEnabled" class="setting" @click="goGoldBean">
        <view class="setting-icon amber">豆</view>
        <view class="setting-content">
          <view class="setting-title">俱乐部</view>
          <view class="muted">金豆成长、会员权益与奖励规则</view>
        </view>
        <view class="setting-arrow">›</view>
      </view>
      <view class="setting last" @click="goSupport">
        <view class="setting-icon purple">帮</view>
        <view class="setting-content">
          <view class="setting-title">帮助与反馈</view>
          <view class="muted">使用指南与问题反馈</view>
        </view>
        <view class="setting-arrow">›</view>
      </view>
    </view>

    <button v-if="!isGuest" class="logout" @click="signOut">退出当前账号</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyProfile } from '@/api/patient'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types/api'
import { goldBeanEnabled } from '@/constants/features'

const auth = useAuthStore()
const roleNames: Record<Role, string> = {
  PLATFORM_ADMIN: '平台管理员',
  DOCTOR: '医生工作台',
  CUSTOMER: '个人健康中心',
}
const isGuest = computed(() => !auth.isLoggedIn)
const isCustomer = computed(() => !isGuest.value && auth.currentWorkbench === 'CUSTOMER')
const workbenchName = computed(() =>
  isGuest.value ? '游客体验' : auth.currentWorkbench ? roleNames[auth.currentWorkbench] : '当前工作台',
)
const tenantDisplayName = computed(() =>
  isGuest.value ? '可先浏览公开健康服务' : '三羊健康平台',
)
const canSwitch = computed(() => (auth.user?.availableWorkbenches.length || 0) > 1)
const goSwitch = () => uni.navigateTo({ url: '/pages/switch-workbench/index' })
const goSupport = () => uni.navigateTo({ url: '/pages/support/index' })
const goAssistant = () => uni.navigateTo({ url: '/pages-customer/medical-assistant/index' })
const goMembership = () => uni.navigateTo({ url: '/pages-customer/member/index' })
const goMessages = () => uni.navigateTo({ url: '/pages/message/index' })
const goGoldBean = () => uni.switchTab({ url: '/pages/club/index' })
const isPlatform = computed(() => !isGuest.value && auth.currentWorkbench === 'PLATFORM_ADMIN')
const profileName = ref('')
const profileDisplayName = computed(() => {
  if (isGuest.value) return '游客'
  if (isCustomer.value) return profileName.value || '三羊健康用户'
  return auth.user?.displayName?.trim() || '三羊健康用户'
})
const avatarText = computed(() => profileDisplayName.value.slice(0, 1) || 'R')

async function loadProfileName() {
  if (!isCustomer.value) {
    profileName.value = ''
    return
  }
  try {
    const profile = await getMyProfile()
    profileName.value = profile?.name?.trim() || ''
  } catch {
    profileName.value = ''
  }
}

onShow(() => void loadProfileName())

async function signOut() {
  await auth.signOut()
  uni.reLaunch({ url: '/pages/home/index' })
}
const goLogin = () => uni.navigateTo({ url: '/pages/login/index?from=guest' })
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
  line-height: 1.35;
  font-weight: 750;
}
.profile-meta {
  margin-top: 5rpx;
  color: rgba(255, 255, 255, 0.65);
  font-size: 22rpx;
  line-height: 1.4;
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
  line-height: 1.2;
  font-weight: 750;
}
.profile-stats text:last-child {
  margin-top: 5rpx;
  color: rgba(255, 255, 255, 0.6);
  font-size: 20rpx;
  line-height: 1.4;
}
.guest-account-card {
  padding: 34rpx 30rpx 30rpx;
}
.guest-account-title {
  color: #23473d;
  font-size: 32rpx;
  line-height: 1.45;
  font-weight: 730;
}
.guest-account-copy {
  margin-top: 10rpx;
  color: #71867e;
  font-size: 25rpx;
  line-height: 1.65;
}
.guest-account-button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 88rpx;
  margin: 24rpx 0 0;
  border: 0;
  border-radius: 22rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 29rpx;
  line-height: 1.4;
  font-weight: 720;
}
.guest-account-button::after {
  border: 0;
}
.guest-account-button-hover {
  opacity: 0.86;
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
.cyan {
  background: #e2f7f7;
  color: #168991;
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
  display: flex;
  justify-content: center;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  margin-left: 20rpx;
}
.setting-title {
  margin-bottom: 4rpx;
  font-size: 27rpx;
  line-height: 1.4;
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
</style>

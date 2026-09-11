<template>
  <GoldBeanAdminPanel v-if="showPlatformAdmin" ref="adminPage" />
  <template v-else-if="showGoldBean">
    <!-- Mount both customer panels together so the first tab display always has a stable ref. -->
    <LegendaryClubPanel v-show="showLegendary" ref="legendaryPage" />
    <GoldBeanPanel v-show="!showLegendary" ref="goldBeanPage" />
  </template>

  <view v-else-if="goldBeanEnabled" class="page club-page">
    <view class="club-card">
      <view class="club-title">俱乐部</view>
      <view class="club-copy">
        {{ isGuest ? '登录后即可进入金豆会员俱乐部，查看会员权益。' : '俱乐部金豆服务面向普通会员开放。' }}
      </view>
      <button v-if="isGuest" class="club-button" hover-class="club-button-hover" @click="goLogin">
        登录后进入
      </button>
      <button v-else class="club-button secondary" hover-class="club-button-hover" @click="goWorkbench">
        返回工作台
      </button>
    </view>
  </view>

  <MessagePanel v-else ref="messagePage" />
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { goldBeanEnabled } from '@/constants/features'
import { useAuthStore } from '@/stores/auth'
import { getGoldBeanLegendarySummary } from '@/api/gold-bean'
import GoldBeanAdminPanel from '@/components/GoldBeanAdminPanel.vue'
import GoldBeanPanel from '@/components/GoldBeanPanel.vue'
import LegendaryClubPanel from '@/components/LegendaryClubPanel.vue'
import MessagePanel from '@/components/MessagePanel.vue'

type RefreshablePage = { refresh: () => Promise<void> }

const auth = useAuthStore()
const isGuest = computed(() => !auth.isLoggedIn)
const showGoldBean = computed(
  () => goldBeanEnabled && !isGuest.value && auth.currentWorkbench === 'CUSTOMER',
)
const showPlatformAdmin = computed(
  () => goldBeanEnabled && !isGuest.value && auth.currentWorkbench === 'PLATFORM_ADMIN',
)
const legendaryEligible = ref(false)
const showLegendary = computed(() => showGoldBean.value && legendaryEligible.value)
const adminPage = ref<RefreshablePage | null>(null)
const goldBeanPage = ref<RefreshablePage | null>(null)
const legendaryPage = ref<RefreshablePage | null>(null)
const messagePage = ref<RefreshablePage | null>(null)

async function refreshActivePage() {
  uni.setNavigationBarTitle({
    title: showPlatformAdmin.value ? '金豆会员运营' : goldBeanEnabled ? '俱乐部' : '消息',
  })
  await nextTick()
  if (showPlatformAdmin.value) {
    legendaryEligible.value = false
    await adminPage.value?.refresh()
  } else if (showGoldBean.value) {
    try {
      legendaryEligible.value = (await getGoldBeanLegendarySummary()).eligible
    } catch {
      legendaryEligible.value = false
    }
    await nextTick()
    if (showLegendary.value) await legendaryPage.value?.refresh()
    else await goldBeanPage.value?.refresh()
  } else if (!goldBeanEnabled) {
    legendaryEligible.value = false
    await messagePage.value?.refresh()
  } else {
    legendaryEligible.value = false
  }
}

onShow(() => void refreshActivePage())

const goLogin = () => uni.navigateTo({ url: '/pages/login/index?from=club' })
const goWorkbench = () => uni.switchTab({ url: '/pages/workbench/index' })
</script>

<style scoped>
.club-page {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 120rpx;
}
.club-card {
  width: 100%;
  box-sizing: border-box;
  padding: 40rpx 36rpx 40rpx;
  border: 1rpx solid #d6e9e2;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16rpx 38rpx rgba(20, 78, 63, 0.09);
  text-align: center;
}
.club-title {
  color: #17332d;
  font-size: 38rpx;
  line-height: 1.35;
  font-weight: 760;
}
.club-copy {
  margin-top: 14rpx;
  color: #71817b;
  font-size: 26rpx;
  line-height: 1.7;
}
.club-button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 92rpx;
  margin: 32rpx 0 0;
  border: 0;
  border-radius: 24rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 29rpx;
  font-weight: 720;
}
.club-button.secondary {
  background: #e8f7f2;
  color: #0f765f;
}
.club-button::after {
  border: 0;
}
.club-button-hover {
  opacity: 0.84;
}
</style>

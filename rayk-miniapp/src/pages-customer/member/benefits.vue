<template>
  <view class="page member-page">
    <view class="title">会员权益</view>
    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view v-for="item in items" :key="item.benefitCode" class="card">
        <view class="section-title">{{ item.benefitName }}</view>
        <view class="muted">{{ item.description }}</view>
        <view class="count">{{ displayRemaining(item) }}</view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import PageState from '@/components/PageState.vue'
import { getMembershipBenefits, type MembershipBenefit } from '@/api/membership'

const loading = ref(true)
const error = ref('')
const items = ref<MembershipBenefit[]>([])

onShow(async () => {
  try {
    items.value = await getMembershipBenefits()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '权益加载失败'
  } finally {
    loading.value = false
  }
})

const displayRemaining = (item: MembershipBenefit) =>
  item.remaining == null ? (item.available ? '已解锁' : '未解锁') : `剩余 ${item.remaining} 次`
</script>

<style scoped>
.member-page { min-height: 100vh; padding: 24rpx; background: #f2faf7; }
.title { padding: 12rpx 4rpx 20rpx; color: #153b34; font-size: 42rpx; font-weight: 700; }
.card { margin-bottom: 18rpx; padding: 26rpx; border-radius: 24rpx; background: #fff; }
.section-title { color: #153b34; font-size: 30rpx; font-weight: 700; }
.muted { margin-top: 10rpx; color: #748b84; font-size: 24rpx; }
.count { margin-top: 18rpx; color: #0f8066; font-size: 25rpx; }
</style>

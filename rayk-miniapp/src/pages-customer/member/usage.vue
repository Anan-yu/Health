<template>
  <view class="page member-page">
    <view class="title">权益使用记录</view>
    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view v-for="item in items" :key="`${item.benefitCode}-${item.occurredAt}`" class="card">
        <view class="row">
          <view class="section-title">{{ item.benefitName || item.benefitCode }}</view>
          <view class="status">{{ item.usageStatus }}</view>
        </view>
        <view class="muted">{{ item.bizType }} · {{ formatDate(item.occurredAt) }}</view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import PageState from '@/components/PageState.vue'
import { getMembershipUsage, type MembershipUsage } from '@/api/membership'

const loading = ref(true)
const error = ref('')
const items = ref<MembershipUsage[]>([])

onShow(async () => {
  try {
    items.value = await getMembershipUsage()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '记录加载失败'
  } finally {
    loading.value = false
  }
})

const formatDate = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '')
</script>

<style scoped>
.member-page { min-height: 100vh; padding: 24rpx; background: #f2faf7; }
.title { padding: 12rpx 4rpx 20rpx; color: #153b34; font-size: 42rpx; font-weight: 700; }
.card { margin-bottom: 18rpx; padding: 26rpx; border-radius: 24rpx; background: #fff; }
.row { display: flex; justify-content: space-between; align-items: center; }
.section-title { color: #153b34; font-size: 28rpx; font-weight: 700; }
.status { color: #0f8066; font-size: 23rpx; }
.muted { margin-top: 10rpx; color: #748b84; font-size: 24rpx; }
</style>

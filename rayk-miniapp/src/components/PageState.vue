<template>
  <view v-if="loading" class="state">
    <view class="spinner" />
    <view class="state-title">正在加载</view>
    <view class="state-copy">健康数据整理中，请稍候</view>
  </view>
  <view v-else-if="error" class="state error-state">
    <view class="state-icon">!</view>
    <view class="state-title">加载遇到问题</view>
    <view class="state-copy">{{ error }}</view>
  </view>
  <view v-else-if="empty" class="state empty-state">
    <view class="state-title">暂无数据</view>
    <view class="state-copy">完成相关操作后，内容会显示在这里</view>
  </view>
  <slot v-else />
</template>

<script setup lang="ts">
defineProps<{ loading?: boolean; error?: string; empty?: boolean }>()
</script>

<style scoped>
.state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 380rpx;
  padding: 80rpx 30rpx;
  box-sizing: border-box;
  flex-direction: column;
  text-align: center;
}
.spinner {
  width: 54rpx;
  height: 54rpx;
  border: 6rpx solid #dcebe6;
  border-top-color: #0f7a62;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}
.state-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 76rpx;
  border-radius: 26rpx;
  background: #fff0ee;
  color: #bd4d40;
  font-size: 34rpx;
  font-weight: 750;
}
.state-title {
  margin-top: 24rpx;
  color: #28443c;
  font-size: 29rpx;
  font-weight: 680;
}
.empty-state .state-title {
  margin-top: 0;
}
.state-copy {
  max-width: 480rpx;
  margin-top: 10rpx;
  color: #8a9893;
  font-size: 23rpx;
  line-height: 1.6;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

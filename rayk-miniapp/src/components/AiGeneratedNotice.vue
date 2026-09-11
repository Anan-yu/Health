<template>
  <view class="ai-generated-notice" role="note">
    <view class="ai-generated-head">
      <view class="ai-generated-title">人工智能生成内容</view>
      <view class="ai-generated-tag">重要提示</view>
    </view>
    <view class="ai-generated-copy">{{ description }}</view>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    description?: string
  }>(),
  {
    description: '本页面部分内容由人工智能生成，仅供健康管理参考，不作为临床诊断依据。',
  },
)
</script>

<style scoped>
.ai-generated-notice {
  margin: 0 24rpx 18rpx;
  padding: 20rpx 22rpx;
  border: 1rpx solid #f0d59b;
  border-radius: 22rpx;
  background: #fff8e8;
  box-sizing: border-box;
}
.ai-generated-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
}
.ai-generated-title {
  color: #6c4a13;
  font-size: 28rpx;
  font-weight: 800;
  line-height: 1.4;
}
.ai-generated-tag {
  flex: 0 0 auto;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  color: #9a6818;
  background: #ffedbf;
  font-size: 21rpx;
  line-height: 1.35;
}
.ai-generated-copy {
  margin-top: 8rpx;
  color: #795f34;
  font-size: 24rpx;
  line-height: 1.55;
}
</style>

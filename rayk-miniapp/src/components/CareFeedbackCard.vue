<template>
  <view
    class="care-feedback"
    :class="[`tone-${tone}`, { clickable: !!actionText }]"
    @click="emit('action')"
  >
    <view class="care-icon">
      <view class="pulse-ring" />
      <text>{{ icon }}</text>
    </view>
    <view class="care-content">
      <view class="care-title">{{ title }}</view>
      <view class="care-message">{{ message }}</view>
      <view v-if="detail" class="care-detail">{{ detail }}</view>
    </view>
    <view v-if="actionText" class="care-action">{{ actionText }} ›</view>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    message: string
    detail?: string
    icon?: string
    tone?: 'life' | 'warm' | 'calm'
    actionText?: string
  }>(),
  {
    detail: '',
    icon: '心',
    tone: 'life',
    actionText: '',
  },
)

const emit = defineEmits<{ action: [] }>()
</script>

<style scoped>
.care-feedback {
  position: relative;
  display: flex;
  align-items: center;
  gap: 22rpx;
  overflow: hidden;
  margin: 24rpx 0 30rpx;
  padding: 26rpx 28rpx;
  border: 1rpx solid rgba(186, 224, 211, 0.86);
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 94% 0%, rgba(110, 214, 178, 0.2), transparent 34%),
    linear-gradient(135deg, #f7fffc, #eaf8f3);
  box-shadow: 0 14rpx 34rpx rgba(21, 91, 72, 0.08);
}
.care-feedback.clickable:active {
  transform: scale(0.992);
}
.care-feedback.tone-warm {
  border-color: rgba(237, 208, 142, 0.75);
  background:
    radial-gradient(circle at 94% 0%, rgba(255, 209, 111, 0.22), transparent 36%),
    linear-gradient(135deg, #fffdf8, #fff6e4);
}
.care-feedback.tone-calm {
  border-color: rgba(193, 215, 228, 0.8);
  background:
    radial-gradient(circle at 94% 0%, rgba(150, 201, 222, 0.2), transparent 35%),
    linear-gradient(135deg, #fbfdff, #edf6f9);
}
.care-icon {
  position: relative;
  display: flex;
  flex: 0 0 76rpx;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 76rpx;
  border-radius: 25rpx;
  background: linear-gradient(145deg, #159477, #087159);
  color: #fff;
  font-size: 27rpx;
  font-weight: 800;
  box-shadow: 0 10rpx 24rpx rgba(14, 124, 98, 0.2);
}
.tone-warm .care-icon {
  background: linear-gradient(145deg, #d69b2d, #ad7413);
  box-shadow: 0 10rpx 24rpx rgba(191, 128, 23, 0.18);
}
.tone-calm .care-icon {
  background: linear-gradient(145deg, #397b92, #245c70);
  box-shadow: 0 10rpx 24rpx rgba(48, 108, 130, 0.18);
}
.pulse-ring {
  position: absolute;
  width: 88rpx;
  height: 88rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.23);
  border-radius: 29rpx;
}
.care-content {
  flex: 1;
  min-width: 0;
}
.care-title {
  color: #173f35;
  font-size: 27rpx;
  font-weight: 760;
  line-height: 1.4;
}
.care-message {
  margin-top: 7rpx;
  color: #557168;
  font-size: 23rpx;
  line-height: 1.55;
}
.care-detail {
  margin-top: 8rpx;
  color: #82928c;
  font-size: 20rpx;
}
.care-action {
  flex: 0 0 auto;
  color: #0c765e;
  font-size: 22rpx;
  font-weight: 680;
}
</style>

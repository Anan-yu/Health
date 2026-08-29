<template>
  <view class="home-video-card" :class="{ 'is-empty': !videoSrc, 'has-error': hasError }">
    <view class="video-header">
      <view class="video-heading">
        <view class="video-kicker">三羊健康 · 首页视频</view>
        <view class="video-title">{{ title }}</view>
      </view>
      <view class="video-status" :class="{ 'video-status-ready': videoSrc && !hasError }">
        <text class="video-status-dot" />
        <text>{{ videoSrc && !hasError ? '可播放' : hasError ? '需检查' : '待配置' }}</text>
      </view>
    </view>

    <view v-if="videoSrc && !hasError" class="video-frame">
      <video
        :key="videoKey"
        class="video-player"
        :src="videoSrc"
        :poster="poster"
        controls
        show-play-btn
        show-center-play-btn
        show-fullscreen-btn
        enable-progress-gesture
        object-fit="cover"
        aria-label="首页健康介绍视频"
        @error="handleError"
      />
    </view>
    <view v-else class="video-placeholder" aria-label="首页视频尚未配置">
      <view class="video-placeholder-play" aria-hidden="true">
        <view class="play-triangle" />
      </view>
      <view class="video-placeholder-title">{{ hasError ? '视频暂时无法播放' : '首页视频播放栏' }}</view>
      <view class="video-placeholder-copy">
        {{ hasError ? '请检查视频地址或稍后重试' : '配置视频地址后即可播放' }}
      </view>
      <view v-if="hasError" class="video-retry" @click.stop="retry">重新加载</view>
    </view>

    <view class="video-footer">
      <view class="video-footer-mark" aria-hidden="true">视</view>
      <text>{{ hasError ? '视频加载失败，请稍后再试' : videoSrc ? '点击播放，了解三羊健康服务' : '视频内容即将上线' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string
    poster?: string
    title?: string
  }>(),
  {
    src: '',
    poster: '',
    title: '认识三羊健康',
  },
)

const videoSrc = props.src.trim()
const poster = props.poster.trim()
const hasError = ref(false)
const videoKey = ref(0)

const handleError = () => {
  hasError.value = true
}

const retry = () => {
  hasError.value = false
  videoKey.value += 1
}
</script>

<style scoped>
.home-video-card {
  position: relative;
  overflow: hidden;
  margin: 24rpx 0 30rpx;
  padding: 26rpx 28rpx 22rpx;
  border: 1rpx solid rgba(186, 224, 211, 0.86);
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 94% 0%, rgba(110, 214, 178, 0.2), transparent 34%),
    linear-gradient(135deg, #f7fffc, #eaf8f3);
  box-shadow: 0 14rpx 34rpx rgba(21, 91, 72, 0.08);
}
.home-video-card.has-error {
  border-color: rgba(237, 208, 142, 0.78);
  background:
    radial-gradient(circle at 94% 0%, rgba(255, 209, 111, 0.2), transparent 36%),
    linear-gradient(135deg, #fffdf8, #fff6e4);
}
.video-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}
.video-heading {
  min-width: 0;
}
.video-kicker {
  color: #0f7a62;
  font-size: 19rpx;
  line-height: 1.4;
  letter-spacing: 1rpx;
}
.video-title {
  overflow: hidden;
  margin-top: 5rpx;
  color: #173f35;
  font-size: 29rpx;
  line-height: 1.4;
  font-weight: 760;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.video-status {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  min-height: 48rpx;
  padding: 0 16rpx;
  border: 1rpx solid #e2ddd0;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.7);
  color: #917b4c;
  font-size: 20rpx;
  line-height: 1.4;
}
.video-status-ready {
  border-color: rgba(186, 224, 211, 0.9);
  background: rgba(229, 248, 240, 0.9);
  color: #0f7a62;
}
.video-status-dot {
  width: 12rpx;
  height: 12rpx;
  margin-right: 8rpx;
  border-radius: 50%;
  background: currentColor;
}
.video-frame,
.video-placeholder {
  width: 100%;
  height: 360rpx;
  overflow: hidden;
  border-radius: 22rpx;
  background: #17342c;
}
.video-player {
  display: block;
  width: 100%;
  height: 100%;
}
.video-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(135deg, rgba(8, 92, 73, 0.96), rgba(21, 147, 119, 0.92)),
    #0a6955;
  color: #fff;
  flex-direction: column;
  text-align: center;
}
.has-error .video-placeholder {
  background: linear-gradient(135deg, #8f6a2c, #bd9143);
}
.video-placeholder-play {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 84rpx;
  height: 84rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.64);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
}
.play-triangle {
  width: 0;
  height: 0;
  margin-left: 6rpx;
  border-top: 13rpx solid transparent;
  border-bottom: 13rpx solid transparent;
  border-left: 20rpx solid #fff;
}
.video-placeholder-title {
  margin-top: 20rpx;
  font-size: 28rpx;
  line-height: 1.4;
  font-weight: 720;
}
.video-placeholder-copy {
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 21rpx;
  line-height: 1.5;
}
.video-retry {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 150rpx;
  min-height: 76rpx;
  margin-top: 18rpx;
  padding: 0 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.64);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 23rpx;
  line-height: 1.4;
  font-weight: 680;
}
.video-retry:active {
  opacity: 0.82;
}
.video-footer {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  color: #6d8179;
  font-size: 20rpx;
  line-height: 1.45;
}
.video-footer-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 34rpx;
  height: 34rpx;
  margin-right: 9rpx;
  border-radius: 11rpx;
  background: #d9f1e8;
  color: #0f7a62;
  font-size: 18rpx;
  font-weight: 720;
}
.has-error .video-footer-mark {
  background: #f6e7c2;
  color: #966c23;
}
.home-video-card:active {
  opacity: 0.94;
}
.home-page.elder-page .home-video-card {
  margin-top: 28rpx;
  margin-bottom: 34rpx;
  padding: 30rpx 30rpx 26rpx;
  border-width: 2rpx;
  border-radius: 32rpx;
}
.home-page.elder-page .video-kicker {
  font-size: 23rpx;
}
.home-page.elder-page .video-title {
  font-size: 32rpx;
}
.home-page.elder-page .video-status {
  min-height: 56rpx;
  padding: 0 18rpx;
  font-size: 24rpx;
}
.home-page.elder-page .video-frame,
.home-page.elder-page .video-placeholder {
  height: 390rpx;
  border-radius: 24rpx;
}
.home-page.elder-page .video-placeholder-title {
  font-size: 32rpx;
}
.home-page.elder-page .video-placeholder-copy,
.home-page.elder-page .video-footer {
  font-size: 24rpx;
}
.home-page.elder-page .video-footer-mark {
  width: 42rpx;
  height: 42rpx;
  font-size: 22rpx;
}
</style>

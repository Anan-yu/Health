<template>
  <view class="page scan-page">
    <view class="scan-stage">
      <view class="stage-glow stage-glow-top" />
      <view class="stage-glow stage-glow-bottom" />

      <view class="stage-head">
        <view class="stage-copy">
          <view class="stage-title">面部健康检测</view>
          <view class="stage-subtitle">看向镜头，20 秒了解当前身体状态</view>
        </view>
        <view class="service-state">
          <view class="state-dot" />
          <text>即将开放</text>
        </view>
      </view>

      <view class="camera-zone">
        <view class="pulse-ring pulse-ring-one" />
        <view class="pulse-ring pulse-ring-two" />
        <view class="face-frame">
          <view class="corner corner-left-top" />
          <view class="corner corner-right-top" />
          <view class="corner corner-left-bottom" />
          <view class="corner corner-right-bottom" />
          <view class="portrait">
            <view class="portrait-head" />
            <view class="portrait-neck" />
            <view class="portrait-shoulders" />
          </view>
          <view class="scan-line" />
        </view>
      </view>

      <view class="capture-hint">
        <view class="capture-dot" />
        <text>请保持面部居中并自然注视镜头</text>
      </view>

      <view class="stage-meta">
        <view class="meta-item">
          <text class="meta-value">20</text>
          <text class="meta-unit">秒</text>
          <view class="meta-label">面部采集</view>
        </view>
        <view class="meta-divider" />
        <view class="meta-item">
          <text class="meta-value">10</text>
          <text class="meta-unit">秒</text>
          <view class="meta-label">结果分析</view>
        </view>
        <view class="meta-divider" />
        <view class="meta-item">
          <text class="meta-value">无</text>
          <view class="meta-label">额外设备</view>
        </view>
      </view>
    </view>

    <view class="result-card">
      <view class="card-title">一次检测，多项体征</view>
      <view class="card-copy">检测结果将与健康档案、问卷和体检报告共同完善健康画像。</view>
      <view class="indicator-grid">
        <view v-for="indicator in indicators" :key="indicator.name" class="indicator-item">
          <view class="indicator-icon">{{ indicator.icon }}</view>
          <view class="indicator-name">{{ indicator.name }}</view>
        </view>
      </view>
    </view>

    <view class="ready-card">
      <view class="ready-head">
        <view>
          <view class="card-title">开始前请确认</view>
          <view class="card-copy">良好的采集环境有助于获得更稳定的结果。</view>
        </view>
        <view class="ready-badge">约 30 秒</view>
      </view>
      <view class="ready-list">
        <view v-for="item in readyItems" :key="item.title" class="ready-item">
          <view class="ready-check">✓</view>
          <view>
            <view class="ready-title">{{ item.title }}</view>
            <view class="ready-copy">{{ item.copy }}</view>
          </view>
        </view>
      </view>
    </view>

    <button class="start-button" :disabled="true">开始健康检测</button>
    <view class="status-copy">检测服务接入完成后即可使用</view>
    <view class="privacy-note">
      <view class="privacy-shield">安</view>
      <text>检测数据加密传输，仅用于健康分析</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const indicators = [
  { icon: '心', name: '心率' },
  { icon: '压', name: '血压' },
  { icon: '氧', name: '血氧' },
  { icon: '呼', name: '呼吸' },
  { icon: '变', name: '心率变异性' },
]

const readyItems = [
  { title: '光线均匀', copy: '避免强光、逆光或环境过暗' },
  { title: '面部无遮挡', copy: '取下口罩、帽子和有色眼镜' },
  { title: '保持稳定', copy: '采集时请勿说话或大幅移动' },
]
</script>

<style scoped>
.scan-page {
  padding-top: 24rpx;
  padding-bottom: calc(54rpx + env(safe-area-inset-bottom));
}
.scan-stage {
  position: relative;
  overflow: hidden;
  padding: 36rpx 32rpx 32rpx;
  border-radius: 38rpx;
  background:
    radial-gradient(circle at 50% 48%, rgba(73, 222, 177, 0.18), transparent 35%),
    linear-gradient(150deg, #083e36 0%, #075849 54%, #08705a 100%);
  color: #fff;
  box-shadow: 0 24rpx 52rpx rgba(7, 76, 63, 0.2);
}
.stage-glow {
  position: absolute;
  border-radius: 50%;
  background: rgba(94, 224, 184, 0.08);
}
.stage-glow-top {
  top: -150rpx;
  right: -120rpx;
  width: 360rpx;
  height: 360rpx;
  border: 42rpx solid rgba(142, 255, 219, 0.05);
}
.stage-glow-bottom {
  bottom: -180rpx;
  left: -130rpx;
  width: 330rpx;
  height: 330rpx;
}
.stage-head {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}
.stage-copy {
  min-width: 0;
}
.stage-title {
  font-size: 39rpx;
  line-height: 1.25;
  font-weight: 760;
  letter-spacing: -1rpx;
}
.stage-subtitle {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.7);
  font-size: 23rpx;
  line-height: 1.5;
}
.service-state {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  min-height: 48rpx;
  padding: 0 18rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.82);
  font-size: 20rpx;
}
.state-dot {
  width: 10rpx;
  height: 10rpx;
  margin-right: 10rpx;
  border-radius: 50%;
  background: #ffd37b;
  box-shadow: 0 0 18rpx rgba(255, 211, 123, 0.7);
}
.camera-zone {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 330rpx;
  margin-top: 12rpx;
}
.pulse-ring {
  position: absolute;
  border: 1rpx solid rgba(110, 245, 204, 0.13);
  border-radius: 50%;
}
.pulse-ring-one {
  width: 274rpx;
  height: 274rpx;
}
.pulse-ring-two {
  width: 330rpx;
  height: 330rpx;
}
.face-frame {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 218rpx;
  height: 260rpx;
}
.corner {
  position: absolute;
  width: 42rpx;
  height: 42rpx;
  border-color: #6ce7bf;
  border-style: solid;
}
.corner-left-top {
  top: 0;
  left: 0;
  border-width: 4rpx 0 0 4rpx;
  border-radius: 16rpx 0 0;
}
.corner-right-top {
  top: 0;
  right: 0;
  border-width: 4rpx 4rpx 0 0;
  border-radius: 0 16rpx 0 0;
}
.corner-left-bottom {
  bottom: 0;
  left: 0;
  border-width: 0 0 4rpx 4rpx;
  border-radius: 0 0 0 16rpx;
}
.corner-right-bottom {
  right: 0;
  bottom: 0;
  border-width: 0 4rpx 4rpx 0;
  border-radius: 0 0 16rpx;
}
.portrait {
  position: relative;
  width: 150rpx;
  height: 194rpx;
}
.portrait-head {
  position: absolute;
  top: 5rpx;
  left: 50%;
  z-index: 2;
  width: 94rpx;
  height: 118rpx;
  border: 2rpx solid rgba(179, 255, 232, 0.72);
  border-radius: 48% 48% 44% 44% / 42% 42% 55% 55%;
  background: linear-gradient(180deg, rgba(117, 235, 201, 0.22), rgba(89, 205, 172, 0.12));
  box-shadow:
    inset 0 0 30rpx rgba(103, 238, 198, 0.05),
    0 0 24rpx rgba(103, 238, 198, 0.08);
  transform: translateX(-50%);
}
.portrait-neck {
  position: absolute;
  top: 112rpx;
  left: 50%;
  z-index: 1;
  width: 42rpx;
  height: 35rpx;
  background: rgba(101, 219, 184, 0.15);
  transform: translateX(-50%);
}
.portrait-shoulders {
  position: absolute;
  bottom: 4rpx;
  left: 50%;
  width: 144rpx;
  height: 69rpx;
  overflow: hidden;
  transform: translateX(-50%);
}
.portrait-shoulders::before {
  position: absolute;
  top: 6rpx;
  left: 50%;
  box-sizing: border-box;
  width: 180rpx;
  height: 105rpx;
  border: 2rpx solid rgba(179, 255, 232, 0.58);
  border-radius: 50% 50% 0 0;
  background: linear-gradient(180deg, rgba(112, 231, 196, 0.18), rgba(112, 231, 196, 0.04) 70%);
  content: '';
  transform: translateX(-50%);
}
.scan-line {
  position: absolute;
  top: 121rpx;
  right: 16rpx;
  left: 16rpx;
  height: 2rpx;
  background: linear-gradient(90deg, transparent, #77efc7 16%, #d2ffef 50%, #77efc7 84%, transparent);
  box-shadow: 0 0 18rpx rgba(100, 242, 196, 0.88);
}
.capture-hint {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.74);
  font-size: 21rpx;
}
.capture-dot {
  width: 10rpx;
  height: 10rpx;
  margin-right: 10rpx;
  border-radius: 50%;
  background: #60e2b8;
  box-shadow: 0 0 16rpx rgba(96, 226, 184, 0.7);
}
.stage-meta {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-top: 28rpx;
  padding-top: 26rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.12);
}
.meta-item {
  flex: 1;
  text-align: center;
}
.meta-value {
  font-size: 34rpx;
  line-height: 1;
  font-weight: 760;
  font-variant-numeric: tabular-nums;
}
.meta-unit {
  margin-left: 3rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 20rpx;
}
.meta-label {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.56);
  font-size: 19rpx;
  line-height: 1.3;
}
.meta-divider {
  width: 1rpx;
  height: 50rpx;
  background: rgba(255, 255, 255, 0.14);
}
.result-card,
.ready-card {
  margin-top: 24rpx;
  padding: 30rpx;
  border: 1rpx solid rgba(214, 230, 224, 0.95);
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12rpx 32rpx rgba(30, 84, 69, 0.065);
}
.card-title {
  color: #173a32;
  font-size: 31rpx;
  line-height: 1.35;
  font-weight: 730;
}
.card-copy {
  margin-top: 7rpx;
  color: #74857f;
  font-size: 22rpx;
  line-height: 1.55;
}
.indicator-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx 12rpx;
  margin-top: 28rpx;
}
.indicator-item {
  display: flex;
  align-items: center;
  box-sizing: border-box;
  width: calc(33.333% - 8rpx);
  min-height: 70rpx;
  padding: 10rpx 12rpx;
  border-radius: 20rpx;
  background: #f1f8f5;
}
.indicator-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 42rpx;
  height: 42rpx;
  margin-right: 10rpx;
  border-radius: 14rpx;
  background: #d8f1e8;
  color: #08715b;
  font-size: 18rpx;
  font-weight: 750;
}
.indicator-name {
  min-width: 0;
  color: #29483f;
  font-size: 20rpx;
  line-height: 1.25;
  font-weight: 650;
}
.ready-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}
.ready-badge {
  flex: 0 0 auto;
  padding: 9rpx 16rpx;
  border-radius: 999rpx;
  background: #fff1d5;
  color: #9a6612;
  font-size: 19rpx;
  font-weight: 650;
}
.ready-list {
  margin-top: 25rpx;
  border-top: 1rpx solid #e8efec;
}
.ready-item {
  display: flex;
  align-items: center;
  min-height: 92rpx;
  border-bottom: 1rpx solid #e8efec;
}
.ready-item:last-child {
  border-bottom: 0;
}
.ready-check {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 42rpx;
  height: 42rpx;
  margin-right: 18rpx;
  border-radius: 50%;
  background: #e4f5ef;
  color: #0d7c63;
  font-size: 20rpx;
  font-weight: 760;
}
.ready-title {
  color: #29443d;
  font-size: 23rpx;
  line-height: 1.35;
  font-weight: 670;
}
.ready-copy {
  margin-top: 3rpx;
  color: #87958f;
  font-size: 20rpx;
  line-height: 1.4;
}
.start-button {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 96rpx;
  margin-top: 28rpx;
  border: 0;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #11886c 0%, #086550 100%);
  color: #fff;
  font-size: 29rpx;
  font-weight: 720;
  box-shadow: 0 15rpx 32rpx rgba(11, 111, 87, 0.2);
}
.start-button[disabled] {
  background: #dfe9e5;
  color: #86948f;
  box-shadow: none;
}
.start-button::after {
  display: none;
}
.status-copy {
  margin-top: 12rpx;
  color: #87958f;
  font-size: 20rpx;
  line-height: 1.4;
  text-align: center;
}
.privacy-note {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 26rpx;
  color: #71847d;
  font-size: 20rpx;
  line-height: 1.4;
}
.privacy-shield {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34rpx;
  height: 34rpx;
  margin-right: 10rpx;
  border-radius: 11rpx;
  background: #e7f3ef;
  color: #16735f;
  font-size: 17rpx;
  font-weight: 750;
}
</style>

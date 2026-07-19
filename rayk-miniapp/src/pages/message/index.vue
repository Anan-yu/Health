<template>
  <view class="page message-page">
    <view class="page-heading">
      <view>
        <view class="eyebrow">NOTIFICATIONS</view>
        <view class="title">消息中心</view>
        <view class="subtitle">任务进度与重要动态都会在这里提醒</view>
      </view>
      <view class="message-count">2</view>
    </view>

    <view class="filter-row">
      <view class="filter active">全部</view>
      <view class="filter">业务提醒</view>
      <view class="filter">系统通知</view>
    </view>

    <view class="card notification unread">
      <view class="notification-icon report-icon">报</view>
      <view class="notification-content">
        <view class="notification-head">
          <view class="notification-title">健康报告已发布</view>
          <view class="unread-dot" />
        </view>
        <view class="notification-copy">新的健康管理报告已经生成，可前往报告中心查看。</view>
        <view class="notification-time">刚刚 · 业务提醒</view>
      </view>
      <view class="chevron">›</view>
    </view>

    <view class="card notification">
      <view class="notification-icon system-icon">系</view>
      <view class="notification-content">
        <view class="notification-title">欢迎使用 RayK A1</view>
        <view class="notification-copy">你的健康管理工作台已经准备就绪。</view>
        <view class="notification-time">今天 · 系统通知</view>
      </view>
      <view class="chevron">›</view>
    </view>

    <view class="message-footer">仅展示最近 30 天消息</view>
  </view>
</template>

<style scoped>
.message-page {
  padding-top: 34rpx;
}
.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 6rpx 32rpx;
}
.page-heading .title {
  margin-top: 8rpx;
}
.message-count {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  border-radius: 22rpx;
  background: #e4f6f0;
  color: #0d755d;
  font-size: 27rpx;
  font-weight: 750;
}
.filter-row {
  display: flex;
  gap: 14rpx;
  margin-bottom: 24rpx;
}
.filter {
  padding: 13rpx 24rpx;
  border: 1rpx solid #e1e9e6;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  color: #77857f;
  font-size: 23rpx;
}
.filter.active {
  border-color: #0f7a62;
  background: #0f7a62;
  color: #fff;
}
.notification {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
}
.notification.unread {
  border-color: #cfe9e0;
  box-shadow: 0 12rpx 34rpx rgba(23, 100, 81, 0.1);
}
.notification-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  font-size: 24rpx;
  font-weight: 750;
}
.report-icon {
  background: #e1f5ee;
  color: #0e765e;
}
.system-icon {
  background: #eaf1ff;
  color: #476eae;
}
.notification-content {
  flex: 1;
  min-width: 0;
  margin-left: 22rpx;
}
.notification-head {
  display: flex;
  align-items: center;
}
.notification-title {
  font-size: 28rpx;
  font-weight: 680;
}
.unread-dot {
  width: 12rpx;
  height: 12rpx;
  margin-left: 12rpx;
  border-radius: 50%;
  background: #ef6b56;
}
.notification-copy {
  margin-top: 8rpx;
  color: #70817b;
  font-size: 23rpx;
  line-height: 1.55;
}
.notification-time {
  margin-top: 12rpx;
  color: #a0aaa6;
  font-size: 20rpx;
}
.chevron {
  margin-left: 12rpx;
  color: #a4b0ac;
  font-size: 38rpx;
}
.message-footer {
  padding: 30rpx;
  color: #9aa6a2;
  text-align: center;
  font-size: 21rpx;
}
</style>

<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
onLaunch(() => useAuthStore().hydrate())
</script>
<style>
page {
  background: #f4f7f6;
  color: #152e29;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}
.page {
  padding: 28rpx;
}
.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 22rpx;
  box-shadow: 0 8rpx 30rpx rgba(30, 70, 60, 0.06);
}
.title {
  font-size: 38rpx;
  font-weight: 700;
  margin-bottom: 12rpx;
}
.subtitle {
  color: #718078;
  font-size: 26rpx;
  line-height: 1.6;
}
.section-title {
  font-size: 30rpx;
  font-weight: 650;
  margin: 30rpx 0 18rpx;
}
.primary {
  background: #176b57;
  color: #fff;
  border-radius: 18rpx;
}
.danger {
  background: #fff;
  color: #b42318;
  border: 1px solid #efb5b0;
  border-radius: 18rpx;
}
.input,
.textarea {
  background: #f5f7f6;
  border-radius: 16rpx;
  padding: 22rpx;
  margin: 14rpx 0;
}
.row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: center;
}
.muted {
  color: #7b8794;
  font-size: 24rpx;
}
.metric {
  font-size: 44rpx;
  font-weight: 700;
  color: #176b57;
}
</style>

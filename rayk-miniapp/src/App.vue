<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
onLaunch(() => useAuthStore().hydrate())
</script>
<style>
page {
  --brand: #0f7a62;
  --brand-dark: #095c4a;
  --brand-soft: #e8f7f2;
  --mint: #d6f3e9;
  --ink: #17332d;
  --muted: #71817b;
  --line: #e6eeeb;
  --warm: #fff7e8;
  min-height: 100%;
  background:
    radial-gradient(circle at 90% 3%, rgba(183, 235, 218, 0.42), transparent 23%),
    linear-gradient(180deg, #f5faf8 0%, #f3f7f6 46%, #f7f8f8 100%);
  color: var(--ink);
  font-family:
    -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', 'Segoe UI', sans-serif;
  font-size: 28rpx;
}
.page {
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
  padding: 30rpx 28rpx calc(54rpx + env(safe-area-inset-bottom));
}
@media screen and (min-width: 768px) {
  .page {
    max-width: 920rpx;
    margin: 0 auto;
  }
}
.card {
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.96);
  border: 1rpx solid rgba(220, 233, 228, 0.92);
  border-radius: 28rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(23, 71, 59, 0.07);
}
.title {
  font-size: 40rpx;
  line-height: 1.3;
  font-weight: 750;
  letter-spacing: -1rpx;
  margin-bottom: 10rpx;
}
.subtitle {
  color: var(--muted);
  font-size: 26rpx;
  line-height: 1.6;
}
.section-title {
  color: var(--ink);
  font-size: 30rpx;
  font-weight: 700;
  margin: 26rpx 0 18rpx;
}
.section-label {
  color: var(--brand);
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  text-transform: uppercase;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 34rpx 4rpx 20rpx;
}
.section-head .section-title {
  margin: 0;
}
.primary {
  background: linear-gradient(135deg, #11856a, #0b6b57);
  color: #fff;
  border: 0;
  border-radius: 20rpx;
  font-weight: 650;
  box-shadow: 0 12rpx 24rpx rgba(15, 122, 98, 0.2);
}
.danger {
  background: #fff;
  color: #b42318;
  border: 1rpx solid #f3d0cc;
  border-radius: 20rpx;
}
.input,
.textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 88rpx;
  background: #f7faf9;
  border: 1rpx solid #e1ebe7;
  border-radius: 20rpx;
  padding: 22rpx 24rpx;
  margin: 16rpx 0;
}
.row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: center;
}
.muted {
  color: var(--muted);
  font-size: 24rpx;
}
.metric {
  color: var(--brand);
  font-size: 48rpx;
  line-height: 1.15;
  font-weight: 760;
}
.icon-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 34rpx;
  font-weight: 750;
}
.soft-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 58rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 24rpx;
  font-weight: 650;
}
.eyebrow {
  color: var(--brand);
  font-size: 23rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}
.list-card {
  position: relative;
  overflow: hidden;
}
.list-card::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 6rpx;
  background: linear-gradient(180deg, #36b795, #0f7a62);
  content: '';
}
button {
  font-size: 28rpx;
}
button::after {
  border: 0;
}
progress {
  border-radius: 999rpx;
  overflow: hidden;
}
</style>

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
  --line: #dfeae6;
  --surface: rgba(255, 255, 255, 0.97);
  --shadow: 0 14rpx 38rpx rgba(20, 78, 63, 0.075);
  --warm: #fff7e8;
  min-height: 100%;
  background:
    radial-gradient(circle at 92% 2%, rgba(167, 230, 210, 0.5), transparent 25%),
    radial-gradient(circle at 4% 38%, rgba(224, 244, 237, 0.58), transparent 20%),
    linear-gradient(180deg, #f6fbf9 0%, #f2f7f5 52%, #f7f9f8 100%);
  color: var(--ink);
  font-family:
    -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', 'Segoe UI', sans-serif;
  font-size: 28rpx;
}
.page {
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
  padding: 32rpx 28rpx calc(58rpx + env(safe-area-inset-bottom));
}
@media screen and (min-width: 768px) {
  .page {
    max-width: 920rpx;
    margin: 0 auto;
  }
}
.card {
  box-sizing: border-box;
  background: var(--surface);
  border: 1rpx solid rgba(215, 230, 224, 0.94);
  border-radius: 30rpx;
  padding: 30rpx 30rpx 32rpx;
  margin-bottom: 24rpx;
  box-shadow: var(--shadow);
}
.title {
  font-size: 42rpx;
  line-height: 1.25;
  font-weight: 760;
  letter-spacing: -1rpx;
  margin-bottom: 12rpx;
}
.subtitle {
  color: var(--muted);
  font-size: 25rpx;
  line-height: 1.65;
}
.section-title {
  color: var(--ink);
  font-size: 32rpx;
  line-height: 1.35;
  font-weight: 730;
  margin: 28rpx 0 20rpx;
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
  margin: 38rpx 4rpx 22rpx;
}
.section-head .section-title {
  margin: 0;
}
.primary {
  background: linear-gradient(135deg, #11856a, #0b6b57);
  color: #fff;
  border: 0;
  min-height: 90rpx;
  border-radius: 24rpx;
  font-weight: 680;
  box-shadow: 0 14rpx 30rpx rgba(15, 122, 98, 0.2);
}
.danger {
  background: #fff;
  color: #b42318;
  border: 1rpx solid #f3d0cc;
  min-height: 84rpx;
  border-radius: 22rpx;
}
.input,
.textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 88rpx;
  background: rgba(250, 252, 251, 0.98);
  border: 1rpx solid #dce8e3;
  border-radius: 22rpx;
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
  font-variant-numeric: tabular-nums;
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
  border: 1rpx solid rgba(197, 226, 216, 0.75);
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
  font-weight: 650;
}
button::after {
  border: 0;
}
progress {
  border-radius: 999rpx;
  overflow: hidden;
}
</style>

<script setup lang="ts">
import { onHide, onLaunch, onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import {
  startVoiceReminderRuntime,
  stopVoiceReminderRuntime,
} from '@/utils/voice-reminder-runtime'

onLaunch(() => {
  useAuthStore().hydrate()
  startVoiceReminderRuntime()
})
onShow(startVoiceReminderRuntime)
onHide(stopVoiceReminderRuntime)
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
  line-height: 1.5;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
}
view,
text,
button,
input,
textarea,
navigator,
picker {
  font-family:
    -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', 'Segoe UI', sans-serif;
}
.page {
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
  padding: 32rpx 28rpx calc(58rpx + env(safe-area-inset-bottom));
}
.elder-page {
  --elder-text: 32rpx;
  --elder-muted: 27rpx;
  --elder-title: 46rpx;
  --elder-section: 36rpx;
  padding: 30rpx 24rpx calc(76rpx + env(safe-area-inset-bottom));
  font-size: var(--elder-text);
  line-height: 1.65;
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
.elder-page .card {
  padding: 34rpx 32rpx 36rpx;
  margin-bottom: 28rpx;
  border-width: 2rpx;
  border-radius: 32rpx;
  box-shadow: 0 16rpx 38rpx rgba(20, 78, 63, 0.09);
}
.title {
  font-size: 42rpx;
  line-height: 1.25;
  font-weight: 760;
  letter-spacing: -1rpx;
  margin-bottom: 12rpx;
  text-align: left;
}
.elder-page .title {
  margin-bottom: 16rpx;
  font-size: var(--elder-title);
  line-height: 1.32;
  letter-spacing: 0;
}
.subtitle {
  color: var(--muted);
  font-size: 25rpx;
  line-height: 1.65;
  text-align: left;
}
.elder-page .subtitle,
.elder-page .muted {
  color: #566d65;
  font-size: var(--elder-muted);
  line-height: 1.7;
}
.section-title {
  color: var(--ink);
  font-size: 32rpx;
  line-height: 1.35;
  font-weight: 730;
  margin: 28rpx 0 20rpx;
  text-align: left;
}
.elder-page .section-title {
  font-size: var(--elder-section);
  line-height: 1.4;
}
.section-label {
  color: var(--brand);
  font-size: 22rpx;
  line-height: 1.4;
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
.elder-page .section-head {
  margin: 42rpx 4rpx 24rpx;
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
.elder-page .primary,
.elder-page .primary-button,
.elder-page .submit-button,
.elder-page .download-button,
.elder-page .feedback-button {
  min-height: 104rpx;
  border-radius: 26rpx;
  font-size: 32rpx;
  line-height: 1.35;
  font-weight: 720;
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
.elder-page .input,
.elder-page .textarea,
.elder-page input,
.elder-page textarea,
.elder-page .picker-value {
  font-size: 30rpx;
  line-height: 1.55;
}
.elder-page .input,
.elder-page .textarea {
  min-height: 104rpx;
  padding: 26rpx 28rpx;
  border-width: 2rpx;
  border-radius: 24rpx;
}
.row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: center;
}
.elder-page .row {
  min-height: 64rpx;
  gap: 24rpx;
}
.muted {
  color: var(--muted);
  font-size: 24rpx;
  line-height: 1.55;
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
.elder-page .soft-button {
  min-height: 72rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
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
.elder-page button {
  min-height: 96rpx;
  font-size: 32rpx;
}
.elder-page button[disabled] {
  opacity: 0.58;
}
.elder-page .status,
.elder-page .pill,
.elder-page .filter {
  font-size: 27rpx;
}
.elder-page .filter {
  min-height: 70rpx;
  padding: 0 28rpx;
}
button::after {
  border: 0;
}
progress {
  border-radius: 999rpx;
  overflow: hidden;
}
</style>

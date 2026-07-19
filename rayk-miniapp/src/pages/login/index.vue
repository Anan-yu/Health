<template>
  <view class="page login-page">
    <view class="hero">
      <view class="orb orb-one" />
      <view class="orb orb-two" />
      <view class="brand-row">
        <view class="logo">A1</view>
        <view>
          <view class="brand-name">RayK A1</view>
          <view class="brand-tag">FUNCTIONAL HEALTH</view>
        </view>
      </view>
      <view class="hero-title">让每一份健康数据<br /><text>清晰、有序、可行动</text></view>
      <view class="hero-copy">连接检验报告、AI评估与专业健康管理服务</view>
      <view class="trust-row">
        <view><text class="trust-dot" />隐私保护</view>
        <view><text class="trust-dot" />专业复核</view>
        <view><text class="trust-dot" />持续随访</view>
      </view>
    </view>

    <view v-if="expired" class="notice">登录状态已过期，请重新登录</view>

    <!-- #ifdef MP-WEIXIN -->
    <view class="card login-card">
      <view class="card-kicker">欢迎使用</view>
      <view class="card-title">微信身份快捷登录</view>
      <view class="subtitle">安全识别当前微信身份，自动进入对应工作台</view>
      <button class="wechat" :loading="wechatLoading" @click="handleWeChatLogin">
        <text class="wechat-mark">微</text> 微信一键登录
      </button>
      <view class="agreement">登录即表示同意《用户服务协议》和《隐私政策》</view>
      <view v-if="wechatError" class="error">{{ wechatError }}</view>
    </view>
    <!-- #endif -->

    <view class="developer-trigger" @click="showDeveloper = !showDeveloper">
      <text>开发调试身份</text><text>{{ showDeveloper ? '收起' : '展开' }} ›</text>
    </view>
    <view v-if="showDeveloper" class="card development-card">
      <view class="row development-head">
        <view>
          <view class="card-title">选择体验身份</view>
          <view class="subtitle">用于本机功能调试</view>
        </view>
        <view class="dev-badge">DEV</view>
      </view>
      <view class="roles">
        <view
          v-for="item in accounts"
          :key="item.username"
          class="role"
          :class="{ active: username === item.username }"
          @click="username = item.username"
        >
          <view class="role-icon">{{ item.icon }}</view>
          <view class="role-content">
            <text class="role-name">{{ item.name }}</text>
            <text class="muted">{{ item.description }}</text>
          </view>
          <view class="selector"><text v-if="username === item.username">✓</text></view>
        </view>
      </view>
      <input v-model="password" class="input password" password placeholder="测试密码" />
      <button class="primary enter-button" :loading="loading" @click="handleLogin">
        进入 RayK A1
      </button>
      <view v-if="error" class="error">{{ error }}</view>
    </view>
    <view class="security-note">健康数据加密传输 · 关键操作全程留痕</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'

const accounts = [
  { username: 'platform_admin', icon: '平', name: '平台管理员', description: '平台基础查看' },
  { username: 'tenant_admin', icon: '机', name: '机构管理员', description: '机构与人员管理' },
  { username: 'doctor', icon: '医', name: '医生', description: '评估审核与发布' },
  { username: 'health_manager', icon: '健', name: '健康管理师', description: '客户、报告与随访' },
  { username: 'customer', icon: '客', name: '普通客户', description: '个人健康中心' },
]
const username = ref('doctor'),
  password = ref('RayK@123456'),
  loading = ref(false),
  wechatLoading = ref(false),
  showDeveloper = ref(false),
  error = ref(''),
  wechatError = ref(''),
  expired = ref(false)
const auth = useAuthStore()

// #ifdef H5
showDeveloper.value = true
// #endif

onLoad((query) => {
  expired.value = query?.expired === '1'
})

async function handleWeChatLogin() {
  wechatLoading.value = true
  wechatError.value = ''
  try {
    const result = await uni.login({ provider: 'weixin' })
    if (!result.code) throw new Error('微信未返回登录凭证')
    await auth.loginWithWeChat(result.code)
    uni.switchTab({ url: '/pages/home/index' })
  } catch (e) {
    wechatError.value = e instanceof Error ? e.message : '微信登录失败'
  } finally {
    wechatLoading.value = false
  }
}

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    uni.switchTab({ url: '/pages/home/index' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  padding-top: 0;
}
.hero {
  position: relative;
  overflow: hidden;
  margin: 0 -28rpx 28rpx;
  padding: calc(48rpx + env(safe-area-inset-top)) 42rpx 54rpx;
  border-radius: 0 0 54rpx 54rpx;
  background: linear-gradient(145deg, #075744 0%, #0c7960 55%, #23a27f 100%);
  color: #fff;
  box-shadow: 0 24rpx 48rpx rgba(9, 92, 74, 0.2);
}
.orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.09);
}
.orb-one {
  top: -80rpx;
  right: -30rpx;
  width: 290rpx;
  height: 290rpx;
}
.orb-two {
  right: 120rpx;
  bottom: -130rpx;
  width: 240rpx;
  height: 240rpx;
}
.brand-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18rpx;
}
.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 82rpx;
  height: 82rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.35);
  border-radius: 25rpx;
  background: rgba(255, 255, 255, 0.16);
  font-size: 32rpx;
  font-weight: 800;
  backdrop-filter: blur(12rpx);
}
.brand-name {
  font-size: 34rpx;
  font-weight: 750;
}
.brand-tag {
  margin-top: 2rpx;
  color: rgba(255, 255, 255, 0.66);
  font-size: 18rpx;
  letter-spacing: 3rpx;
}
.hero-title {
  position: relative;
  margin-top: 48rpx;
  font-size: 49rpx;
  line-height: 1.38;
  font-weight: 760;
}
.hero-title text {
  color: #c9f6e7;
}
.hero-copy {
  position: relative;
  margin-top: 18rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 25rpx;
}
.trust-row {
  position: relative;
  display: flex;
  gap: 24rpx;
  margin-top: 36rpx;
  color: rgba(255, 255, 255, 0.82);
  font-size: 22rpx;
}
.trust-dot {
  display: inline-block;
  width: 9rpx;
  height: 9rpx;
  margin-right: 9rpx;
  border-radius: 50%;
  background: #9bf0d5;
}
.login-card {
  padding: 36rpx;
}
.card-kicker {
  color: #0f7a62;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}
.card-title {
  margin: 8rpx 0;
  font-size: 34rpx;
  font-weight: 730;
}
.wechat {
  margin-top: 30rpx;
  background: linear-gradient(135deg, #08bd5c, #05a94f);
  color: #fff;
  border: 0;
  border-radius: 20rpx;
  font-weight: 650;
  box-shadow: 0 12rpx 26rpx rgba(7, 193, 96, 0.2);
}
.wechat-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36rpx;
  height: 36rpx;
  margin-right: 8rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  font-size: 20rpx;
}
.agreement {
  margin-top: 18rpx;
  color: #94a09c;
  text-align: center;
  font-size: 20rpx;
}
.developer-trigger {
  display: flex;
  justify-content: space-between;
  padding: 18rpx 10rpx 24rpx;
  color: #74847e;
  font-size: 24rpx;
}
.development-card {
  padding: 34rpx;
}
.development-head {
  margin-bottom: 24rpx;
}
.dev-badge {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #fff1cf;
  color: #8a6200;
  font-size: 20rpx;
  font-weight: 750;
}
.roles {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
.role {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 20rpx;
  border: 1rpx solid #e2ebe8;
  border-radius: 20rpx;
  background: #fbfcfc;
}
.role.active {
  border-color: #4eb89a;
  background: #edf9f5;
  box-shadow: inset 0 0 0 1rpx rgba(15, 122, 98, 0.08);
}
.role-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 58rpx;
  height: 58rpx;
  margin-right: 14rpx;
  border-radius: 18rpx;
  background: #eaf2ef;
  color: #3d665b;
  font-size: 23rpx;
  font-weight: 750;
}
.role.active .role-icon {
  background: #cceee2;
  color: #0b7058;
}
.role-content {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}
.role-name {
  overflow: hidden;
  margin-bottom: 4rpx;
  font-size: 25rpx;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role .muted {
  overflow: hidden;
  font-size: 20rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.selector {
  position: absolute;
  top: 10rpx;
  right: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  color: #0f7a62;
  font-size: 20rpx;
  font-weight: 800;
}
.password {
  margin-top: 24rpx;
}
.enter-button {
  margin-top: 18rpx;
}
.notice,
.error {
  padding: 18rpx;
  color: #b42318;
  text-align: center;
}
.notice {
  margin-bottom: 20rpx;
  border: 1rpx solid #f6cbc6;
  border-radius: 18rpx;
  background: #fff0ee;
}
.security-note {
  padding: 20rpx 0;
  color: #91a09b;
  text-align: center;
  font-size: 21rpx;
}
</style>

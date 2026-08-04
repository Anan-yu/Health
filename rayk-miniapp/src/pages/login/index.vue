<template>
  <view class="page login-page elder-page">
    <view class="hero">
      <view class="orb orb-one" />
      <view class="orb orb-two" />
      <view class="brand-row">
        <view class="logo">AI</view>
        <view>
          <view class="brand-name">致宇健康</view>
          <view class="brand-tag">AI 智能健康管理</view>
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
      <view class="card-title">{{ supportsPhoneLogin ? '微信授权手机号登录' : '微信一键登录' }}</view>
      <view v-if="!supportsPhoneLogin" class="login-card-tip">
        客户可通过微信一键登录，工作人员仅首次登录需要完成绑定。
      </view>
      <button
        v-if="supportsPhoneLogin"
        class="wechat"
        :loading="wechatLoading"
        :disabled="Boolean(identified)"
        open-type="getPhoneNumber"
        hover-class="wechat-hover"
        phone-number-no-quota-toast
        @getphonenumber="handleWeChatLogin"
      >
        微信一键登录
      </button>
      <button
        v-else
        class="wechat"
        :loading="wechatLoading"
        :disabled="Boolean(identified)"
        hover-class="wechat-hover"
        @click="handleWeChatLogin()"
      >
        微信一键登录
      </button>
      <view v-if="wechatLoading" class="recognizing">
        {{ supportsPhoneLogin ? '正在安全识别微信身份与授权手机号…' : '正在安全识别微信身份…' }}
      </view>
      <view v-if="identified" class="identified">
        <view class="identified-mark">✓</view>
        <view
          ><text>已识别：{{ identified.category }}</text
          ><text>将进入{{ identified.workbench }}</text></view
        >
      </view>
      <view class="agreement">登录即表示同意《用户服务协议》和《隐私政策》</view>
      <view v-if="wechatError" class="error">{{ wechatError }}</view>
    </view>
    <view v-if="!supportsPhoneLogin" class="card staff-login-card">
      <view class="staff-login-heading">
        <view class="card-title">工作人员登录</view>
        <view class="staff-login-badge">首次绑定</view>
      </view>
      <view class="staff-role-switch">
        <button
          class="staff-role-option"
          :class="{ active: staffLoginMode === 'admin' }"
          @click="staffLoginMode = 'admin'"
        >
          平台管理员
        </button>
        <button
          class="staff-role-option"
          :class="{ active: staffLoginMode === 'doctor' }"
          @click="staffLoginMode = 'doctor'"
        >
          医生
        </button>
      </view>
      <view class="staff-login-guide">
        <view class="staff-guide-row">
          <text class="staff-guide-index">1</text>
          <text>{{ staffLoginMode === 'admin' ? '填写账号密码并绑定当前微信' : '填写一次性绑定码并绑定当前微信' }}</text>
        </view>
        <view class="staff-guide-row staff-guide-row-muted">
          <text class="staff-guide-index">2</text>
          <text>绑定成功后，日常登录直接点击上方“微信一键登录”</text>
        </view>
      </view>
      <view v-if="staffLoginMode === 'admin'" class="staff-credentials">
        <input
          v-model="staffUsername"
          class="input staff-username-input"
          maxlength="50"
          placeholder="请输入平台管理员账号"
        />
        <input
          v-model="staffPassword"
          class="input staff-password-input"
          password
          maxlength="100"
          placeholder="请输入平台管理员密码"
        />
      </view>
      <input
        v-else
        v-model="staffInviteCode"
        class="input staff-invite-input"
        maxlength="10"
        placeholder="请输入 10 位绑定码"
      />
      <button
        class="secondary staff-login-button"
        :loading="staffLoading"
        :disabled="Boolean(identified)"
        @click="handleStaffLogin"
      >
        {{ staffLoginMode === 'admin' ? '首次绑定管理员微信' : '首次绑定医生微信' }}
      </button>
      <view class="staff-login-note">
        {{
          staffLoginMode === 'admin'
            ? '绑定成功后无需再次输入账号密码，直接使用微信一键登录。'
            : '绑定码仅首次使用，绑定后无需重复输入。'
        }}
      </view>
      <view v-if="staffError" class="error">{{ staffError }}</view>
    </view>
    <!-- #endif -->

    <!-- #ifdef H5 -->
    <view v-if="!isDevBuild" class="card browser-tip">
      <view class="card-title">请在微信中使用</view>
      <view class="subtitle">正式账号通过微信小程序登录；此网页仅用于展示与本地运维验证。</view>
    </view>
    <!-- #endif -->

    <view v-if="isDevBuild" class="developer-trigger" @click="showDeveloper = !showDeveloper">
      <text>开发调试身份</text><text>{{ showDeveloper ? '收起' : '展开' }} ›</text>
    </view>
    <view v-if="isDevBuild && showDeveloper" class="card development-card">
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
        进入致宇健康
      </button>
      <view v-if="error" class="error">{{ error }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import type { AuthData, Role } from '@/types/api'

const accounts = [
  { username: 'platform_admin', icon: '平', name: '平台管理员', description: '平台基础查看' },
  { username: 'doctor', icon: '医', name: '医生', description: '本院体检者查询与报告查看' },
  { username: 'customer', icon: '客', name: '普通客户', description: '个人健康中心' },
]
const username = ref('doctor'),
  password = ref('RayK@123456'),
  loading = ref(false),
  wechatLoading = ref(false),
  showDeveloper = ref(true),
  error = ref(''),
  wechatError = ref(''),
  staffLoginMode = ref<'admin' | 'doctor'>('doctor'),
  staffUsername = ref(''),
  staffPassword = ref(''),
  staffInviteCode = ref(''),
  staffLoading = ref(false),
  staffError = ref(''),
  expired = ref(false),
  identified = ref<{ category: string; workbench: string } | null>(null)
const auth = useAuthStore()
const isDevBuild = import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEVELOPMENT_LOGIN === 'true'
const supportsPhoneLogin = import.meta.env.VITE_WECHAT_PHONE_LOGIN === 'true'
const workbenchNames: Record<Role, string> = {
  PLATFORM_ADMIN: '平台管理工作台',
  DOCTOR: '医生工作台',
  CUSTOMER: '个人健康中心',
}
const identityLabels: Record<Role, string> = {
  PLATFORM_ADMIN: '管理员',
  DOCTOR: '医生',
  CUSTOMER: '客户',
}
const identifiedFor = (data: AuthData) => ({
  category: identityLabels[data.defaultWorkbench],
  workbench: workbenchNames[data.defaultWorkbench],
})

onLoad((query) => {
  expired.value = query?.expired === '1'
})

async function handleWeChatLogin(event?: { detail?: { code?: string; errMsg?: string } }) {
  wechatLoading.value = true
  wechatError.value = ''
  const phoneCode = supportsPhoneLogin ? event?.detail?.code : undefined
  try {
    const result = await uni.login({ provider: 'weixin' })
    if (!result.code) throw new Error('微信未返回登录凭证')
    const data: AuthData = await auth.loginWithWeChat(result.code, phoneCode)
    identified.value = identifiedFor(data)
    await new Promise((resolve) => setTimeout(resolve, 900))
    uni.switchTab({ url: '/pages/home/index' })
  } catch (e) {
    const phoneError = event?.detail?.errMsg ?? ''
    if (supportsPhoneLogin && !phoneCode && /deny|cancel/i.test(phoneError)) {
      wechatError.value = '您取消了手机号授权，请重新点击并允许授权'
    } else if (supportsPhoneLogin && !phoneCode && !isDevBuild) {
      wechatError.value = '当前小程序未取得手机号授权凭证，请确认已使用正式 AppID 并开通手机号快速验证'
    } else {
      wechatError.value = e instanceof Error ? e.message : '微信登录失败，请重试'
    }
  } finally {
    wechatLoading.value = false
  }
}

async function handleStaffLogin() {
  if (staffLoginMode.value === 'admin') {
    await handleAdminLogin()
    return
  }
  const inviteCode = staffInviteCode.value.trim().toUpperCase()
  if (inviteCode.length !== 10) {
    staffError.value = '请输入有效的 10 位绑定码'
    return
  }
  staffLoading.value = true
  staffError.value = ''
  try {
    const result = await uni.login({ provider: 'weixin' })
    if (!result.code) throw new Error('微信未返回登录凭证')
    const data: AuthData = await auth.loginWithWeChatInvite(result.code, inviteCode)
    identified.value = identifiedFor(data)
    await new Promise((resolve) => setTimeout(resolve, 900))
    uni.switchTab({ url: '/pages/home/index' })
  } catch (e) {
    staffError.value = e instanceof Error ? e.message : '工作人员登录失败，请重试'
  } finally {
    staffLoading.value = false
  }
}

async function handleAdminLogin() {
  const usernameValue = staffUsername.value.trim()
  const passwordValue = staffPassword.value
  if (!usernameValue || !passwordValue) {
    staffError.value = '请输入平台管理员账号和密码'
    return
  }
  staffLoading.value = true
  staffError.value = ''
  try {
    const result = await uni.login({ provider: 'weixin' })
    if (!result.code) throw new Error('微信未返回登录凭证')
    const data: AuthData = await auth.loginWithWeChatAdmin(
      result.code,
      usernameValue,
      passwordValue,
    )
    identified.value = identifiedFor(data)
    await new Promise((resolve) => setTimeout(resolve, 900))
    uni.switchTab({ url: '/pages/home/index' })
  } catch (e) {
    staffError.value = e instanceof Error ? e.message : '平台管理员登录失败，请重试'
  } finally {
    staffLoading.value = false
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
.login-card-tip {
  margin-top: 8rpx;
  color: #71877f;
  font-size: 22rpx;
  line-height: 1.55;
}
.wechat {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 92rpx;
  margin-top: 30rpx;
  background: linear-gradient(135deg, #08bd5c, #05a94f);
  color: #fff;
  border: 0;
  border-radius: 24rpx;
  font-size: 30rpx;
  line-height: 92rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
  box-shadow: 0 14rpx 30rpx rgba(7, 193, 96, 0.24);
}
.wechat::after {
  border: 0;
}
.wechat-hover {
  opacity: 0.9;
  transform: translateY(1rpx);
}
.agreement {
  margin-top: 18rpx;
  color: #94a09c;
  text-align: center;
  font-size: 20rpx;
}
.recognizing {
  margin-top: 18rpx;
  color: #5f8479;
  text-align: center;
  font-size: 22rpx;
}
.identified {
  display: flex;
  align-items: center;
  gap: 15rpx;
  margin-top: 22rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #e7f8f1;
  color: #176c57;
  font-size: 22rpx;
}
.identified-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  background: #16a36f;
  color: #fff;
  font-weight: 800;
}
.identified text {
  display: block;
}
.identified text + text {
  margin-top: 4rpx;
  color: #5d8377;
  font-size: 20rpx;
}
.browser-tip {
  padding: 34rpx;
}
.staff-login-card {
  margin-top: 20rpx;
  padding: 30rpx 34rpx 34rpx;
}
.staff-login-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}
.staff-login-heading .card-title {
  margin-bottom: 0;
}
.staff-login-badge {
  flex: 0 0 auto;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #e3f6ee;
  color: #0f7a62;
  font-size: 20rpx;
  font-weight: 700;
}
.staff-role-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  margin: 20rpx 0 16rpx;
}
.staff-role-option {
  height: 70rpx;
  margin: 0;
  border: 1rpx solid #dce9e5;
  border-radius: 18rpx;
  background: #f5faf8;
  color: #55766c;
  font-size: 24rpx;
  line-height: 70rpx;
}
.staff-role-option::after {
  border: 0;
}
.staff-role-option.active {
  border-color: #0f7a62;
  background: #e0f6ed;
  color: #087056;
  font-weight: 700;
}
.staff-credentials {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-top: 18rpx;
}
.staff-login-guide {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #f2f8f5;
}
.staff-guide-row {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  color: #315f52;
  font-size: 22rpx;
  line-height: 1.55;
}
.staff-guide-row + .staff-guide-row {
  margin-top: 12rpx;
}
.staff-guide-row-muted {
  color: #71877f;
}
.staff-guide-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 30rpx;
  height: 30rpx;
  border-radius: 50%;
  background: #cdeee2;
  color: #0f7a62;
  font-size: 18rpx;
  font-weight: 700;
  line-height: 30rpx;
}
.staff-invite-input {
  margin-top: 18rpx;
  text-transform: uppercase;
}
.staff-password-input {
  margin-top: 0;
}
.staff-login-button {
  width: 100%;
  min-height: 84rpx;
  margin-top: 18rpx;
  border-radius: 22rpx;
  font-size: 27rpx;
}
.staff-login-note {
  margin-top: 14rpx;
  color: #83938e;
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
  padding: 34rpx 34rpx 28rpx;
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
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 92rpx;
  margin-top: 18rpx;
  border-radius: 24rpx;
  font-size: 30rpx;
  line-height: 92rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
  box-shadow: 0 14rpx 30rpx rgba(15, 122, 98, 0.2);
}
.enter-button::after {
  border: 0;
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
.login-page.elder-page .hero {
  margin-right: -24rpx;
  margin-left: -24rpx;
}
.login-page.elder-page .brand-name {
  font-size: 38rpx;
}
.login-page.elder-page .brand-tag {
  font-size: 22rpx;
}
.login-page.elder-page .hero-copy {
  font-size: 28rpx;
  line-height: 1.65;
}
.login-page.elder-page .trust-row {
  flex-wrap: wrap;
  font-size: 26rpx;
}
.login-page.elder-page .card-title {
  font-size: 38rpx;
}
.login-page.elder-page .wechat,
.login-page.elder-page .enter-button {
  height: 106rpx;
  font-size: 34rpx;
  line-height: 106rpx;
}
.login-page.elder-page .agreement,
.login-page.elder-page .recognizing {
  font-size: 25rpx;
  line-height: 1.65;
}
</style>

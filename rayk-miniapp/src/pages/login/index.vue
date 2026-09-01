<template>
  <view class="page login-page elder-page">
    <view class="hero">
      <view class="brand-row">
        <image class="logo" :src="logoArt" mode="aspectFill" />
        <view>
          <view class="brand-name">三羊健康</view>
          <view class="brand-tag">AI 智能健康管理</view>
        </view>
      </view>
      <image class="hero-art" :src="heroArt" mode="aspectFit" />
      <view class="hero-title">让每一份健康数据<br /><text>清晰、有序、可行动</text></view>
      <view class="hero-copy">连接检验报告、AI评估与专业健康管理服务</view>
    </view>

    <view class="trust-panel">
      <view class="trust-item">
        <image class="trust-icon" :src="securityArt" mode="aspectFit" />
        <view>
          <view class="trust-name">隐私保护</view>
          <view class="trust-copy">数据安全加密</view>
        </view>
      </view>
      <view class="trust-item">
        <image class="trust-icon" :src="membershipArt" mode="aspectFit" />
        <view>
          <view class="trust-name">专业可信</view>
          <view class="trust-copy">医疗级 AI 分析</view>
        </view>
      </view>
      <view class="trust-item">
        <image class="trust-icon" :src="followupArt" mode="aspectFit" />
        <view>
          <view class="trust-name">持续随访</view>
          <view class="trust-copy">健康管理跟踪</view>
        </view>
      </view>
    </view>

    <view v-if="expired" class="notice">登录状态已过期，请重新登录</view>

    <!-- #ifdef MP-WEIXIN -->
    <view class="card login-card">
      <view class="card-title">{{ supportsPhoneLogin ? '微信授权手机号登录' : '微信一键登录' }}</view>
      <view class="login-subtitle">快捷登录，安全可靠</view>
      <image class="wechat-mark" :src="wechatArt" mode="aspectFit" />
      <button
        v-if="supportsPhoneLogin && legalAgreed"
        class="wechat"
        :loading="wechatLoading"
        :disabled="Boolean(identified) || wechatLoading"
        open-type="getPhoneNumber"
        hover-class="wechat-hover"
        phone-number-no-quota-toast
        @getphonenumber="handleWeChatLogin"
      >
        {{ supportsPhoneLogin ? '授权手机号并登录' : '微信一键登录' }}
      </button>
      <button
        v-else
        class="wechat"
        :loading="wechatLoading"
        :disabled="Boolean(identified) || wechatLoading"
        hover-class="wechat-hover"
        @click="handleWeChatLogin()"
      >
        {{ supportsPhoneLogin ? '授权手机号并登录' : '微信一键登录' }}
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
      <view class="agreement">
        <button
          class="agreement-toggle"
          :class="{ checked: legalAgreed }"
          :aria-label="
            legalAgreed
              ? '已同意用户服务协议和隐私政策'
              : '未同意用户服务协议和隐私政策，点击勾选'
          "
          hover-class="agreement-toggle-hover"
          @tap="toggleLegalAgreement"
        >
          <view class="agreement-mark">{{ legalAgreed ? '✓' : '' }}</view>
          <text class="agreement-copy">我已阅读并同意</text>
        </button>
        <text class="agreement-link" hover-class="agreement-link-hover" @tap="openLegal('service')">《用户服务协议》</text>
        <text class="agreement-copy agreement-joiner">和</text>
        <text class="agreement-link" hover-class="agreement-link-hover" @tap="openLegal('privacy')">《隐私政策》</text>
      </view>
      <view v-if="wechatError" class="error">{{ wechatError }}</view>
      <view class="service-heading">
        <view class="service-heading-line" />
        <view>健康服务一站直达</view>
        <view class="service-heading-line" />
      </view>
      <view class="benefits-grid">
        <view class="benefit-item">
          <image class="benefit-icon" :src="profileArt" mode="aspectFit" />
          <view class="benefit-name">健康档案</view>
          <view class="benefit-copy">记录健康数据</view>
        </view>
        <view class="benefit-item">
          <image class="benefit-icon" :src="assessmentArt" mode="aspectFit" />
          <view class="benefit-name">健康评估</view>
          <view class="benefit-copy">智能 AI 解读</view>
        </view>
        <view class="benefit-item">
          <image class="benefit-icon" :src="careArt" mode="aspectFit" />
          <view class="benefit-name">持续管理</view>
          <view class="benefit-copy">跟踪健康改善</view>
        </view>
      </view>
    </view>
    <view class="security-line"><image class="security-mark" :src="securityArt" mode="aspectFit" />数据安全保障中</view>
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
          @click="selectDeveloperAccount(item)"
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
        进入三羊健康
      </button>
      <view v-if="error" class="error">{{ error }}</view>
    </view>

    <view v-if="showConsentDialog" class="consent-mask" @tap="closeConsentDialog">
      <view class="consent-dialog" @tap.stop>
        <view class="consent-title">请先同意相关协议</view>
        <view class="consent-copy-text">登录前请阅读并同意《用户服务协议》和《隐私政策》。</view>
        <view class="consent-actions">
          <button class="consent-secondary" hover-class="consent-secondary-hover" @tap="closeConsentDialog">暂不</button>
          <button class="consent-primary" hover-class="consent-primary-hover" @tap="confirmLegalAgreement">同意并继续</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import type { AuthData, Role } from '@/types/api'
import heroArt from '@/assets/ui/login/login-hero-ai.png'
import wechatArt from '@/assets/ui/login/login-wechat.png'
import securityArt from '@/assets/ui/login/login-security.png'
import membershipArt from '@/assets/ui/login/login-membership.png'
import followupArt from '@/assets/ui/login/login-followup.png'
import profileArt from '@/assets/ui/login/login-profile.png'
import assessmentArt from '@/assets/ui/login/login-assessment.png'
import careArt from '@/assets/ui/login/login-care.png'
import logoArt from '@/assets/ui/login/brand-logo-sheep.png'

const accounts = [
  {
    username: 'admin',
    password: '123456',
    icon: '平',
    name: '平台管理员',
    description: '平台基础查看',
  },
  {
    username: 'doctor',
    password: 'RayK@123456',
    icon: '医',
    name: '医生',
    description: '本院体检者查询与报告查看',
  },
  {
    username: 'customer',
    password: 'RayK@123456',
    icon: '客',
    name: '普通客户',
    description: '个人健康中心',
  },
]
const username = ref(accounts[1].username),
  password = ref(accounts[1].password),
  loading = ref(false),
  wechatLoading = ref(false),
  showDeveloper = ref(true),
  error = ref(''),
  wechatError = ref(''),
  expired = ref(false),
  identified = ref<{ category: string; workbench: string } | null>(null),
  legalAgreed = ref(false),
  showConsentDialog = ref(false),
  pendingLogin = ref<'wechat' | 'developer' | null>(null)
const auth = useAuthStore()
const LEGAL_CONSENT_STORAGE_KEY = 'rayk_legal_consent_2026.09'
const isDevBuild = import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEVELOPMENT_LOGIN === 'true'
// Enterprise production builds must use the verified phone credential even when a
// checkout does not contain the ignored local .env.production file. Development
// builds can opt in explicitly through VITE_WECHAT_PHONE_LOGIN.
const supportsPhoneLogin =
  import.meta.env.VITE_WECHAT_PHONE_LOGIN === 'true' ||
  (!isDevBuild && import.meta.env.MODE === 'production')
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
type LegalDocument = 'service' | 'privacy'
const openLegal = (type: LegalDocument) => {
  uni.navigateTo({ url: `/pages/legal/index?type=${type}` })
}
const identifiedFor = (data: AuthData) => ({
  category: identityLabels[data.defaultWorkbench],
  workbench: workbenchNames[data.defaultWorkbench],
})
const selectDeveloperAccount = (account: (typeof accounts)[number]) => {
  username.value = account.username
  password.value = account.password
  error.value = ''
}
onLoad((query) => {
  expired.value = query?.expired === '1'
  legalAgreed.value = uni.getStorageSync(LEGAL_CONSENT_STORAGE_KEY) === true
})

function toggleLegalAgreement() {
  legalAgreed.value = !legalAgreed.value
  if (legalAgreed.value) uni.setStorageSync(LEGAL_CONSENT_STORAGE_KEY, true)
  else uni.removeStorageSync(LEGAL_CONSENT_STORAGE_KEY)
}

function ensureLegalAgreement(target: 'wechat' | 'developer') {
  if (legalAgreed.value) return true
  pendingLogin.value = target
  showConsentDialog.value = true
  return false
}

function closeConsentDialog() {
  showConsentDialog.value = false
  pendingLogin.value = null
}

async function confirmLegalAgreement() {
  legalAgreed.value = true
  uni.setStorageSync(LEGAL_CONSENT_STORAGE_KEY, true)
  showConsentDialog.value = false
  const target = pendingLogin.value
  pendingLogin.value = null
  if (target === 'developer') await handleLogin()
  else if (target === 'wechat' && !supportsPhoneLogin) await handleWeChatLogin()
  else if (target === 'wechat') {
    uni.showToast({ title: '已同意，请再次点击授权手机号并登录', icon: 'none' })
  }
}

async function handleWeChatLogin(
  event?: { detail?: { code?: string; errMsg?: string; encryptedData?: string; iv?: string } },
) {
  if (wechatLoading.value) return
  if (!ensureLegalAgreement('wechat')) return
  wechatLoading.value = true
  wechatError.value = ''
  const phoneError = event?.detail?.errMsg ?? ''
  const phoneCode = supportsPhoneLogin ? event?.detail?.code?.trim() || undefined : undefined
  // A getPhoneNumber event is the only safe source of the one-time phone
  // credential. Do not send an empty value to the server (which used to be
  // reported as the generic 10205 authorization failure); it also makes a
  // cancelled/unsupported client immediately actionable for development
  // package users.
  if (supportsPhoneLogin && !phoneCode) {
    wechatError.value = /deny|cancel/i.test(phoneError)
      ? '您取消了手机号授权，请重新点击并允许授权'
      : '微信未返回手机号授权凭证，请重新点击授权手机号并确认授权；开发包请使用真机预览'
    wechatLoading.value = false
    return
  }
  try {
    const result = await uni.login({ provider: 'weixin' })
    if (!result.code) throw new Error('微信未返回登录凭证')
    const data: AuthData = await auth.loginWithWeChat(result.code, phoneCode)
    identified.value = identifiedFor(data)
    await new Promise((resolve) => setTimeout(resolve, 900))
    uni.switchTab({ url: '/pages/home/index' })
  } catch (e) {
    wechatError.value = e instanceof Error ? e.message : '微信登录失败，请重试'
  } finally {
    wechatLoading.value = false
  }
}

async function handleLogin() {
  if (!ensureLegalAgreement('developer')) return
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
  padding: calc(42rpx + env(safe-area-inset-top)) 42rpx 44rpx;
  border-radius: 0 0 46rpx 46rpx;
  background: linear-gradient(145deg, #f5fbfa 0%, #edf8f4 56%, #dff3ec 100%);
  color: #163b35;
  box-shadow: 0 18rpx 42rpx rgba(20, 103, 82, 0.08);
}
.orb {
  position: absolute;
  border-radius: 50%;
  border: 1rpx solid rgba(37, 174, 137, 0.12);
  background: rgba(129, 229, 197, 0.16);
}
.orb-one {
  top: -100rpx;
  right: -54rpx;
  width: 310rpx;
  height: 310rpx;
}
.orb-two {
  right: 100rpx;
  bottom: -150rpx;
  width: 260rpx;
  height: 260rpx;
}
.brand-row {
  position: relative;
  z-index: 1;
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
  border: 0;
  border-radius: 25rpx;
  background: transparent;
  object-fit: cover;
  font-size: 32rpx;
  font-weight: 800;
  backdrop-filter: blur(12rpx);
}
.brand-name {
  font-size: 34rpx;
  font-weight: 750;
  color: #145849;
}
.hero-art {
  position: absolute;
  z-index: 0;
  top: 34rpx;
  right: -10rpx;
  width: 310rpx;
  height: 310rpx;
  opacity: 0.58;
}
.brand-tag {
  margin-top: 2rpx;
  color: #6b9b8d;
  font-size: 18rpx;
  letter-spacing: 3rpx;
}
.hero-title {
  position: relative;
  z-index: 1;
  margin-top: 48rpx;
  font-size: 49rpx;
  line-height: 1.38;
  font-weight: 760;
  color: #102a36;
}
.hero-title text {
  color: #102a36;
}
.hero-copy {
  position: relative;
  z-index: 1;
  margin-top: 18rpx;
  color: #81939e;
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
.trust-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10rpx;
  margin: -2rpx 0 24rpx;
  padding: 22rpx 16rpx;
  border: 1rpx solid rgba(215, 230, 224, 0.94);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 12rpx 28rpx rgba(20, 78, 63, 0.06);
}
.trust-item {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8rpx;
  padding: 0 4rpx;
}
.trust-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  object-fit: contain;
}
.trust-icon-safe { background: #e0f7ec; color: #159a67; }
.trust-icon-pro { background: #e7f8ef; color: #27a87a; }
.trust-icon-care { background: #e6f7f1; color: #12a777; }
.trust-name {
  color: #244b41;
  font-size: 22rpx;
  font-weight: 700;
  white-space: nowrap;
}
.trust-copy {
  margin-top: 4rpx;
  color: #8b9da5;
  font-size: 18rpx;
  line-height: 1.3;
  white-space: nowrap;
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
  padding: 38rpx 34rpx 28rpx;
  margin-bottom: 20rpx;
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.96);
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
.login-subtitle {
  margin-top: 4rpx;
  color: #8a9ca4;
  font-size: 25rpx;
}
.wechat-mark {
  display: block;
  width: 220rpx;
  height: 220rpx;
  margin: 18rpx auto 0;
  object-fit: contain;
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
  display: flex;
  align-items: flex-start;
  justify-content: center;
  flex-wrap: wrap;
  gap: 0 4rpx;
  margin-top: 18rpx;
  color: #94a09c;
  text-align: center;
  font-size: 20rpx;
}
.agreement-toggle {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 10rpx;
  min-height: 88rpx;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 14rpx;
  background: transparent;
  color: #94a09c;
  font-size: inherit;
  line-height: 1.65;
  text-align: left;
}
.agreement-toggle::after {
  border: 0;
}
.agreement-toggle-hover,
.agreement-link-hover,
.consent-secondary-hover,
.consent-primary-hover {
  opacity: 0.78;
}
.agreement-copy {
  display: inline-flex;
  align-items: center;
  min-height: 88rpx;
}
.agreement-joiner {
  padding: 0 2rpx;
}
.agreement-link {
  display: inline-flex;
  align-items: center;
  min-height: 88rpx;
  padding: 0 6rpx;
  color: #0f7a62;
  font-weight: 700;
  text-decoration: none;
}
.agreement-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30rpx;
  height: 30rpx;
  border: 2rpx solid #a9beb7;
  border-radius: 50%;
  background: #fff;
  color: transparent;
  font-size: 18rpx;
  font-weight: 800;
  vertical-align: -3rpx;
}
.agreement-toggle.checked .agreement-mark {
  border-color: #18af72;
  background: #18af72;
  color: #fff;
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
.login-benefits { padding-bottom: 28rpx; }
.service-heading {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin: 30rpx 0 20rpx;
  color: #718892;
  font-size: 27rpx;
  white-space: nowrap;
}
.service-heading-line { flex: 1; height: 1rpx; background: #e5eeeb; }
.benefits-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 0;
}
.benefit-item {
  min-width: 0;
  padding: 20rpx 10rpx 18rpx;
  border-radius: 18rpx;
  background: #f4faf7;
  text-align: center;
}
.benefit-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 66rpx;
  height: 66rpx;
  margin: 0 auto 12rpx;
  border-radius: 50%;
  object-fit: contain;
}
.benefit-icon-profile {
  background: #e0f4ed;
  color: #0d765e;
}
.benefit-icon-report {
  background: #e7efff;
  color: #3970c7;
}
.benefit-icon-care {
  background: #fff1d7;
  color: #ae7208;
}
.benefit-name {
  color: #31574b;
  font-size: 23rpx;
  font-weight: 650;
  white-space: nowrap;
}
.benefit-copy {
  margin-top: 6rpx;
  color: #82948d;
  font-size: 19rpx;
  line-height: 1.35;
  white-space: nowrap;
}
.security-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin: 8rpx 0 24rpx;
  color: #82949d;
  font-size: 23rpx;
}
.security-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38rpx;
  height: 38rpx;
  object-fit: contain;
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
.consent-mask {
  position: fixed;
  z-index: 1000;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36rpx 28rpx calc(36rpx + env(safe-area-inset-bottom));
  background: rgba(18, 37, 32, 0.52);
}
.consent-dialog {
  width: 100%;
  max-width: 680rpx;
  padding: 38rpx 32rpx 30rpx;
  border-radius: 32rpx;
  background: #fff;
  box-shadow: 0 24rpx 60rpx rgba(11, 58, 43, 0.22);
}
.consent-title {
  color: #173e34;
  font-size: 36rpx;
  font-weight: 780;
  line-height: 1.45;
  text-align: center;
}
.consent-copy-text {
  margin-top: 18rpx;
  color: #526d64;
  font-size: 28rpx;
  line-height: 1.7;
  text-align: left;
}
.consent-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 28rpx;
}
.consent-actions button {
  flex: 1;
  min-height: 88rpx;
  margin: 0;
  border-radius: 20rpx;
  font-size: 28rpx;
  line-height: 88rpx;
}
.consent-actions button::after {
  border: 0;
}
.consent-secondary {
  border: 2rpx solid #d5e5df;
  background: #f7fbf9;
  color: #53756a;
}
.consent-primary {
  background: #0f7a62;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 10rpx 22rpx rgba(15, 122, 98, 0.2);
}
</style>

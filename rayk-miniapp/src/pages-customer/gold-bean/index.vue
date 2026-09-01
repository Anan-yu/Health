<template>
  <view class="page gold-bean-page elder-page">
    <view v-if="!goldBeanEnabled" class="disabled-card">
      <view class="disabled-title">金豆会员暂未开放</view>
      <view class="disabled-copy">该功能仅用于开发环境验收，正式版本不会启用。</view>
    </view>

    <PageState v-else :loading="loading" :error="error" :empty="!summary">
      <view v-if="summary" class="content">
        <view class="hero-card">
          <view class="hero-topline">
            <view>
              <view class="eyebrow">GOLD BEAN MEMBERSHIP</view>
              <view class="hero-title">{{ summary.memberLevelName }}</view>
            </view>
            <view class="level-badge">{{ summary.historicalLevelName }} · 历史最高</view>
          </view>
          <view class="hero-caption">
            {{ summary.registrationFeeStatus === 'PAID' ? '已完成开发环境注册记录' : '完成注册后开始累计金豆' }}
          </view>
          <view class="hero-stats">
            <view class="hero-stat">
              <text class="hero-stat-value">{{ summary.totalBalance }}</text>
              <text class="hero-stat-label">金豆总额</text>
            </view>
            <view class="hero-stat">
              <text class="hero-stat-value">{{ summary.directReferralCount }}</text>
              <text class="hero-stat-label">直推人数</text>
            </view>
            <view class="hero-stat">
              <text class="hero-stat-value">{{ summary.tradeLimitPercent }}%</text>
              <text class="hero-stat-label">交易额度</text>
            </view>
          </view>
        </view>

        <view v-if="summary.reminderText" class="reminder-card">
          <view class="reminder-title">当前提醒</view>
          <view class="reminder-copy">{{ summary.reminderText }}</view>
        </view>

        <view class="section-label"><view class="section-line" />金豆账户</view>
        <view class="balance-grid">
          <view class="balance-card bank-card">
            <view class="balance-label">数字银行</view>
            <view class="balance-value">{{ summary.digitalBankBalance }}</view>
            <view class="balance-note">仅支持传奇人物购买</view>
          </view>
          <view class="balance-card trading-card">
            <view class="balance-label">可交易金豆</view>
            <view class="balance-value">{{ summary.tradingBalance }}</view>
            <view class="balance-note">用户间交易额度</view>
          </view>
        </view>

        <view class="card progress-card">
          <view class="card-heading">直推成长</view>
          <view class="progress-copy">
            <text>{{ summary.directReferralCount }} 人</text>
            <text v-if="summary.nextLevelName">距离{{ summary.nextLevelName }}还需 {{ Math.max(summary.nextLevelThreshold - summary.directReferralCount, 0) }} 人</text>
            <text v-else>已达到最高等级</text>
          </view>
          <view class="progress-track"><view class="progress-fill" :style="{ width: `${referralProgress}%` }" /></view>
          <view class="level-steps">
            <view v-for="step in levelSteps" :key="step.code" class="level-step" :class="{ reached: summary.directReferralCount >= step.threshold }">
              <view class="step-dot" />
              <view class="step-name">{{ step.name }}</view>
              <view class="step-threshold">{{ step.threshold }}人</view>
            </view>
          </view>
        </view>

        <view class="card reward-card">
          <view class="card-heading">每日奖励与活跃保护</view>
          <view class="reward-row">
            <view class="reward-main"><text class="reward-number">{{ summary.dailyRewardDays }}</text><text> / {{ summary.dailyRewardTotalDays }} 天</text></view>
            <view class="reward-label">普通会员每日 60 金豆</view>
          </view>
          <view class="progress-track reward-track"><view class="progress-fill" :style="{ width: `${dailyProgress}%` }" /></view>
          <view class="protection-copy">{{ protectionText }}</view>
        </view>

        <view v-if="summary.registrationFeeStatus !== 'PAID'" class="card register-card">
          <view class="card-heading">开发环境注册演示</view>
          <view class="register-copy">新会员注册费规则为 998 元。此处只记录开发测试状态，不创建真实订单、不扣款。</view>
          <view class="field-label">推荐码（首个平台测试账号可留空）</view>
          <input v-model="registerForm.referralCode" class="text-input" maxlength="32" placeholder="请输入推荐人的推荐码" />
          <view class="field-label">所在地区（仅支持省和市）</view>
          <picker mode="region" level="city" @change="onRegisterRegionChange">
            <view class="picker-field" :class="{ placeholder: !registerRegionLabel }">
              <text class="picker-text">{{ registerRegionLabel || '请选择所在省和市' }}</text>
              <text class="picker-arrow">›</text>
            </view>
          </picker>
          <view class="field-helper">请选择省和市，系统将按“省 / 市”保存。</view>
          <button class="primary-button" :disabled="registering" @click="register">{{ registering ? '正在记录…' : '记录开发注册状态' }}</button>
        </view>

        <view class="card referral-card">
          <view class="card-heading">我的推荐码</view>
          <view class="referral-code">{{ summary.referralCode }}</view>
          <view class="referral-note">推荐关系只按直推计算；金豆不可提现。</view>
        </view>

        <view v-if="summary.regionOpenAllowed || summary.regionCity" class="card region-card">
          <view class="card-heading">区域权限</view>
          <view v-if="summary.regionCity" class="region-current">已开辟：{{ summary.regionCity }}</view>
          <view v-else class="region-copy">钻石会员可申请开辟一个城市区域，区域不可更改，最多三级。</view>
          <input v-if="summary.regionOpenAllowed" v-model="regionForm.city" class="text-input" maxlength="64" placeholder="请输入要开辟的城市" />
          <input v-if="summary.regionOpenAllowed" v-model="regionForm.parentRegionId" class="text-input" maxlength="20" placeholder="上级区域 ID（可选）" />
          <button v-if="summary.regionOpenAllowed" class="secondary-button" :disabled="openingRegion" @click="openRegion">{{ openingRegion ? '正在申请…' : '申请开辟区域' }}</button>
        </view>

        <view class="card ledger-card">
          <view class="ledger-header"><view class="card-heading">最近金豆记录</view><button class="text-button" :disabled="ledgerLoading" @click="loadLedger">刷新</button></view>
          <view v-if="ledgerLoading" class="ledger-empty">正在加载记录…</view>
          <view v-else-if="!ledger.length" class="ledger-empty">暂无金豆记录</view>
          <view v-else v-for="item in ledger" :key="item.id" class="ledger-row">
            <view class="ledger-copy"><view class="ledger-title">{{ item.description }}</view><view class="ledger-meta">{{ formatDate(item.createdAt) }} · {{ bucketName(item.bucket) }}</view></view>
            <view class="ledger-amount" :class="item.direction === 'DEBIT' ? 'debit' : ''">{{ item.direction === 'DEBIT' ? '-' : '+' }}{{ item.amount }}</view>
          </view>
        </view>

        <view class="disclaimer">开发环境演示功能；金豆不可提现，任何真实注册收费、交易结算和分润上线前须完成合规、合同与支付验收。</view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import PageState from '@/components/PageState.vue'
import { goldBeanEnabled } from '@/constants/features'
import {
  getGoldBeanLedger,
  getGoldBeanSummary,
  openGoldRegion,
  registerGoldBean,
  type GoldBeanLedgerEntry,
  type GoldBeanSummary,
} from '@/api/gold-bean'

const loading = ref(true)
const error = ref('')
const summary = ref<GoldBeanSummary>()
const ledger = ref<GoldBeanLedgerEntry[]>([])
const ledgerLoading = ref(false)
const registering = ref(false)
const openingRegion = ref(false)
const registerForm = reactive({ referralCode: '', province: '', city: '' })
const regionForm = reactive({ city: '', parentRegionId: '' })

type RegionPickerChangeEvent = { detail?: { value?: unknown } }

const registerRegionLabel = computed(() => {
  if (!registerForm.province || !registerForm.city) return ''
  return `${registerForm.province} / ${registerForm.city}`
})

const levelSteps = [
  { code: 'ORDINARY', name: '普通', threshold: 0 },
  { code: 'COPPER', name: '铜牌', threshold: 2 },
  { code: 'SILVER', name: '银牌', threshold: 9 },
  { code: 'GOLD', name: '金牌', threshold: 29 },
  { code: 'DIAMOND', name: '钻石', threshold: 50 },
]

const referralProgress = computed(() => {
  const current = summary.value?.directReferralCount || 0
  const threshold = summary.value?.nextLevelThreshold || 50
  return Math.min(100, Math.round((current / Math.max(threshold, 1)) * 100))
})
const dailyProgress = computed(() => {
  const current = summary.value?.dailyRewardDays || 0
  const total = summary.value?.dailyRewardTotalDays || 20
  return Math.min(100, Math.round((current / Math.max(total, 1)) * 100))
})
const protectionText = computed(() => {
  if (!summary.value?.protectionUntil) return '当前没有活跃保护期；完成直推后可刷新 7 天保护期。'
  return `当前活跃保护期至 ${formatDate(summary.value.protectionUntil)}，保护期内推荐新人可刷新。`
})

const formatDate = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '—')
const bucketName = (value: string) => (value === 'DIGITAL_BANK' ? '数字银行' : '可交易')

function onRegisterRegionChange(event: RegionPickerChangeEvent) {
  const values = Array.isArray(event.detail?.value)
    ? event.detail.value.map((value) => String(value || '').trim())
    : []
  const [province = '', city = ''] = values
  if (!province || !city) {
    registerForm.province = ''
    registerForm.city = ''
    uni.showToast({ title: '请选择省和市', icon: 'none' })
    return
  }
  registerForm.province = province
  registerForm.city = city
}

async function loadLedger() {
  ledgerLoading.value = true
  try {
    ledger.value = await getGoldBeanLedger()
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '金豆记录加载失败', icon: 'none' })
  } finally {
    ledgerLoading.value = false
  }
}

async function load() {
  if (!goldBeanEnabled) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    summary.value = await getGoldBeanSummary()
    await loadLedger()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '金豆会员信息加载失败'
  } finally {
    loading.value = false
  }
}

async function register() {
  if (registering.value) return
  if (!registerForm.province || !registerForm.city) {
    uni.showToast({ title: '请选择所在省和市', icon: 'none' })
    return
  }
  registering.value = true
  try {
    summary.value = await registerGoldBean({
      referralCode: registerForm.referralCode.trim() || undefined,
      city: registerRegionLabel.value,
    })
    await loadLedger()
    uni.showToast({ title: '开发注册状态已记录', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '注册记录失败', icon: 'none' })
  } finally {
    registering.value = false
  }
}

async function openRegion() {
  if (openingRegion.value || !regionForm.city.trim()) {
    uni.showToast({ title: '请先填写城市', icon: 'none' })
    return
  }
  openingRegion.value = true
  try {
    const region = await openGoldRegion({
      city: regionForm.city.trim(),
      parentRegionId: regionForm.parentRegionId.trim() || undefined,
    })
    if (summary.value) {
      summary.value = { ...summary.value, regionId: region.id, regionCity: region.city, regionOpenAllowed: false }
    }
    regionForm.city = ''
    regionForm.parentRegionId = ''
    uni.showToast({ title: '区域申请已记录', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '区域申请失败', icon: 'none' })
  } finally {
    openingRegion.value = false
  }
}

onShow(() => void load())
</script>

<style scoped>
.gold-bean-page { min-height: 100vh; padding: 24rpx; background: linear-gradient(180deg, #f3fbf7 0%, #f8fbfa 100%); }
.content { padding-bottom: 24rpx; }
.hero-card { padding: 34rpx 30rpx 28rpx; border-radius: 32rpx; color: #fff; background: linear-gradient(135deg, #143f34 0%, #0c8066 72%, #18aa86 100%); box-shadow: 0 18rpx 42rpx rgba(8, 91, 70, .18); }
.hero-topline { display: flex; align-items: flex-start; justify-content: space-between; gap: 14rpx; }
.eyebrow { color: rgba(255,255,255,.66); font-size: 18rpx; font-weight: 750; letter-spacing: 3rpx; }
.hero-title { margin-top: 14rpx; font-size: 46rpx; font-weight: 820; line-height: 1.2; }
.level-badge { flex: none; max-width: 240rpx; padding: 10rpx 14rpx; border: 1rpx solid rgba(255,255,255,.2); border-radius: 20rpx; color: #d8f5e9; background: rgba(255,255,255,.1); font-size: 20rpx; line-height: 1.35; text-align: center; }
.hero-caption { margin-top: 12rpx; color: rgba(255,255,255,.78); font-size: 23rpx; }
.hero-stats { display: flex; margin-top: 30rpx; padding-top: 24rpx; border-top: 1rpx solid rgba(255,255,255,.16); }
.hero-stat { flex: 1; text-align: center; }
.hero-stat + .hero-stat { border-left: 1rpx solid rgba(255,255,255,.16); }
.hero-stat-value { display: block; font-size: 40rpx; font-weight: 800; line-height: 1.2; }
.hero-stat-label { display: block; margin-top: 5rpx; color: rgba(255,255,255,.68); font-size: 21rpx; }
.reminder-card { margin-top: 20rpx; padding: 24rpx 26rpx; border: 2rpx solid #f2dfac; border-radius: 26rpx; background: #fff9e9; }
.reminder-title { color: #80580d; font-size: 27rpx; font-weight: 780; }
.reminder-copy { margin-top: 8rpx; color: #8b7351; font-size: 24rpx; line-height: 1.55; }
.section-label { display: flex; align-items: center; gap: 12rpx; margin: 30rpx 8rpx 18rpx; color: #183c34; font-size: 32rpx; font-weight: 800; }
.section-line { width: 8rpx; height: 34rpx; border-radius: 6rpx; background: #23b88e; }
.balance-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16rpx; }
.balance-card { min-height: 194rpx; padding: 24rpx 22rpx; border: 1rpx solid #d6eee5; border-radius: 26rpx; box-sizing: border-box; background: #fff; }
.bank-card { background: linear-gradient(145deg, #f4fffb, #fff); }
.trading-card { border-color: #cce8f1; background: linear-gradient(145deg, #f3fcff, #fff); }
.balance-label { color: #476a60; font-size: 23rpx; font-weight: 700; }
.balance-value { margin-top: 18rpx; color: #0b906e; font-size: 48rpx; font-weight: 820; line-height: 1; }
.trading-card .balance-value { color: #247fa9; }
.balance-note { margin-top: 14rpx; color: #82958f; font-size: 20rpx; line-height: 1.35; }
.card { margin-top: 20rpx; padding: 26rpx; border: 1rpx solid #dcece6; border-radius: 28rpx; background: #fff; box-shadow: 0 10rpx 26rpx rgba(24, 91, 75, .045); }
.card-heading { color: #173e34; font-size: 29rpx; font-weight: 800; }
.progress-copy { display: flex; align-items: baseline; justify-content: space-between; gap: 16rpx; margin-top: 18rpx; color: #78918a; font-size: 22rpx; }
.progress-copy text:first-child { color: #0b8e6d; font-size: 34rpx; font-weight: 800; }
.progress-track { height: 14rpx; margin-top: 16rpx; overflow: hidden; border-radius: 14rpx; background: #e1f0eb; }
.progress-fill { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #4bc99b, #0c8e6c); }
.level-steps { display: flex; margin-top: 22rpx; }
.level-step { position: relative; flex: 1; text-align: center; }
.level-step:not(:last-child)::after { content: ''; position: absolute; top: 9rpx; left: 50%; width: 100%; height: 2rpx; background: #dcebe6; }
.level-step.reached:not(:last-child)::after { background: #83d8bb; }
.step-dot { position: relative; z-index: 1; width: 18rpx; height: 18rpx; margin: 0 auto; border: 4rpx solid #dcebe6; border-radius: 50%; background: #fff; box-sizing: border-box; }
.level-step.reached .step-dot { border-color: #18a47f; background: #18a47f; }
.step-name { margin-top: 8rpx; color: #46665d; font-size: 19rpx; }
.level-step.reached .step-name { color: #0d896a; font-weight: 700; }
.step-threshold { margin-top: 2rpx; color: #95a7a1; font-size: 17rpx; }
.reward-row { display: flex; align-items: baseline; justify-content: space-between; gap: 12rpx; margin-top: 18rpx; }
.reward-main { color: #759089; font-size: 22rpx; }
.reward-number { color: #0a906e; font-size: 40rpx; font-weight: 800; }
.reward-label { color: #7a938c; font-size: 21rpx; }
.reward-track { margin-top: 14rpx; }
.protection-copy { margin-top: 16rpx; color: #69847a; font-size: 22rpx; line-height: 1.5; }
.register-card { border-color: #f0dda4; background: linear-gradient(135deg, #fffaf0, #fff); }
.register-copy, .region-copy, .referral-note { margin-top: 10rpx; color: #7e8e88; font-size: 22rpx; line-height: 1.55; }
.field-label { margin-top: 18rpx; color: #48665e; font-size: 23rpx; font-weight: 700; }
.text-input { height: 78rpx; margin-top: 10rpx; padding: 0 20rpx; border: 2rpx solid #d8e9e2; border-radius: 16rpx; box-sizing: border-box; color: #23463c; background: #fff; font-size: 25rpx; }
.picker-field { display: flex; align-items: center; justify-content: space-between; min-height: 78rpx; margin-top: 10rpx; padding: 0 20rpx; border: 2rpx solid #d8e9e2; border-radius: 16rpx; box-sizing: border-box; color: #23463c; background: #fff; font-size: 25rpx; }
.picker-field.placeholder { color: #9aaea7; }
.picker-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.picker-arrow { margin-left: 12rpx; color: #0d8568; font-size: 38rpx; line-height: 1; }
.field-helper { margin-top: 8rpx; color: #81958e; font-size: 20rpx; line-height: 1.4; }
.primary-button, .secondary-button { height: 84rpx; margin-top: 20rpx; border: 0; border-radius: 18rpx; font-size: 28rpx; font-weight: 750; line-height: 84rpx; }
.primary-button { color: #fff; background: #0d8568; }
.secondary-button { color: #0d8064; background: #ddf5eb; }
.primary-button[disabled], .secondary-button[disabled] { opacity: .55; }
.referral-card { text-align: center; }
.referral-code { margin-top: 18rpx; padding: 18rpx 16rpx; border: 2rpx dashed #8ed6c0; border-radius: 16rpx; color: #087d60; background: #f1fcf7; font-family: monospace; font-size: 34rpx; font-weight: 800; letter-spacing: 2rpx; word-break: break-all; }
.region-current { margin-top: 16rpx; color: #0a8969; font-size: 28rpx; font-weight: 750; }
.ledger-header { display: flex; align-items: center; justify-content: space-between; }
.text-button { min-width: 88rpx; height: 52rpx; margin: 0; padding: 0 12rpx; border: 1rpx solid #bfe7da; border-radius: 14rpx; color: #0c8568; background: #f3fcf8; font-size: 21rpx; line-height: 50rpx; }
.text-button[disabled] { opacity: .55; }
.ledger-empty { padding: 28rpx 0 6rpx; color: #94a49e; text-align: center; font-size: 22rpx; }
.ledger-row { display: flex; align-items: center; gap: 18rpx; padding: 20rpx 0; border-bottom: 1rpx solid #edf2ef; }
.ledger-row:last-child { border-bottom: 0; }
.ledger-copy { flex: 1; min-width: 0; }
.ledger-title { overflow: hidden; color: #31554b; font-size: 23rpx; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.ledger-meta { margin-top: 5rpx; color: #97a7a2; font-size: 19rpx; }
.ledger-amount { flex: none; color: #0c936e; font-size: 28rpx; font-weight: 800; }
.ledger-amount.debit { color: #bd6b4f; }
.disclaimer { padding: 28rpx 10rpx 10rpx; color: #8b9b95; text-align: center; font-size: 20rpx; line-height: 1.55; }
.disabled-card { margin-top: 30rpx; padding: 40rpx 28rpx; border: 1rpx solid #dcebe6; border-radius: 28rpx; text-align: center; background: #fff; }
.disabled-title { color: #244a40; font-size: 34rpx; font-weight: 800; }
.disabled-copy { margin-top: 12rpx; color: #7f938c; font-size: 24rpx; line-height: 1.55; }
@media (max-width: 360px) { .hero-title { font-size: 40rpx; } .balance-value { font-size: 42rpx; } .progress-copy { display: block; } .progress-copy text { display: block; } .progress-copy text + text { margin-top: 8rpx; } }
</style>

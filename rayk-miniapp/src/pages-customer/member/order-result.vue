<template>
  <view class="page member-page">
    <view class="checkout-page">
      <view class="brand-row">
        <view class="brand-mark"><view class="brand-mark-inner">✓</view></view>
        <view class="brand-copy">
          <view class="eyebrow">HEALTH MEMBERSHIP</view>
          <view class="brand-name">三羊健康会员</view>
        </view>
        <view class="security-badge"><view class="security-dot">✓</view>安全支付</view>
      </view>

      <view class="status-card" :class="{ 'is-paid': isPaid, 'is-waiting': paymentWaiting }">
        <view class="status-decoration status-decoration-one" />
        <view class="status-decoration status-decoration-two" />
        <view class="status-icon-wrap"><view class="status-icon">✓</view></view>
        <view class="status-kicker">{{ statusKicker }}</view>
        <view class="status-title">{{ statusTitle }}</view>
        <view class="order-meta">
          <view class="plan-name">{{ planLabel }}</view>
          <view class="price"><text>¥</text>{{ priceLabel }}<text class="price-unit">/年</text></view>
        </view>
        <view class="status-description">{{ statusDescription }}</view>
      </view>

      <view class="benefit-card">
        <view class="section-title">支付后立即享受</view>
        <view class="benefit-grid">
          <view class="benefit-item">
            <view class="benefit-symbol symbol-ai">AI</view>
            <view class="benefit-title">AI 健康服务</view>
            <view class="benefit-caption">评估 · 报告</view>
          </view>
          <view class="benefit-item">
            <view class="benefit-symbol symbol-followup">随</view>
            <view class="benefit-title">持续健康随访</view>
            <view class="benefit-caption">动态跟进</view>
          </view>
          <view class="benefit-item">
            <view class="benefit-symbol symbol-trend">↗</view>
            <view class="benefit-title">健康趋势档案</view>
            <view class="benefit-caption">长期记录</view>
          </view>
        </view>
      </view>

      <view v-if="order?.paymentEnabled" class="payment-notice">
        <view class="notice-icon">i</view>
        <view class="notice-copy">
          <view class="notice-title">微信支付完成后自动开通</view>
          <view class="notice-text">微信发货通知确认权益后，会员状态会自动更新。</view>
        </view>
      </view>
      <view v-else class="payment-notice dev-notice">
        <view class="notice-icon">!</view>
        <view class="notice-copy">
          <view class="notice-title">当前为开发环境</view>
          <view class="notice-text">本次操作用于模拟会员开通，不会产生真实扣款。</view>
        </view>
      </view>

      <button v-if="!isPaid" class="primary-button" :disabled="paying || paymentWaiting" @click="pay">
        {{ paymentButtonLabel }}
      </button>
      <button v-else class="primary-button" @click="back">会员已开通，返回会员中心</button>
      <button class="ghost-button" @click="back">{{ isPaid ? '返回会员中心' : '稍后再支付' }}</button>
      <view class="footnote">服务仅用于健康管理参考，不替代临床诊断与医生面诊</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import {
  createMembershipOrder,
  createWechatMembershipPayment,
  getMembershipOrder,
  payMembershipOrder,
  type MembershipOrder,
  type MembershipPaymentParams,
} from '@/api/membership'

const orderNo = ref('')
const order = ref<MembershipOrder>()
const paying = ref(false)
const paymentWaiting = ref(false)

const isPaid = computed(() => order.value?.status === 'PAID')
const planLabel = computed(() => order.value?.planName || '年度健康会员')
const priceLabel = computed(() => (order.value ? (order.value.amountCent / 100).toFixed(0) : '399'))
const statusKicker = computed(() => {
  if (isPaid.value) return 'PAYMENT COMPLETE'
  if (paymentWaiting.value) return 'PAYMENT VERIFYING'
  if (paying.value) return 'SECURE CHECKOUT'
  return 'MEMBERSHIP CHECKOUT'
})
const statusTitle = computed(() => {
  if (isPaid.value) return '会员已开通'
  if (paymentWaiting.value) return '权益确认中'
  if (paying.value) return '正在打开微信支付'
  return '准备开启年度会员'
})
const statusDescription = computed(() => {
  if (isPaid.value) return '年度健康管理服务已成功解锁，感谢你的支持。'
  if (paymentWaiting.value) return '支付已完成，正在等待微信发货通知确认会员权益。'
  if (paying.value) return '正在连接微信安全支付，请在支付面板中完成确认。'
  return order.value?.paymentEnabled ? '确认支付后，立即解锁年度会员专属健康服务。' : '确认后立即开通年度会员专属健康服务。'
})
const paymentButtonLabel = computed(() => {
  if (paying.value) return '正在打开微信支付…'
  if (paymentWaiting.value) return '权益确认中…'
  if (order.value?.paymentEnabled) return `立即支付 ¥${priceLabel.value}/年`
  return '模拟开通会员'
})

type WechatVirtualPaymentError = Error & { errCode?: number; errMsg?: string }
type WechatVirtualPaymentApi = {
  requestVirtualPayment: (options: {
    mode: 'short_series_goods' | 'short_series_coin' | string
    signData: string
    paySig: string
    signature: string
    success?: () => void
    fail?: (error: { errMsg?: string; errCode?: number }) => void
  }) => void
}

onLoad(async (options) => {
  orderNo.value = String(options?.orderNo || '')
  if (orderNo.value) order.value = await getMembershipOrder(orderNo.value)
})

const wait = (milliseconds: number) => new Promise((resolve) => setTimeout(resolve, milliseconds))

const invokeWechatPayment = (params: MembershipPaymentParams) =>
  new Promise<void>((resolve, reject) => {
    // #ifdef MP-WEIXIN
    const wechat = (globalThis as unknown as { wx?: WechatVirtualPaymentApi }).wx
    if (!wechat?.requestVirtualPayment) {
      reject(new Error('当前微信基础库不支持虚拟支付，请升级微信后重试'))
      return
    }
    wechat.requestVirtualPayment({
      mode: params.mode,
      signData: params.signData,
      paySig: params.paySig,
      signature: params.signature,
      success: () => resolve(),
      fail: (error) => {
        const code = error.errCode
        const message = code === -2 ? '支付已取消' : error.errMsg || '微信虚拟支付失败'
        const failure = Object.assign(new Error(message), {
          errCode: code,
          errMsg: error.errMsg,
        }) as WechatVirtualPaymentError
        reject(failure)
      },
    })
    // #endif
    // #ifndef MP-WEIXIN
    reject(new Error('请在微信小程序中完成支付'))
    // #endif
  })

const refreshPaidOrder = async () => {
  for (let attempt = 0; attempt < 6; attempt += 1) {
    order.value = await getMembershipOrder(orderNo.value)
    if (order.value.status === 'PAID') return true
    await wait(1500)
  }
  return false
}

const isClosedVirtualPaymentOrder = (cause: unknown) => {
  if (!(cause instanceof Error)) return false
  const paymentError = cause as WechatVirtualPaymentError
  return `${paymentError.message} ${paymentError.errMsg || ''}`.toUpperCase().includes('ORDER_CLOSED')
}

const requestWechatPayment = async () => {
  const payment = await createWechatMembershipPayment(orderNo.value)
  await invokeWechatPayment(payment)
  const paid = await refreshPaidOrder()
  paymentWaiting.value = !paid
  uni.showToast({ title: paid ? '会员已开通' : '支付成功，权益确认中', icon: paid ? 'success' : 'none' })
}

const pay = async () => {
  if (!orderNo.value || !order.value || order.value.status !== 'PENDING') return
  paying.value = true
  try {
    if (order.value.paymentEnabled) {
      try {
        await requestWechatPayment()
      } catch (cause) {
        // WeChat virtual-payment order numbers are single-use. If the user
        // retries after WeChat has closed the previous attempt, create a new
        // backend order so the next request gets a fresh outTradeNo.
        if (!isClosedVirtualPaymentOrder(cause)) throw cause
        const replacement = await createMembershipOrder(order.value.planCode)
        orderNo.value = replacement.orderNo
        order.value = replacement
        await requestWechatPayment()
      }
    } else {
      order.value = await payMembershipOrder(orderNo.value)
      uni.showToast({ title: '会员已开通', icon: 'success' })
    }
  } catch (cause) {
    const message = isClosedVirtualPaymentOrder(cause)
      ? '微信支付订单已失效，请重新点击支付'
      : cause instanceof Error
        ? cause.message
        : '开通失败'
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    paying.value = false
  }
}

const back = () => uni.navigateBack()
</script>

<style scoped>
.member-page { min-height: 100vh; padding: 28rpx 24rpx 52rpx; box-sizing: border-box; background: linear-gradient(180deg, #eefaf6 0%, #f9fcfb 42%, #f2faf7 100%); }
.checkout-page { width: 100%; }
.brand-row { display: flex; align-items: center; padding: 8rpx 8rpx 22rpx; }
.brand-mark { display: flex; align-items: center; justify-content: center; width: 58rpx; height: 58rpx; border-radius: 20rpx; background: linear-gradient(145deg, #62d58e, #0ca78b); box-shadow: 0 8rpx 18rpx rgba(24, 150, 111, .18); }
.brand-mark-inner { display: flex; align-items: center; justify-content: center; width: 34rpx; height: 34rpx; border: 2rpx solid rgba(255, 255, 255, .9); border-radius: 50%; color: #fff; font-size: 22rpx; font-weight: 700; }
.brand-copy { margin-left: 14rpx; }
.eyebrow { color: #5e9f8a; font-size: 18rpx; font-weight: 800; letter-spacing: 3rpx; }
.brand-name { margin-top: 4rpx; color: #143f35; font-size: 29rpx; font-weight: 800; }
.security-badge { display: flex; align-items: center; margin-left: auto; padding: 12rpx 16rpx; border: 1rpx solid #cfece1; border-radius: 24rpx; color: #318b72; background: rgba(255, 255, 255, .72); font-size: 21rpx; }
.security-dot { display: flex; align-items: center; justify-content: center; width: 28rpx; height: 28rpx; margin-right: 7rpx; border-radius: 50%; color: #fff; background: #22b889; font-size: 17rpx; font-weight: 700; }
.status-card { position: relative; overflow: hidden; padding: 36rpx 28rpx 30rpx; border-radius: 32rpx; color: #fff; background: linear-gradient(135deg, #08735f 0%, #0b9677 54%, #15b58f 100%); box-shadow: 0 16rpx 34rpx rgba(18, 119, 91, .18); text-align: center; }
.status-card::before { position: absolute; top: -160rpx; right: -120rpx; width: 360rpx; height: 360rpx; border: 1rpx solid rgba(255, 255, 255, .12); border-radius: 50%; content: ''; }
.status-card.is-paid { background: linear-gradient(135deg, #086b58 0%, #0b9a78 100%); }
.status-decoration { position: absolute; border-radius: 50%; background: rgba(255, 255, 255, .08); }
.status-decoration-one { right: 48rpx; bottom: -86rpx; width: 190rpx; height: 190rpx; }
.status-decoration-two { left: -64rpx; top: 80rpx; width: 130rpx; height: 130rpx; }
.status-icon-wrap { position: relative; z-index: 1; display: flex; align-items: center; justify-content: center; width: 96rpx; height: 96rpx; margin: 0 auto 16rpx; border: 1rpx solid rgba(255, 255, 255, .42); border-radius: 50%; background: rgba(255, 255, 255, .14); }
.status-icon { display: flex; align-items: center; justify-content: center; width: 68rpx; height: 68rpx; border-radius: 50%; color: #0b8d6e; background: #fff; font-size: 38rpx; font-weight: 700; }
.status-kicker { position: relative; z-index: 1; color: rgba(255, 255, 255, .72); font-size: 18rpx; font-weight: 800; letter-spacing: 3rpx; }
.status-title { position: relative; z-index: 1; margin-top: 8rpx; font-size: 38rpx; font-weight: 800; }
.order-meta { position: relative; z-index: 1; display: flex; align-items: center; justify-content: space-between; margin-top: 24rpx; padding: 20rpx 22rpx; border: 1rpx solid rgba(255, 255, 255, .22); border-radius: 20rpx; background: rgba(4, 88, 70, .22); text-align: left; }
.plan-name { color: rgba(255, 255, 255, .95); font-size: 27rpx; font-weight: 700; }
.price { color: #fff5c9; font-size: 38rpx; font-weight: 800; white-space: nowrap; }
.price > text:first-child { font-size: 23rpx; }
.price-unit { margin-left: 3rpx; color: rgba(255, 255, 255, .82); font-size: 21rpx; font-weight: 500; }
.status-description { position: relative; z-index: 1; margin-top: 18rpx; color: rgba(255, 255, 255, .86); font-size: 23rpx; line-height: 1.5; }
.benefit-card { margin-top: 22rpx; padding: 26rpx 22rpx 22rpx; border: 1rpx solid #dcefe8; border-radius: 30rpx; background: rgba(255, 255, 255, .96); box-shadow: 0 10rpx 26rpx rgba(24, 91, 75, .06); }
.section-title { color: #153f35; font-size: 29rpx; font-weight: 800; }
.benefit-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12rpx; margin-top: 20rpx; }
.benefit-item { min-height: 148rpx; padding: 16rpx 6rpx 12rpx; border: 1rpx solid #e3f1ec; border-radius: 20rpx; text-align: center; background: #f8fcfa; }
.benefit-symbol { display: flex; align-items: center; justify-content: center; width: 54rpx; height: 54rpx; margin: 0 auto 10rpx; border-radius: 17rpx; font-size: 22rpx; font-weight: 800; }
.symbol-ai { color: #078f72; background: #d9f7e9; }
.symbol-followup { color: #0b8da6; background: #d9f3f7; }
.symbol-trend { color: #a56a13; background: #fff0d3; font-size: 31rpx; }
.benefit-title { color: #24483f; font-size: 22rpx; font-weight: 700; line-height: 1.3; }
.benefit-caption { margin-top: 6rpx; color: #829891; font-size: 19rpx; line-height: 1.3; }
.payment-notice { display: flex; align-items: flex-start; margin-top: 20rpx; padding: 20rpx 22rpx; border: 1rpx solid #d4ede4; border-radius: 22rpx; background: #f3fcf8; }
.dev-notice { border-color: #f2dfb4; background: #fffaf0; }
.notice-icon { display: flex; align-items: center; justify-content: center; width: 34rpx; height: 34rpx; flex: none; margin-top: 2rpx; border-radius: 50%; color: #fff; background: #1aaa82; font-size: 22rpx; font-weight: 800; }
.dev-notice .notice-icon { background: #d99528; }
.notice-copy { margin-left: 12rpx; }
.notice-title { color: #26725e; font-size: 23rpx; font-weight: 700; }
.dev-notice .notice-title { color: #9a6619; }
.notice-text { margin-top: 6rpx; color: #78908a; font-size: 20rpx; line-height: 1.45; }
.primary-button, .ghost-button { width: 100%; height: 88rpx; margin: 24rpx 0 0; padding: 0; border: 0; border-radius: 44rpx; font-size: 29rpx; font-weight: 700; line-height: 88rpx; }
.primary-button { color: #fff; background: linear-gradient(90deg, #5bcf7c, #08a9d4); box-shadow: 0 10rpx 22rpx rgba(20, 173, 157, .2); }
.ghost-button { margin-top: 14rpx; color: #0d8066; background: #e9f7f2; }
.primary-button[disabled] { opacity: .64; }
button::after { border: 0; }
.footnote { margin: 20rpx 24rpx 0; color: #9aa9a5; font-size: 19rpx; line-height: 1.45; text-align: center; }
@media (max-width: 360px) {
  .brand-name { font-size: 26rpx; }
  .security-badge { padding-left: 12rpx; padding-right: 12rpx; font-size: 19rpx; }
  .status-title { font-size: 34rpx; }
  .plan-name, .price { font-size: 24rpx; }
  .benefit-title { font-size: 20rpx; }
  .benefit-caption { font-size: 17rpx; }
}
</style>

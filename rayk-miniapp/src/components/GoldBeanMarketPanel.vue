<template>
  <view class="page market-page">
    <view v-if="!goldBeanEnabled" class="disabled-card">
      <view class="disabled-title">金豆会员暂未开放</view>
      <view class="disabled-copy">该功能仅用于开发环境验收，正式版本不会启用。</view>
    </view>

    <PageState v-else :loading="loading" :error="error" :empty="!summary">
      <view v-if="summary" class="market-content">
        <view class="market-hero">
          <view class="market-hero-top">
            <view>
              <view class="market-eyebrow">GOLD BEAN MARKET</view>
              <view class="market-title">金豆集市</view>
            </view>
            <view class="price-badge">卖家到账 ￥1.00 / 豆</view>
          </view>
          <view class="market-subtitle">{{ marketScopeText }}</view>
          <view class="market-stats">
            <view class="market-stat">
              <text class="market-stat-value">{{ formatAmount(activeTotal) }}</text>
              <text class="market-stat-label">{{ activeView === 'MARKET' ? '当前挂单' : '我的挂单' }}</text>
            </view>
            <view v-if="!legendaryEligible" class="market-stat">
              <text class="market-stat-value">{{ formatGoldBeanBalance(summary.tradingBalance) }}</text>
              <text class="market-stat-label">可交易金豆</text>
            </view>
            <view class="market-stat">
              <text class="market-stat-value">{{ formatGoldBeanBalance(summary.digitalBankBalance) }}</text>
              <text class="market-stat-label">数字银行</text>
            </view>
          </view>
        </view>

        <view v-if="summary.registrationFeeStatus !== 'PAID'" class="card locked-card">
          <view class="card-title">完成注册后使用集市</view>
          <view class="locked-copy">注册成功并完成会员费支付后，才可以发布或购买金豆挂单。</view>
        </view>

        <template v-else>
          <view class="market-rule-card">
            <view class="market-rule-title">交易规则</view>
            <view class="market-rule-copy">卖家结算单价 ￥1.00 / 金豆；买家实际支付含 {{ surchargePercent }}% 虚拟支付服务费。{{ marketRuleText }}</view>
          </view>

          <view v-if="legendaryEligible" class="legendary-market-notice">
            <view class="legendary-market-notice-title">传奇人物专属交易方式</view>
            <view class="legendary-market-notice-copy">仅可购买全国数字银行金豆挂单，买入后全部进入数字银行；不能向平台直接购买，也不能发布挂单。</view>
          </view>

          <view v-else class="market-tabs" role="tablist" aria-label="集市内容切换">
            <view class="market-tab" :class="{ active: activeView === 'MARKET' }" role="tab" :aria-selected="activeView === 'MARKET'" @tap="switchView('MARKET')">市场挂单</view>
            <view class="market-tab" :class="{ active: activeView === 'MINE' }" role="tab" :aria-selected="activeView === 'MINE'" @tap="switchView('MINE')">我的挂单</view>
          </view>

          <view v-if="!legendaryEligible" class="card publish-card">
            <view class="card-heading-row">
              <view>
                <view class="card-title">发布挂单</view>
                <view class="card-subtitle">只需填写数量，单价固定为 ￥1.00 / 金豆</view>
              </view>
              <view class="limit-badge">交易额度 {{ summary.tradeLimitPercent }}%</view>
            </view>
            <view class="field-label">出售来源</view>
            <view class="source-options" role="radiogroup" aria-label="选择出售的金豆来源">
              <view
                v-for="source in listingBuckets"
                :key="source.value"
                class="source-option"
                :class="{ active: listingForm.bucket === source.value }"
                role="radio"
                :aria-checked="listingForm.bucket === source.value"
                @tap="setListingBucket(source.value)"
              >
                <view class="source-option-title">{{ source.label }}</view>
                <view class="source-option-note">{{ source.note }}</view>
              </view>
            </view>
            <view class="publish-input-row">
              <input v-model="listingForm.quantity" class="text-input publish-input" type="number" step="1" inputmode="numeric" placeholder="请输入发布数量" />
              <button class="publish-button" :disabled="listingSubmitting" @click="createListing">{{ listingSubmitting ? '发布中…' : '发布挂单' }}</button>
            </view>
            <view class="field-helper">可发布余额 {{ formatGoldBeanBalance(listingSourceBalance) }} 豆；系统会同时校验账户总交易额度和该来源余额。</view>
          </view>

          <view class="list-heading-row">
            <view class="list-heading">{{ activeView === 'MARKET' ? '正在出售' : '我的挂单' }}</view>
            <view class="list-count">共 {{ activeTotal }} 条</view>
          </view>

          <view v-if="activeLoading" class="card list-state">正在加载挂单…</view>
          <view v-else-if="!activeRecords.length" class="card list-state">{{ activeView === 'MARKET' ? '暂时没有符合条件的挂单' : '你还没有发布过挂单' }}</view>
          <template v-else>
            <view v-for="listing in activeRecords" :key="listing.id" class="card listing-card">
              <view class="listing-topline">
                <view class="listing-source">
                  <view class="source-dot" :class="listing.bucket === 'DIGITAL_BANK' ? 'bank' : 'trading'" />
                  {{ bucketName(listing.bucket) }}
                </view>
                <view class="listing-time">{{ formatDate(listing.createdAt) }}</view>
              </view>
              <view class="listing-main-row">
                <view class="listing-quantity">{{ formatAmount(listing.remainingQuantity) }}<text> 豆可买</text></view>
                <view v-if="listing.mine" class="mine-badge">我的挂单</view>
                <view v-else class="open-badge">出售中</view>
              </view>
              <view class="listing-meta">区域 {{ listing.regionCity || '待补充' }} · 卖家到账 ￥{{ (listing.unitPriceCent / 100).toFixed(2) }} / 豆 · 剩余结算额 ￥{{ listingTotal(listing).toFixed(2) }}</view>
              <view v-if="listing.mine" class="mine-action-row">
                <view class="mine-note">原始数量 {{ formatAmount(listing.quantity) }} 豆 · {{ tradeStatusName(listing.status) }}</view>
                <button v-if="listing.status === 'OPEN'" class="cancel-button" :disabled="cancellingListingId === listing.id" @click="cancelListing(listing)">{{ cancellingListingId === listing.id ? '下架中…' : '下架' }}</button>
              </view>
              <view v-else class="buy-row">
                <input v-model="buyQuantities[listing.id]" class="text-input buy-input" type="number" step="1" inputmode="numeric" placeholder="请输入买入数量" />
                <button class="buy-button" :disabled="buyingListingId === listing.id" @click="buyListing(listing)">{{ buyingListingId === listing.id ? '成交中…' : '买入' }}</button>
              </view>
            </view>
          </template>

          <view v-if="activeTotal > activePageSize" class="pagination-card">
            <view class="pagination-button" :class="{ disabled: activePage <= 1 || activeLoading }" role="button" :aria-disabled="activePage <= 1 || activeLoading" @tap="changePage(-1)">上一页</view>
            <view class="pagination-info">第 {{ activePage }} / {{ activePageCount }} 页</view>
            <view class="pagination-button" :class="{ disabled: activePage >= activePageCount || activeLoading }" role="button" :aria-disabled="activePage >= activePageCount || activeLoading" @tap="changePage(1)">下一页</view>
          </view>
        </template>

        <view class="market-disclaimer">挂单、支付和交割均由服务端鉴权与校验；页面展示不代表已完成交易。</view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import PageState from '@/components/PageState.vue'
import { goldBeanEnabled } from '@/constants/features'
import { formatGoldBeanAmount, formatGoldBeanBalance, isValidIntegerGoldBeanAmount } from '@/utils/gold-bean-amount'
import {
  buyGoldBeanTradeListing,
  cancelGoldBeanTrade,
  cancelGoldBeanTradeListing,
  createGoldBeanTradeListing,
  createWechatGoldBeanTradePayment,
  getGoldBeanLegendarySummary,
  getGoldBeanSummary,
  getGoldBeanTrade,
  getGoldBeanTradeMarket,
  getMyGoldBeanTradeListings,
  type GoldBeanSummary,
  type GoldBeanPaymentParams,
  type GoldBeanTradeListing,
  type GoldBeanTradeListingPage,
} from '@/api/gold-bean'
import { isCancelledWechatVirtualPayment, isWechatVirtualPaymentCapabilityRestricted, requestWechatVirtualPayment } from '@/utils/wechat-virtual-payment'

type MarketView = 'MARKET' | 'MINE'
type ListingBucket = 'TRADING' | 'DIGITAL_BANK'

const PAGE_SIZE = 12
const loading = ref(true)
const error = ref('')
const summary = ref<GoldBeanSummary>()
const legendaryEligible = ref(false)
const activeView = ref<MarketView>('MARKET')
const marketLoading = ref(false)
const mineLoading = ref(false)
const listingSubmitting = ref(false)
const buyingListingId = ref('')
const cancellingListingId = ref('')
const marketPage = ref<GoldBeanTradeListingPage>({ records: [], total: 0, page: 1, size: PAGE_SIZE })
const minePage = ref<GoldBeanTradeListingPage>({ records: [], total: 0, page: 1, size: PAGE_SIZE })
const listingForm = reactive<{ quantity: string; bucket: ListingBucket }>({ quantity: '10', bucket: 'TRADING' })
const buyQuantities = reactive<Record<string, string>>({})

const listingBuckets: ReadonlyArray<{ value: ListingBucket; label: string; note: string }> = [
  { value: 'TRADING', label: '可交易金豆', note: '普通会员可购买；买家到账各一半' },
  { value: 'DIGITAL_BANK', label: '数字银行金豆', note: '仅传奇人物可购买；买家全部进入数字银行' },
]

const activePageData = computed(() => (activeView.value === 'MARKET' ? marketPage.value : minePage.value))
const activeRecords = computed(() => activePageData.value.records)
const activeTotal = computed(() => activePageData.value.total)
const activePage = computed(() => activePageData.value.page)
const activePageSize = computed(() => activePageData.value.size || PAGE_SIZE)
const activePageCount = computed(() => Math.max(1, Math.ceil(activeTotal.value / activePageSize.value)))
const activeLoading = computed(() => (activeView.value === 'MARKET' ? marketLoading.value : mineLoading.value))
const listingSourceBalance = computed(() => {
  if (!summary.value) return 0
  return listingForm.bucket === 'DIGITAL_BANK' ? summary.value.digitalBankBalance : summary.value.tradingBalance
})
const marketScopeText = computed(() => {
  if (legendaryEligible.value) return '传奇人物可购买全国各地的数字银行金豆挂单'
  return summary.value?.regionCity
    ? `普通会员仅可交易本注册区域（${summary.value.regionCity}）的挂单`
    : '普通会员需先完成注册区域城市后才能交易'
})
const marketRuleText = computed(() => {
  if (legendaryEligible.value) return '传奇人物不受区域限制，可购买全国各地的数字银行挂单，买入后全部进入数字银行。'
  return summary.value?.regionCity
    ? `普通会员仅可购买本注册区域（${summary.value.regionCity}）的可交易金豆，买入后分别进入可交易金豆和数字银行。`
    : '普通会员必须先完成注册区域城市，才可以购买可交易金豆。'
})

const formatAmount = (value: number) => formatGoldBeanAmount(value)
const formatDate = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '—')
const bucketName = (value: string) => (value === 'DIGITAL_BANK' ? '数字银行金豆' : '可交易金豆')
const tradeStatusName = (value: string) => ({ OPEN: '出售中', FILLED: '已售罄', CANCELLED: '已下架' }[value] || value)
const listingTotal = (listing: GoldBeanTradeListing) => Math.ceil(listing.remainingQuantity * listing.unitPriceCent - Number.EPSILON) / 100
const surchargePercent = computed(() => summary.value?.virtualPaymentSurchargePercent ?? 12)
const buyerPaymentTotal = (listing: GoldBeanTradeListing, quantity: number) => {
  const baseAmountCent = Math.ceil(quantity * listing.unitPriceCent - Number.EPSILON)
  return Math.ceil((baseAmountCent * (100 + surchargePercent.value)) / 100 - Number.EPSILON) / 100
}

function setListingBucket(bucket: ListingBucket) {
  listingForm.bucket = bucket
}

function seedBuyQuantity(listings: GoldBeanTradeListing[]) {
  listings.forEach((listing) => {
    if (!buyQuantities[listing.id]) buyQuantities[listing.id] = String(Math.min(10, listing.remainingQuantity))
  })
}

async function loadMarketPage(pageNumber = marketPage.value.page) {
  if (marketLoading.value || !summary.value || summary.value.registrationFeeStatus !== 'PAID') return
  marketLoading.value = true
  try {
    const response = await getGoldBeanTradeMarket({ page: Math.max(1, pageNumber), size: PAGE_SIZE })
    marketPage.value = response
    seedBuyQuantity(response.records)
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '金豆集市加载失败', icon: 'none' })
  } finally {
    marketLoading.value = false
  }
}

async function loadMinePage(pageNumber = minePage.value.page) {
  if (legendaryEligible.value || mineLoading.value || !summary.value || summary.value.registrationFeeStatus !== 'PAID') return
  mineLoading.value = true
  try {
    minePage.value = await getMyGoldBeanTradeListings({ page: Math.max(1, pageNumber), size: PAGE_SIZE })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '我的挂单加载失败', icon: 'none' })
  } finally {
    mineLoading.value = false
  }
}

async function loadActivePage() {
  if (activeView.value === 'MARKET') await loadMarketPage(activePage.value)
  else await loadMinePage(activePage.value)
}

async function switchView(view: MarketView) {
  if (legendaryEligible.value && view !== 'MARKET') {
    uni.showToast({ title: '传奇人物仅可浏览并购买数字银行挂单', icon: 'none' })
    return
  }
  if (activeView.value === view) return
  activeView.value = view
  if (summary.value?.registrationFeeStatus === 'PAID') await loadActivePage()
}

async function changePage(delta: number) {
  if (activeLoading.value) return
  const targetPage = activePage.value + delta
  if (targetPage < 1 || targetPage > activePageCount.value) return
  if (activeView.value === 'MARKET') await loadMarketPage(targetPage)
  else await loadMinePage(targetPage)
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
    const legendary = await getGoldBeanLegendarySummary().catch(() => undefined)
    legendaryEligible.value = Boolean(legendary?.eligible)
    if (legendaryEligible.value) activeView.value = 'MARKET'
    if (summary.value.registrationFeeStatus === 'PAID') await loadActivePage()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '金豆集市加载失败'
  } finally {
    loading.value = false
  }
}

defineExpose({ refresh: load })

async function createListing() {
  if (listingSubmitting.value) return
  if (legendaryEligible.value) {
    uni.showToast({ title: '传奇人物不能发布金豆挂单', icon: 'none' })
    return
  }
  const quantity = Number(listingForm.quantity)
  if (!isValidIntegerGoldBeanAmount(quantity, 1_000_000)) {
    uni.showToast({ title: '请输入整数发布数量', icon: 'none' })
    return
  }
  listingSubmitting.value = true
  try {
    await createGoldBeanTradeListing({ quantity, bucket: listingForm.bucket })
    listingForm.quantity = '10'
    await load()
    uni.showToast({ title: '挂单已发布', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '挂单发布失败', icon: 'none' })
  } finally {
    listingSubmitting.value = false
  }
}

async function buyListing(listing: GoldBeanTradeListing) {
  if (buyingListingId.value) return
  if (legendaryEligible.value && listing.bucket !== 'DIGITAL_BANK') {
    uni.showToast({ title: '传奇人物只能购买数字银行金豆挂单', icon: 'none' })
    return
  }
  const quantity = Number(buyQuantities[listing.id])
  if (!isValidIntegerGoldBeanAmount(quantity, listing.remainingQuantity)) {
    uni.showToast({ title: '请输入整数买入数量', icon: 'none' })
    return
  }
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '确认买入',
      content: `将使用金豆虚拟道具支付 ￥${buyerPaymentTotal(listing, quantity).toFixed(2)}（卖家到账 ￥${(Math.ceil(quantity * listing.unitPriceCent) / 100).toFixed(2)}），买入 ${formatAmount(quantity)} 个${bucketName(listing.bucket)}。`,
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    })
  })
  if (!confirmed) return

  buyingListingId.value = listing.id
  try {
    const trade = await buyGoldBeanTradeListing(listing.id, quantity)
    try {
      const payment = await createWechatGoldBeanTradePayment(trade.tradeNo)
      await requestWechatVirtualPayment(payment as GoldBeanPaymentParams)
    } catch (cause) {
      if (isCancelledWechatVirtualPayment(cause) || isWechatVirtualPaymentCapabilityRestricted(cause)) {
        await cancelGoldBeanTrade(trade.tradeNo).catch(() => undefined)
      }
      throw cause
    }
    let completed = false
    for (let attempt = 0; attempt < 8; attempt += 1) {
      const latest = await getGoldBeanTrade(trade.tradeNo)
      if (latest.status === 'COMPLETED') {
        completed = true
        break
      }
      if (latest.status === 'REFUND_REQUIRED') break
      await new Promise((resolve) => setTimeout(resolve, 1500))
    }
    await load()
    uni.showToast({ title: completed ? '买入成功' : '支付成功，平台正在确认卖家收款', icon: completed ? 'success' : 'none' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '买入失败', icon: 'none' })
  } finally {
    buyingListingId.value = ''
  }
}

async function cancelListing(listing: GoldBeanTradeListing) {
  if (cancellingListingId.value) return
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '下架挂单',
      content: `确认下架剩余 ${formatAmount(listing.remainingQuantity)} 个${bucketName(listing.bucket)}的挂单吗？`,
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    })
  })
  if (!confirmed) return

  cancellingListingId.value = listing.id
  try {
    await cancelGoldBeanTradeListing(listing.id)
    await load()
    uni.showToast({ title: '挂单已下架', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '挂单下架失败', icon: 'none' })
  } finally {
    cancellingListingId.value = ''
  }
}
</script>

<style scoped>
.market-page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: linear-gradient(180deg, #f1faf6 0%, #f8fbfa 100%); }
.market-content { padding-bottom: 42rpx; }
.market-hero { padding: 32rpx 28rpx 26rpx; border-radius: 30rpx; color: #f2fff9; background: linear-gradient(135deg, #16473a 0%, #0a8065 72%, #18ad88 100%); box-shadow: 0 18rpx 42rpx rgba(8, 91, 70, .18); }
.market-hero-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 14rpx; }
.market-eyebrow { color: rgba(255, 255, 255, .66); font-size: 18rpx; font-weight: 800; letter-spacing: 3rpx; }
.market-title { margin-top: 10rpx; font-size: 44rpx; font-weight: 820; line-height: 1.2; }
.price-badge { flex: none; padding: 10rpx 14rpx; border: 1rpx solid rgba(255, 255, 255, .22); border-radius: 18rpx; color: #d9f8eb; background: rgba(255, 255, 255, .1); font-size: 21rpx; font-weight: 700; }
.market-subtitle { margin-top: 12rpx; color: rgba(255, 255, 255, .8); font-size: 23rpx; }
.market-stats { display: flex; margin-top: 26rpx; padding-top: 22rpx; border-top: 1rpx solid rgba(255, 255, 255, .16); }
.market-stat { flex: 1; text-align: center; }
.market-stat + .market-stat { border-left: 1rpx solid rgba(255, 255, 255, .16); }
.market-stat-value { display: block; font-size: 31rpx; font-weight: 800; line-height: 1.2; }
.market-stat-label { display: block; margin-top: 6rpx; color: rgba(255, 255, 255, .68); font-size: 19rpx; }
.card { margin-top: 18rpx; padding: 24rpx; border: 1rpx solid #deece7; border-radius: 24rpx; box-sizing: border-box; background: #fff; box-shadow: 0 10rpx 24rpx rgba(24, 91, 75, .05); }
.card-title { color: #1a4338; font-size: 28rpx; font-weight: 800; }
.card-subtitle { margin-top: 7rpx; color: #8a9d96; font-size: 20rpx; line-height: 1.4; }
.locked-card { border-color: #eadcb4; background: #fffaf0; }
.locked-copy { margin-top: 12rpx; color: #7e806f; font-size: 22rpx; line-height: 1.55; }
.market-rule-card { margin-top: 18rpx; padding: 20rpx 22rpx; border: 1rpx solid #ccebdd; border-radius: 20rpx; background: #effaf5; }
.market-rule-title { color: #24705b; font-size: 23rpx; font-weight: 800; }
.market-rule-copy { margin-top: 8rpx; color: #65867c; font-size: 20rpx; line-height: 1.55; }
.legendary-market-notice { margin-top: 20rpx; padding: 20rpx 22rpx; border: 1rpx solid #ccebdd; border-radius: 20rpx; background: #f4fcf8; }
.legendary-market-notice-title { color: #24705b; font-size: 23rpx; font-weight: 800; }
.legendary-market-notice-copy { margin-top: 8rpx; color: #65867c; font-size: 20rpx; line-height: 1.55; }
.market-tabs { display: flex; gap: 10rpx; margin-top: 20rpx; padding: 6rpx; border-radius: 18rpx; background: #e9f4ef; }
.market-tab { display: flex; flex: 1; min-height: 76rpx; align-items: center; justify-content: center; border-radius: 14rpx; color: #78938a; font-size: 24rpx; font-weight: 700; }
.market-tab.active { color: #0a7d61; background: #fff; box-shadow: 0 5rpx 14rpx rgba(15, 122, 95, .08); }
.card-heading-row, .list-heading-row, .listing-topline, .listing-main-row, .mine-action-row, .buy-row { display: flex; align-items: center; justify-content: space-between; gap: 14rpx; }
.limit-badge { flex: none; padding: 8rpx 10rpx; border-radius: 13rpx; color: #9a6b22; background: #fff3d4; font-size: 19rpx; font-weight: 700; }
.field-label { margin-top: 20rpx; color: #48665e; font-size: 22rpx; font-weight: 700; }
.source-options { display: flex; gap: 10rpx; margin-top: 10rpx; }
.source-option { display: flex; flex: 1; min-width: 0; min-height: 90rpx; flex-direction: column; justify-content: center; padding: 0 14rpx; border: 2rpx solid #dcece6; border-radius: 16rpx; box-sizing: border-box; background: #fbfdfc; }
.source-option.active { border-color: #0d896c; background: #effaf5; }
.source-option:active, .market-tab:active, .pagination-button:active, .market-entry-card:active { opacity: .76; }
.source-option-title { overflow: hidden; color: #315d50; font-size: 22rpx; font-weight: 750; text-overflow: ellipsis; white-space: nowrap; }
.source-option-note { margin-top: 4rpx; overflow: hidden; color: #849991; font-size: 18rpx; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }
.publish-input-row { display: flex; align-items: center; gap: 10rpx; margin-top: 14rpx; }
.text-input { height: 78rpx; padding: 0 18rpx; border: 2rpx solid #d8e9e2; border-radius: 15rpx; box-sizing: border-box; color: #23463c; background: #fff; font-size: 24rpx; }
.publish-input { flex: 1; min-width: 0; margin: 0; }
.publish-button, .buy-button, .cancel-button { margin: 0; border: 0; font-weight: 750; }
.publish-button { flex: 0 0 164rpx; width: 164rpx; min-height: 78rpx; padding: 0 8rpx; border-radius: 15rpx; color: #fff; background: #0d8568; font-size: 23rpx; line-height: 78rpx; }
.publish-button[disabled], .buy-button[disabled], .cancel-button[disabled] { opacity: .52; }
.field-helper { margin-top: 8rpx; color: #81958e; font-size: 19rpx; line-height: 1.4; }
.list-heading-row { margin: 26rpx 4rpx 0; }
.list-heading { color: #21463c; font-size: 27rpx; font-weight: 800; }
.list-count { color: #91a19b; font-size: 20rpx; }
.list-state { padding: 46rpx 24rpx; color: #93a39d; text-align: center; font-size: 22rpx; }
.listing-card { padding: 22rpx; }
.listing-topline { align-items: flex-start; }
.listing-source { display: flex; align-items: center; min-width: 0; color: #386b5c; font-size: 22rpx; font-weight: 750; }
.source-dot { flex: none; width: 14rpx; height: 14rpx; margin-right: 8rpx; border-radius: 50%; }
.source-dot.trading { background: #39b68c; }
.source-dot.bank { background: #6b8fe2; }
.listing-time { flex: none; color: #a0aea8; font-size: 18rpx; }
.listing-main-row { margin-top: 16rpx; }
.listing-quantity { color: #0b8d6b; font-size: 38rpx; font-weight: 820; line-height: 1.1; }
.listing-quantity text { color: #769088; font-size: 20rpx; font-weight: 500; }
.mine-badge, .open-badge { flex: none; padding: 8rpx 11rpx; border-radius: 12rpx; font-size: 19rpx; font-weight: 700; }
.mine-badge { color: #0d8064; background: #e8f8f1; }
.open-badge { color: #8c6b31; background: #fff4d9; }
.listing-meta { margin-top: 10rpx; color: #879b93; font-size: 20rpx; line-height: 1.4; }
.mine-action-row { margin-top: 16rpx; align-items: center; }
.mine-note { flex: 1; min-width: 0; color: #7e918a; font-size: 20rpx; }
.cancel-button { flex: none; min-width: 104rpx; min-height: 64rpx; padding: 0 12rpx; border: 1rpx solid #f0c9b7; border-radius: 13rpx; color: #bd6b4d; background: #fff8f4; font-size: 21rpx; line-height: 62rpx; }
.buy-row { margin-top: 16rpx; }
.buy-input { flex: 1; min-width: 0; margin: 0; }
.buy-button { flex: 0 0 132rpx; width: 132rpx; min-height: 78rpx; border-radius: 15rpx; color: #fff; background: #0d8568; font-size: 24rpx; line-height: 78rpx; }
.pagination-card { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; margin-top: 20rpx; padding: 6rpx; border: 1rpx solid #dcece6; border-radius: 18rpx; background: #fff; }
.pagination-button { display: flex; min-width: 142rpx; min-height: 72rpx; align-items: center; justify-content: center; border-radius: 13rpx; color: #0d8064; background: #effaf5; font-size: 21rpx; font-weight: 700; }
.pagination-button.disabled { color: #a4b1ac; background: #f5f8f6; opacity: .7; pointer-events: none; }
.pagination-info { flex: 1; color: #7e938a; text-align: center; font-size: 20rpx; }
.market-disclaimer { padding: 28rpx 10rpx 8rpx; color: #8b9b95; text-align: center; font-size: 19rpx; line-height: 1.5; }
.disabled-card { margin-top: 30rpx; padding: 40rpx 28rpx; border: 1rpx solid #dcebe6; border-radius: 28rpx; text-align: center; background: #fff; }
.disabled-title { color: #244a40; font-size: 34rpx; font-weight: 800; }
.disabled-copy { margin-top: 10rpx; color: #81968d; font-size: 22rpx; }
</style>

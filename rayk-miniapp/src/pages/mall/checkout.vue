<template>
  <view class="page elder-page checkout-page">
    <view v-if="loading" class="card state-card">正在准备订单，请稍候…</view>

    <template v-else-if="product">
      <view class="section-title">确认商品</view>
      <view class="card item-card">
        <image v-if="product.mainImageUrl" class="item-image" :src="product.mainImageUrl" mode="aspectFill" />
        <view v-else class="item-image placeholder">康</view>
        <view class="item-info">
          <view class="item-name">{{ product.productName }}</view>
          <view v-if="product.subtitle" class="item-subtitle">{{ product.subtitle }}</view>
          <view class="item-meta">
            <text class="item-price">¥{{ (product.priceCent / 100).toFixed(2) }}</text>
            <text>× {{ quantity }}</text>
          </view>
        </view>
      </view>

      <view class="section-title address-title">收货地址</view>
      <view v-if="!addresses.length" class="card empty-address" @click="openAddress">
        <view class="empty-address-title">还没有收货地址</view>
        <view class="muted">点击这里新增地址，保存后返回继续下单</view>
        <view class="link-button">去新增地址 ›</view>
      </view>
      <view v-else class="address-list">
        <view
          v-for="address in addresses"
          :key="address.id"
          class="card address-option"
          :class="{ selected: selectedAddressId === address.id }"
          @click="selectedAddressId = address.id"
        >
          <view class="address-check">{{ selectedAddressId === address.id ? '✓' : '' }}</view>
          <view class="address-content">
            <view class="address-top">
              <text class="address-name">{{ address.receiverName }}</text>
              <text>{{ address.receiverPhone }}</text>
              <text v-if="address.isDefault" class="default-tag">默认</text>
            </view>
            <view class="address-text">{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</view>
          </view>
        </view>
        <view class="manage-address" @click="openAddress">管理收货地址 ›</view>
      </view>

      <view class="card total-card">
        <view class="total-row"><text>商品金额</text><text>¥{{ totalAmount }}</text></view>
        <view class="total-row"><text>配送费</text><text>包邮</text></view>
        <view class="total-divider" />
        <view class="total-row total-final"><text>应付金额</text><text>¥{{ totalAmount }}</text></view>
      </view>

      <view class="pay-note">订单创建后保留 {{ orderExpireMinutes }} 分钟，支付完成后以微信支付通知为准。</view>
      <button class="primary-button submit-button" :disabled="submitting || !selectedAddressId" @click="submitOrder">
        {{ submitting ? submittingLabel : '提交订单并支付' }}
      </button>
    </template>

    <view v-else class="card state-card">商品不存在或已下架，请返回商城重新选择。</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { createMallOrder, createWechatMallPayment, getMallAddresses, getMallProduct, waitForMallPaymentConfirmation } from '@/api/mall'
import type { MallAddress, MallProduct } from '@/types/api'
import { requestWechatPayment } from '@/utils/wechat-payment'

const product = ref<MallProduct>()
const productId = ref('')
const quantity = ref(1)
const addresses = ref<MallAddress[]>([])
const selectedAddressId = ref('')
const loading = ref(true)
const submitting = ref(false)
const submittingLabel = ref('正在创建订单…')
const pendingOrder = ref<{ orderNo: string; addressId: string; quantity: number }>()
const orderExpireMinutes = 30

const totalAmount = computed(() => {
  if (!product.value) return '0.00'
  return ((product.value.priceCent * quantity.value) / 100).toFixed(2)
})

const loadAddresses = async () => {
  try {
    addresses.value = await getMallAddresses()
    if (!addresses.value.some((address) => address.id === selectedAddressId.value)) {
      selectedAddressId.value = addresses.value.find((address) => address.isDefault)?.id || addresses.value[0]?.id || ''
    }
  } catch {
    addresses.value = []
  }
}

onLoad((query) => {
  productId.value = String(query?.productId || '')
  const requestedQuantity = Number(query?.quantity || 1)
  quantity.value = Number.isInteger(requestedQuantity) && requestedQuantity > 0 ? requestedQuantity : 1
  if (!productId.value) {
    loading.value = false
    return
  }
  Promise.all([getMallProduct(productId.value), loadAddresses()])
    .then(([value]) => { product.value = value })
    .catch(() => { product.value = undefined })
    .finally(() => { loading.value = false })
})
onShow(() => { if (productId.value) void loadAddresses() })

const openAddress = () => uni.navigateTo({ url: '/pages/mall/address' })

const submitOrder = async () => {
  if (!product.value || !selectedAddressId.value || submitting.value) return
  submitting.value = true
  try {
    const reusableOrder = pendingOrder.value
    const orderNo = reusableOrder
      && reusableOrder.addressId === selectedAddressId.value
      && reusableOrder.quantity === quantity.value
      ? reusableOrder.orderNo
      : (await createMallOrder({ productId: product.value.id, addressId: selectedAddressId.value, quantity: quantity.value })).orderNo
    pendingOrder.value = { orderNo, addressId: selectedAddressId.value, quantity: quantity.value }
    submittingLabel.value = '正在获取微信支付…'
    const payment = await createWechatMallPayment(orderNo)
    submittingLabel.value = '正在打开微信支付…'
    await requestWechatPayment(payment)
    submittingLabel.value = '正在确认支付结果…'
    const confirmedOrder = await waitForMallPaymentConfirmation(orderNo)
    if (confirmedOrder?.status === 'PAID') pendingOrder.value = undefined
    uni.showToast({
      title: confirmedOrder?.status === 'PAID' ? '支付已完成' : '支付结果确认中，请到订单查看',
      icon: confirmedOrder?.status === 'PAID' ? 'success' : 'none',
      duration: confirmedOrder?.status === 'PAID' ? 1500 : 2600,
    })
    setTimeout(
      () => uni.redirectTo({ url: '/pages/mall/orders' }),
      confirmedOrder?.status === 'PAID' ? 700 : 2700,
    )
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '订单支付未完成', icon: 'none', duration: 2600 })
  } finally {
    submittingLabel.value = '正在创建订单…'
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-page { padding-top: 20rpx; padding-bottom: 36rpx; }
.section-title { margin: 6rpx 0 18rpx; color: #21483e; font-size: 35rpx; font-weight: 750; }
.item-card { display: flex; gap: 22rpx; padding: 22rpx; }
.item-image { width: 180rpx; height: 180rpx; flex: 0 0 auto; border-radius: 18rpx; background: #eef8f4; }
.placeholder { display: flex; align-items: center; justify-content: center; color: #159172; font-size: 54rpx; font-weight: 750; }
.item-info { display: flex; flex: 1; min-width: 0; flex-direction: column; }
.item-name { color: #294b41; font-size: 31rpx; font-weight: 720; line-height: 1.45; }
.item-subtitle { margin-top: 10rpx; color: #84928d; font-size: 24rpx; line-height: 1.45; }
.item-meta { display: flex; align-items: baseline; justify-content: space-between; margin-top: auto; color: #70817b; font-size: 25rpx; }
.item-price { color: #c4770b; font-size: 34rpx; font-weight: 750; }
.address-title { margin-top: 30rpx; }
.empty-address { padding: 32rpx; }
.empty-address-title { color: #2d5146; font-size: 30rpx; font-weight: 700; }
.muted { margin-top: 10rpx; color: #82918b; font-size: 24rpx; line-height: 1.5; }
.link-button { margin-top: 20rpx; color: #0f8067; font-size: 27rpx; font-weight: 700; }
.address-option { display: flex; align-items: flex-start; gap: 16rpx; margin-bottom: 16rpx; padding: 22rpx; border: 2rpx solid transparent; }
.address-option.selected { border-color: #21ad86; background: #f1fbf7; }
.address-check { display: flex; align-items: center; justify-content: center; width: 42rpx; height: 42rpx; flex: 0 0 auto; border: 2rpx solid #c9ddd6; border-radius: 50%; color: #fff; background: #fff; font-size: 27rpx; font-weight: 700; }
.selected .address-check { border-color: #0f8067; background: #0f8067; }
.address-content { flex: 1; min-width: 0; }
.address-top { display: flex; align-items: center; gap: 18rpx; color: #71827b; font-size: 25rpx; }
.address-name { color: #294b41; font-size: 29rpx; font-weight: 700; }
.default-tag { padding: 5rpx 12rpx; border-radius: 10rpx; color: #0f8067; background: #ddf5ec; font-size: 20rpx; }
.address-text { margin-top: 12rpx; color: #52665e; font-size: 26rpx; line-height: 1.55; }
.manage-address { margin: 20rpx 0 0; color: #0f8067; text-align: right; font-size: 25rpx; }
.total-card { margin-top: 24rpx; padding: 26rpx; }
.total-row { display: flex; justify-content: space-between; margin-top: 14rpx; color: #6e8078; font-size: 26rpx; }
.total-row:first-child { margin-top: 0; }
.total-divider { height: 1rpx; margin: 22rpx 0 4rpx; background: #e4eeea; }
.total-final { color: #25493e; font-size: 30rpx; font-weight: 700; }
.total-final text:last-child { color: #c4770b; font-size: 38rpx; }
.pay-note { margin: 22rpx 10rpx; color: #84938d; font-size: 22rpx; line-height: 1.5; }
.submit-button { display: flex; align-items: center; justify-content: center; min-height: 96rpx; margin-top: 8rpx; border-radius: 20rpx; background: linear-gradient(135deg, #49cf81, #09a6bd); color: #fff; font-size: 31rpx; font-weight: 700; line-height: 1.2; }
.submit-button::after { border: 0; }
.submit-button[disabled] { opacity: 0.55; }
.state-card { padding: 60rpx 24rpx; text-align: center; }
</style>

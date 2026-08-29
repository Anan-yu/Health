<template>
  <view class="page elder-page detail-page">
    <view v-if="product" class="card product-detail">
      <image v-if="product.mainImageUrl" class="detail-image" :src="product.mainImageUrl" mode="aspectFill" />
      <view v-else class="detail-image placeholder">康</view>
      <view class="detail-name">{{ product.productName }}</view>
      <view v-if="product.subtitle" class="detail-subtitle">{{ product.subtitle }}</view>
      <view class="detail-price">¥{{ (product.priceCent / 100).toFixed(2) }}</view>
      <view class="detail-stock">剩余库存 {{ product.stock }} 件</view>
      <view class="detail-description">{{ product.description || '平台精选健康管理用品，具体使用请以商品说明为准。' }}</view>
    </view>
    <view v-else class="card state-card">正在加载商品…</view>

    <view v-if="product && !isPlatform" class="card buy-card">
      <view class="buy-row"><text>购买数量</text><view class="stepper"><button @click="decrease">−</button><text>{{ quantity }}</text><button @click="increase">＋</button></view></view>
      <button class="primary-button" :disabled="product.stock < 1" @click="checkout">选择地址并下单</button>
    </view>
    <view v-if="product && isPlatform" class="card admin-note">
      <view class="admin-note-title">管理员只读浏览</view>
      <view class="admin-note-copy">如需维护商品，请前往工作台中的“商城商品”。</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getMallProduct } from '@/api/mall'
import { useAuthStore } from '@/stores/auth'
import type { MallProduct } from '@/types/api'

const auth = useAuthStore()
const isPlatform = computed(() => auth.currentWorkbench === 'PLATFORM_ADMIN')
const product = ref<MallProduct>()
const productId = ref('')
const quantity = ref(1)
onLoad((query) => {
  productId.value = String(query?.productId || '')
  if (!productId.value) return
  getMallProduct(productId.value).then((value) => { product.value = value }).catch(() => undefined)
})
const decrease = () => { quantity.value = Math.max(1, quantity.value - 1) }
const increase = () => { quantity.value = Math.min(product.value?.stock || 1, quantity.value + 1) }
const checkout = () => uni.navigateTo({ url: `/pages/mall/checkout?productId=${productId.value}&quantity=${quantity.value}` })
</script>

<style scoped>
.detail-page { padding-top: 20rpx; }
.product-detail { padding: 22rpx; }
.detail-image { width: 100%; height: 520rpx; border-radius: 22rpx; background: #eef8f4; }
.placeholder { display: flex; align-items: center; justify-content: center; color: #159172; font-size: 80rpx; font-weight: 750; }
.detail-name { margin-top: 26rpx; font-size: 40rpx; font-weight: 750; }
.detail-subtitle { margin-top: 10rpx; color: #73847d; font-size: 27rpx; }
.detail-price { margin-top: 24rpx; color: #d37d0b; font-size: 46rpx; font-weight: 750; }
.detail-stock { margin-top: 6rpx; color: #87958e; font-size: 24rpx; }
.detail-description { margin-top: 28rpx; padding-top: 24rpx; border-top: 1rpx solid #e5eeeb; color: #52665e; font-size: 28rpx; line-height: 1.75; white-space: pre-wrap; }
.buy-card { margin-top: 22rpx; padding: 26rpx; }
.admin-note { margin-top: 22rpx; padding: 26rpx; }
.admin-note-title { color: #0f8067; font-size: 29rpx; font-weight: 700; }
.admin-note-copy { margin-top: 8rpx; color: #71817b; font-size: 25rpx; line-height: 1.6; }
.buy-row { display: flex; align-items: center; justify-content: space-between; font-size: 29rpx; font-weight: 650; }
.stepper { display: flex; align-items: center; gap: 18rpx; }
.stepper button { display: flex; align-items: center; justify-content: center; width: 84rpx; height: 84rpx; min-width: 84rpx; min-height: 84rpx; margin: 0; padding: 0; border-radius: 16rpx; color: #0d8066; background: #e4f5ef; font-size: 40rpx; font-weight: 700; line-height: 1; }
.stepper button::after { border: 0; }
.stepper text { min-width: 40rpx; text-align: center; font-size: 31rpx; }
.primary-button { display: flex; align-items: center; justify-content: center; min-height: 96rpx; margin-top: 28rpx; border-radius: 18rpx; background: #0f8067; color: #fff; font-size: 30rpx; font-weight: 700; line-height: 1.2; }
.primary-button::after { border: 0; }
.primary-button[disabled] { opacity: 0.55; }
.state-card { padding: 60rpx 20rpx; text-align: center; }
</style>

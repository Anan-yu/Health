<template>
  <view class="page elder-page mall-page">
    <view class="mall-hero">
      <view class="eyebrow">HEALTH MARKET</view>
      <view class="hero-title">健康商城</view>
      <view class="hero-copy">{{ isPlatform ? '浏览平台当前上架的实物商品目录。' : '精选健康管理用品，订单、收货和售后信息清楚可查。' }}</view>
      <view v-if="!isPlatform" class="hero-actions">
        <button class="hero-button" @click="goOrders">我的订单</button>
        <button class="hero-button light" @click="goAddress">收货地址</button>
      </view>
    </view>

    <view class="section-head">
      <view>
        <view class="section-title">商品目录</view>
        <view class="section-copy">当前商品由平台统一维护</view>
      </view>
      <view class="refresh" @click="load">刷新</view>
    </view>

    <view v-if="loading" class="card state-card">正在加载商品，请稍候…</view>
    <view v-else-if="!products.length" class="card state-card">
      <view class="empty-title">商城正在准备商品</view>
      <view class="muted">{{ isPlatform ? '平台发布商品后，会在这里显示商品目录。' : '平台发布商品后，会在这里显示价格、库存和购买入口。' }}</view>
    </view>
    <view v-else class="product-grid">
      <view v-for="product in products" :key="product.id" class="product-card" @click="open(product.id)">
        <image v-if="product.mainImageUrl" class="product-image" :src="product.mainImageUrl" mode="aspectFill" />
        <view v-else class="product-image placeholder">康</view>
        <view class="product-name">{{ product.productName }}</view>
        <view v-if="product.subtitle" class="product-subtitle">{{ product.subtitle }}</view>
        <view class="product-bottom">
          <text class="price">¥{{ (product.priceCent / 100).toFixed(2) }}</text>
          <text class="stock">库存 {{ product.stock }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMallProducts } from '@/api/mall'
import { mallEnabled } from '@/constants/features'
import { useAuthStore } from '@/stores/auth'
import type { MallProduct } from '@/types/api'

const auth = useAuthStore()
const isPlatform = computed(() => auth.currentWorkbench === 'PLATFORM_ADMIN')
const products = ref<MallProduct[]>([])
const loading = ref(false)
const load = async () => {
  loading.value = true
  try {
    products.value = await getMallProducts()
  } catch {
    products.value = []
  } finally {
    loading.value = false
  }
}
const open = (productId: string) => uni.navigateTo({ url: `/pages/mall/detail?productId=${productId}` })
const goOrders = () => uni.navigateTo({ url: '/pages/mall/orders' })
const goAddress = () => uni.navigateTo({ url: '/pages/mall/address' })
onShow(() => {
  if (!mallEnabled) {
    uni.navigateTo({ url: '/pages/message/index' })
    return
  }
  void load()
})
</script>

<style scoped>
.mall-page { padding-top: 20rpx; }
.mall-hero {
  padding: 34rpx;
  border-radius: 34rpx;
  color: #fff;
  background: linear-gradient(135deg, #0b6c57, #16a783);
  box-shadow: 0 18rpx 40rpx rgba(11, 108, 87, 0.2);
}
.eyebrow { color: rgba(255, 255, 255, 0.68); font-size: 21rpx; letter-spacing: 4rpx; }
.hero-title { margin-top: 12rpx; font-size: 46rpx; font-weight: 750; }
.hero-copy { margin-top: 12rpx; color: rgba(255,255,255,.78); font-size: 26rpx; line-height: 1.6; }
.hero-actions { display: flex; gap: 16rpx; margin-top: 28rpx; }
.hero-button { flex: 1; height: 72rpx; margin: 0; border: 1rpx solid rgba(255,255,255,.32); border-radius: 18rpx; color: #fff; background: rgba(255,255,255,.14); font-size: 26rpx; line-height: 72rpx; }
.hero-button.light { color: #146b58; background: #fff; }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; margin: 34rpx 0 20rpx; }
.section-title { font-size: 34rpx; font-weight: 750; }
.section-copy { margin-top: 6rpx; color: #82918b; font-size: 24rpx; }
.refresh { padding: 14rpx 22rpx; border-radius: 18rpx; color: #0d8066; background: #e3f4ef; font-size: 24rpx; }
.state-card { padding: 48rpx 30rpx; text-align: center; }
.empty-title { margin-bottom: 14rpx; font-size: 31rpx; font-weight: 700; }
.product-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx; }
.product-card { padding: 18rpx; border: 2rpx solid #e3eeeb; border-radius: 26rpx; background: #fff; box-shadow: 0 10rpx 24rpx rgba(32,78,65,.06); }
.product-image { width: 100%; height: 250rpx; border-radius: 18rpx; background: #eef8f4; }
.product-image.placeholder { display: flex; align-items: center; justify-content: center; color: #159172; font-size: 52rpx; font-weight: 750; }
.product-name { margin-top: 18rpx; font-size: 29rpx; font-weight: 700; line-height: 1.4; }
.product-subtitle { min-height: 38rpx; margin-top: 6rpx; color: #81918b; font-size: 22rpx; line-height: 1.45; }
.product-bottom { display: flex; align-items: baseline; justify-content: space-between; margin-top: 16rpx; }
.price { color: #d37d0b; font-size: 31rpx; font-weight: 750; }
.stock { color: #8c9b95; font-size: 21rpx; }
</style>

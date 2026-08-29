<template>
  <view class="page elder-page orders-page">
    <view class="page-heading">
      <view>
        <view class="section-title">我的实物订单</view>
        <view class="section-copy">查看支付状态和收货信息</view>
      </view>
      <view class="refresh" @click="load">刷新</view>
    </view>
    <view v-if="loading" class="card state-card">正在加载订单，请稍候…</view>
    <view v-else-if="!orders.length" class="card state-card">
      <view class="empty-title">还没有订单</view>
      <view class="muted">在商城选择实物商品后，订单会显示在这里。</view>
      <button class="small-primary" @click="goMall">去商城看看</button>
    </view>
    <view v-else>
      <view v-for="order in orders" :key="order.orderNo" class="card order-card">
        <view class="order-head">
          <view class="order-no">订单号 {{ order.orderNo }}</view>
          <view class="order-status" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</view>
        </view>
        <view v-for="item in order.items" :key="`${order.orderNo}-${item.productId}`" class="order-item">
          <image v-if="item.mainImageUrl" class="order-image" :src="item.mainImageUrl" mode="aspectFill" />
          <view v-else class="order-image placeholder">康</view>
          <view class="order-item-info">
            <view class="order-item-name">{{ item.productName }}</view>
            <view class="order-item-meta">¥{{ (item.unitPriceCent / 100).toFixed(2) }} × {{ item.quantity }}</view>
          </view>
          <view class="order-item-total">¥{{ (item.totalCent / 100).toFixed(2) }}</view>
        </view>
        <view class="order-address">收货：{{ order.receiverName }} {{ order.receiverPhone }}<br />{{ order.province }}{{ order.city }}{{ order.district }}{{ order.detailAddress }}</view>
        <view class="order-foot">
          <view class="order-created">下单时间 {{ formatTime(order.createdAt) }}</view>
          <view class="order-amount">合计 ¥{{ (order.amountCent / 100).toFixed(2) }}</view>
        </view>
        <view v-if="order.status === 'PENDING_PAYMENT'" class="order-actions">
          <button class="cancel-button" :disabled="busyOrder === order.orderNo" @click="cancel(order)">取消订单</button>
          <button class="pay-button" :disabled="busyOrder === order.orderNo" @click="pay(order)">{{ busyOrder === order.orderNo ? '处理中…' : '立即支付' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { cancelMallOrder, createWechatMallPayment, getMallOrders, waitForMallPaymentConfirmation } from '@/api/mall'
import type { MallOrder } from '@/types/api'
import { requestWechatPayment } from '@/utils/wechat-payment'

const orders = ref<MallOrder[]>([])
const loading = ref(false)
const busyOrder = ref('')
const statusLabels: Record<string, string> = {
  PENDING_PAYMENT: '待付款', PAID: '待发货', SHIPPED: '配送中', COMPLETED: '已完成', CANCELLED: '已取消',
}
const statusLabel = (status: string) => statusLabels[status] || status
const statusClass = (status: string) => status.toLowerCase().replace(/_/g, '-')
const formatTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '—'

const load = async () => {
  loading.value = true
  try { orders.value = await getMallOrders() } catch { orders.value = [] } finally { loading.value = false }
}
const goMall = () => uni.switchTab({ url: '/pages/mall/index' })
const pay = async (order: MallOrder) => {
  if (busyOrder.value) return
  busyOrder.value = order.orderNo
  try {
    const params = await createWechatMallPayment(order.orderNo)
    await requestWechatPayment(params)
    const confirmedOrder = await waitForMallPaymentConfirmation(order.orderNo)
    uni.showToast({
      title: confirmedOrder?.status === 'PAID' ? '支付已完成' : '支付结果确认中，请刷新订单查看',
      icon: confirmedOrder?.status === 'PAID' ? 'success' : 'none',
      duration: confirmedOrder?.status === 'PAID' ? 1500 : 2600,
    })
    await load()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '支付未完成', icon: 'none', duration: 2600 })
  } finally { busyOrder.value = '' }
}
const cancel = (order: MallOrder) => {
  if (busyOrder.value) return
  uni.showModal({ title: '取消订单', content: '确认取消这个未付款订单吗？', success: async (result) => {
    if (!result.confirm) return
    busyOrder.value = order.orderNo
    try { await cancelMallOrder(order.orderNo); await load(); uni.showToast({ title: '订单已取消', icon: 'success' }) }
    catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '取消失败', icon: 'none' }) }
    finally { busyOrder.value = '' }
  } })
}
onShow(load)
</script>

<style scoped>
.orders-page { padding-top: 20rpx; padding-bottom: 30rpx; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 22rpx; }
.section-title { color: #21483e; font-size: 36rpx; font-weight: 750; }
.section-copy { margin-top: 7rpx; color: #84938d; font-size: 24rpx; }
.refresh { padding: 14rpx 22rpx; border-radius: 18rpx; color: #0d8066; background: #e3f4ef; font-size: 24rpx; }
.state-card { padding: 60rpx 24rpx; text-align: center; }
.empty-title { color: #2d5146; font-size: 31rpx; font-weight: 700; }
.muted { margin-top: 12rpx; color: #82918b; font-size: 24rpx; line-height: 1.5; }
.small-primary { width: 240rpx; margin: 24rpx auto 0; border-radius: 16rpx; color: #fff; background: #0f8067; font-size: 25rpx; }
.order-card { margin-bottom: 20rpx; padding: 24rpx; }
.order-head, .order-foot { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.order-no, .order-created { overflow: hidden; color: #84938d; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.order-status { flex: 0 0 auto; color: #0f8067; font-size: 25rpx; font-weight: 700; }
.order-status.pending-payment { color: #c9780b; }
.order-status.cancelled { color: #9b8178; }
.order-item { display: flex; align-items: center; gap: 16rpx; margin-top: 22rpx; padding-top: 20rpx; border-top: 1rpx solid #e7efec; }
.order-image { width: 118rpx; height: 118rpx; flex: 0 0 auto; border-radius: 16rpx; background: #eef8f4; }
.placeholder { display: flex; align-items: center; justify-content: center; color: #159172; font-size: 38rpx; font-weight: 750; }
.order-item-info { flex: 1; min-width: 0; }
.order-item-name { overflow: hidden; color: #304f45; font-size: 27rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.order-item-meta { margin-top: 9rpx; color: #81918b; font-size: 23rpx; }
.order-item-total { color: #c4770b; font-size: 27rpx; font-weight: 700; }
.order-address { margin-top: 18rpx; padding: 16rpx; border-radius: 14rpx; color: #657870; background: #f5faf8; font-size: 23rpx; line-height: 1.65; }
.order-foot { margin-top: 18rpx; }
.order-amount { flex: 0 0 auto; color: #294b41; font-size: 27rpx; font-weight: 700; }
.order-actions { display: flex; justify-content: flex-end; gap: 14rpx; margin-top: 20rpx; }
.order-actions button { margin: 0; padding: 0 24rpx; border-radius: 15rpx; font-size: 24rpx; line-height: 62rpx; }
.cancel-button { border: 1rpx solid #d9e5e0; color: #6c7d76; background: #fff; }
.pay-button { color: #fff; background: #0f8067; }
</style>

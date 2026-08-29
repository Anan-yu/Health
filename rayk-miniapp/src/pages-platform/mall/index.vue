<template>
  <view class="page mall-admin-page">
    <view class="admin-hero">
      <view class="eyebrow">PHYSICAL GOODS</view>
      <view class="hero-title">商城商品</view>
      <view class="hero-copy">维护实物商品、价格、库存和上下架状态。商品发布后，客户才能在商城看到。</view>
    </view>

    <view class="card editor-card">
      <view class="section-head"><view class="section-title">{{ editingId ? '编辑商品' : '新增商品' }}</view><view class="reset" @click="reset">清空</view></view>
      <input v-model="form.productName" class="field-input" placeholder="商品名称（必填）" />
      <input v-model="form.subtitle" class="field-input" placeholder="一句话卖点（可选）" />
      <input v-model="form.mainImageUrl" class="field-input" placeholder="商品主图地址（可选）" />
      <textarea v-model="form.description" class="field-textarea" placeholder="商品说明、使用方式和注意事项（可选）" />
      <view class="form-row">
        <input v-model="form.priceYuan" class="field-input" type="digit" placeholder="售价（元）" />
        <input v-model="form.stock" class="field-input" type="number" placeholder="库存" />
        <input v-model="form.sortOrder" class="field-input" type="number" placeholder="排序" />
      </view>
      <view class="form-row status-row">
        <view class="status-label">上架状态</view>
        <picker :range="statusOptions" range-key="label" :value="statusIndex" @change="statusIndex = Number($event.detail.value)">
          <view class="status-picker">{{ statusOptions[statusIndex].label }} ›</view>
        </picker>
      </view>
      <button class="primary-button" :disabled="saving" @click="save">{{ saving ? '保存中…' : editingId ? '保存商品' : '发布商品' }}</button>
    </view>

    <view class="section-head list-head"><view class="section-title">商品列表</view><view class="refresh" @click="load">刷新</view></view>
    <view v-if="loading" class="card state-card">正在加载商品…</view>
    <view v-else-if="!products.length" class="card state-card">还没有商品，请先新增实物商品。</view>
    <view v-else>
      <view v-for="product in products" :key="product.id" class="card product-card">
        <image v-if="product.mainImageUrl" class="product-image" :src="product.mainImageUrl" mode="aspectFill" />
        <view v-else class="product-image placeholder">康</view>
        <view class="product-info">
          <view class="product-name">{{ product.productName }}</view>
          <view class="product-meta">¥{{ (product.priceCent / 100).toFixed(2) }} · 库存 {{ product.stock }} · 已售 {{ product.soldCount }}</view>
          <view class="product-status" :class="{ active: product.status === 'ACTIVE' }">{{ product.status === 'ACTIVE' ? '已上架' : '已下架' }}</view>
        </view>
        <view class="product-actions"><button @click="edit(product)">编辑</button><button v-if="product.status === 'ACTIVE'" class="danger" @click="disable(product)">下架</button></view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createMallAdminProduct, disableMallAdminProduct, getMallAdminProducts, updateMallAdminProduct } from '@/api/mall'
import { mallEnabled } from '@/constants/features'
import type { MallProduct } from '@/types/api'

type ProductForm = { productName: string; subtitle: string; description: string; mainImageUrl: string; priceYuan: string; stock: string; sortOrder: string; status: 'ACTIVE' | 'INACTIVE' }
const blankForm = (): ProductForm => ({ productName: '', subtitle: '', description: '', mainImageUrl: '', priceYuan: '', stock: '0', sortOrder: '0', status: 'ACTIVE' })
const form = reactive(blankForm())
const products = ref<MallProduct[]>([])
const editingId = ref('')
const loading = ref(false)
const saving = ref(false)
const statusOptions = [{ label: '上架销售', value: 'ACTIVE' as const }, { label: '暂不上架', value: 'INACTIVE' as const }]
const statusIndex = computed({ get: () => statusOptions.findIndex((item) => item.value === form.status), set: (value: number) => { form.status = statusOptions[value]?.value || 'ACTIVE' } })

const load = async () => {
  loading.value = true
  try { products.value = await getMallAdminProducts() } catch { products.value = [] } finally { loading.value = false }
}
const reset = () => { editingId.value = ''; Object.assign(form, blankForm()) }
const edit = (product: MallProduct) => {
  editingId.value = product.id
  Object.assign(form, { productName: product.productName, subtitle: product.subtitle || '', description: product.description || '', mainImageUrl: product.mainImageUrl || '', priceYuan: (product.priceCent / 100).toFixed(2), stock: String(product.stock), sortOrder: String(product.sortOrder), status: product.status === 'ACTIVE' ? 'ACTIVE' : 'INACTIVE' })
  uni.pageScrollTo({ scrollTop: 0, duration: 200 })
}
const save = async () => {
  const price = Number(form.priceYuan)
  const stock = Number(form.stock)
  const sortOrder = Number(form.sortOrder || 0)
  if (!form.productName.trim() || !Number.isFinite(price) || price <= 0 || !Number.isInteger(stock) || stock < 0 || !Number.isInteger(sortOrder) || sortOrder < 0) {
    uni.showToast({ title: '请填写正确的名称、售价和库存', icon: 'none' }); return
  }
  const data = { productName: form.productName.trim(), subtitle: form.subtitle.trim(), description: form.description.trim(), mainImageUrl: form.mainImageUrl.trim(), priceCent: Math.round(price * 100), stock, status: form.status, sortOrder }
  saving.value = true
  try { if (editingId.value) await updateMallAdminProduct(editingId.value, data); else await createMallAdminProduct(data); reset(); await load(); uni.showToast({ title: '商品已保存', icon: 'success' }) }
  catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '保存失败', icon: 'none' }) }
  finally { saving.value = false }
}
const disable = (product: MallProduct) => {
  uni.showModal({ title: '下架商品', content: `确认下架“${product.productName}”吗？已付款订单不受影响。`, success: async (result) => {
    if (!result.confirm) return
    try { await disableMallAdminProduct(product.id); await load(); uni.showToast({ title: '商品已下架', icon: 'success' }) }
    catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '下架失败', icon: 'none' }) }
  } })
}
onShow(() => {
  if (!mallEnabled) {
    uni.switchTab({ url: '/pages/message/index' })
    return
  }
  void load()
})
</script>

<style scoped>
.mall-admin-page { padding-top: 24rpx; padding-bottom: 40rpx; }
.admin-hero { position: relative; overflow: hidden; padding: 32rpx; border-radius: 30rpx; color: #fff; background: linear-gradient(135deg, #075d4d, #14a27f); }
.admin-hero::after { position: absolute; right: -90rpx; top: -140rpx; width: 330rpx; height: 330rpx; border: 32rpx solid rgba(255,255,255,.1); border-radius: 50%; content: ''; }
.eyebrow { color: rgba(255,255,255,.68); font-size: 19rpx; letter-spacing: 3rpx; }
.hero-title { margin-top: 10rpx; font-size: 42rpx; font-weight: 750; }
.hero-copy { position: relative; z-index: 1; margin-top: 12rpx; color: rgba(255,255,255,.84); font-size: 25rpx; line-height: 1.6; }
.editor-card { margin-top: 22rpx; padding: 26rpx; }
.section-head { display: flex; align-items: center; justify-content: space-between; }
.section-title { color: #22493f; font-size: 32rpx; font-weight: 750; }
.reset, .refresh { color: #0f8067; font-size: 24rpx; }
.editor-card .field-input, .editor-card .field-textarea { box-sizing: border-box; width: 100%; margin-top: 14rpx; border: 2rpx solid #dceae5; border-radius: 16rpx; color: #294b41; background: #fbfefd; font-size: 26rpx; }
/* WeChat Android native inputs clip glyphs when vertical padding consumes their default height. */
.editor-card .field-input { height: 88rpx; padding: 0 19rpx; line-height: 88rpx; }
.editor-card .field-textarea { min-height: 150rpx; padding: 19rpx; line-height: 1.55; }
.form-row { display: flex; gap: 12rpx; }
.form-row input { flex: 1; min-width: 0; }
.status-row { align-items: center; justify-content: space-between; margin-top: 18rpx; }
.status-label { color: #52665e; font-size: 26rpx; }
.status-picker { padding: 12rpx 18rpx; border-radius: 14rpx; color: #0f8067; background: #e4f6ef; font-size: 25rpx; }
.primary-button { margin-top: 22rpx; border-radius: 17rpx; color: #fff; background: #0f8067; font-size: 28rpx; }
.list-head { margin: 30rpx 4rpx 18rpx; }
.state-card { padding: 55rpx 24rpx; text-align: center; color: #81918b; font-size: 25rpx; }
.product-card { display: flex; align-items: flex-start; gap: 18rpx; margin-bottom: 16rpx; padding: 20rpx; }
.product-image { width: 140rpx; height: 140rpx; flex: 0 0 auto; border-radius: 16rpx; background: #eef8f4; }
.placeholder { display: flex; align-items: center; justify-content: center; color: #159172; font-size: 44rpx; font-weight: 750; }
.product-info { flex: 1; min-width: 0; }
.product-name { color: #2d5045; font-size: 28rpx; font-weight: 700; line-height: 1.45; }
.product-meta { margin-top: 9rpx; color: #7d8c86; font-size: 22rpx; line-height: 1.5; }
.product-status { display: inline-block; margin-top: 12rpx; padding: 5rpx 12rpx; border-radius: 10rpx; color: #9a8179; background: #f2eeeb; font-size: 20rpx; }
.product-status.active { color: #0f8067; background: #e2f6ef; }
.product-actions { display: flex; flex-direction: column; gap: 10rpx; }
.product-actions button { margin: 0; padding: 0 17rpx; border: 1rpx solid #d7e6e0; border-radius: 12rpx; color: #397062; background: #fff; font-size: 22rpx; line-height: 52rpx; }
.product-actions .danger { color: #ad6255; border-color: #ecd3cc; }
</style>

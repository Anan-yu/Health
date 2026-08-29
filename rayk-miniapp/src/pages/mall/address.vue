<template>
  <view class="page elder-page address-page">
    <view class="section-head"><view class="section-title">我的收货地址</view><button class="small-button" @click="startCreate">新增地址</button></view>
    <view v-if="!addresses.length && !editing" class="card state-card">还没有收货地址，请先新增一条。</view>
    <view v-for="address in addresses" :key="address.id" class="card address-card">
      <view class="address-top"><text class="address-name">{{ address.receiverName }}</text><text>{{ address.receiverPhone }}</text><text v-if="address.isDefault" class="default-tag">默认</text></view>
      <view class="address-text">{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</view>
      <view class="address-actions"><button @click="startEdit(address)">编辑</button><button class="danger" @click="remove(address.id)">删除</button></view>
    </view>
    <view v-if="editing" class="card form-card">
      <view class="form-title">{{ editingId ? '编辑地址' : '新增地址' }}</view>
      <input v-model="form.receiverName" class="field-input" placeholder="收货人姓名" />
      <input v-model="form.receiverPhone" class="field-input" type="number" maxlength="11" placeholder="手机号码" />
      <view class="form-row"><input v-model="form.province" class="field-input" placeholder="省" /><input v-model="form.city" class="field-input" placeholder="市" /><input v-model="form.district" class="field-input" placeholder="区/县" /></view>
      <textarea v-model="form.detailAddress" class="field-textarea" placeholder="详细地址，如街道、楼栋和门牌号" />
      <label class="switch-row"><text>设为默认地址</text><switch :checked="form.isDefault" color="#0f8067" @change="onDefaultChange" /></label>
      <view class="form-actions"><button @click="cancelEdit">取消</button><button class="primary-button" @click="save">保存地址</button></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createMallAddress, deleteMallAddress, getMallAddresses, updateMallAddress } from '@/api/mall'
import type { MallAddress } from '@/types/api'

const addresses = ref<MallAddress[]>([])
const editing = ref(false)
const editingId = ref('')
const emptyForm = () => ({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false })
const form = reactive(emptyForm())
const load = async () => { try { addresses.value = await getMallAddresses() } catch { addresses.value = [] } }
const startCreate = () => { editingId.value = ''; Object.assign(form, emptyForm()); editing.value = true }
const startEdit = (address: MallAddress) => { editingId.value = address.id; Object.assign(form, address); editing.value = true }
const cancelEdit = () => { editing.value = false }
const onDefaultChange = (event: unknown) => {
  const change = event as { detail?: { value?: boolean } }
  form.isDefault = Boolean(change.detail?.value)
}
const save = async () => {
  if (!form.receiverName || !/^1\d{10}$/.test(form.receiverPhone) || !form.province || !form.city || !form.district || !form.detailAddress) {
    uni.showToast({ title: '请完整填写地址信息', icon: 'none' }); return
  }
  try {
    if (editingId.value) await updateMallAddress(editingId.value, { ...form })
    else await createMallAddress({ ...form })
    editing.value = false; await load(); uni.showToast({ title: '地址已保存', icon: 'success' })
  } catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '保存失败', icon: 'none' }) }
}
const remove = (id: string) => {
  uni.showModal({ title: '删除地址', content: '确认删除这条收货地址吗？', success: async (result) => {
    if (!result.confirm) return
    try { await deleteMallAddress(id); await load() } catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '删除失败', icon: 'none' }) }
  } })
}
onShow(load)
</script>

<style scoped>
.address-page { padding-top: 20rpx; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 22rpx; }
.section-title { font-size: 36rpx; font-weight: 750; }
.small-button { display: flex; align-items: center; justify-content: center; min-width: 148rpx; min-height: 72rpx; margin: 0; padding: 0 22rpx; border-radius: 16rpx; color: #0f8067; background: #e3f5ef; font-size: 25rpx; font-weight: 700; line-height: 1.2; }
.small-button::after { border: 0; }
.address-card { margin-bottom: 18rpx; padding: 26rpx; }
.address-top { display: flex; align-items: center; gap: 20rpx; font-size: 27rpx; color: #687b73; }
.address-name { color: #203f35; font-size: 31rpx; font-weight: 700; }
.default-tag { margin-left: auto; padding: 6rpx 14rpx; border-radius: 12rpx; color: #0f8067; background: #e2f5ef; font-size: 21rpx; }
.address-text { margin-top: 16rpx; color: #405a50; font-size: 28rpx; line-height: 1.6; }
.address-actions { display: flex; justify-content: flex-end; gap: 18rpx; margin-top: 18rpx; }
.address-actions button { display: flex; align-items: center; justify-content: center; min-width: 124rpx; min-height: 72rpx; margin: 0; padding: 0 24rpx; border: 1rpx solid #d5e4df; border-radius: 14rpx; color: #447166; background: #fff; font-size: 24rpx; font-weight: 700; line-height: 1.2; }
.address-actions button::after { border: 0; }
.address-actions .danger { color: #b5685a; }
.state-card { padding: 60rpx 20rpx; text-align: center; }
.form-card { margin-top: 24rpx; padding: 26rpx; }
.form-title { margin-bottom: 18rpx; font-size: 32rpx; font-weight: 700; }
.form-card .field-input, .form-card .field-textarea { box-sizing: border-box; width: 100%; margin-top: 14rpx; border: 2rpx solid #dceae5; border-radius: 16rpx; background: #fbfefd; font-size: 27rpx; }
/* Keep the native input text inside a fixed content line on WeChat Android. */
.form-card .field-input { height: 90rpx; padding: 0 20rpx; line-height: 90rpx; }
.form-card .field-textarea { min-height: 150rpx; padding: 20rpx; line-height: 1.55; }
.form-row { display: flex; gap: 12rpx; }
.form-row input { flex: 1; min-width: 0; }
.switch-row { display: flex; align-items: center; justify-content: space-between; margin-top: 22rpx; color: #3e5b51; font-size: 27rpx; }
.form-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.form-actions button { display: flex; align-items: center; justify-content: center; min-height: 88rpx; flex: 1; margin: 0; border-radius: 16rpx; color: #4e6b61; background: #edf5f2; font-size: 27rpx; line-height: 1.2; }
.form-actions button::after { border: 0; }
.form-actions .primary-button { color: #fff; background: #0f8067; }
</style>

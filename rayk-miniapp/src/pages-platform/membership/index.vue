<template>
  <PlatformAdminShell>
    <view class="page membership-admin-page">
    <view class="membership-hero">
      <view class="hero-eyebrow">CUSTOMER MEMBERSHIP</view>
      <view class="hero-title">会员管理</view>
      <view class="hero-copy">按客户手机号管理年度健康会员状态</view>
      <view class="hero-note">仅平台管理员可操作，开通或取消后客户下次刷新会员页面即可看到最新状态。</view>
    </view>

    <view class="card search-card">
      <view class="section-label">查找客户</view>
      <view class="search-row">
        <input
          v-model="phone"
          class="phone-input"
          type="number"
          maxlength="11"
          placeholder="请输入客户手机号"
          confirm-type="search"
          @confirm="search"
        />
        <view class="search-button" :class="{ disabled: searching }" @click="search">
          {{ searching ? '查找中' : '查找' }}
        </view>
      </view>
      <view class="search-hint">手机号仅用于匹配客户，不会在页面展示完整号码。</view>
    </view>

    <view v-if="error" class="error-card">
      <view class="error-title">查找失败</view>
      <view class="error-copy">{{ error }}</view>
    </view>

    <view v-if="customer" class="card customer-card">
      <view class="customer-head">
        <view class="customer-avatar">客</view>
        <view class="customer-identity">
          <view class="customer-name">{{ customer.displayName || '未设置姓名' }}</view>
          <view class="customer-phone">{{ customer.phoneMasked || '手机号已隐藏' }}</view>
        </view>
        <view class="status-badge" :class="{ active: customer.active }">
          {{ customer.active ? '会员有效' : '免费用户' }}
        </view>
      </view>

      <view class="membership-status" :class="{ active: customer.active }">
        <view>
          <view class="status-label">当前状态</view>
          <view class="status-value">{{ customer.active ? customer.planName : '体验基础健康管理服务' }}</view>
        </view>
        <view v-if="customer.active" class="expire-info">
          <view class="status-label">有效期至</view>
          <view class="expire-value">{{ formatExpire(customer.expireAt) }}</view>
        </view>
      </view>

      <view class="action-row">
        <view
          class="action-button enable-button"
          :class="{ disabled: customer.active || saving }"
          @click="updateMembership(true)"
        >
          {{ customer.active ? '已开通' : '开通年度会员' }}
        </view>
        <view
          class="action-button disable-button"
          :class="{ disabled: !customer.active || saving }"
          @click="updateMembership(false)"
        >
          {{ customer.active ? '取消会员' : '当前未开通' }}
        </view>
      </view>
    </view>

    <view v-else-if="!searching && !error" class="empty-card">
      <view class="empty-icon">会</view>
      <view class="empty-title">输入手机号开始管理</view>
      <view class="empty-copy">查找后可查看客户当前会员状态，并进行开通或取消操作。</view>
    </view>
    </view>
  </PlatformAdminShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getPlatformCustomerMembership, updatePlatformCustomerMembership } from '@/api/admin'
import PlatformAdminShell from '@/components/PlatformAdminShell.vue'
import type { PlatformCustomerMembership } from '@/types/api'

const phone = ref('')
const customer = ref<PlatformCustomerMembership | null>(null)
const searching = ref(false)
const saving = ref(false)
const error = ref('')

const isMobile = (value: string) => /^1[3-9]\d{9}$/.test(value)
const formatExpire = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '—')

const search = async () => {
  const value = phone.value.trim()
  if (!isMobile(value)) {
    uni.showToast({ title: '请输入正确的11位手机号', icon: 'none' })
    return
  }
  searching.value = true
  error.value = ''
  customer.value = null
  try {
    customer.value = await getPlatformCustomerMembership(value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '客户信息查找失败'
  } finally {
    searching.value = false
  }
}

const updateMembership = async (active: boolean) => {
  if (!customer.value || saving.value || customer.value.active === active) return
  const action = active ? '开通年度会员' : '取消会员'
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: `确认${action}`,
      content: active
        ? `确认给${customer.value?.displayName || '该客户'}开通365天年度会员吗？`
        : `确认取消${customer.value?.displayName || '该客户'}的年度会员吗？`,
      confirmText: '确认',
      cancelText: '返回',
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    })
  })
  if (!confirmed) return
  saving.value = true
  try {
    customer.value = await updatePlatformCustomerMembership({ phone: phone.value.trim(), active })
    uni.showToast({ title: active ? '会员已开通' : '会员已取消', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : `${action}失败`, icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.membership-admin-page {
  padding-top: 28rpx;
}
.membership-hero {
  position: relative;
  overflow: hidden;
  padding: 34rpx 30rpx 30rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #0b6b57, #159579);
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(12, 102, 82, 0.2);
}
.membership-hero::after {
  position: absolute;
  top: -110rpx;
  right: -70rpx;
  width: 300rpx;
  height: 300rpx;
  border: 34rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  content: '';
}
.hero-eyebrow {
  position: relative;
  color: rgba(232, 255, 248, 0.72);
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}
.hero-title {
  position: relative;
  margin-top: 9rpx;
  font-size: 42rpx;
  font-weight: 760;
}
.hero-copy {
  position: relative;
  margin-top: 8rpx;
  color: rgba(238, 255, 249, 0.9);
  font-size: 25rpx;
  line-height: 1.5;
}
.hero-note {
  position: relative;
  margin-top: 18rpx;
  padding-top: 17rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.2);
  color: rgba(238, 255, 249, 0.75);
  font-size: 21rpx;
  line-height: 1.55;
}
.search-card,
.customer-card {
  margin-top: 24rpx;
  padding: 28rpx;
}
.section-label {
  color: #20483e;
  font-size: 29rpx;
  font-weight: 720;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 20rpx;
}
.phone-input {
  flex: 1;
  min-width: 0;
  height: 82rpx;
  box-sizing: border-box;
  padding: 0 24rpx;
  border: 2rpx solid #d7e8e2;
  border-radius: 18rpx;
  background: #f7fcfa;
  color: #25463e;
  font-size: 28rpx;
}
.search-button {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 144rpx;
  height: 82rpx;
  border-radius: 18rpx;
  background: #0f8068;
  color: #fff;
  font-size: 27rpx;
  font-weight: 700;
}
.search-button.disabled,
.action-button.disabled {
  opacity: 0.5;
  pointer-events: none;
}
.search-hint {
  margin-top: 14rpx;
  color: #879893;
  font-size: 21rpx;
  line-height: 1.5;
}
.error-card {
  margin-top: 20rpx;
  padding: 24rpx 28rpx;
  border: 2rpx solid #f1d9d2;
  border-radius: 22rpx;
  background: #fff8f5;
}
.error-title {
  color: #a74d43;
  font-size: 27rpx;
  font-weight: 700;
}
.error-copy {
  margin-top: 8rpx;
  color: #8c655e;
  font-size: 23rpx;
  line-height: 1.5;
}
.customer-head {
  display: flex;
  align-items: center;
}
.customer-avatar,
.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 70rpx;
  height: 70rpx;
  flex: 0 0 auto;
  border-radius: 22rpx;
  background: #e0f6ee;
  color: #0d8068;
  font-size: 28rpx;
  font-weight: 750;
}
.customer-identity {
  flex: 1;
  min-width: 0;
  margin-left: 18rpx;
}
.customer-name {
  overflow: hidden;
  color: #25483e;
  font-size: 30rpx;
  font-weight: 720;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.customer-phone {
  margin-top: 6rpx;
  color: #84958f;
  font-size: 22rpx;
}
.status-badge {
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: #edf1f0;
  color: #687b74;
  font-size: 21rpx;
  font-weight: 650;
}
.status-badge.active {
  background: #e3f8ef;
  color: #0b8066;
}
.membership-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
  padding: 22rpx;
  border-radius: 20rpx;
  background: #f3f7f5;
}
.membership-status.active {
  background: #edf9f4;
}
.status-label {
  color: #879690;
  font-size: 21rpx;
}
.status-value,
.expire-value {
  margin-top: 7rpx;
  color: #2a4e43;
  font-size: 26rpx;
  font-weight: 700;
}
.expire-info {
  margin-left: 20rpx;
  text-align: right;
}
.expire-value {
  color: #0b8066;
  font-size: 23rpx;
}
.action-row {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}
.action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 78rpx;
  border-radius: 18rpx;
  font-size: 25rpx;
  font-weight: 700;
}
.enable-button {
  background: linear-gradient(135deg, #4bcf83, #0eae9f);
  color: #fff;
}
.disable-button {
  border: 2rpx solid #edcfc9;
  background: #fff8f6;
  color: #a85c50;
}
.empty-card {
  display: flex;
  align-items: center;
  flex-direction: column;
  margin-top: 24rpx;
  padding: 54rpx 34rpx;
  border: 2rpx dashed #d5e6e0;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.75);
  text-align: center;
}
.empty-icon {
  width: 78rpx;
  height: 78rpx;
  font-size: 31rpx;
}
.empty-title {
  margin-top: 20rpx;
  color: #315148;
  font-size: 29rpx;
  font-weight: 700;
}
.empty-copy {
  max-width: 540rpx;
  margin-top: 10rpx;
  color: #899891;
  font-size: 23rpx;
  line-height: 1.6;
}
</style>

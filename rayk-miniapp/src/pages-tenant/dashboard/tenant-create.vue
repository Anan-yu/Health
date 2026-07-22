<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view class="eyebrow">TENANT ONBOARDING</view>
      <view class="title">新建入驻机构</view>
      <view class="subtitle">预设管理员手机号后，该号码微信授权登录会自动进入机构管理工作台</view>
    </view>
    <view class="card form-card">
      <view class="field-label">机构编号</view>
      <input v-model="form.tenantCode" class="input" maxlength="50" placeholder="例如：SUZHOU_CLINIC" />
      <view class="field-label">机构名称</view>
      <input v-model="form.tenantName" class="input" maxlength="100" placeholder="请输入机构名称" />
      <view class="field-label">服务套餐</view>
      <input v-model="form.servicePlan" class="input" maxlength="50" placeholder="例如：STANDARD" />
      <view class="section-divider" />
      <view class="section-title">预设机构管理员</view>
      <view class="field-label">管理员姓名</view>
      <input v-model="form.adminName" class="input" maxlength="50" placeholder="请输入管理员姓名" />
      <view class="field-label">管理员手机号</view>
      <input v-model="form.adminPhone" class="input" type="number" maxlength="11" placeholder="用于微信手机号自动识别" />
      <view v-if="formError" class="form-error">{{ formError }}</view>
      <button class="primary save-button" :loading="saving" @click="save">创建机构并预设管理员</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createPlatformTenant } from '@/api/admin'
import type { CreatePlatformTenantPayload } from '@/types/api'

const saving = ref(false)
const formError = ref('')
const form = reactive<CreatePlatformTenantPayload>({
  tenantCode: '',
  tenantName: '',
  servicePlan: 'STANDARD',
  adminName: '',
  adminPhone: '',
})
async function save() {
  const payload = {
    tenantCode: form.tenantCode.trim().toUpperCase(),
    tenantName: form.tenantName.trim(),
    servicePlan: form.servicePlan.trim(),
    adminName: form.adminName.trim(),
    adminPhone: form.adminPhone.trim(),
  }
  if (!/^[A-Z0-9_-]{2,50}$/.test(payload.tenantCode)) {
    formError.value = '机构编号仅支持大写字母、数字、下划线或连字符'
    return
  }
  if (!payload.tenantName || !payload.servicePlan || !payload.adminName || !/^1[3-9]\d{9}$/.test(payload.adminPhone)) {
    formError.value = '请完整填写机构资料及有效的管理员手机号'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await createPlatformTenant(payload)
    uni.showToast({ title: '机构已创建', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : '创建失败，请稍后重试'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.admin-page { padding-top: 34rpx; }
.page-heading { margin: 0 6rpx 28rpx; }
.page-heading .title { margin-top: 8rpx; }
.form-card { padding: 30rpx; }
.field-label { margin: 25rpx 0 12rpx; color: #294a41; font-size: 25rpx; font-weight: 650; }
.field-label:first-child { margin-top: 0; }
.section-divider { height: 1rpx; margin: 34rpx 0 30rpx; background: #e8efec; }
.save-button { margin-top: 30rpx; }
.form-error { margin-top: 16rpx; color: #b42318; font-size: 23rpx; }
</style>

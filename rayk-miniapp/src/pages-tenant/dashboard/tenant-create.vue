<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view class="eyebrow">HOSPITAL ONBOARDING</view>
      <view class="title">新建合作医院</view>
      <view class="subtitle">创建后可在医院详情中预录入医生姓名和手机号。</view>
    </view>
    <view class="card form-card">
      <view class="field-label">医院编号</view>
      <input v-model="form.tenantCode" class="input" maxlength="50" placeholder="例如：SUZHOU_CLINIC" />
      <view class="field-label">医院名称</view>
      <input v-model="form.tenantName" class="input" maxlength="100" placeholder="请输入合作医院名称" />
      <view class="field-label">服务套餐</view>
      <input v-model="form.servicePlan" class="input" maxlength="50" placeholder="例如：STANDARD" />
      <view v-if="formError" class="form-error">{{ formError }}</view>
      <button class="primary save-button" :loading="saving" @click="save">创建合作医院</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createPlatformTenant } from '@/api/admin'
import type { CreatePlatformTenantPayload } from '@/types/api'

const saving = ref(false)
const formError = ref('')
const form = reactive<CreatePlatformTenantPayload>({ tenantCode: '', tenantName: '', servicePlan: 'STANDARD' })
async function save() {
  const payload = {
    tenantCode: form.tenantCode.trim().toUpperCase(),
    tenantName: form.tenantName.trim(),
    servicePlan: form.servicePlan.trim(),
  }
  if (!/^[A-Z0-9_-]{2,50}$/.test(payload.tenantCode)) {
    formError.value = '医院编号仅支持大写字母、数字、下划线或连字符'
    return
  }
  if (!payload.tenantName || !payload.servicePlan) {
    formError.value = '请完整填写医院资料'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await createPlatformTenant(payload)
    uni.showToast({ title: '医院已创建', icon: 'success' })
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
.save-button { margin-top: 30rpx; }
.form-error { margin-top: 16rpx; color: #b42318; font-size: 23rpx; }
</style>

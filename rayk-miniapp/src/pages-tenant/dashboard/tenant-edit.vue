<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view class="eyebrow">TENANT MANAGEMENT</view>
      <view class="title">编辑机构资料</view>
      <view class="subtitle">修改后会立即同步至平台概览与机构工作台</view>
    </view>
    <PageState :loading="loading" :error="error" :empty="!form">
      <view v-if="form" class="card form-card">
        <view class="field-label">机构名称</view>
        <input v-model="form.tenantName" class="input" maxlength="100" placeholder="请输入机构名称" />
        <view class="field-label">服务套餐</view>
        <input v-model="form.servicePlan" class="input" maxlength="50" placeholder="例如：STANDARD" />
        <view class="field-label">机构状态</view>
        <picker :range="statusOptions" range-key="label" @change="changeStatus">
          <view class="status-picker"><text>{{ selectedStatus.label }}</text><text>选择 ›</text></view>
        </picker>
        <view v-if="formError" class="form-error">{{ formError }}</view>
        <button class="primary save-button" :loading="saving" @click="save">保存机构信息</button>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getPlatformTenant, updatePlatformTenant } from '@/api/admin'
import type { UpdatePlatformTenantPayload } from '@/types/api'
import PageState from '@/components/PageState.vue'

const tenantId = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const formError = ref('')
const form = ref<UpdatePlatformTenantPayload>()
const statusOptions = [
  { label: '正常服务', value: 'ACTIVE' as const },
  { label: '暂停服务', value: 'DISABLED' as const },
]
const selectedStatus = computed(
  () => statusOptions.find((item) => item.value === form.value?.status) || statusOptions[0],
)

async function load() {
  if (!tenantId.value) {
    error.value = '未找到机构编号'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const tenant = await getPlatformTenant(tenantId.value)
    form.value = {
      tenantName: tenant.name,
      servicePlan: tenant.servicePlan,
      status: tenant.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE',
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '机构信息加载失败'
  } finally {
    loading.value = false
  }
}
function changeStatus(event: { detail: { value: number | string } }) {
  if (form.value) form.value.status = statusOptions[Number(event.detail.value)].value
}
async function save() {
  if (!form.value) return
  const tenantName = form.value.tenantName.trim()
  const servicePlan = form.value.servicePlan.trim()
  if (!tenantName || !servicePlan) {
    formError.value = '请填写机构名称和服务套餐'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await updatePlatformTenant(tenantId.value, { ...form.value, tenantName, servicePlan })
    uni.showToast({ title: '机构信息已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}
onLoad((query) => {
  tenantId.value = typeof query?.id === 'string' ? query.id : ''
  void load()
})
</script>

<style scoped>
.admin-page { padding-top: 34rpx; }
.page-heading { margin: 0 6rpx 28rpx; }
.page-heading .title { margin-top: 8rpx; }
.form-card { padding: 30rpx; }
.field-label { margin: 25rpx 0 12rpx; color: #294a41; font-size: 25rpx; font-weight: 650; }
.field-label:first-child { margin-top: 0; }
.status-picker { display: flex; justify-content: space-between; padding: 23rpx 24rpx; border: 1rpx solid #dce9e4; border-radius: 16rpx; background: #f8fbfa; color: #36564d; font-size: 27rpx; }
.status-picker text:last-child { color: #82948d; font-size: 22rpx; }
.save-button { margin-top: 30rpx; }
.form-error { margin-top: 16rpx; color: #b42318; font-size: 23rpx; }
</style>

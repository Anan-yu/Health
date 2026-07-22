<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view>
        <view class="eyebrow">TEAM & ACCESS</view>
        <view class="title">员工与角色</view>
        <view class="subtitle">仅展示当前机构员工及其已生效角色</view>
      </view>
      <view class="count-badge">{{ staff.length }} 人</view>
    </view>
    <view class="card invitation-card">
      <view class="section-title">预录入专业人员</view>
      <view class="subtitle">填写手机号后，对方微信授权手机号登录将自动进入对应工作台</view>
      <input v-model="form.displayName" class="input" placeholder="姓名" />
      <input v-model="form.phone" class="input" type="number" maxlength="11" placeholder="手机号" />
      <picker :range="roleOptions" range-key="label" @change="changeRole">
        <view class="role-picker">{{ selectedRole.label }} <text>⌄</text></view>
      </picker>
      <button class="primary invite-button" :loading="submitting" @click="submitStaff">保存并预录入</button>
      <view v-if="formError" class="form-error">{{ formError }}</view>
    </view>
    <PageState :loading="loading" :error="error" :empty="staff.length === 0">
      <view v-for="item in staff" :key="item.id" class="card staff-card">
        <view class="staff-avatar">{{ item.displayName.slice(0, 1) }}</view>
        <view class="staff-main">
          <view class="staff-head">
            <view class="section-title">{{ item.displayName }}</view>
            <StatusTag :status="item.status" />
          </view>
          <view class="muted">账号 {{ item.username }} · 编号 {{ item.id }}</view>
          <view class="roles">
            <text v-for="role in item.roles" :key="role" class="role-tag">{{
              roleName(role)
            }}</text>
          </view>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createTenantStaff, getTenantStaff } from '@/api/admin'
import type { Role, TenantStaff } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const staff = ref<TenantStaff[]>([])
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const formError = ref('')
const form = ref({ displayName: '', phone: '', roleCode: 'DOCTOR' as 'DOCTOR' | 'HEALTH_MANAGER' })
const roleOptions = [
  { label: '医生', value: 'DOCTOR' as const },
  { label: '健康管理师', value: 'HEALTH_MANAGER' as const },
]
const selectedRole = computed(() => roleOptions.find((item) => item.value === form.value.roleCode) || roleOptions[0])
const roleName = (role: Role) =>
  ({
    PLATFORM_ADMIN: '平台管理员',
    TENANT_ADMIN: '机构管理员',
    DOCTOR: '医生',
    HEALTH_MANAGER: '健康管理师',
    CUSTOMER: '普通客户',
  })[role]

async function loadStaff() {
  loading.value = true
  error.value = ''
  try {
    staff.value = await getTenantStaff()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '员工列表加载失败'
  } finally {
    loading.value = false
  }
}

function changeRole(event: { detail: { value: number | string } }) {
  form.value.roleCode = roleOptions[Number(event.detail.value)].value
}

async function submitStaff() {
  const phone = form.value.phone.trim()
  if (!form.value.displayName.trim() || !/^1[3-9]\d{9}$/.test(phone)) {
    formError.value = '请填写姓名和有效的 11 位手机号'
    return
  }
  submitting.value = true
  formError.value = ''
  try {
    await createTenantStaff({ ...form.value, displayName: form.value.displayName.trim(), phone })
    form.value = { displayName: '', phone: '', roleCode: 'DOCTOR' }
    uni.showToast({ title: '已预录入，等待微信授权登录', icon: 'success' })
    await loadStaff()
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : '预录入失败'
  } finally {
    submitting.value = false
  }
}

onShow(loadStaff)
</script>

<style scoped>
.admin-page {
  padding-top: 34rpx;
}
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 0 6rpx 28rpx;
}
.page-heading .title {
  margin-top: 8rpx;
}
.count-badge {
  padding: 10rpx 17rpx;
  border-radius: 999rpx;
  background: #e4f6f0;
  color: #0d765e;
  font-size: 22rpx;
}
.staff-card {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
  padding: 26rpx 24rpx;
}
.invitation-card {
  margin-bottom: 22rpx;
  padding: 28rpx;
}
.invitation-card .input {
  margin-top: 16rpx;
}
.role-picker {
  display: flex;
  justify-content: space-between;
  margin-top: 16rpx;
  padding: 22rpx 24rpx;
  border: 1rpx solid #dce9e4;
  border-radius: 16rpx;
  background: #f8fbfa;
  color: #36564d;
  font-size: 27rpx;
}
.invite-button {
  margin-top: 18rpx;
}
.form-error {
  margin-top: 14rpx;
  color: #b42318;
  font-size: 23rpx;
}
.staff-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: #dff3ec;
  color: #0d7059;
  font-size: 28rpx;
  font-weight: 750;
}
.staff-main {
  flex: 1;
  min-width: 0;
  margin-left: 20rpx;
}
.staff-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.roles {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 13rpx;
}
.role-tag {
  padding: 7rpx 13rpx;
  border-radius: 10rpx;
  background: #edf5f2;
  color: #426b60;
  font-size: 20rpx;
}
</style>

<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view class="heading-copy">
        <view class="title">合作医院管理</view>
        <view class="subtitle">创建、编辑、启停合作医院，并统一预录入医生。</view>
      </view>
      <view class="heading-actions">
        <view class="create-tenant" @click="createTenant">新建医院</view>
        <view class="refresh" @click="load">刷新</view>
      </view>
    </view>
    <PageState :loading="loading" :error="error" :empty="!overview"
      ><template v-if="overview"
        ><view class="metric-grid"
          ><view v-for="item in metrics" :key="item.label" class="card metric-card"
            ><view class="metric-value">{{ item.value }}</view
            ><view class="metric-label">{{ item.label }}</view></view
           ></view
         ><view class="card admin-phone-card">
           <view class="section-title">管理员登录手机号</view>
           <view class="muted admin-phone-tip">预录入后，平台管理员与医生一样通过微信授权手机号登录。</view>
           <view v-if="adminPhoneMasked" class="muted admin-phone-current">当前手机号：{{ adminPhoneMasked }}</view>
           <input v-model="adminPhone" class="input" type="number" maxlength="11" placeholder="请输入管理员手机号" />
           <view v-if="adminPhoneError" class="form-error">{{ adminPhoneError }}</view>
           <button class="secondary admin-phone-button" :loading="adminPhoneSaving" @click="saveAdminPhone">
             保存管理员手机号
           </button>
         </view
         ><view class="section-heading"
          ><view class="section-title">合作医院</view
          ><view class="muted"
            >正常 {{ overview.activeTenantCount }} / {{ overview.tenantCount }}</view
          ></view
        ><view
          v-for="tenant in overview.tenants"
          :key="tenant.id"
          class="card tenant-card"
          @click="editTenant(tenant.id)"
          ><view class="tenant-icon">院</view
          ><view class="tenant-main"
            ><view class="section-title">{{ tenant.name }}</view
            ><view class="muted"
              >{{ tenant.code }} · {{ tenant.servicePlan }} · {{ tenant.userCount }} 位医生</view
            ></view
          ><StatusTag :status="tenant.status" /><view class="edit-arrow">管理 ›</view></view
        ></template
      ></PageState
    >
  </view>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPlatformAdminProfile, getPlatformOverview, updatePlatformAdminPhone } from '@/api/admin'
import type { PlatformOverview } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const overview = ref<PlatformOverview>(),
  loading = ref(true),
  error = ref(''),
  adminPhone = ref(''),
  adminPhoneMasked = ref(''),
  adminPhoneError = ref(''),
  adminPhoneSaving = ref(false)
const metrics = computed(() =>
  !overview.value
    ? []
    : [
        { label: '合作医院', value: overview.value.tenantCount },
        { label: '预录入医生', value: overview.value.userCount },
        { label: '手机号用户', value: overview.value.phoneCustomerCount },
        { label: '健康随访任务', value: overview.value.pendingFollowupCount },
      ],
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [overviewData, admin] = await Promise.all([getPlatformOverview(), getPlatformAdminProfile()])
    overview.value = overviewData
    adminPhone.value = ''
    adminPhoneMasked.value = admin.phoneMasked || ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '平台概览加载失败'
  } finally {
    loading.value = false
  }
}
onShow(load)
const createTenant = () => uni.navigateTo({ url: '/pages-tenant/dashboard/tenant-create' })
const editTenant = (tenantId: string) =>
  uni.navigateTo({ url: `/pages-tenant/dashboard/tenant-edit?id=${tenantId}` })
async function saveAdminPhone() {
  const phone = adminPhone.value.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    adminPhoneError.value = '请输入有效的 11 位手机号'
    return
  }
  adminPhoneSaving.value = true
  adminPhoneError.value = ''
  try {
    const admin = await updatePlatformAdminPhone(phone)
    adminPhone.value = ''
    adminPhoneMasked.value = admin.phoneMasked || ''
    uni.showToast({ title: '管理员手机号已保存', icon: 'success' })
  } catch (cause) {
    adminPhoneError.value = cause instanceof Error ? cause.message : '保存失败，请稍后重试'
  } finally {
    adminPhoneSaving.value = false
  }
}
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
  gap: 18rpx;
}
.heading-copy {
  flex: 1;
  min-width: 0;
}
.heading-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12rpx;
}
.create-tenant,
.refresh {
  box-sizing: border-box;
  min-width: 120rpx;
  min-height: 64rpx;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: #e4f6f0;
  color: #0d765e;
  font-size: 22rpx;
  line-height: 1.25;
  text-align: center;
  white-space: nowrap;
}
.create-tenant {
  background: #0d765e;
  color: #fff;
}
.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}
.metric-card {
  min-height: 142rpx;
  padding: 28rpx 26rpx;
}
.metric-value {
  color: #0d765e;
  font-size: 46rpx;
  font-weight: 760;
}
.metric-label {
  margin-top: 5rpx;
  color: #657b74;
  font-size: 22rpx;
}
.admin-phone-card {
  margin: 24rpx 0 8rpx;
  padding: 26rpx;
}
.admin-phone-tip {
  margin: 10rpx 0 18rpx;
  line-height: 1.5;
}
.admin-phone-current {
  margin: 0 0 14rpx;
  color: #0d765e;
}
.admin-phone-button {
  margin-top: 16rpx;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  margin: 34rpx 6rpx 18rpx;
}
.tenant-card {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
  padding: 25rpx;
}
.edit-arrow {
  margin-left: 14rpx;
  color: #0d765e;
  font-size: 21rpx;
  white-space: nowrap;
}
.tenant-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68rpx;
  height: 68rpx;
  border-radius: 21rpx;
  background: #dff3ec;
  color: #0d7059;
  font-weight: 750;
}
.tenant-main {
  flex: 1;
  min-width: 0;
  margin: 0 18rpx;
}
</style>

<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view class="eyebrow">DOCTOR DIRECTORY</view>
      <view class="title">预录入医生管理</view>
      <view class="subtitle">按合作医院查看已预录入的医生账号，维护微信手机号登录信息。</view>
    </view>

    <PageState :loading="loading" :error="error" :empty="!loading && groups.length === 0">
      <template v-if="groups.length">
        <view class="summary-grid">
          <view class="card summary-card">
            <view class="summary-value">{{ totalDoctors }}</view>
            <view class="summary-label">预录入医生</view>
          </view>
          <view class="card summary-card">
            <view class="summary-value">{{ groups.length }}</view>
            <view class="summary-label">覆盖医院</view>
          </view>
        </view>

        <view class="section-heading">
          <view class="section-title">医生名单</view>
          <view class="muted">共 {{ totalDoctors }} 位</view>
        </view>

        <view v-for="group in groups" :key="group.tenant.id" class="card hospital-card">
          <view class="hospital-head">
            <view class="hospital-icon">院</view>
            <view class="hospital-copy">
              <view class="hospital-name">{{ group.tenant.name }}</view>
              <view class="muted">{{ group.tenant.code }} · {{ group.doctors.length }} 位医生</view>
            </view>
            <view class="hospital-action" @click="manageHospital(group.tenant.id)">管理 ›</view>
          </view>

          <view v-if="group.doctors.length" class="doctor-list">
            <view v-for="doctor in group.doctors" :key="doctor.id" class="doctor-row">
              <view class="doctor-avatar">医</view>
              <view class="doctor-info">
                <view class="doctor-name">{{ doctor.displayName }}</view>
                <view class="doctor-phone">{{ doctor.phoneMasked || '未设置手机号' }}</view>
              </view>
              <view class="doctor-status">{{ doctor.status === 'ACTIVE' ? '可登录' : '已停用' }}</view>
            </view>
          </view>
          <view v-else class="empty-doctors">该医院暂未预录入医生，可进入医院详情添加。</view>
        </view>
      </template>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPlatformDoctors, getPlatformOverview } from '@/api/admin'
import type { TenantStaff, TenantSummary } from '@/types/api'
import PageState from '@/components/PageState.vue'

interface DoctorGroup {
  tenant: TenantSummary
  doctors: TenantStaff[]
}

const groups = ref<DoctorGroup[]>([])
const loading = ref(true)
const error = ref('')
const totalDoctors = computed(() => groups.value.reduce((total, group) => total + group.doctors.length, 0))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const overview = await getPlatformOverview()
    const results = await Promise.all(
      overview.tenants.map(async (tenant) => {
        try {
          return { tenant, doctors: await getPlatformDoctors(tenant.id) }
        } catch {
          return { tenant, doctors: [] }
        }
      }),
    )
    groups.value = results
    uni.setNavigationBarTitle({ title: '预录入医生' })
  } catch (cause) {
    groups.value = []
    error.value = cause instanceof Error ? cause.message : '医生名单加载失败'
  } finally {
    loading.value = false
  }
}

const manageHospital = (tenantId: string) =>
  uni.navigateTo({ url: `/pages-tenant/dashboard/tenant-edit?id=${tenantId}` })

onShow(load)
</script>

<style scoped>
.admin-page {
  padding-top: 34rpx;
}
.page-heading {
  margin: 0 6rpx 28rpx;
}
.page-heading .title {
  margin-top: 8rpx;
}
.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}
.summary-card {
  min-height: 128rpx;
  padding: 26rpx;
}
.summary-value {
  color: #0d765e;
  font-size: 44rpx;
  font-weight: 760;
}
.summary-label {
  margin-top: 6rpx;
  color: #657b74;
  font-size: 22rpx;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 34rpx 8rpx 16rpx;
}
.hospital-card {
  margin-bottom: 18rpx;
  padding: 24rpx;
}
.hospital-head {
  display: flex;
  align-items: center;
}
.hospital-icon,
.doctor-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 64rpx;
  height: 64rpx;
  border-radius: 20rpx;
  background: #dff3ec;
  color: #0f7a62;
  font-weight: 700;
}
.hospital-copy {
  min-width: 0;
  flex: 1;
  margin-left: 18rpx;
}
.hospital-name {
  overflow: hidden;
  color: #24463c;
  font-size: 28rpx;
  font-weight: 680;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hospital-action {
  margin-left: 14rpx;
  color: #0d765e;
  font-size: 22rpx;
  white-space: nowrap;
}
.doctor-list {
  margin-top: 22rpx;
  border-top: 1rpx solid #e5eee9;
}
.doctor-row {
  display: flex;
  align-items: center;
  min-height: 86rpx;
  border-bottom: 1rpx solid #edf3f0;
}
.doctor-row:last-child {
  border-bottom: 0;
}
.doctor-avatar {
  width: 52rpx;
  height: 52rpx;
  border-radius: 17rpx;
  font-size: 22rpx;
}
.doctor-info {
  min-width: 0;
  flex: 1;
  margin-left: 16rpx;
}
.doctor-name {
  color: #294b41;
  font-size: 25rpx;
  font-weight: 650;
}
.doctor-phone {
  margin-top: 4rpx;
  color: #82948d;
  font-size: 21rpx;
}
.doctor-status {
  padding: 7rpx 12rpx;
  border-radius: 999rpx;
  background: #e7f7f0;
  color: #0b7a5d;
  font-size: 20rpx;
}
.empty-doctors {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #e5eee9;
  color: #82948d;
  font-size: 22rpx;
  line-height: 1.55;
}
</style>

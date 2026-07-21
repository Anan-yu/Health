<template>
  <view class="page detail-page">
    <PageState :loading="loading" :empty="!item">
      <view class="profile-hero">
        <view class="avatar">{{ item?.name.slice(0, 1) }}</view>
        <view class="profile-content">
          <view class="profile-head"
            ><view class="profile-name">{{ item?.name }}</view
            ><StatusTag :status="item?.status || ''"
          /></view>
          <view class="profile-id">客户档案 · {{ item?.id }}</view>
        </view>
      </view>
      <view class="section-head"
        ><view
          ><view class="eyebrow">PROFILE</view><view class="section-title">基础档案</view></view
        ></view
      >
      <view class="card info-card">
        <view class="info-item"
          ><text>性别</text><text>{{ item?.gender || '未填写' }}</text></view
        >
        <view class="info-item"
          ><text>出生日期</text><text>{{ item?.birthDate || '未填写' }}</text></view
        >
        <view class="info-item"
          ><text>联系方式</text><text>{{ item?.phoneMasked || '未填写' }}</text></view
        >
        <view class="info-item"
          ><text>负责医生</text><text>{{ item?.assignedDoctorId || '待分配' }}</text></view
        >
        <view class="info-item last"
          ><text>健康管理师</text><text>{{ item?.assignedManagerId || '待分配' }}</text></view
        >
      </view>
      <view class="section-head"
        ><view
          ><view class="eyebrow">ACTIONS</view><view class="section-title">快捷操作</view></view
        ></view
      >
      <view class="action-grid">
        <view v-if="canUploadReport" class="action-card primary-action" @click="report"
          ><view class="action-icon">传</view
          ><view><text>上传检验报告</text><text>创建新的报告任务</text></view
          ><text class="action-arrow">›</text></view
        >
        <view v-if="canManageFollowup" class="action-card" @click="followup"
          ><view class="action-icon followup-icon">访</view
          ><view><text>查看随访</text><text>跟进健康管理计划</text></view
          ><text class="action-arrow">›</text></view
        >
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getPatient } from '@/api/patient'
import type { Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const item = ref<Patient>(),
  loading = ref(true)
onLoad(async (q) => {
  try {
    item.value = await getPatient(String(q?.id || ''))
  } finally {
    loading.value = false
  }
})
const report = () =>
  uni.navigateTo({ url: `/pages-customer/lab-report/upload?patientId=${item.value?.id || ''}` })
const followup = () =>
  uni.navigateTo({ url: `/pages-business/followup/index?patientId=${item.value?.id || ''}` })
const canUploadReport = computed(() => auth.permissions.includes('lab-report:manage'))
const canManageFollowup = computed(() =>
  auth.permissions.some((permission) =>
    ['followup:manage', 'followup:create'].includes(permission),
  ),
)
</script>

<style scoped>
.detail-page {
  padding-top: 24rpx;
}
.profile-hero {
  display: flex;
  align-items: center;
  padding: 36rpx 32rpx;
  border-radius: 34rpx;
  background: linear-gradient(140deg, #16443a, #0c725b);
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(15, 91, 73, 0.2);
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 100rpx;
  height: 100rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.22);
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 38rpx;
  font-weight: 760;
}
.profile-content {
  flex: 1;
  min-width: 0;
  margin-left: 24rpx;
}
.profile-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
}
.profile-name {
  font-size: 37rpx;
  font-weight: 750;
}
.profile-id {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.65);
  font-size: 22rpx;
}
.info-card {
  padding: 6rpx 30rpx;
}
.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 0;
  border-bottom: 1rpx solid #edf1f0;
}
.info-item.last {
  border-bottom: 0;
}
.info-item text:first-child {
  color: #7b8a85;
  font-size: 23rpx;
}
.info-item text:last-child {
  color: #29453d;
  font-size: 25rpx;
  font-weight: 620;
}
.action-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18rpx;
}
.action-card {
  display: flex;
  align-items: center;
  padding: 26rpx;
  border: 1rpx solid #e1eae7;
  border-radius: 26rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(31, 74, 63, 0.05);
}
.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 70rpx;
  height: 70rpx;
  margin-right: 20rpx;
  border-radius: 23rpx;
  background: #dff3ec;
  color: #0e735c;
  font-size: 23rpx;
  font-weight: 740;
}
.followup-icon {
  background: #eaf1ff;
  color: #436eae;
}
.action-card > view:nth-child(2) {
  display: flex;
  flex: 1;
  flex-direction: column;
}
.action-card > view:nth-child(2) text:first-child {
  font-size: 27rpx;
  font-weight: 670;
}
.action-card > view:nth-child(2) text:last-child {
  margin-top: 5rpx;
  color: #899691;
  font-size: 21rpx;
}
.action-arrow {
  color: #9eaba6;
  font-size: 38rpx;
}
</style>

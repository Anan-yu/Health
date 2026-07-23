<template>
  <view class="page detail-page"
    ><PageState :loading="loading" :empty="!item">
      <view class="profile-hero"
        ><view class="avatar">{{ item?.name.slice(0, 1) }}</view
        ><view class="profile-content"
          ><view class="profile-head"
            ><view class="profile-name">{{ item?.name }}</view
            ><StatusTag :status="item?.status || ''" /></view></view
      ></view>
      <view class="section-head"
        ><view
          ><view class="eyebrow">基础信息</view><view class="section-title">基础档案</view></view
        ></view
      >
      <view class="card info-card"
        ><view class="info-item"
          ><text>性别</text><text>{{ genderLabel }}</text></view
        ><view class="info-item"
          ><text>出生日期</text><text>{{ item?.birthDate || '未填写' }}</text></view
        ><view class="info-item last"
          ><text>联系方式</text><text>{{ item?.phoneMasked || '未填写' }}</text></view
        ></view
      >
      <view class="section-head"
        ><view
          ><view class="eyebrow">健康资料</view
          ><view class="section-title">健康资料查看</view></view
        ></view
      >
      <view class="action-grid"
        ><view class="action-card" @click="reports"
          ><view class="action-icon">报</view
          ><view><text>检验报告</text><text>查看检验报告</text></view
          ><text>›</text></view
        ><view class="action-card" @click="assessments"
          ><view class="action-icon ai">AI</view
          ><view><text>AI 评估</text><text>查看评估结果与健康建议</text></view
          ><text>›</text></view
        ><view class="action-card" @click="healthReports"
          ><view class="action-icon health">康</view
          ><view><text>健康评估报告</text><text>查看并下载已发布报告</text></view
          ><text>›</text></view
        ></view
      >
    </PageState></view
  >
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getPatient } from '@/api/patient'
import type { Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const item = ref<Patient>(),
  loading = ref(true)
const genderLabel = computed(() => {
  if (item.value?.gender === 'MALE') return '男'
  if (item.value?.gender === 'FEMALE') return '女'
  return '未填写'
})
onLoad(async (q) => {
  try {
    item.value = await getPatient(String(q?.id || ''))
  } finally {
    loading.value = false
  }
})
const reports = () => uni.navigateTo({ url: '/pages-business/lab-report/index' })
const assessments = () => uni.navigateTo({ url: '/pages-business/assessment/index' })
const healthReports = () => {
  if (!item.value) return
  uni.navigateTo({
    url: `/pages-business/health-report/index?patientId=${item.value.id}&name=${encodeURIComponent(item.value.name)}`,
  })
}
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
.info-card {
  padding: 6rpx 30rpx;
}
.info-item {
  display: flex;
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
  gap: 18rpx;
}
.action-card {
  display: flex;
  align-items: center;
  padding: 26rpx;
  border: 1rpx solid #e1eae7;
  border-radius: 26rpx;
  background: #fff;
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
  font-weight: 740;
}
.action-icon.ai {
  background: #eee5ff;
  color: #7250b9;
}
.action-icon.health {
  background: #fff0d3;
  color: #a26605;
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
.action-card > text {
  color: #9eaba6;
  font-size: 38rpx;
}
</style>

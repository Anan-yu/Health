<template>
  <view class="page">
    <view class="title">我的健康档案</view>
    <PageState :loading="loading" :error="error" :empty="!patient">
      <view class="card"
        ><view class="row"
          ><text>姓名</text><text>{{ patient?.name }}</text></view
        ><view class="row"
          ><text>出生日期</text><text>{{ patient?.birthDate || '待完善' }}</text></view
        ><view class="row"
          ><text>身高 / 体重</text
          ><text>{{ profile?.heightCm || '-' }} cm / {{ profile?.weightKg || '-' }} kg</text></view
        ><view class="row"
          ><text>BMI</text><text>{{ profile?.bmi || '-' }}</text></view
        ><view class="row"
          ><text>运动 / 睡眠</text><text>{{ lifestyleLabel }}</text></view
        ><view class="row"
          ><text>最近更新</text><text>{{ formatTime(profile?.updatedAt) }}</text></view
        ></view
      >
      <view class="card"
        ><view class="row"
          ><view
            ><view class="section-title">档案完整度</view
            ><view class="subtitle">补充生活习惯和健康史，帮助医生更好解读报告</view></view
          ><text class="completion">{{ profile?.profileCompleteness || 0 }}%</text></view
        ><progress :percent="profile?.profileCompleteness || 0" active-color="#176b57"
      /></view>
      <button class="primary-button" @click="edit">完善健康档案</button>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getHealthProfile, getMyProfile } from '@/api/patient'
import type { HealthProfile, Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'

const patient = ref<Patient | null>(null)
const profile = ref<HealthProfile | null>(null)
const loading = ref(true)
const error = ref('')
const lifestyleLabel = computed(() => {
  const exercise = profile.value?.exerciseFrequency || '待完善'
  const sleep = profile.value?.sleepQuality || '待完善'
  return `${exercise} / ${sleep}`
})
const formatTime = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '待完善')
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    patient.value = await getMyProfile()
    if (patient.value) profile.value = await getHealthProfile(patient.value.id)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '档案加载失败'
  } finally {
    loading.value = false
  }
})
const edit = () => uni.navigateTo({ url: '/pages-customer/profile/edit' })
</script>

<style scoped>
.completion {
  color: #0f7a62;
  font-size: 32rpx;
  font-weight: 750;
}
.primary-button {
  margin-top: 26rpx;
  border-radius: 16rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 28rpx;
}
</style>

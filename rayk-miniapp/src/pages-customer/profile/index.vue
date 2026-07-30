<template>
  <view class="page elder-page">
    <view class="title">我的健康档案</view>
    <PageState :loading="loading" :error="error" :empty="!patient">
      <view class="card"
        ><view class="row"
          ><text>姓名</text><text>{{ patient?.name }}</text></view
        ><view class="row"
          ><text>性别</text><text>{{ genderLabel }}</text></view
        ><view class="row"
          ><text>出生日期</text><text>{{ patient?.birthDate || '待完善' }}</text></view
        ><view class="row"
          ><text>身高</text><text>{{ profile?.heightCm || '-' }} cm</text></view
        ><view class="row"
          ><text>体重</text><text>{{ profile?.weightKg || '-' }} kg</text></view
        ><view class="row"
          ><text>腰围</text><text>{{ profile?.waistCm ?? '-' }} cm</text></view
        ><view class="row"
          ><text>近三个月体重变化</text
          ><text>{{ profile?.recentWeightChangeKg ?? '-' }} kg</text></view
        ><view class="row"
          ><text>最近更新</text><text>{{ formatTime(profile?.updatedAt) }}</text></view
        ></view
      >
      <view class="card completion-card"
        ><view class="row"
          ><view
            ><view class="section-title">档案完整度</view
            ><view class="subtitle">补充生活习惯和健康史，帮助医生更好解读报告</view></view
          ><text class="completion">{{ profile?.profileCompleteness || 0 }}%</text></view
        ><progress :percent="profile?.profileCompleteness || 0" active-color="#176b57"
        />
        <button class="profile-action" @click="edit">
          <text>完善健康档案</text><text class="profile-action-arrow">›</text>
        </button>
      </view>
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
const genderLabel = computed(
  () => ({ MALE: '男', FEMALE: '女' })[patient.value?.gender || ''] || '待完善',
)
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
.card .row {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #edf2f0;
}
.card .row:last-child {
  border-bottom: 0;
}
.card .row > text:first-child {
  color: #506b62;
}
.card .row > text:last-child {
  color: #163c32;
  font-weight: 650;
  text-align: right;
}
.completion {
  color: #0f7a62;
  font-size: 38rpx;
  font-weight: 750;
}
.completion-card {
  padding: 22rpx 30rpx 28rpx;
}
.completion-card .row {
  padding: 4rpx 0 16rpx;
}
.profile-action {
  position: relative;
  display: flex;
  box-sizing: border-box;
  width: 100%;
  min-height: 88rpx;
  margin: 26rpx 0 0;
  padding: 0 26rpx;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #14866b, #0b7059);
  color: #fff;
  font-size: 30rpx;
  font-weight: 720;
  box-shadow: 0 10rpx 22rpx rgba(15, 122, 98, 0.16);
}
.profile-action::after {
  display: none;
}
.profile-action:active {
  background: #0b6e58;
  box-shadow: none;
}
.profile-action-arrow {
  position: absolute;
  right: 28rpx;
  color: rgba(255, 255, 255, 0.88);
  font-size: 38rpx;
  line-height: 1;
}
</style>

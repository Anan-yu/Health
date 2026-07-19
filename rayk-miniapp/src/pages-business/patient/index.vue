<template>
  <view class="page patient-page">
    <view class="patient-header">
      <view>
        <view class="eyebrow">CLIENT CARE</view>
        <view class="title">客户管理</view>
        <view class="subtitle">客户档案与健康服务分配</view>
      </view>
      <view class="create-button" @click="create">＋ 新建</view>
    </view>

    <view class="search-box">
      <text class="search-icon">⌕</text>
      <input v-model="keyword" placeholder="搜索客户姓名或联系方式" />
      <text v-if="keyword" class="clear" @click="keyword = ''">×</text>
    </view>

    <view class="client-summary">
      <view class="summary-icon">客</view>
      <view
        ><text>{{ items.length }}</text
        ><text>当前服务客户</text></view
      >
      <view class="summary-line" />
      <view
        ><text>{{ activeCount }}</text
        ><text>档案状态正常</text></view
      >
    </view>

    <PageState :loading="loading" :error="error" :empty="filteredItems.length === 0">
      <view
        v-for="item in filteredItems"
        :key="item.id"
        class="card client-card"
        @click="detail(item.id)"
      >
        <view class="client-avatar">{{ item.name.slice(0, 1) }}</view>
        <view class="client-content">
          <view class="client-head"
            ><view class="client-name">{{ item.name }}</view
            ><StatusTag :status="item.status"
          /></view>
          <view class="client-meta"
            >{{ item.gender }} · {{ item.phoneMasked || '未填写联系方式' }}</view
          >
          <view class="service-row">
            <text>医生 {{ item.assignedDoctorId || '待分配' }}</text>
            <text>管理师 {{ item.assignedManagerId || '待分配' }}</text>
          </view>
        </view>
        <view class="client-arrow">›</view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPatients } from '@/api/patient'
import type { Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const items = ref<Patient[]>([]),
  keyword = ref(''),
  loading = ref(true),
  error = ref('')
const activeCount = computed(() => items.value.filter((item) => item.status === 'ACTIVE').length)
const filteredItems = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return items.value
  return items.value.filter(
    (item) => item.name.toLowerCase().includes(value) || item.phoneMasked?.includes(value),
  )
})

onShow(async () => {
  loading.value = true
  try {
    items.value = (await getPatients()).records
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
const detail = (id: string) => uni.navigateTo({ url: `/pages-business/patient/detail?id=${id}` })
const create = () => uni.navigateTo({ url: '/pages-business/patient/create' })
</script>

<style scoped>
.patient-page {
  padding-top: 34rpx;
}
.patient-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 4rpx 28rpx;
}
.patient-header .title {
  margin-top: 8rpx;
}
.create-button {
  flex: 0 0 auto;
  margin-left: 20rpx;
  padding: 15rpx 22rpx;
  border-radius: 999rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 23rpx;
  font-weight: 650;
  box-shadow: 0 8rpx 18rpx rgba(15, 122, 98, 0.18);
}
.search-box {
  display: flex;
  align-items: center;
  height: 86rpx;
  box-sizing: border-box;
  padding: 0 24rpx;
  border: 1rpx solid #dfe9e6;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 8rpx 24rpx rgba(30, 72, 61, 0.05);
}
.search-box input {
  flex: 1;
  height: 100%;
  font-size: 24rpx;
}
.search-icon {
  margin-right: 14rpx;
  color: #769087;
  font-size: 33rpx;
}
.clear {
  color: #9ba8a4;
  font-size: 34rpx;
}
.client-summary {
  display: flex;
  align-items: center;
  margin: 22rpx 0;
  padding: 24rpx 26rpx;
  border-radius: 25rpx;
  background: linear-gradient(135deg, #e3f6ef, #f3faf7);
}
.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62rpx;
  height: 62rpx;
  margin-right: 18rpx;
  border-radius: 20rpx;
  background: #c9ebdf;
  color: #0d725a;
  font-size: 22rpx;
  font-weight: 750;
}
.client-summary > view:not(.summary-icon):not(.summary-line) {
  display: flex;
  flex-direction: column;
}
.client-summary text:first-child {
  color: #19473b;
  font-size: 30rpx;
  font-weight: 740;
}
.client-summary text:last-child {
  color: #789087;
  font-size: 19rpx;
}
.summary-line {
  width: 1rpx;
  height: 50rpx;
  margin: 0 28rpx;
  background: #c9ddd6;
}
.client-card {
  display: flex;
  align-items: center;
  padding: 26rpx 24rpx;
}
.client-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 82rpx;
  height: 82rpx;
  border-radius: 26rpx;
  background: linear-gradient(145deg, #dff3ec, #c8eadd);
  color: #0d7059;
  font-size: 29rpx;
  font-weight: 750;
}
.client-content {
  flex: 1;
  min-width: 0;
  margin-left: 22rpx;
}
.client-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.client-name {
  font-size: 29rpx;
  font-weight: 690;
}
.client-meta {
  margin-top: 7rpx;
  color: #7d8c87;
  font-size: 22rpx;
}
.service-row {
  display: flex;
  gap: 18rpx;
  margin-top: 14rpx;
  color: #93a09b;
  font-size: 20rpx;
}
.client-arrow {
  margin-left: 10rpx;
  color: #a3afab;
  font-size: 40rpx;
}
</style>

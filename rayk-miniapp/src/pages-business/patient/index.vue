<template>
  <view class="page patient-page">
    <view class="patient-header"
      ><view
        ><view class="title">体检者查询</view
        ><view class="subtitle">按姓名或完整手机号查询体检者</view></view
      ></view
    >
    <view class="search-box"
      ><text>⌕</text
      ><input
        v-model="keyword"
        confirm-type="search"
        placeholder="输入姓名或11位手机号"
        @confirm="load"
      /><text v-if="keyword" class="clear" @click="clear">×</text></view
    >
    <view class="client-summary"
      ><view class="summary-icon">客</view
      ><view
        ><text>{{ items.length }}</text
        ><text>查询结果</text></view
      ><view class="summary-line" /><view><text>资料</text><text>健康资料与报告</text></view></view
    >
    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view v-for="item in items" :key="item.id" class="card client-card" @click="detail(item.id)">
        <view class="client-avatar">{{ item.name.slice(0, 1) }}</view
        ><view class="client-content"
          ><view class="client-head"
            ><view class="client-name">{{ item.name }}</view
            ><StatusTag :status="item.status" /></view
          ><view class="client-meta"
            >{{ genderLabel(item.gender) }} · {{ item.phoneMasked || '未填写联系方式' }}</view
          ><view class="service-row">查看健康档案、检验报告与 AI 评估</view></view
        ><view class="client-arrow">›</view>
      </view>
    </PageState>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPatients } from '@/api/patient'
import type { Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<Patient[]>([]),
  keyword = ref(''),
  loading = ref(true),
  error = ref('')
async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = (await getPatients(keyword.value.trim())).records
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}
function clear() {
  keyword.value = ''
  void load()
}
function genderLabel(value: string) {
  return value === 'MALE' ? '男' : value === 'FEMALE' ? '女' : '未填写'
}
onShow(() => void load())
const detail = (id: string) => uni.navigateTo({ url: `/pages-business/patient/detail?id=${id}` })
</script>
<style scoped>
.patient-page {
  padding-top: 34rpx;
}
.patient-header {
  margin: 0 4rpx 28rpx;
}
.search-box {
  display: flex;
  align-items: center;
  height: 86rpx;
  padding: 0 24rpx;
  border: 1rpx solid #dfe9e6;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 8rpx 24rpx rgba(30, 72, 61, 0.05);
  color: #769087;
}
.search-box input {
  flex: 1;
  height: 100%;
  margin-left: 14rpx;
  font-size: 24rpx;
}
.clear {
  font-size: 36rpx;
}
.client-summary {
  display: flex;
  align-items: center;
  margin: 22rpx 0;
  padding: 24rpx 26rpx;
  border-radius: 25rpx;
  background: linear-gradient(135deg, #e3f6ef, #f3faf7);
}
.summary-icon,
.client-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #c9ebdf;
  color: #0d725a;
  font-weight: 750;
}
.summary-icon {
  width: 62rpx;
  height: 62rpx;
  margin-right: 18rpx;
  border-radius: 20rpx;
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
  flex: 0 0 auto;
  width: 82rpx;
  height: 82rpx;
  border-radius: 26rpx;
  background: linear-gradient(145deg, #dff3ec, #c8eadd);
  font-size: 29rpx;
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

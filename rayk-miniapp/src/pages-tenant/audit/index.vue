<template>
  <view class="page audit-page">
    <view class="page-heading">
      <view>
        <view class="eyebrow">SECURITY AUDIT</view>
        <view class="title">跨租户审计</view>
        <view class="subtitle">受控查看全平台已脱敏操作记录</view>
      </view>
      <view class="refresh" @click="load">刷新</view>
    </view>
    <view class="filter-card">
      <input v-model="tenantId" type="number" placeholder="输入机构 ID 筛选，留空查看全部" />
      <view class="filter-button" @click="load">查询</view>
    </view>
    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view v-for="item in items" :key="item.id" class="card audit-card">
        <view class="row">
          <view>
            <view class="section-title">{{ operationLabel(item.operationType) }}</view>
            <view class="tenant-label"
              >机构 {{ item.tenantId }} · 操作人 {{ item.operatorId }}</view
            >
          </view>
          <view class="result" :class="item.result === 'SUCCESS' ? 'success' : 'failure'">
            {{ item.result === 'SUCCESS' ? '成功' : '失败' }}
          </view>
        </view>
        <view class="audit-line"
          >资源：{{ item.resourceType }}{{ item.resourceId ? ` · ${item.resourceId}` : '' }}</view
        >
        <view v-if="item.detailMasked" class="audit-line">{{ item.detailMasked }}</view>
        <view class="audit-foot">
          <text>{{ item.createdAt }}</text>
          <text>请求 {{ item.requestId || '-' }}</text>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getAuditLogs } from '@/api/audit'
import type { AuditLog } from '@/types/api'
import PageState from '@/components/PageState.vue'

const items = ref<AuditLog[]>([])
const tenantId = ref('')
const loading = ref(true)
const error = ref('')
const operationLabel = (operation: string) =>
  ({ CREATE: '创建', UPDATE: '更新', DELETE: '删除', READ: '访问' })[operation] || operation

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    items.value = (await getAuditLogs(tenantId.value.trim() || undefined)).records
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '审计记录加载失败'
  } finally {
    loading.value = false
  }
}
onShow(load)
</script>

<style scoped>
.audit-page {
  padding-top: 34rpx;
}
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 0 6rpx 24rpx;
}
.page-heading .title {
  margin-top: 8rpx;
}
.refresh {
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background: #e4f6f0;
  color: #0d765e;
  font-size: 22rpx;
}
.filter-card {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 24rpx;
  padding: 13rpx 14rpx 13rpx 22rpx;
  border: 1rpx solid #dfe9e6;
  border-radius: 22rpx;
  background: #fff;
}
.filter-card input {
  flex: 1;
  height: 58rpx;
  font-size: 23rpx;
}
.filter-button {
  padding: 14rpx 24rpx;
  border-radius: 16rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 22rpx;
}
.audit-card {
  margin-bottom: 18rpx;
}
.tenant-label {
  margin-top: 7rpx;
  color: #789087;
  font-size: 20rpx;
}
.result {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}
.result.success {
  background: #e4f6f0;
  color: #0d765e;
}
.result.failure {
  background: #fff0ee;
  color: #bd4d40;
}
.audit-line {
  margin-top: 12rpx;
  color: #6f8079;
  font-size: 22rpx;
  line-height: 1.55;
}
.audit-foot {
  display: flex;
  margin-top: 18rpx;
  color: #9aa6a2;
  font-size: 19rpx;
  justify-content: space-between;
  gap: 18rpx;
}
.audit-foot text:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

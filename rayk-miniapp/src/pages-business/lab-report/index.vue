<template>
  <view class="page task-page">
    <view class="task-hero">
      <view>
        <view class="eyebrow-light">REPORT WORKFLOW</view>
        <view class="hero-title">报告任务</view>
        <view class="hero-copy">跟踪上传、指标确认和评估处理进度</view>
      </view>
      <view class="task-total"
        ><text>{{ items.length }}</text
        ><text>全部任务</text></view
      >
    </view>
    <view class="filter-row">
      <view
        v-for="filter in filters"
        :key="filter.code"
        class="filter"
        :class="{ active: activeFilter === filter.code }"
        @click="activeFilter = filter.code"
      >
        {{ filter.label }}
      </view>
    </view>
    <PageState :loading="loading" :empty="filteredItems.length === 0">
      <view v-for="item in filteredItems" :key="item.id" class="card task-card" @click="open(item.id)">
        <view class="task-index">{{ item.reportDate.slice(8, 10) }}</view>
        <view class="task-content">
          <view class="task-head"
            ><view class="task-title">{{ item.reportName }}</view
            ><StatusTag :status="item.status"
          /></view>
          <view class="task-sub">客户 ID {{ item.patientId }} · {{ item.reportDate }}</view>
          <view class="task-foot"
            ><text>{{ item.indicators.length }} 项指标</text><text>处理任务 ›</text></view
          >
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getLabReports } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<LabReport[]>([]),
  loading = ref(true)
const filters = [
  { code: 'ALL' as const, label: '全部' },
  { code: 'PENDING' as const, label: '待处理' },
  { code: 'COMPLETED' as const, label: '已完成' },
]
const activeFilter = ref<(typeof filters)[number]['code']>('ALL')
const completedStatuses = new Set(['PUBLISHED'])
const filteredItems = computed(() => {
  if (activeFilter.value === 'ALL') return items.value
  return items.value.filter((item) =>
    activeFilter.value === 'COMPLETED'
      ? completedStatuses.has(item.status)
      : !completedStatuses.has(item.status),
  )
})
onShow(async () => {
  loading.value = true
  try {
    items.value = (await getLabReports()).records
  } finally {
    loading.value = false
  }
})
const open = (id: string) => uni.navigateTo({ url: `/pages-business/lab-report/detail?id=${id}` })
</script>

<style scoped>
.task-page {
  padding-top: 24rpx;
}
.task-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 34rpx;
  border-radius: 34rpx;
  background: linear-gradient(140deg, #173f37, #0b715a);
  color: #fff;
  box-shadow: 0 18rpx 40rpx rgba(15, 94, 75, 0.18);
}
.eyebrow-light {
  color: #91d9c4;
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}
.hero-title {
  margin-top: 12rpx;
  font-size: 40rpx;
  font-weight: 750;
}
.hero-copy {
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.66);
  font-size: 21rpx;
}
.task-total {
  display: flex;
  flex: 0 0 auto;
  margin-left: 18rpx;
  padding-left: 24rpx;
  border-left: 1rpx solid rgba(255, 255, 255, 0.18);
  flex-direction: column;
  text-align: center;
}
.task-total text:first-child {
  font-size: 38rpx;
  font-weight: 760;
}
.task-total text:last-child {
  color: rgba(255, 255, 255, 0.62);
  font-size: 19rpx;
}
.filter-row {
  display: flex;
  gap: 14rpx;
  margin: 28rpx 0 22rpx;
}
.filter {
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: #e9efed;
  color: #73827d;
  font-size: 22rpx;
}
.filter.active {
  background: #dff3ec;
  color: #0d745c;
  font-weight: 650;
}
.task-card {
  display: flex;
  align-items: center;
  padding: 26rpx;
}
.task-index {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 72rpx;
  height: 78rpx;
  border-radius: 23rpx;
  background: #e4f5ef;
  color: #0f755e;
  font-size: 30rpx;
  font-weight: 740;
}
.task-content {
  flex: 1;
  min-width: 0;
  margin-left: 22rpx;
}
.task-head,
.task-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.task-title {
  overflow: hidden;
  font-size: 28rpx;
  font-weight: 680;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-sub {
  margin-top: 8rpx;
  color: #7d8c87;
  font-size: 22rpx;
}
.task-foot {
  margin-top: 16rpx;
  color: #98a39f;
  font-size: 20rpx;
}
.task-foot text:last-child {
  color: #0f7a62;
}
</style>

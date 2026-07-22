<template>
  <view class="page"
    ><view class="title">医生审核任务</view
    ><view class="filter-row"
      ><view
        v-for="filter in filters"
        :key="filter.code"
        class="filter"
        :class="{ active: activeFilter === filter.code }"
        @click="activeFilter = filter.code"
        >{{ filter.label }}</view
      ></view
    ><PageState :loading="loading" :empty="filteredItems.length === 0"
      ><view v-for="item in filteredItems" :key="item.id" class="card" @click="open(item.id)"
        ><view class="row"
          ><view
            ><view class="section-title">{{ item.patient.name }}</view
            ><view class="muted">评估 #{{ item.assessment.id }}</view></view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle"
          >风险等级 {{ item.assessment.overallRiskLevel }} · 点击进入移动端审核</view
        ></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getReviewTasks } from '@/api/review'
import type { ReviewTask } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<ReviewTask[]>([]),
  loading = ref(true)
const filters = [
  { code: 'ALL' as const, label: '全部' },
  { code: 'PENDING' as const, label: '待处理' },
  { code: 'COMPLETED' as const, label: '已完成' },
]
const activeFilter = ref<(typeof filters)[number]['code']>('ALL')
const completedStatuses = new Set(['APPROVED', 'PUBLISHED', 'REJECTED'])
const filteredItems = computed(() => {
  if (activeFilter.value === 'ALL') return items.value
  return items.value.filter((item) =>
    activeFilter.value === 'COMPLETED'
      ? completedStatuses.has(item.status)
      : !completedStatuses.has(item.status),
  )
})
onShow(async () => {
  try {
    items.value = (await getReviewTasks()).records
  } finally {
    loading.value = false
  }
})
const open = (id: string) => uni.navigateTo({ url: `/pages-business/review/detail?id=${id}` })
</script>

<style scoped>
.filter-row {
  display: flex;
  gap: 14rpx;
  margin: 24rpx 0;
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
</style>

<template>
  <view class="page"
    ><view class="title">我的随访</view
    ><view class="filter-row"
      ><view
        v-for="filter in filters"
        :key="filter.code"
        class="filter"
        :class="{ active: activeFilter === filter.code }"
        @click="activeFilter = filter.code"
        >{{ filter.label }}</view
      ></view
    ><PageState :loading="loading" :error="error" :empty="filteredItems.length === 0"
      ><view v-for="item in filteredItems" :key="item.id" class="card"
        ><view class="row"
          ><view class="section-title">{{ item.title }}</view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle">{{ item.content }}</view
        ><view class="muted">计划完成：{{ item.dueDate }}</view
        ><button v-if="item.status !== 'COMPLETED'" size="mini" @click="feedback(item.id)">
          填写反馈
        </button></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyFollowups } from '@/api/followup'
import type { Followup } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<Followup[]>([]),
  loading = ref(true),
  error = ref('')
const filters = [
  { code: 'ALL' as const, label: '全部' },
  { code: 'PENDING' as const, label: '待处理' },
  { code: 'COMPLETED' as const, label: '已完成' },
]
const activeFilter = ref<(typeof filters)[number]['code']>('ALL')
const filteredItems = computed(() =>
  activeFilter.value === 'ALL'
    ? items.value
    : items.value.filter((item) => item.status === activeFilter.value),
)
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    items.value = await getMyFollowups()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '随访任务加载失败'
  } finally {
    loading.value = false
  }
})
const feedback = (id: string) =>
  uni.navigateTo({ url: `/pages-customer/followup/feedback?id=${id}` })
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

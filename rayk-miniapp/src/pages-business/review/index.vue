<template>
  <view class="page"
    ><view class="title">医生审核任务</view
    ><PageState :loading="loading" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card" @click="open(item.id)"
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
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getReviewTasks } from '@/api/review'
import type { ReviewTask } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<ReviewTask[]>([]),
  loading = ref(true)
onShow(async () => {
  try {
    items.value = (await getReviewTasks()).records
  } finally {
    loading.value = false
  }
})
const open = (id: string) => uni.navigateTo({ url: `/pages-business/review/detail?id=${id}` })
</script>

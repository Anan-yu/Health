<template>
  <view class="page"
    ><view class="title">报告任务</view
    ><PageState :loading="loading" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card" @click="open(item.id)"
        ><view class="row"
          ><view class="section-title">{{ item.reportName }}</view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle">客户 {{ item.patientId }} · {{ item.reportDate }}</view
        ><view class="muted">{{ item.indicators.length }} 项指标</view></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getLabReports } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<LabReport[]>([]),
  loading = ref(true)
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

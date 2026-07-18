<template>
  <view class="page"
    ><PageState :loading="loading" :empty="!report"
      ><view class="card"
        ><view class="row"
          ><view class="title">{{ report?.reportName }}</view
          ><StatusTag :status="report?.status || ''" /></view
        ><view class="subtitle">报告日期 {{ report?.reportDate }}</view></view
      ><view class="section-title">检验指标</view
      ><view v-for="item in report?.indicators" :key="item.id" class="card row"
        ><view
          ><view>{{ item.name }}</view
          ><view class="muted"
            >参考 {{ item.referenceLow ?? '-' }} ~ {{ item.referenceHigh ?? '-' }}
            {{ item.unit }}</view
          ></view
        ><view
          ><text class="metric">{{ item.value }}</text
          ><StatusTag :status="item.abnormalFlag || 'NORMAL'" /></view></view></PageState
  ></view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getLabReport } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const report = ref<LabReport>(),
  loading = ref(true)
onLoad(async (q) => {
  try {
    report.value = await getLabReport(String(q?.id || ''))
  } finally {
    loading.value = false
  }
})
</script>

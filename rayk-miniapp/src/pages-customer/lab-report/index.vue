<template>
  <view class="page"
    ><view class="row"
      ><view class="title">我的检验报告</view
      ><button size="mini" @click="upload">上传</button></view
    ><PageState :loading="loading" :empty="reports.length === 0"
      ><view v-for="item in reports" :key="item.id" class="card" @click="detail(item.id)"
        ><view class="row"
          ><view class="section-title">{{ item.reportName }}</view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle"
          >{{ item.reportDate }} · {{ item.indicators.length }} 项指标</view
        ></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyLabReports } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const reports = ref<LabReport[]>([]),
  loading = ref(true)
onShow(async () => {
  loading.value = true
  try {
    reports.value = await getMyLabReports()
  } finally {
    loading.value = false
  }
})
const upload = () => uni.navigateTo({ url: '/pages-customer/lab-report/upload' })
const detail = (id: string) => uni.navigateTo({ url: `/pages-customer/lab-report/detail?id=${id}` })
</script>

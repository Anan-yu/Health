<template>
  <view class="page"
    ><PageState :loading="loading" :empty="!item"
      ><view class="card"
        ><view class="row"
          ><view class="title">{{ item?.reportName }}</view
          ><StatusTag :status="item?.status || ''" /></view
        ><view class="subtitle">客户 {{ item?.patientId }} · {{ item?.reportDate }}</view></view
      ><view v-for="indicator in item?.indicators" :key="indicator.id" class="card row"
        ><view>{{ indicator.name }}</view
        ><view
          ><text class="metric">{{ indicator.value }}</text> {{ indicator.unit }}</view
        ></view
      ><button
        v-if="item?.status === 'UPLOADED' || item?.status === 'WAITING_CONFIRMATION'"
        class="primary"
        @click="correct"
      >
        进入OCR指标校对
      </button></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getLabReport } from '@/api/lab-report'
import type { LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const item = ref<LabReport>(),
  loading = ref(true),
  id = ref('')
onLoad(async (q) => {
  id.value = String(q?.id || '')
  try {
    item.value = await getLabReport(id.value)
  } finally {
    loading.value = false
  }
})
const correct = () => uni.navigateTo({ url: `/pages-business/indicator/index?id=${id.value}` })
</script>

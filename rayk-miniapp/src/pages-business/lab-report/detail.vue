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
      ><view v-if="awaitingCustomerConfirmation" class="card notice">
        客户正在核对 OCR 识别数据；确认后系统将自动提交 AI 初评。
      </view></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
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
const awaitingCustomerConfirmation = computed(() => item.value?.status === 'WAITING_CONFIRMATION')
</script>

<style scoped>
.notice {
  margin-top: 20rpx;
  color: #397267;
  line-height: 1.7;
}
</style>

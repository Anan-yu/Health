<template>
  <view class="page"
    ><view class="title">AI评估任务</view
    ><PageState :loading="loading" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card"
        ><view class="row"
          ><view class="section-title">客户 {{ item.patientId }}</view
          ><StatusTag :status="item.overallRiskLevel" /></view
        ><view class="subtitle">模型版本 {{ item.modelVersion }} · {{ item.status }}</view
        ><view v-for="model in item.results.results" :key="model.modelCode" class="row model"
          ><text>{{ model.modelName }}</text
          ><text class="metric">{{ model.score }}</text></view
        ></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getAssessments } from '@/api/assessment'
import type { Assessment } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<Assessment[]>([]),
  loading = ref(true)
onShow(async () => {
  try {
    items.value = (await getAssessments()).records
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.model {
  padding: 16rpx 0;
  border-top: 1px solid #edf1ef;
}
</style>

<template>
  <view class="page"
    ><view class="title">AI评估结果</view
    ><PageState :loading="loading" :error="error" :empty="items.length === 0"
      ><view v-for="assessment in items" :key="assessment.id" class="card"
        ><view class="row"
          ><view class="section-title">评估 #{{ assessment.id }}</view
          ><StatusTag :status="assessment.overallRiskLevel" /></view
        ><view
          v-for="model in assessment.results?.results || []"
          :key="model.modelCode"
          class="model"
          ><view class="row"
            ><text>{{ model.modelName }}</text
            ><text class="metric">{{ model.score }}</text></view
          ><view v-for="evidence in model.evidence" :key="evidence" class="subtitle"
            >• {{ evidence }}</view
          ><view class="muted"
            >缺失指标：{{ model.missingIndicators.join('、') || '无' }}</view
          ></view
        ></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyAssessments } from '@/api/assessment'
import type { Assessment } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<Assessment[]>([]),
  loading = ref(true),
  error = ref('')
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    items.value = await getMyAssessments()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '评估结果加载失败'
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.model {
  padding: 20rpx 0;
  border-top: 1px solid #edf1ef;
}
</style>

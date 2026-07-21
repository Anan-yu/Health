<template>
  <view class="page"
    ><view class="title">AI评估任务</view
    ><PageState :loading="loading" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card"
        ><view class="row"
          ><view class="section-title">客户 {{ item.patientId }}</view
          ><StatusTag :status="item.overallRiskLevel" /></view
        ><view class="subtitle">规则版本 {{ item.modelVersion }} · {{ item.status }}</view
        ><view v-if="item.results.interpretation" class="interpretation"
          ><text class="interpretation-source">{{
            item.results.interpretation.source === 'DEEPSEEK' ? 'AI综合解读' : '规则辅助解读'
          }}</text
          ><view>{{ displayInterpretation(item.results.interpretation.summary) }}</view></view
        ><view
          v-for="(model, index) in item.results.results"
          :key="model.modelCode"
          class="row model"
          ><text>评估维度 {{ String(index + 1).padStart(2, '0') }}</text
          ><view class="model-result"
            ><text class="metric">{{ model.score ?? '—' }}</text
            ><StatusTag :status="model.riskLevel" /></view></view></view></PageState
  ></view>
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
function displayInterpretation(value: string) {
  return value.split('模型').join('评估维度')
}
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
.interpretation {
  margin: 20rpx 0;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #eef8f5;
  color: #31564d;
  line-height: 1.6;
}
.interpretation-source {
  display: block;
  margin-bottom: 8rpx;
  color: #08745d;
  font-size: 22rpx;
  font-weight: 650;
}
.model-result {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
</style>

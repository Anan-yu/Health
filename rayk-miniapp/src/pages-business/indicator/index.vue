<template>
  <view class="page"
    ><view class="title">OCR指标校对</view
    ><view class="card"
      ><view class="subtitle"
        >当前使用 MockOcrService。请人工核对全部值，再确认并发起 AI 评估。</view
      ></view
    ><view v-for="item in indicators" :key="item.code" class="card"
      ><view class="row"
        ><input v-model="item.name" class="input half" /><StatusTag :status="flag(item)" /></view
      ><view class="row"
        ><input v-model.number="item.value" class="input half" type="digit" /><input
          v-model="item.unit"
          class="input half" /></view
      ><view class="muted"
        >参考范围 {{ item.referenceLow ?? '-' }} ~ {{ item.referenceHigh ?? '-' }}</view
      ></view
    ><button class="primary" :loading="loading" @click="save">确认全部指标</button
    ><button :disabled="!confirmed" @click="ai">创建AI评估任务</button></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { saveIndicators, confirmIndicators, submitAi } from '@/api/lab-report'
import type { Indicator } from '@/types/api'
import StatusTag from '@/components/StatusTag.vue'
const id = ref(''),
  loading = ref(false),
  confirmed = ref(false)
const indicators = ref<Indicator[]>([
  {
    code: 'fasting_glucose',
    name: '空腹血糖',
    value: 6.2,
    unit: 'mmol/L',
    referenceLow: 3.9,
    referenceHigh: 6.1,
  },
  { code: 'hba1c', name: '糖化血红蛋白', value: 5.8, unit: '%', referenceLow: 4, referenceHigh: 6 },
  { code: 'crp', name: 'C反应蛋白', value: 4.1, unit: 'mg/L', referenceHigh: 3 },
])
onLoad((q) => (id.value = String(q?.id || '')))
const flag = (i: Indicator) =>
  i.referenceHigh !== undefined && i.value > i.referenceHigh ? 'HIGH' : 'NORMAL'
async function save() {
  loading.value = true
  try {
    await saveIndicators(id.value, indicators.value)
    await confirmIndicators(id.value)
    confirmed.value = true
    uni.showToast({ title: '指标已确认' })
  } finally {
    loading.value = false
  }
}
async function ai() {
  loading.value = true
  try {
    await submitAi(id.value)
    uni.redirectTo({ url: '/pages-business/assessment/index' })
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.half {
  width: 43%;
}
</style>

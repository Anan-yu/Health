<template>
  <view class="page"
    ><view class="title">确认OCR指标</view
    ><view class="card"
      ><view class="subtitle">模拟OCR已完成。请逐项核对名称、数值、单位和参考范围。</view></view
    ><view v-for="(item, index) in indicators" :key="item.code" class="card"
      ><input v-model="item.name" class="input" /><view class="row"
        ><input v-model.number="item.value" class="input value" type="digit" /><input
          v-model="item.unit"
          class="input value" /></view
      ><view class="muted">指标 {{ index + 1 }} / {{ indicators.length }}</view></view
    ><button class="primary" :loading="loading" @click="confirm">保存并确认</button
    ><button :disabled="!confirmed" @click="evaluate">发起AI评估</button
    ><view v-if="error" class="error">{{ error }}</view></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { saveIndicators, confirmIndicators, submitAi } from '@/api/lab-report'
import type { Indicator } from '@/types/api'
const id = ref(''),
  loading = ref(false),
  confirmed = ref(false),
  error = ref('')
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
onLoad((q) => {
  id.value = String(q?.id || '')
})
async function confirm() {
  loading.value = true
  try {
    await saveIndicators(id.value, indicators.value)
    await confirmIndicators(id.value)
    confirmed.value = true
    uni.showToast({ title: '指标已确认' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '确认失败'
  } finally {
    loading.value = false
  }
}
async function evaluate() {
  loading.value = true
  try {
    await submitAi(id.value)
    uni.redirectTo({ url: '/pages-customer/assessment/index' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '评估失败'
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.value {
  width: 44%;
}
.error {
  color: #b42318;
  padding: 18rpx;
}
</style>

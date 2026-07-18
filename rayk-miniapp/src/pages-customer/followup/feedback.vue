<template>
  <view class="page"
    ><view class="title">随访反馈</view
    ><view class="card">
      <textarea
        v-model="feedback"
        class="textarea"
        placeholder="记录睡眠、饮食、运动和方案执行情况"
      /><button class="primary" :loading="loading" @click="submit">提交反馈</button></view
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { sendFeedback } from '@/api/followup'
const id = ref(''),
  feedback = ref('本周按计划记录了睡眠和饮食，整体执行良好。'),
  loading = ref(false)
onLoad((q) => (id.value = String(q?.id || '')))
async function submit() {
  loading.value = true
  try {
    await sendFeedback(id.value, feedback.value)
    uni.showToast({ title: '反馈已提交' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally {
    loading.value = false
  }
}
</script>

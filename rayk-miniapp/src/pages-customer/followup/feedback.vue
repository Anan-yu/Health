<template>
  <view class="page"
    ><view class="title">随访反馈</view
    ><view class="card">
      <textarea
        v-model="feedback"
        class="textarea"
        :maxlength="1000"
        placeholder="记录睡眠、饮食、运动和方案执行情况"
      /><view class="count">{{ feedback.length }}/1000</view
      ><view v-if="error" class="error">{{ error }}</view
      ><button class="primary" :loading="loading" :disabled="loading" @click="submit">
        提交反馈
      </button></view
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { sendFeedback } from '@/api/followup'
const id = ref(''),
  feedback = ref('本周按计划记录了睡眠和饮食，整体执行良好。'),
  loading = ref(false),
  error = ref('')
onLoad((q) => (id.value = String(q?.id || '')))
async function submit() {
  error.value = ''
  if (!id.value) {
    error.value = '缺少随访任务编号，请从随访列表重新进入'
    return
  }
  const content = feedback.value.trim()
  if (!content) {
    error.value = '请填写随访反馈'
    return
  }
  if (content.length > 1000) {
    error.value = '随访反馈不能超过 1000 字'
    return
  }
  loading.value = true
  try {
    await sendFeedback(id.value, content)
    uni.showToast({ title: '反馈已提交' })
    globalThis.setTimeout(() => uni.navigateBack(), 500)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '反馈提交失败'
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.count {
  margin: 10rpx 0 18rpx;
  color: #8a9893;
  text-align: right;
  font-size: 20rpx;
}
.error {
  margin-bottom: 18rpx;
  color: #b42318;
  font-size: 23rpx;
}
</style>

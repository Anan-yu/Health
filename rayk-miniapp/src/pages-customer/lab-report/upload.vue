<template>
  <view class="page"
    ><view class="title">上传检验报告</view
    ><view class="card"
      ><view class="section-title">第一步：报告信息</view
      ><input v-model="patientId" class="input" placeholder="客户档案ID（演示：30001）" /><input
        v-model="reportName"
        class="input"
        placeholder="报告名称"
      /><view class="section-title">第二步：模拟上传</view
      ><view class="upload-zone" @click="choose"
        ><text>{{ fileName || '选择 PDF/JPG/PNG，最大20MB' }}</text></view
      ><progress v-if="progress > 0" :percent="progress" active-color="#176b57" /><view
        class="steps"
        ><text :class="{ on: state !== 'IDLE' }">已选择</text
        ><text :class="{ on: state === 'OCR_PROCESSING' || state === 'WAITING_CONFIRMATION' }"
          >OCR处理中</text
        ><text :class="{ on: state === 'WAITING_CONFIRMATION' }">等待人工确认</text></view
      ><button class="primary" :loading="loading" @click="submit">创建模拟报告</button
      ><view v-if="error" class="error">{{ error }} <text @click="submit">重试</text></view></view
    >
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { createLabReport } from '@/api/lab-report'
const patientId = ref('30001'),
  reportName = ref('生化检验报告'),
  fileName = ref(''),
  progress = ref(0),
  state = ref('IDLE'),
  loading = ref(false),
  error = ref('')
function choose() {
  fileName.value = 'demo-lab-report.pdf'
  progress.value = 100
  state.value = 'SELECTED'
}
async function submit() {
  if (!patientId.value || !reportName.value) {
    error.value = '请填写完整信息'
    return
  }
  loading.value = true
  error.value = ''
  state.value = 'OCR_PROCESSING'
  try {
    const report = await createLabReport(patientId.value, reportName.value)
    state.value = 'WAITING_CONFIRMATION'
    uni.showToast({ title: '已创建' })
    setTimeout(
      () => uni.navigateTo({ url: `/pages-customer/lab-report/confirm?id=${report.id}` }),
      500,
    )
  } catch (e) {
    state.value = 'FAILED'
    error.value = e instanceof Error ? e.message : '上传失败'
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.upload-zone {
  padding: 70rpx 20rpx;
  text-align: center;
  border: 2rpx dashed #9bb7af;
  border-radius: 20rpx;
  color: #527067;
  margin: 20rpx 0;
}
.steps {
  display: flex;
  justify-content: space-between;
  padding: 24rpx 0;
  color: #a0aaa6;
  font-size: 22rpx;
}
.steps .on {
  color: #176b57;
}
.error {
  color: #b42318;
  padding: 20rpx;
}
</style>

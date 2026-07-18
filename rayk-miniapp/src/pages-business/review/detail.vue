<template>
  <view class="page"
    ><PageState :loading="loading" :empty="!task"
      ><view class="card"
        ><view class="row"
          ><view
            ><view class="title">{{ task?.patient.name }}</view
            ><view class="muted"
              >{{ task?.patient.gender }} · {{ task?.patient.birthDate }}</view
            ></view
          ><StatusTag :status="task?.status || ''" /></view></view
      ><view class="card"
        ><view class="section-title">报告与评估</view
        ><view class="subtitle"
          >模型版本 {{ task?.assessment.modelVersion }} · 风险
          {{ task?.assessment.overallRiskLevel }}</view
        ><view
          v-for="model in task?.assessment.results.results"
          :key="model.modelCode"
          class="model"
          ><view class="row"
            ><text>{{ model.modelName }}</text
            ><text class="metric">{{ model.score }}</text></view
          ><view v-for="e in model.evidence" :key="e" class="subtitle">证据：{{ e }}</view
          ><view class="muted">缺失：{{ model.missingIndicators.join('、') || '无' }}</view
          ><view class="muted">建议：{{ model.recommendations.join('、') }}</view></view
        ></view
      ><view class="card"
        ><view class="section-title">医生意见</view
        ><textarea v-model="opinion" class="textarea" placeholder="填写人工审核意见" /></view
      ><view class="actions"
        ><button class="danger" @click="reject">退回修改</button
        ><button class="primary" @click="approve">审核通过</button></view
      ><button v-if="task?.status === 'APPROVED'" class="primary" @click="publish">
        发布健康报告
      </button></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getReviewTask, approveReview, rejectReview, publishReview } from '@/api/review'
import type { ReviewTask } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const id = ref(''),
  task = ref<ReviewTask>(),
  loading = ref(true),
  opinion = ref('已核对演示指标，同意作为健康管理参考。')
onLoad(async (q) => {
  id.value = String(q?.id || '')
  try {
    task.value = await getReviewTask(id.value)
  } finally {
    loading.value = false
  }
})
async function approve() {
  task.value = await approveReview(id.value, opinion.value)
  uni.showToast({ title: '审核通过' })
}
async function reject() {
  task.value = await rejectReview(id.value, opinion.value)
  uni.showToast({ title: '已退回' })
}
function publish() {
  uni.showModal({
    title: '确认发布',
    content: '发布后客户将能查看该模拟健康报告，是否继续？',
    success: async (r) => {
      if (r.confirm) {
        await publishReview(id.value)
        uni.showToast({ title: '发布成功' })
        setTimeout(() => uni.navigateBack(), 600)
      }
    },
  })
}
</script>
<style scoped>
.model {
  padding: 20rpx 0;
  border-top: 1px solid #edf1ef;
}
.actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  margin-bottom: 20rpx;
}
</style>

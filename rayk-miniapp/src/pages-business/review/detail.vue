<template>
  <view class="page review-page">
    <PageState :loading="loading" :error="error" :empty="!task">
      <view class="profile-card">
        <view>
          <view class="eyebrow-light">CLINICAL REVIEW</view>
          <view class="profile-name">{{ task?.patient.name }}</view>
          <view class="profile-meta"
            >{{ task?.patient.gender }} · {{ task?.patient.birthDate }}</view
          >
        </view>
        <StatusTag :status="task?.status || ''" />
      </view>

      <view class="section-head">
        <view>
          <view class="eyebrow">ASSESSMENT</view>
          <view class="section-title">AI 评估复核</view>
        </view>
        <text class="risk-label">{{ task?.assessment.overallRiskLevel }}</text>
      </view>

      <view v-for="(model, index) in editableModels" :key="model.modelCode" class="card model-card">
        <view class="model-head">
          <view>
            <view class="model-name">{{ model.modelName }}</view>
            <view class="muted">{{ model.modelCode }} · AI 得分 {{ model.score }}</view>
          </view>
          <picker
            v-if="canEdit"
            :range="riskOptions"
            :value="riskOptions.indexOf(model.riskLevel)"
            @change="changeRisk(index, $event)"
          >
            <view class="risk-picker">{{ riskText(model.riskLevel) }}⌄</view>
          </picker>
          <StatusTag v-else :status="model.riskLevel" />
        </view>

        <view class="field-label">证据依据</view>
        <textarea
          v-if="canEdit"
          v-model="model.evidenceText"
          class="textarea evidence-input"
          placeholder="每行填写一条证据"
        />
        <view v-else class="text-list">
          <view v-for="item in lines(model.evidenceText)" :key="item">• {{ item }}</view>
        </view>

        <view class="field-label">健康管理建议</view>
        <textarea
          v-if="canEdit"
          v-model="model.recommendationsText"
          class="textarea evidence-input"
          placeholder="每行填写一条建议"
        />
        <view v-else class="text-list">
          <view v-for="item in lines(model.recommendationsText)" :key="item">• {{ item }}</view>
        </view>
        <view v-if="model.missingIndicators.length" class="missing">
          缺失指标：{{ model.missingIndicators.join('、') }}
        </view>
      </view>

      <view class="card opinion-card">
        <view class="section-title">医生综合意见</view>
        <textarea
          v-model="opinion"
          class="textarea opinion-input"
          :disabled="!canEdit"
          placeholder="填写人工审核意见；退回时请说明需要校正的内容"
        />
      </view>

      <view v-if="canEdit" class="actions">
        <button class="secondary" :loading="saving" @click="() => saveEdits()">保存编辑</button>
        <button class="danger" :loading="saving" @click="reject">退回校正</button>
        <button class="primary" :loading="saving" @click="approve">审核通过</button>
      </view>
      <button
        v-if="task?.status === 'APPROVED'"
        class="primary publish-button"
        :loading="saving"
        @click="publish"
      >
        发布 PDF 健康报告
      </button>
      <view v-if="task?.reviewOpinion" class="review-history">
        <view class="field-label">审核记录</view>
        <view class="subtitle">{{ task.reviewOpinion }}</view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { approveReview, editReview, getReviewTask, publishReview, rejectReview } from '@/api/review'
import type { ReviewTask } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

interface EditableModel {
  modelCode: string
  modelName: string
  score: number
  riskLevel: string
  evidenceText: string
  recommendationsText: string
  missingIndicators: string[]
}

const id = ref('')
const task = ref<ReviewTask>()
const editableModels = ref<EditableModel[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const opinion = ref('')
const riskOptions = ['LOW', 'ATTENTION', 'HIGH']
const canEdit = computed(() => task.value?.status === 'WAITING_REVIEW')

const listValue = (value: unknown) =>
  Array.isArray(value) ? value.map(String) : typeof value === 'string' ? [value] : []

function syncTask(value: ReviewTask) {
  task.value = value
  opinion.value = value.reviewOpinion || ''
  editableModels.value = (value.assessment.results.results || []).map((model) => ({
    modelCode: model.modelCode,
    modelName: model.modelName,
    score: model.score,
    riskLevel: model.riskLevel,
    evidenceText: listValue(model.evidence).join('\n'),
    recommendationsText: listValue(model.recommendations).join('\n'),
    missingIndicators: model.missingIndicators || [],
  }))
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    syncTask(await getReviewTask(id.value))
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '审核任务加载失败'
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  id.value = String(query?.id || '')
  if (!id.value) {
    error.value = '审核任务参数缺失'
    loading.value = false
    return
  }
  void load()
})

function lines(value: string) {
  return value
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
}

function riskText(value: string) {
  return value === 'LOW' ? '低风险' : value === 'HIGH' ? '高风险' : '需关注'
}

function changeRisk(index: number, event: { detail: { value: string | number } }) {
  editableModels.value[index].riskLevel = riskOptions[Number(event.detail.value)]
}

async function saveEdits(showSuccess = true) {
  if (!canEdit.value || saving.value) return false
  saving.value = true
  try {
    const currentOpinion = opinion.value
    const updated = await editReview(
      id.value,
      editableModels.value.map((model) => ({
        modelCode: model.modelCode,
        riskLevel: model.riskLevel,
        evidence: lines(model.evidenceText),
        recommendations: lines(model.recommendationsText),
      })),
      opinion.value.trim(),
    )
    syncTask(updated)
    opinion.value = currentOpinion
    if (showSuccess) uni.showToast({ title: '审核修改已保存' })
    return true
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '保存失败', icon: 'none' })
    return false
  } finally {
    saving.value = false
  }
}

async function approve() {
  if (!opinion.value.trim()) {
    uni.showToast({ title: '请填写审核意见', icon: 'none' })
    return
  }
  if (!(await saveEdits(false))) return
  saving.value = true
  try {
    syncTask(await approveReview(id.value, opinion.value.trim()))
    uni.showToast({ title: '审核通过' })
  } finally {
    saving.value = false
  }
}

async function reject() {
  if (!opinion.value.trim()) {
    uni.showToast({ title: '请说明退回原因', icon: 'none' })
    return
  }
  saving.value = true
  try {
    syncTask(await rejectReview(id.value, opinion.value.trim()))
    uni.showToast({ title: '已退回健康管理师校正', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function publish() {
  uni.showModal({
    title: '确认发布',
    content: '发布后客户将能查看并下载正式 PDF 健康报告，是否继续？',
    success: async (result) => {
      if (!result.confirm) return
      saving.value = true
      try {
        await publishReview(id.value)
        uni.showToast({ title: '发布成功' })
        setTimeout(() => uni.navigateBack(), 600)
      } finally {
        saving.value = false
      }
    },
  })
}
</script>

<style scoped>
.review-page {
  padding-top: 24rpx;
}
.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 34rpx;
  border-radius: 34rpx;
  background: linear-gradient(140deg, #173f37, #0b715a);
  color: #fff;
  box-shadow: 0 18rpx 40rpx rgba(15, 94, 75, 0.18);
}
.eyebrow-light {
  color: #91d9c4;
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}
.profile-name {
  margin-top: 10rpx;
  font-size: 38rpx;
  font-weight: 750;
}
.profile-meta {
  margin-top: 6rpx;
  color: rgba(255, 255, 255, 0.68);
  font-size: 21rpx;
}
.risk-label {
  color: #b97816;
  font-size: 22rpx;
  font-weight: 700;
}
.model-card {
  margin-top: 20rpx;
}
.model-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.model-name {
  font-size: 28rpx;
  font-weight: 680;
}
.risk-picker {
  padding: 12rpx 18rpx;
  border-radius: 16rpx;
  background: #fff2d8;
  color: #a76b13;
  font-size: 22rpx;
  font-weight: 650;
}
.field-label {
  margin: 24rpx 0 10rpx;
  color: #61746d;
  font-size: 22rpx;
  font-weight: 650;
}
.evidence-input {
  min-height: 128rpx;
}
.opinion-input {
  min-height: 170rpx;
}
.text-list {
  color: #52665f;
  font-size: 23rpx;
  line-height: 1.75;
}
.missing {
  margin-top: 18rpx;
  padding: 15rpx 18rpx;
  border-radius: 15rpx;
  background: #f6f8f7;
  color: #88958f;
  font-size: 20rpx;
}
.opinion-card {
  margin-top: 24rpx;
}
.actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin: 24rpx 0;
}
.actions button {
  padding: 0 8rpx;
  font-size: 23rpx;
}
.publish-button {
  margin-top: 24rpx;
}
.review-history {
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 22rpx;
  background: #eef7f4;
  white-space: pre-wrap;
}
</style>

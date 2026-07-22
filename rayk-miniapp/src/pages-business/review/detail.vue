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

      <view v-if="task?.assessment.results.interpretation" class="card interpretation-card">
        <view class="row">
          <view class="section-title">AI 综合解读</view>
          <text class="interpretation-source">
            {{
              task.assessment.results.interpretation.source === 'DEEPSEEK'
                ? 'AI 辅助生成'
                : '规则辅助生成'
            }}
          </text>
        </view>
        <view class="interpretation-summary">
          {{ displayInterpretation(task.assessment.results.interpretation.summary) }}
        </view>
        <view class="missing">
          不确定性：{{ displayInterpretation(task.assessment.results.interpretation.uncertainty) }}
        </view>
      </view>

      <view v-if="unassessedModelCount" class="coverage-note">
        本次报告已覆盖 {{ evaluatedModels.length }} 个可评估维度；另有 {{ unassessedModelCount }}
        个专项维度未覆盖，需专门检验项目才可评估。
      </view>
      <view v-for="(model, index) in evaluatedModels" :key="model.modelCode" class="card model-card">
        <view class="model-head">
          <view>
            <view class="model-name">评估维度 {{ String(index + 1).padStart(2, '0') }}</view>
            <view class="muted"
              >AI 得分 {{ model.score ?? '暂无评分' }} · 数据完整度 {{ model.dataCompleteness
              }}%</view
            >
          </view>
          <picker
            v-if="canEdit"
            :range="riskOptions"
            :value="riskOptions.indexOf(model.riskLevel)"
            @change="changeRisk(model.modelCode, $event)"
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
          当前报告尚缺 {{ model.missingIndicators.length }} 项相关指标；可结合专项检查进一步完善。
        </view>
      </view>

      <view class="card opinion-card">
        <view class="section-title">医生综合意见</view>
        <textarea
          v-model="opinion"
          class="textarea opinion-input"
          :disabled="!canEdit"
          placeholder="填写医生审核意见；退回时请说明需要由客户重新确认的内容"
        />
      </view>

      <view v-if="canEdit" class="actions">
        <button class="secondary" :loading="saving" @click="() => saveEdits()">保存编辑</button>
        <button class="danger" :loading="saving" @click="reject">退回客户确认</button>
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
  status?: 'EVALUATED' | 'INSUFFICIENT_DATA'
  score: number | null
  riskLevel: string
  dataCompleteness: number
  evidenceText: string
  recommendationsText: string
  missingIndicators: string[]
}

function displayInterpretation(value: string) {
  return value.split('模型').join('评估维度')
}

const id = ref('')
const task = ref<ReviewTask>()
const editableModels = ref<EditableModel[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const opinion = ref('')
const riskOptions = ['INSUFFICIENT_DATA', 'LOW', 'ATTENTION', 'HIGH']
const canEdit = computed(() => task.value?.status === 'WAITING_REVIEW')
const evaluatedModels = computed(() =>
  editableModels.value.filter((model) => model.status !== 'INSUFFICIENT_DATA'),
)
const unassessedModelCount = computed(
  () => editableModels.value.length - evaluatedModels.value.length,
)

const listValue = (value: unknown) =>
  Array.isArray(value) ? value.map(String) : typeof value === 'string' ? [value] : []

function syncTask(value: ReviewTask) {
  task.value = value
  opinion.value = value.reviewOpinion || ''
  editableModels.value = (value.assessment.results.results || []).map((model) => ({
    modelCode: model.modelCode,
    status: model.status,
    score: model.score,
    riskLevel: model.riskLevel,
    dataCompleteness: model.dataCompleteness ?? 0,
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
  if (value === 'INSUFFICIENT_DATA') return '数据不足'
  return value === 'LOW' ? '低风险' : value === 'HIGH' ? '高风险' : '需关注'
}

function changeRisk(modelCode: string, event: { detail: { value: string | number } }) {
  const target = editableModels.value.find((model) => model.modelCode === modelCode)
  if (target) target.riskLevel = riskOptions[Number(event.detail.value)]
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
    uni.showToast({ title: '已退回客户重新确认', icon: 'none' })
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
.interpretation-card {
  margin-top: 20rpx;
  border: 1px solid #d8eee7;
  background: linear-gradient(145deg, #edf8f5, #ffffff 72%);
}
.interpretation-source {
  color: #08745d;
  font-size: 21rpx;
  font-weight: 650;
}
.interpretation-summary {
  margin: 18rpx 0 12rpx;
  color: #254c42;
  line-height: 1.75;
}
.coverage-note {
  margin: 22rpx 4rpx 0;
  padding: 20rpx 24rpx;
  border-radius: 18rpx;
  background: #f2f8f6;
  color: #547069;
  font-size: 23rpx;
  line-height: 1.65;
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

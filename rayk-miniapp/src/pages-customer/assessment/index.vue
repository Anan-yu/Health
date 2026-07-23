<template>
  <view class="page">
    <view class="title">AI评估结果</view>
    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view v-for="assessment in items" :key="assessment.id" class="assessment-block">
        <view class="card assessment-head">
          <view>
            <view class="section-title">评估 #{{ assessment.id }}</view>
            <view class="subtitle">规则版本 {{ assessment.modelVersion }}</view>
          </view>
          <StatusTag :status="assessment.overallRiskLevel" />
        </view>

        <view v-if="assessment.results?.interpretation" class="card interpretation-card">
          <view class="row">
            <view class="section-title">综合解读</view>
            <text class="source-tag">
              {{ interpretationSource(assessment.results.interpretation.source) }}
            </text>
          </view>
          <view class="interpretation-summary">
            {{ displayInterpretation(assessment.results.interpretation.summary) }}
          </view>
          <view
            v-for="finding in assessment.results.interpretation.crossModelFindings"
            :key="finding.title"
            class="finding"
          >
            <view class="finding-title">{{ displayInterpretation(finding.title) }}</view>
            <view class="subtitle">{{ displayInterpretation(finding.explanation) }}</view>
          </view>
          <view class="uncertainty"
            >不确定性：{{ displayInterpretation(assessment.results.interpretation.uncertainty) }}</view
          >
        </view>

        <view class="card">
          <view class="section-title">健康维度评估</view>
          <view v-if="unassessedModelCount(assessment)" class="coverage-note">
            本次报告已覆盖 {{ evaluatedModels(assessment).length }} 个可评估维度；其余
            {{ unassessedModelCount(assessment) }} 个专项维度需要相应检验项目覆盖后才能评估。
          </view>
          <view
            v-for="(model, index) in evaluatedModels(assessment)"
            :key="model.modelCode"
            class="model"
          >
            <view class="row model-head">
              <view>
                <view class="model-name">评估维度 {{ String(index + 1).padStart(2, '0') }}</view>
                <view class="muted">
                  数据完整度 {{ model.dataCompleteness ?? 0 }}% · 可信度
                  {{ confidenceText(model.confidence) }}
                </view>
              </view>
              <view class="score-wrap">
                <text class="metric">{{ model.score ?? '—' }}</text>
                <StatusTag :status="model.riskLevel" />
              </view>
            </view>
            <view v-for="evidence in model.evidence" :key="evidence" class="subtitle">
              • {{ evidence }}
            </view>
            <view v-if="model.missingIndicators.length" class="missing">
              当前报告尚缺 {{ model.missingIndicators.length }} 项相关指标；可结合专项检查进一步完善。
            </view>
          </view>
        </view>
        <view class="disclaimer">{{ assessment.disclaimer }}</view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyAssessments } from '@/api/assessment'
import type { Assessment } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const items = ref<Assessment[]>([])
const loading = ref(true)
const error = ref('')

function confidenceText(value?: string) {
  return value === 'HIGH' ? '高' : value === 'LOW' ? '低' : '中'
}

function interpretationSource(value: string) {
  return value === 'DEEPSEEK' ? 'AI 辅助解读' : '规则辅助解读'
}

function displayInterpretation(value: string) {
  return value.split('模型').join('评估维度')
}

function evaluatedModels(assessment: Assessment) {
  return (assessment.results?.results || []).filter((model) => model.status !== 'INSUFFICIENT_DATA')
}

function unassessedModelCount(assessment: Assessment) {
  return (assessment.results?.results || []).length - evaluatedModels(assessment).length
}

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
.assessment-block {
  margin-bottom: 28rpx;
}
.assessment-head,
.model-head,
.score-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.interpretation-card {
  background: linear-gradient(145deg, #ecf8f4, #ffffff 70%);
  border: 1px solid #d9eee7;
}
.source-tag {
  padding: 7rpx 14rpx;
  border-radius: 999rpx;
  color: #08745d;
  background: #dff4ec;
  font-size: 21rpx;
}
.interpretation-summary {
  margin-top: 20rpx;
  color: #203d36;
  line-height: 1.75;
}
.finding {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.85);
}
.finding-title,
.model-name {
  font-weight: 650;
  color: #173f36;
}
.uncertainty,
.missing,
.disclaimer {
  margin-top: 18rpx;
  color: #7b8985;
  font-size: 23rpx;
  line-height: 1.6;
}
.model {
  padding: 24rpx 0;
  border-top: 1px solid #edf1ef;
}
.coverage-note {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #f2f8f6;
  color: #547069;
  font-size: 23rpx;
  line-height: 1.65;
}
.model:first-of-type {
  margin-top: 12rpx;
}
.metric {
  min-width: 42rpx;
  text-align: right;
}
.score-wrap {
  justify-content: flex-end;
}
.disclaimer {
  padding: 0 12rpx;
}
</style>

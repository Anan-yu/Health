<template>
  <view class="page elder-page">
    <view class="title">AI评估结果</view>
    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view v-for="assessment in items" :key="assessment.id" class="assessment-block">
        <view class="card assessment-head">
          <view>
            <view class="section-title">健康评估</view>
            <view class="subtitle">已结合检验报告与健康档案完成评估</view>
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
            {{ cleanHealthText(assessment.results.interpretation.summary) }}
          </view>
          <view
            v-if="assessment.results.interpretation.priorityConcerns?.length"
            class="detail-block"
          >
            <view class="detail-title">本次重点发现</view>
            <view
              v-for="item in assessment.results.interpretation.priorityConcerns"
              :key="item"
              class="detail-item"
              >{{ cleanHealthText(item) }}</view
            >
          </view>
          <view
            v-if="assessment.results.interpretation.crossModelFindings?.length"
            class="detail-block"
          >
            <view class="detail-title">为什么需要关注</view>
            <view
              v-for="finding in assessment.results.interpretation.crossModelFindings"
              :key="finding.title"
              class="finding"
            >
              <view class="finding-title">{{ cleanHealthText(finding.title) }}</view>
              <view class="subtitle">{{ cleanHealthText(finding.explanation) }}</view>
            </view>
          </view>
          <view
            v-if="assessment.results.interpretation.recommendations?.length"
            class="detail-block"
          >
            <view class="detail-title">下一步建议</view>
            <view
              v-for="item in assessment.results.interpretation.recommendations"
              :key="item"
              class="detail-item"
              >{{ cleanHealthText(item) }}</view
            >
          </view>
          <view
            v-if="assessment.results.interpretation.missingDataAdvice?.length"
            class="detail-block"
          >
            <view class="detail-title">需要补充的数据</view>
            <view
              v-for="item in assessment.results.interpretation.missingDataAdvice"
              :key="item"
              class="detail-item"
              >{{ cleanHealthText(item) }}</view
            >
          </view>
          <view
            v-if="assessment.results.interpretation.diagnosticReferences?.length"
            class="detail-block"
          >
            <view class="detail-title">需要进一步确认的健康方向</view>
            <view
              v-for="item in assessment.results.interpretation.diagnosticReferences"
              :key="item.conditionName"
              class="detail-item"
              >{{ cleanHealthText(item.conditionName) }}：{{
                cleanHealthText(item.rationale)
              }}</view
            >
          </view>
          <view class="detail-block">
            <view class="detail-title">当前不能说明什么</view>
            <view class="uncertainty">{{
              cleanHealthText(assessment.results.interpretation.uncertainty)
            }}</view>
          </view>
        </view>

        <HealthDimensionDashboard :models="assessment.results?.results || []" />
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
import { cleanHealthText } from '@/utils/health-text'
import HealthDimensionDashboard from '@/components/HealthDimensionDashboard.vue'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const items = ref<Assessment[]>([])
const loading = ref(true)
const error = ref('')

function interpretationSource(value: string) {
  return value === 'DEEPSEEK' ? 'AI 辅助解读' : '规则辅助解读'
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
.assessment-head {
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
.finding-title {
  font-weight: 650;
  color: #173f36;
}
.detail-block {
  margin-top: 22rpx;
}
.detail-title {
  margin-bottom: 10rpx;
  color: #173f36;
  font-size: 25rpx;
  font-weight: 700;
}
.detail-item {
  position: relative;
  padding: 9rpx 0 9rpx 24rpx;
  color: #526f67;
  font-size: 24rpx;
  line-height: 1.65;
}
.detail-item::before {
  position: absolute;
  left: 0;
  content: '•';
  color: #0b8064;
}
.uncertainty {
  padding: 17rpx;
  border-radius: 14rpx;
  background: #f4f7f6;
  color: #647872;
  font-size: 23rpx;
  line-height: 1.65;
}
.disclaimer {
  margin-top: 18rpx;
  color: #7b8985;
  font-size: 23rpx;
  line-height: 1.6;
}
.disclaimer {
  padding: 0 12rpx;
}
</style>

<template>
  <view class="page elder-page">
    <view class="title">健康总览</view>
    <PageState :loading="loading" :error="error" :empty="items.length === 0">
      <view v-for="assessment in items" :key="assessment.id" class="assessment-block">
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
    error.value = cause instanceof Error ? cause.message : '健康总览加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.assessment-block {
  margin-bottom: 28rpx;
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

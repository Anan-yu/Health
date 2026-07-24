<template>
  <view class="health-dashboard">
    <view class="dashboard-hero">
      <view class="dashboard-top">
        <view>
          <view class="dashboard-title">健康维度仪表盘</view>
        </view>
        <view class="coverage-pill">已评估 {{ evaluated.length }}/{{ models.length }}</view>
      </view>

      <view class="gauge-area">
        <view class="gauge">
          <view class="gauge-arc" />
          <view class="gauge-zone zone-left" />
          <view class="gauge-zone zone-right" />
          <view class="gauge-needle" :style="{ transform: `rotate(${gaugeAngle}deg)` }" />
          <view class="gauge-center" />
          <view class="gauge-score">{{ averageScore }}</view>
        </view>
        <view class="gauge-scale">
          <text>需关注</text><text>保持观察</text><text>状态良好</text>
        </view>
        <view class="overall-caption">{{ overallCaption }}</view>
      </view>

      <view class="dashboard-stats">
        <view class="dashboard-stat"
          ><text>{{ evaluated.length }}</text
          ><text>有效维度</text></view
        >
        <view class="dashboard-divider" />
        <view class="dashboard-stat attention-stat"
          ><text>{{ focusCount }}</text
          ><text>需关注</text></view
        >
        <view class="dashboard-divider" />
        <view class="dashboard-stat"
          ><text>{{ averageCompleteness }}%</text><text>数据覆盖</text></view
        >
      </view>
    </view>

    <view class="dashboard-hint"><view class="pulse-dot" />点击有效健康维度可查看本次评估依据</view>

    <view class="dimension-grid">
      <view
        v-for="model in evaluated"
        :key="model.modelCode"
        class="dimension-card"
        :class="[
          riskClass(model),
          { expanded: expandedCode === model.modelCode, unavailable: !isEvaluated(model) },
        ]"
        @tap="toggle(model)"
      >
        <view class="dimension-main">
          <view class="dimension-icon">{{ dimensionMeta(model.modelCode).icon }}</view>
          <view class="dimension-copy">
            <view class="dimension-name">{{ dimensionMeta(model.modelCode).label }}</view>
            <view class="dimension-description">{{
              dimensionMeta(model.modelCode).description
            }}</view>
          </view>
          <view v-if="isEvaluated(model)" class="dimension-score">{{ model.score ?? '—' }}</view>
          <view v-else class="pending-score">待数据</view>
        </view>

        <template v-if="isEvaluated(model)">
          <view class="dimension-meter">
            <view
              class="dimension-meter-fill"
              :style="{ width: `${normalizedScore(model.score)}%` }"
            />
          </view>
          <view class="dimension-meta">
            <text>{{ riskText(model.riskLevel) }}</text>
            <text>完整度 {{ model.dataCompleteness ?? 0 }}%</text>
            <text class="expand-mark"
              >{{ expandedCode === model.modelCode ? '收起' : '详情' }} ›</text
            >
          </view>
        </template>
        <view v-else class="pending-copy">缺少该维度所需指标，补充相关数据后即可评估</view>

        <view
          v-if="expandedCode === model.modelCode && isEvaluated(model)"
          class="dimension-details"
          @tap.stop
        >
          <view class="detail-head">
            <text>本次评估依据</text>
            <text>{{ confidenceText(model.confidence) }}可信度</text>
          </view>
          <view v-for="evidence in visibleEvidence(model)" :key="evidence" class="evidence-item">
            <view class="evidence-dot" /><text>{{ translateEvidence(evidence) }}</text>
          </view>
          <view v-if="!visibleEvidence(model).length" class="empty-evidence">
            当前指标未触发该健康维度的重点关注规则
          </view>
          <view v-if="model.missingIndicators?.length" class="missing-copy">
            尚缺 {{ model.missingIndicators.length }} 项相关指标，可在后续专项检查中补充。
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Assessment } from '@/types/api'
import { cleanHealthText } from '@/utils/health-text'

type DimensionResult = NonNullable<Assessment['results']['results']>[number]
const props = defineProps<{ models: DimensionResult[] }>()

const dimensionMap: Record<string, { label: string; icon: string; description: string }> = {
  GLUCOSE_METABOLISM: { label: '糖代谢健康', icon: '糖', description: '血糖调节与代谢状态' },
  LIPID_CARDIOVASCULAR: { label: '心血管与血脂', icon: '心', description: '血脂与心血管风险' },
  CHRONIC_INFLAMMATION: { label: '炎症相关健康', icon: '炎', description: '慢性炎症相关信号' },
  LIVER_METABOLIC: { label: '肝脏与代谢', icon: '肝', description: '肝功能与代谢负担' },
  KIDNEY_ELECTROLYTE: { label: '肾脏与电解质', icon: '肾', description: '肾功能与电解质平衡' },
  HEMATOLOGY_ANEMIA: {
    label: '血常规与贫血风险',
    icon: '血',
    description: '血细胞、铁储备与造血营养状态',
  },
  THYROID_HORMONE: { label: '甲状腺健康', icon: '甲', description: '甲状腺激素调节状态' },
  BODY_COMPOSITION: {
    label: '体重与身体成分',
    icon: '体',
    description: '身高、体重、BMI、腰围与运动状态',
  },
  HPA_ADRENAL: { label: '睡眠与恢复', icon: '眠', description: '压力、睡眠与恢复能力' },
  NUTRITION_MICRONUTRIENT: { label: '营养状态', icon: '养', description: '微量营养素水平' },
  GUT_BARRIER: { label: '消化与肠道', icon: '肠', description: '消化吸收与肠道状态' },
  MENTAL_EMOTIONAL: {
    label: '心理与情绪健康',
    icon: '心',
    description: '心情、压力与恐惧焦虑感受',
  },
}
const fallbackMeta = { label: '综合健康状态', icon: '健', description: '综合健康数据评估' }
const expandedCode = ref('')
const evaluated = computed(() => props.models.filter(isEvaluated))
const focusCount = computed(
  () =>
    evaluated.value.filter((model) => model.riskLevel === 'ATTENTION' || model.riskLevel === 'HIGH')
      .length,
)
const averageScore = computed(() => {
  const scores = evaluated.value
    .map((model) => model.score)
    .filter((score): score is number => typeof score === 'number')
  return scores.length
    ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
    : 0
})
const averageCompleteness = computed(() =>
  evaluated.value.length
    ? Math.round(
        evaluated.value.reduce((sum, model) => sum + (model.dataCompleteness ?? 0), 0) /
          evaluated.value.length,
      )
    : 0,
)
const gaugeAngle = computed(() => -180 + normalizedScore(averageScore.value) * 1.8)
const overallCaption = computed(() =>
  focusCount.value ? `${focusCount.value} 项健康方向需关注` : '当前健康状态整体平稳',
)

function isEvaluated(model: DimensionResult) {
  return model.status !== 'INSUFFICIENT_DATA' && typeof model.score === 'number'
}
function normalizedScore(score: number | null | undefined) {
  return Math.max(0, Math.min(100, Number(score ?? 0)))
}
function dimensionMeta(code: string) {
  return dimensionMap[code] || fallbackMeta
}
function riskText(value: string) {
  return value === 'HIGH' ? '重点关注' : value === 'ATTENTION' ? '建议改善' : '状态平稳'
}
function riskClass(model: DimensionResult) {
  if (!isEvaluated(model)) return 'risk-pending'
  return model.riskLevel === 'HIGH'
    ? 'risk-high'
    : model.riskLevel === 'ATTENTION'
      ? 'risk-attention'
      : 'risk-good'
}
function confidenceText(value?: string) {
  return value === 'HIGH' ? '高' : value === 'LOW' ? '低' : '中'
}
function visibleEvidence(model: DimensionResult) {
  return (model.evidence || []).filter((item) => !item.includes('未触发')).slice(0, 4)
}
function translateEvidence(value: string) {
  return cleanHealthText(value)
}
function toggle(model: DimensionResult) {
  if (isEvaluated(model))
    expandedCode.value = expandedCode.value === model.modelCode ? '' : model.modelCode
}
</script>

<style scoped>
.health-dashboard {
  margin-top: 22rpx;
}
.dashboard-hero {
  position: relative;
  overflow: hidden;
  padding: 30rpx 28rpx 24rpx;
  border-radius: 30rpx;
  color: #fff;
  background:
    radial-gradient(circle at 90% 16%, rgba(84, 222, 178, 0.34), transparent 32%),
    linear-gradient(145deg, #073f36, #08745d 55%, #0b8a6e);
  box-shadow: 0 22rpx 44rpx rgba(6, 91, 73, 0.22);
}
.dashboard-hero::after {
  position: absolute;
  right: -90rpx;
  bottom: -180rpx;
  width: 360rpx;
  height: 360rpx;
  border: 28rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  content: '';
}
.dashboard-top,
.dimension-main,
.dimension-meta,
.dashboard-hint,
.detail-head {
  display: flex;
  align-items: center;
}
.dashboard-top {
  position: relative;
  z-index: 1;
  justify-content: space-between;
  gap: 18rpx;
}
.dashboard-eyebrow {
  color: #79e1c2;
  font-size: 19rpx;
  font-weight: 750;
  letter-spacing: 3rpx;
}
.dashboard-title {
  margin-top: 6rpx;
  font-size: 34rpx;
  font-weight: 760;
}
.coverage-pill {
  flex: 0 0 auto;
  padding: 10rpx 18rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.22);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.12);
  font-size: 21rpx;
}
.gauge-area {
  position: relative;
  z-index: 1;
  width: 360rpx;
  margin: 24rpx auto 0;
}
.gauge {
  position: relative;
  width: 360rpx;
  height: 190rpx;
  overflow: hidden;
}
.gauge-arc,
.gauge-zone {
  position: absolute;
  box-sizing: border-box;
  left: 20rpx;
  top: 8rpx;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
}
.gauge-arc {
  border: 24rpx solid rgba(255, 255, 255, 0.14);
}
.gauge-zone {
  border: 24rpx solid transparent;
}
.zone-left {
  border-top-color: #ffc86a;
  border-left-color: #ff8f75;
  transform: rotate(45deg);
}
.zone-right {
  border-top-color: #56d9b1;
  border-right-color: #56d9b1;
  transform: rotate(-45deg);
}
.gauge-needle {
  position: absolute;
  z-index: 3;
  left: 180rpx;
  bottom: 8rpx;
  width: 125rpx;
  height: 5rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #fff, #93f2d5);
  box-shadow: 0 0 14rpx rgba(255, 255, 255, 0.55);
  transform-origin: 0 50%;
  transition: transform 0.6s ease;
}
.gauge-center {
  position: absolute;
  z-index: 4;
  left: 166rpx;
  bottom: -6rpx;
  width: 28rpx;
  height: 28rpx;
  border: 7rpx solid #b5f5df;
  border-radius: 50%;
  background: #075c4c;
}
.gauge-score {
  position: absolute;
  z-index: 2;
  left: 0;
  right: 0;
  bottom: 64rpx;
  text-align: center;
  font-size: 66rpx;
  font-weight: 800;
  line-height: 1;
}
.gauge-scale {
  display: flex;
  justify-content: space-between;
  margin-top: 4rpx;
  color: rgba(255, 255, 255, 0.62);
  font-size: 18rpx;
}
.overall-caption {
  margin-top: 12rpx;
  color: #c9f7e8;
  text-align: center;
  font-size: 21rpx;
}
.dashboard-stats {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-top: 20rpx;
  padding: 20rpx 0 4rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.15);
}
.dashboard-stat {
  display: flex;
  min-width: 120rpx;
  flex-direction: column;
  align-items: center;
  gap: 5rpx;
}
.dashboard-stat text:first-child {
  font-size: 34rpx;
  font-weight: 760;
}
.dashboard-stat text:last-child {
  color: rgba(255, 255, 255, 0.65);
  font-size: 20rpx;
}
.attention-stat text:first-child {
  color: #ffd17d;
}
.dashboard-divider {
  width: 1rpx;
  height: 44rpx;
  background: rgba(255, 255, 255, 0.14);
}
.dashboard-hint {
  gap: 12rpx;
  margin: 22rpx 8rpx 16rpx;
  color: #688078;
  font-size: 22rpx;
}
.pulse-dot {
  width: 13rpx;
  height: 13rpx;
  border-radius: 50%;
  background: #17a17e;
  box-shadow: 0 0 0 8rpx rgba(23, 161, 126, 0.1);
}
.dimension-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}
.dimension-card {
  position: relative;
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
  padding: 22rpx;
  border: 1rpx solid #e1ece8;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 10rpx 28rpx rgba(20, 73, 60, 0.07);
}
.dimension-card.expanded {
  grid-column: 1/-1;
  border-color: #9fd9c7;
  box-shadow: 0 16rpx 36rpx rgba(16, 120, 95, 0.12);
}
.dimension-card::before {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5rpx;
  background: #42b995;
  content: '';
}
.risk-attention::before {
  background: linear-gradient(90deg, #f3b33f, #ffcf75);
}
.risk-high::before {
  background: linear-gradient(90deg, #e95858, #ff8a70);
}
.risk-pending::before {
  background: #dbe5e2;
}
.dimension-main {
  min-width: 0;
  gap: 13rpx;
}
.dimension-icon {
  display: flex;
  flex: 0 0 52rpx;
  width: 52rpx;
  height: 52rpx;
  align-items: center;
  justify-content: center;
  border-radius: 17rpx;
  color: #08745d;
  background: #e4f6f0;
  font-size: 22rpx;
  font-weight: 750;
}
.risk-attention .dimension-icon {
  color: #a46600;
  background: #fff2d9;
}
.risk-high .dimension-icon {
  color: #bd3e35;
  background: #ffebe8;
}
.dimension-copy {
  min-width: 0;
  flex: 1;
}
.dimension-name {
  color: #173f36;
  font-size: 22rpx;
  font-weight: 720;
  line-height: 1.25;
}
.dimension-description {
  display: none;
  margin-top: 5rpx;
  color: #81918c;
  font-size: 20rpx;
}
.expanded .dimension-description {
  display: block;
}
.dimension-score {
  color: #08775f;
  font-size: 42rpx;
  font-weight: 800;
}
.risk-attention .dimension-score {
  color: #bd7600;
}
.risk-high .dimension-score {
  color: #cc4b40;
}
.pending-score {
  flex: 0 0 auto;
  padding: 7rpx 11rpx;
  border-radius: 999rpx;
  color: #899892;
  background: #eef3f1;
  font-size: 18rpx;
}
.dimension-meter {
  height: 7rpx;
  overflow: hidden;
  margin-top: 18rpx;
  border-radius: 999rpx;
  background: #edf3f1;
}
.dimension-meter-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1b9878, #55c9a6);
}
.risk-attention .dimension-meter-fill {
  background: linear-gradient(90deg, #e49b20, #ffc85f);
}
.risk-high .dimension-meter-fill {
  background: linear-gradient(90deg, #da4c45, #ff826d);
}
.dimension-meta {
  justify-content: space-between;
  gap: 7rpx;
  margin-top: 13rpx;
  color: #84928e;
  font-size: 17rpx;
}
.expand-mark {
  flex: 0 0 auto;
  color: #0f7a62;
  font-weight: 650;
}
.pending-copy {
  margin-top: 16rpx;
  color: #8a9994;
  font-size: 19rpx;
  line-height: 1.5;
}
.unavailable {
  box-shadow: none;
}
.dimension-details {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #edf1ef;
}
.detail-head {
  justify-content: space-between;
  margin-bottom: 13rpx;
  color: #173f36;
  font-size: 22rpx;
  font-weight: 700;
}
.detail-head text:last-child {
  color: #7d8e88;
  font-size: 19rpx;
  font-weight: 500;
}
.evidence-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-top: 10rpx;
  color: #587068;
  font-size: 22rpx;
  line-height: 1.55;
}
.evidence-dot {
  flex: 0 0 9rpx;
  width: 9rpx;
  height: 9rpx;
  margin-top: 12rpx;
  border-radius: 50%;
  background: #18a07d;
}
.empty-evidence,
.missing-copy {
  color: #7e8d88;
  font-size: 21rpx;
  line-height: 1.55;
}
.missing-copy {
  margin-top: 14rpx;
  padding: 14rpx 16rpx;
  border-radius: 14rpx;
  background: #f4f7f6;
}
</style>

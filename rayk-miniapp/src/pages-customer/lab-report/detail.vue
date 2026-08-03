<template>
  <view class="page report-detail-page elder-page">
    <PageState :loading="loading" :error="error" :empty="!report">
      <view class="report-hero">
        <view class="report-symbol">报</view>
        <view class="report-head-content">
          <view class="hero-name">{{ report?.reportName }}</view>
          <view class="hero-date">报告日期 · {{ report?.reportDate }}</view>
        </view>
        <StatusTag :status="report?.status || ''" />
      </view>
      <view class="indicator-summary">
        <view
          ><text>{{ resultItemCount }}</text
          ><text>体检项目</text></view
        >
        <view
          ><text>{{ resultGroups.length }}</text
          ><text>检查类目</text></view
        >
        <view
          ><text>{{ summaryCount }}</text
          ><text>检查小结</text></view
        >
      </view>
      <view v-if="isProcessing" class="processing-note">
        <view class="processing-pulse"><view /></view>
        <view>
          <view class="processing-title">正在为你整理这份健康数据</view>
          <view class="processing-copy"
            >识别与评估需要一点时间，你可以先离开本页。完成后可在“我的报告”查看结果。</view
          >
        </view>
      </view>
      <view v-if="isAssessmentFailed" class="assessment-failed-note">
        <view class="assessment-failed-title">体检内容已识别，AI 评估尚未完成</view>
        <view class="assessment-failed-copy">
          已识别的分类项目和检查小结不会丢失，也不需要重新识别原报告。可直接重新生成评估结果和健康报告。
        </view>
        <button
          class="assessment-retry-button"
          :loading="reassessing"
          :disabled="reassessing"
          @click="retryAssessment"
        >
          {{ reassessing ? '正在生成…' : '重新生成评估与健康报告' }}
        </button>
      </view>
      <template v-if="resultGroups.length">
        <view class="section-head result-heading">
          <view>
            <view class="eyebrow">RESULTS</view>
            <view class="section-title">分类体检结果</view>
          </view>
        </view>
        <view v-for="group in resultGroups" :key="group.key" class="card result-card">
          <view class="result-section-head">
            <view class="finding-section">{{ group.section }}</view>
            <view class="result-count"
              >{{ group.indicators.length + group.observations.length }} 项</view
            >
          </view>
          <view
            v-for="(item, index) in group.indicators"
            :key="item.id || `${item.code}-${index}`"
            class="category-indicator-row"
          >
            <view class="category-indicator-copy">
              <view class="indicator-name">{{ item.name }}</view>
              <view class="indicator-ref">参考范围：{{ referenceText(item) }}</view>
            </view>
            <view class="category-indicator-result">
              <view class="category-indicator-value">
                <text>{{ item.value }}</text
                ><text v-if="item.unit">{{ item.unit }}</text>
              </view>
              <StatusTag :status="item.abnormalFlag || 'NORMAL'" />
            </view>
          </view>
          <view
            v-for="(finding, index) in group.observations"
            :key="`${finding.item}-${index}`"
            class="finding-row"
          >
            <view class="finding-name">{{ finding.item }}</view>
            <view class="finding-result">{{ finding.result }}</view>
          </view>
          <view v-if="group.summaries.length" class="finding-summary">
            <view class="finding-summary-title">检查小结</view>
            <view
              v-for="(summary, index) in group.summaries"
              :key="`${summary.item}-${index}`"
              class="finding-summary-item"
            >
              {{ summary.result }}
            </view>
          </view>
        </view>
      </template>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onHide, onLoad, onShow } from '@dcloudio/uni-app'
import { getHealthReports } from '@/api/health-report'
import { getLabReport, getOcrTask, submitAi } from '@/api/lab-report'
import type { Indicator, LabReport, OcrFinding } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const report = ref<LabReport>(),
  loading = ref(true),
  reassessing = ref(false),
  error = ref('')
const summaryLabels = new Set([
  '小结',
  '检查小结',
  '诊断意见',
  '诊断结论',
  '检查结论',
  '影像结论',
  '印象',
  '提示',
  '结论',
])
const normalizeFindingLabel = (value: string) =>
  value
    .trim()
    .replace(/[：:]$/, '')
    .replace(/\s/g, '')
const administrativeFindingMarkers = [
  '姓名',
  '性别',
  '年龄',
  '出生日期',
  '体检日期',
  '检查日期',
  '报告日期',
  '打印日期',
  '咨询电话',
  '联系电话',
  '手机号',
  '身份证',
  '报告编号',
  '报告号',
  '体检编号',
  '住院号',
  '门诊号',
  '床位号',
  '床号',
  '病区',
  '科室',
  '条码号',
  '检查号',
  '检验号',
  '样本号',
  '申请单号',
  '仪器型号',
  '设备编号',
]
const isAdministrativeFinding = (finding: OcrFinding) => {
  const compact = normalizeFindingLabel(finding.item || '')
  return !compact || administrativeFindingMarkers.some((marker) => compact.includes(marker))
}
const isAdministrativeIndicator = (item: Indicator) => {
  const compact = normalizeFindingLabel(`${item.name || ''}${item.code || ''}`)
  return !compact || administrativeFindingMarkers.some((marker) => compact.includes(marker))
}
const visibleIndicators = computed(() =>
  (report.value?.indicators || []).filter((item) => !isAdministrativeIndicator(item)),
)
const visibleFindings = computed(() =>
  (report.value?.findings || []).filter((finding) => !isAdministrativeFinding(finding)),
)
const resultGroups = computed(() => {
  const groups: Array<{
    key: string
    section: string
    indicators: Indicator[]
    observations: Array<{ item: string; result: string }>
    summaries: Array<{ item: string; result: string }>
  }> = []
  for (const finding of visibleFindings.value) {
    // 类目名称、类目分段和出现顺序均以原体检报告为准，不合并后续同名类目。
    const section = finding.section?.trim() || report.value?.reportName?.trim() || '体检结果'
    const item = finding.item?.trim()
    const result = finding.result?.trim()
    if (!item || !result) continue
    let values = groups[groups.length - 1]
    if (!values || values.section !== section) {
      values = {
        key: `${groups.length}-${section}`,
        section,
        indicators: [],
        observations: [],
        summaries: [],
      }
      groups.push(values)
    }
    const target = summaryLabels.has(normalizeFindingLabel(item))
      ? values.summaries
      : values.observations
    target.push({ item, result })
  }
  // 新版体检报告已把数值、单位和参考范围保存在原类目内容中。仅在旧报告没有
  // 任何原始分类结果时展示独立指标，避免重复展示或把指标挪到报告末尾。
  for (const item of visibleFindings.value.length ? [] : visibleIndicators.value) {
    // 旧式数值指标没有保存原始类目，只能归入原报告名称，避免猜测后放错类目。
    const section = report.value?.reportName?.trim() || '检验指标'
    let values = groups[groups.length - 1]
    if (!values || values.section !== section) {
      values = {
        key: `${groups.length}-${section}`,
        section,
        indicators: [],
        observations: [],
        summaries: [],
      }
      groups.push(values)
    }
    values.indicators.push(item)
  }
  return groups
})
const resultItemCount = computed(() =>
  resultGroups.value.reduce(
    (total, group) => total + group.indicators.length + group.observations.length,
    0,
  ),
)
const summaryCount = computed(() =>
  resultGroups.value.reduce((total, group) => total + group.summaries.length, 0),
)
const referenceText = (item: Indicator) => {
  const low = item.referenceLow
  const high = item.referenceHigh
  const range =
    low != null && high != null
      ? `${low}～${high}`
      : low != null
        ? `≥ ${low}`
        : high != null
          ? `≤ ${high}`
          : '以原报告为准'
  return item.unit && range !== '以原报告为准' ? `${range} ${item.unit}` : range
}
const isProcessing = computed(() =>
  ['UPLOADED', 'OCR_PENDING', 'OCR_PROCESSING', 'CONFIRMED', 'AI_PROCESSING'].includes(
    report.value?.status || '',
  ),
)
const isAssessmentFailed = computed(() =>
  ['AI_FAILED', 'FAILED'].includes(report.value?.status || '') && resultItemCount.value > 0,
)
const reportId = ref('')
const autoReturn = ref(false)
let pollTimer: ReturnType<typeof globalThis.setTimeout> | undefined
onLoad((q) => {
  reportId.value = String(q?.id || '')
  autoReturn.value = String(q?.autoReturn || '') === '1'
})
onShow(async () => {
  if (!reportId.value) {
    error.value = '缺少检验报告编号'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    report.value = await getLabReport(reportId.value)
    scheduleOcrPoll()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '检验报告加载失败'
  } finally {
    loading.value = false
  }
})
onHide(stopOcrPoll)
onUnmounted(stopOcrPoll)

function stopOcrPoll() {
  if (pollTimer) {
    globalThis.clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

function scheduleOcrPoll() {
  stopOcrPoll()
  if (!autoReturn.value) return
  pollTimer = globalThis.setTimeout(async () => {
    try {
      const task = await getOcrTask(reportId.value)
      if (task.status === 'SUCCESS') {
        stopOcrPoll()
        uni.showToast({ title: '识别完成，报告已整理', icon: 'success' })
        globalThis.setTimeout(
          () => uni.redirectTo({ url: '/pages-customer/lab-report/index' }),
          500,
        )
        return
      }
      if (task.status === 'FAILED') {
        stopOcrPoll()
        report.value = await getLabReport(reportId.value)
        return
      }
      report.value = await getLabReport(reportId.value)
      scheduleOcrPoll()
    } catch {
      scheduleOcrPoll()
    }
  }, 1800)
}

async function retryAssessment() {
  if (!report.value || reassessing.value) return
  reassessing.value = true
  error.value = ''
  try {
    const assessment = await submitAi(reportId.value)
    const reports = await getHealthReports(report.value.patientId)
    const healthReport = reports.records.find((item) => item.assessment?.id === assessment.id)
    if (healthReport) {
      uni.redirectTo({ url: `/pages-customer/health-report/detail?id=${healthReport.id}` })
      return
    }
    report.value = await getLabReport(reportId.value)
    uni.showToast({ title: '评估已生成，请在健康报告中查看', icon: 'success' })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '评估生成失败，请稍后重试'
    report.value = await getLabReport(reportId.value).catch(() => report.value)
  } finally {
    reassessing.value = false
  }
}
</script>

<style scoped>
.report-detail-page {
  padding-top: 24rpx;
}
.report-hero {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-radius: 32rpx;
  background: linear-gradient(140deg, #e3f6ef, #f6fbf9);
  border: 1rpx solid #d4e9e1;
}
.report-symbol {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 82rpx;
  height: 82rpx;
  border-radius: 26rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 25rpx;
  font-weight: 750;
}
.report-head-content {
  flex: 1;
  min-width: 0;
  margin: 0 20rpx;
}
.hero-name {
  overflow: hidden;
  font-size: 31rpx;
  font-weight: 710;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hero-date {
  margin-top: 8rpx;
  color: #788b84;
  font-size: 22rpx;
}
.indicator-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 22rpx;
  padding: 24rpx 12rpx;
  border: 1rpx solid #e2ebe8;
  border-radius: 25rpx;
  background: #fff;
}
.processing-note {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 22rpx;
  padding: 24rpx;
  border: 1rpx solid #d6ebe3;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #f8fffc, #e7f7f1);
}
.processing-pulse {
  display: flex;
  flex: 0 0 56rpx;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  border-radius: 19rpx;
  background: #d8f2e8;
}
.processing-pulse view {
  width: 18rpx;
  height: 18rpx;
  border: 5rpx solid #85cdb7;
  border-top-color: #08775e;
  border-radius: 50%;
  animation: processing-spin 1.2s linear infinite;
}
.processing-title {
  color: #185447;
  font-size: 25rpx;
  font-weight: 720;
}
.processing-copy {
  margin-top: 7rpx;
  color: #628078;
  font-size: 22rpx;
  line-height: 1.6;
}
.assessment-failed-note {
  margin-top: 22rpx;
  padding: 26rpx;
  border: 1rpx solid #f0ce91;
  border-radius: 22rpx;
  background: #fff8e9;
}
.assessment-failed-title {
  color: #805000;
  font-size: 26rpx;
  font-weight: 720;
}
.assessment-failed-copy {
  margin-top: 9rpx;
  color: #756449;
  font-size: 22rpx;
  line-height: 1.65;
}
.assessment-retry-button {
  margin-top: 20rpx;
  border: 0;
  border-radius: 16rpx;
  background: #0b8064;
  color: #fff;
  font-size: 24rpx;
}
@keyframes processing-spin {
  to {
    transform: rotate(360deg);
  }
}
.indicator-summary view {
  display: flex;
  flex-direction: column;
  text-align: center;
}
.indicator-summary text:first-child {
  color: #174539;
  font-size: 32rpx;
  font-weight: 740;
}
.indicator-summary text:last-child {
  margin-top: 5rpx;
  color: #82908b;
  font-size: 20rpx;
}
.indicator-name {
  color: #23473e;
  font-size: 25rpx;
  font-weight: 700;
}
.indicator-ref {
  margin-top: 8rpx;
  color: #87948f;
  font-size: 21rpx;
}
.result-heading {
  margin-top: 34rpx;
}
.result-card {
  overflow: hidden;
  padding: 0 28rpx;
}
.result-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.result-count {
  padding: 7rpx 14rpx;
  border-radius: 999rpx;
  background: #edf7f3;
  color: #4c766b;
  font-size: 20rpx;
}
.category-indicator-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 22rpx 0;
  border-top: 1rpx solid #e8efec;
}
.category-indicator-copy {
  flex: 1;
  min-width: 0;
}
.category-indicator-result {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 14rpx;
  text-align: right;
}
.category-indicator-value {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  color: #08775e;
}
.category-indicator-value text:first-child {
  font-size: 30rpx;
  font-weight: 780;
}
.category-indicator-value text:last-child {
  max-width: 120rpx;
  margin-left: 7rpx;
  color: #748780;
  font-size: 19rpx;
  word-break: break-all;
}
.finding-section {
  padding: 27rpx 0 20rpx;
  color: #125f4e;
  font-size: 29rpx;
  font-weight: 760;
}
.finding-row {
  padding: 22rpx 0;
  border-top: 1rpx solid #e8efec;
}
.finding-name {
  color: #24483f;
  font-size: 25rpx;
  font-weight: 680;
}
.finding-result {
  margin-top: 10rpx;
  color: #617a72;
  font-size: 23rpx;
  line-height: 1.65;
  word-break: break-all;
}
.finding-summary {
  margin: 18rpx 0 28rpx;
  padding: 22rpx 24rpx;
  border: 1rpx solid #cde8de;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #effaf6, #e3f5ef);
}
.finding-summary-title {
  color: #0c735c;
  font-size: 24rpx;
  font-weight: 760;
}
.finding-summary-item {
  position: relative;
  margin-top: 12rpx;
  padding-left: 20rpx;
  color: #294e44;
  font-size: 24rpx;
  line-height: 1.65;
  word-break: break-all;
}
.finding-summary-item::before {
  position: absolute;
  top: 17rpx;
  left: 0;
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #19a37f;
  content: '';
}
</style>

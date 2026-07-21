<template>
  <view class="ocr-editor">
    <view class="workflow-card">
      <view class="workflow-head">
        <view>
          <view class="eyebrow">REPORT DIGITIZATION</view>
          <view class="workflow-title">报告数字化</view>
          <view class="workflow-copy">识别结果必须经人工核对后才能进入健康评估</view>
        </view>
        <StatusTag :status="displayStatus" />
      </view>
      <view class="steps">
        <view v-for="(step, index) in steps" :key="step.label" class="step">
          <view class="step-dot" :class="{ done: step.done, active: step.active }">
            {{ step.done ? '✓' : index + 1 }}
          </view>
          <text :class="{ active: step.done || step.active }">{{ step.label }}</text>
          <view v-if="index < steps.length - 1" class="step-line" :class="{ done: step.done }" />
        </view>
      </view>
      <view v-if="isProcessing" class="processing-box">
        <view class="scan-icon"><view class="scan-line" /></view>
        <view>
          <view class="processing-title">正在识别报告内容</view>
          <view class="processing-copy">首次使用需要加载本地 OCR 模型，请稍候</view>
        </view>
      </view>
      <view v-else-if="task?.status === 'FAILED'" class="failed-box">
        <view
          ><text>识别失败</text><text>{{ task.errorMessage || '识别服务暂时不可用' }}</text></view
        >
        <button class="soft-button retry-button" :loading="retrying" @click="retry">
          重新识别
        </button>
      </view>
      <view v-else-if="task?.status === 'SUCCESS'" class="result-summary">
        <view
          ><text>{{ task.indicatorCount }}</text
          ><text>识别指标</text></view
        >
        <view
          ><text>{{ confidenceText }}</text
          ><text>整体置信度</text></view
        >
        <view
          ><text>{{ task.attemptCount }}</text
          ><text>识别次数</text></view
        >
      </view>
      <view v-if="task?.warnings?.length" class="warning-list">
        <view v-for="warning in task.warnings" :key="warning">提示 · {{ warning }}</view>
      </view>
    </view>

    <view class="toolbar">
      <view>
        <view class="section-title">指标校对</view>
        <view class="section-copy">共 {{ indicators.length }} 项，请核对数值、单位和参考范围</view>
      </view>
      <button class="preview-button" @click="previewSource">查看原报告</button>
    </view>

    <PageState :loading="loading" :empty="!isProcessing && indicators.length === 0">
      <view
        v-for="(item, index) in indicators"
        :key="`${item.code}-${index}`"
        class="card indicator-card"
      >
        <view class="indicator-head">
          <view class="indicator-number">{{ String(index + 1).padStart(2, '0') }}</view>
          <StatusTag :status="flag(item)" />
          <view class="remove" @click="remove(index)">删除</view>
        </view>
        <view class="field-label">指标名称</view>
        <input v-model="item.name" class="input" placeholder="请输入指标名称" />
        <view class="field-grid">
          <view>
            <view class="field-label">检测结果</view>
            <input v-model.number="item.value" class="input" type="digit" placeholder="数值" />
          </view>
          <view>
            <view class="field-label">单位</view>
            <input v-model="item.unit" class="input" placeholder="单位" />
          </view>
        </view>
        <view class="field-grid">
          <view>
            <view class="field-label">参考下限</view>
            <input
              v-model.number="item.referenceLow"
              class="input"
              type="digit"
              placeholder="可不填"
            />
          </view>
          <view>
            <view class="field-label">参考上限</view>
            <input
              v-model.number="item.referenceHigh"
              class="input"
              type="digit"
              placeholder="可不填"
            />
          </view>
        </view>
      </view>
    </PageState>

    <button v-if="!isProcessing" class="add-button" @click="add">＋ 新增未识别指标</button>
    <view v-if="error" class="error-box">{{ error }}</view>
    <view v-if="!isProcessing" class="action-bar">
      <button class="primary" :loading="saving" @click="saveAndConfirm">保存并确认指标</button>
      <button v-if="confirmed" class="assessment-button" :loading="evaluating" @click="evaluate">
        发起 AI 健康评估
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import {
  confirmIndicators,
  getFileDownloadUrl,
  getLabReport,
  getOcrTask,
  getReportFiles,
  retryOcrTask,
  saveIndicators,
  submitAi,
} from '@/api/lab-report'
import type { Indicator, LabReport, OcrTask } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const props = defineProps<{ reportId: string; assessmentRoute: string }>()
const report = ref<LabReport>()
const task = ref<OcrTask>()
const indicators = ref<Indicator[]>([])
const loading = ref(true)
const saving = ref(false)
const evaluating = ref(false)
const retrying = ref(false)
const confirmed = ref(false)
const error = ref('')
let timer: ReturnType<typeof globalThis.setTimeout> | undefined
let pollFailures = 0
let pollStartedAt = 0

const isProcessing = computed(() => ['PENDING', 'PROCESSING'].includes(task.value?.status || ''))
const displayStatus = computed(() => {
  if (task.value?.status === 'PENDING') return 'OCR_PENDING'
  if (task.value?.status === 'PROCESSING') return 'OCR_PROCESSING'
  if (task.value?.status === 'FAILED') return 'OCR_FAILED'
  return report.value?.status || 'UPLOADED'
})
const confidenceText = computed(() =>
  task.value?.confidence === undefined ? '--' : `${Math.round(task.value.confidence * 100)}%`,
)
const steps = computed(() => {
  const recognized =
    task.value?.status === 'SUCCESS' || report.value?.status === 'WAITING_CONFIRMATION'
  const isConfirmed = confirmed.value || report.value?.status === 'CONFIRMED'
  return [
    { label: '文件上传', done: true, active: false },
    { label: '智能识别', done: recognized, active: isProcessing.value },
    { label: '人工确认', done: isConfirmed, active: recognized && !isConfirmed },
  ]
})

watch(
  () => props.reportId,
  (id) => id && load(),
  { immediate: true },
)
onUnmounted(() => timer && globalThis.clearTimeout(timer))

async function load() {
  loading.value = true
  error.value = ''
  pollFailures = 0
  pollStartedAt = Date.now()
  try {
    report.value = await getLabReport(props.reportId)
    indicators.value = report.value.indicators.map((item) => ({ ...item }))
    confirmed.value = report.value.status === 'CONFIRMED'
    try {
      task.value = await getOcrTask(props.reportId)
    } catch {
      task.value = undefined
    }
    schedulePoll()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '报告加载失败'
  } finally {
    loading.value = false
  }
}

function schedulePoll() {
  if (timer) globalThis.clearTimeout(timer)
  if (!isProcessing.value) return
  if (Date.now() - pollStartedAt > 5 * 60 * 1000) {
    error.value = '识别时间超过预期，请稍后从报告列表重新进入查看进度'
    return
  }
  timer = globalThis.setTimeout(async () => {
    try {
      task.value = await getOcrTask(props.reportId)
      pollFailures = 0
      if (!isProcessing.value) {
        report.value = await getLabReport(props.reportId)
        indicators.value = report.value.indicators.map((item) => ({ ...item }))
      }
    } catch (cause) {
      pollFailures += 1
      if (pollFailures >= 3) {
        error.value = cause instanceof Error ? cause.message : '识别进度查询失败，请稍后重试'
        return
      }
    }
    schedulePoll()
  }, 1800)
}

async function retry() {
  retrying.value = true
  error.value = ''
  try {
    task.value = await retryOcrTask(props.reportId)
    pollFailures = 0
    pollStartedAt = Date.now()
    schedulePoll()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '重新识别失败'
  } finally {
    retrying.value = false
  }
}

function flag(item: Indicator) {
  if (item.referenceLow !== undefined && item.value < item.referenceLow) return 'LOW'
  if (item.referenceHigh !== undefined && item.value > item.referenceHigh) return 'HIGH'
  return 'NORMAL'
}

function add() {
  indicators.value.push({
    code: `manual_${Date.now()}`,
    name: '',
    value: 0,
    unit: '',
  })
}

function remove(index: number) {
  indicators.value.splice(index, 1)
}

async function saveAndConfirm() {
  if (!indicators.value.length) {
    error.value = '请至少添加一项检验指标'
    return
  }
  if (
    indicators.value.some((item) => !item.code.trim() || !item.name.trim() || !item.unit.trim())
  ) {
    error.value = '请补全每项指标的编码、名称和单位'
    return
  }
  const codes = indicators.value.map((item) => item.code.trim())
  if (new Set(codes).size !== codes.length) {
    error.value = '指标编码不能重复'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const normalized = indicators.value.map((item) => ({
      ...item,
      value: Number(item.value),
      referenceLow: optionalNumber(item.referenceLow),
      referenceHigh: optionalNumber(item.referenceHigh),
    }))
    if (normalized.some((item) => !Number.isFinite(item.value))) {
      error.value = '检测结果必须为有效数值'
      return
    }
    if (
      normalized.some(
        (item) =>
          item.referenceLow !== undefined &&
          item.referenceHigh !== undefined &&
          item.referenceLow > item.referenceHigh,
      )
    ) {
      error.value = '参考下限不能高于参考上限'
      return
    }
    await saveIndicators(props.reportId, normalized)
    report.value = await confirmIndicators(props.reportId)
    confirmed.value = true
    indicators.value = report.value.indicators.map((item) => ({ ...item }))
    uni.showToast({ title: '指标已确认' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '指标确认失败'
  } finally {
    saving.value = false
  }
}

function optionalNumber(value: number | undefined) {
  if (value === undefined || value === null || String(value).trim() === '') return undefined
  const normalized = Number(value)
  return Number.isFinite(normalized) ? normalized : undefined
}

async function evaluate() {
  evaluating.value = true
  error.value = ''
  try {
    await submitAi(props.reportId)
    uni.redirectTo({ url: props.assessmentRoute })
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'AI评估创建失败'
  } finally {
    evaluating.value = false
  }
}

async function previewSource() {
  try {
    const files = await getReportFiles(props.reportId)
    if (!files.length) throw new Error('没有可预览的报告文件')
    const file = await getFileDownloadUrl(props.reportId, files[0].id)
    if (!file.downloadUrl) throw new Error('报告预览地址生成失败')
    uni.downloadFile({
      url: file.downloadUrl,
      success: (result) =>
        uni.openDocument({ filePath: result.tempFilePath, showMenu: true, fail: () => undefined }),
      fail: () => (error.value = '报告文件下载失败'),
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '原报告预览失败'
  }
}
</script>

<style scoped>
.ocr-editor {
  padding-bottom: 190rpx;
}
.workflow-card {
  padding: 32rpx;
  border-radius: 32rpx;
  background: linear-gradient(145deg, #173f37, #0a735a);
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(15, 94, 75, 0.18);
}
.workflow-head,
.toolbar,
.indicator-head,
.field-grid,
.failed-box,
.result-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}
.eyebrow {
  color: #91d9c4;
  font-size: 18rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}
.workflow-title {
  margin-top: 10rpx;
  font-size: 36rpx;
  font-weight: 740;
}
.workflow-copy {
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.65);
  font-size: 21rpx;
}
.steps {
  display: flex;
  margin-top: 34rpx;
}
.step {
  position: relative;
  display: flex;
  flex: 1;
  align-items: center;
  flex-direction: column;
  color: rgba(255, 255, 255, 0.46);
  font-size: 19rpx;
}
.step-dot {
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42rpx;
  height: 42rpx;
  margin-bottom: 10rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
}
.step-dot.done {
  background: #5bc7a9;
  color: #0f4b3d;
}
.step-dot.active {
  background: #fff;
  color: #0f745c;
}
.step text.active {
  color: #fff;
}
.step-line {
  position: absolute;
  top: 20rpx;
  left: calc(50% + 25rpx);
  z-index: 1;
  width: calc(100% - 50rpx);
  height: 3rpx;
  background: rgba(255, 255, 255, 0.15);
}
.step-line.done {
  background: #5bc7a9;
}
.processing-box,
.failed-box,
.result-summary {
  margin-top: 28rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.1);
}
.scan-icon {
  position: relative;
  overflow: hidden;
  width: 58rpx;
  height: 58rpx;
  margin-right: 18rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.4);
  border-radius: 17rpx;
}
.scan-line {
  position: absolute;
  top: 27rpx;
  left: 8rpx;
  width: 42rpx;
  height: 3rpx;
  background: #7fe0c5;
}
.processing-box {
  display: flex;
  align-items: center;
}
.processing-title,
.failed-box text:first-child {
  font-size: 24rpx;
  font-weight: 650;
}
.processing-copy,
.failed-box text:last-child {
  display: block;
  margin-top: 5rpx;
  color: rgba(255, 255, 255, 0.6);
  font-size: 19rpx;
}
.retry-button {
  width: auto;
  margin: 0;
  padding: 0 22rpx;
}
.result-summary > view {
  flex: 1;
  text-align: center;
}
.result-summary text {
  display: block;
}
.result-summary text:first-child {
  font-size: 30rpx;
  font-weight: 730;
}
.result-summary text:last-child {
  margin-top: 5rpx;
  color: rgba(255, 255, 255, 0.58);
  font-size: 18rpx;
}
.warning-list {
  margin-top: 18rpx;
  color: #ffe2a3;
  font-size: 19rpx;
  line-height: 1.6;
}
.toolbar {
  margin: 34rpx 4rpx 22rpx;
}
.section-title {
  font-size: 31rpx;
  font-weight: 720;
}
.section-copy {
  margin-top: 5rpx;
  color: #82918c;
  font-size: 20rpx;
}
.preview-button {
  width: auto;
  margin: 0;
  padding: 0 22rpx;
  border: 1rpx solid #cfe4dd;
  background: #eef8f4;
  color: #0f725b;
  font-size: 21rpx;
}
.indicator-card {
  padding: 28rpx;
}
.indicator-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
  border-radius: 18rpx;
  background: #e5f5ef;
  color: #0f745c;
  font-size: 21rpx;
  font-weight: 730;
}
.remove {
  margin-left: auto;
  color: #ad615d;
  font-size: 21rpx;
}
.field-label {
  margin: 20rpx 2rpx 4rpx;
  color: #60766e;
  font-size: 21rpx;
  font-weight: 620;
}
.field-grid > view {
  flex: 1;
  min-width: 0;
}
.add-button {
  border: 2rpx dashed #b8d7cd;
  background: #f4faf7;
  color: #0f755e;
  font-size: 24rpx;
}
.error-box {
  margin: 18rpx 0;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #fff0ef;
  color: #a83e37;
  font-size: 22rpx;
}
.action-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 5;
  max-width: 920rpx;
  margin: auto;
  padding: 18rpx 30rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 30rpx rgba(31, 58, 49, 0.08);
}
.action-bar button {
  margin: 0;
}
.assessment-button {
  margin-top: 12rpx !important;
  border: 1rpx solid #c9e1d9;
  background: #fff;
  color: #0f725b;
}
</style>

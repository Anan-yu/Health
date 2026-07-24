<template>
  <view class="page report-page">
    <PageState :loading="loading" :error="error" :empty="!report">
      <view v-if="report" class="report-hero">
        <view class="eyebrow">HEALTH REPORT</view>
        <view class="hero-title">本次健康评估报告</view>
        <view class="hero-summary">{{ overallSummary }}</view>
        <view class="hero-meta"
          >报告日期：{{ report.publishedAt || '-' }} · 检验报告与健康档案共同评估</view
        >
      </view>

      <view class="section-head"><view class="title">整体健康状态</view></view>
      <view class="card status-card">
        <view v-for="item in statusOverview" :key="item.label" class="status-row">
          <text>{{ item.label }}</text
          ><text :class="`status ${item.level}`">{{ item.text }}</text>
        </view>
      </view>

      <view class="section-head"
        ><view class="eyebrow">FOCUS</view><view class="title">重点健康问题</view></view
      >
      <view v-if="concerns.length" class="focus-list">
        <view v-for="item in concerns" :key="item.code" class="card focus-card">
          <view class="row"
            ><view class="focus-title">{{ item.title }}</view
            ><text :class="`pill ${item.level}`">{{
              item.level === 'high' ? '建议重点关注' : '建议改善'
            }}</text></view
          >
          <view class="focus-description">{{ item.description }}</view>
          <view class="focus-next">下一步：{{ item.next }}</view>
        </view>
      </view>
      <view v-else class="card good-card"
        >当前已确认的数据未提示需要优先改善的健康问题，请保持良好生活习惯并按需复评。</view
      >

      <view class="section-head"
        ><view class="eyebrow">EVIDENCE</view><view class="title">关键依据</view></view
      >
      <view class="card evidence-card">
        <template v-if="evidenceItems.length">
          <view v-for="item in evidenceItems" :key="item" class="evidence-item">{{ item }}</view>
        </template>
        <view v-else class="muted">本次暂未提取到可展示的关键依据。</view>
        <view class="data-action" @click="openLabReport">查看原检验报告</view>
      </view>

      <view class="section-head"
        ><view class="eyebrow">DIRECTION</view><view class="title">本次优先改善方向</view></view
      >
      <view class="card direction-card">
        <view v-for="(item, index) in directions" :key="item" class="direction-item"
          ><text>{{ index + 1 }}</text
          ><view>{{ item }}</view></view
        >
      </view>

      <view v-if="isCustomer" class="plan-card" @click="openFollowup">
        <view
          ><view class="plan-kicker">HEALTH FOLLOW-UP</view
          ><view class="plan-title">本周健康计划已生成</view
          ><view class="plan-copy">健康随访会根据本次重点问题安排轻量任务与反馈。</view></view
        ><text>›</text>
      </view>
      <button class="download-button" :loading="downloading" @click="download">
        下载 PDF 健康报告
      </button>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getHealthReport, getHealthReportDownloadUrl } from '@/api/health-report'
import { getApiBaseUrl, getRequestHeaders } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import type { Assessment, HealthReport } from '@/types/api'
import { cleanHealthText } from '@/utils/health-text'
import PageState from '@/components/PageState.vue'

type Focus = {
  code: string
  title: string
  level: 'attention' | 'high'
  description: string
  next: string
  evidence: string[]
  recommendations: string[]
}
const labels: Record<string, string> = {
  GLUCOSE_METABOLISM: '糖代谢健康',
  LIPID_CARDIOVASCULAR: '心血管与血脂健康',
  CHRONIC_INFLAMMATION: '炎症相关健康',
  LIVER_METABOLIC: '肝脏与代谢健康',
  KIDNEY_ELECTROLYTE: '肾脏与电解质健康',
  HEMATOLOGY_ANEMIA: '血液与营养状态',
  THYROID_HORMONE: '甲状腺健康',
  BODY_COMPOSITION: '体重与身体成分',
  HPA_ADRENAL: '睡眠与恢复',
  NUTRITION_MICRONUTRIENT: '营养状态',
  GUT_BARRIER: '消化与肠道健康',
  MENTAL_EMOTIONAL: '心理与情绪健康',
}
const id = ref(''),
  report = ref<HealthReport | null>(null),
  loading = ref(true),
  downloading = ref(false),
  error = ref('')
const auth = useAuthStore()
const isCustomer = computed(() => auth.currentWorkbench === 'CUSTOMER')
const assessment = computed<Assessment | undefined>(() => report.value?.assessment)
const evaluated = computed(() =>
  (assessment.value?.results?.results || []).filter((item) => item.status !== 'INSUFFICIENT_DATA'),
)
const concerns = computed<Focus[]>(() =>
  evaluated.value
    .filter((item) => item.riskLevel === 'ATTENTION' || item.riskLevel === 'HIGH')
    .slice(0, 3)
    .map((item) => ({
      code: item.modelCode,
      title: labels[item.modelCode] || '健康状态关注',
      level: item.riskLevel === 'HIGH' ? 'high' : 'attention',
      description:
        cleanHealthText((item.evidence || []).find((value) => !value.includes('未触发')) || '') ||
        '本次数据提示该方向需要持续关注。',
      next:
        cleanHealthText((item.recommendations || [])[0] || '') || '结合后续健康随访持续观察变化。',
      evidence: (item.evidence || []).map(cleanHealthText).filter(Boolean),
      recommendations: (item.recommendations || []).map(cleanHealthText).filter(Boolean),
    })),
)
const overallSummary = computed(() =>
  concerns.value.length
    ? `本次发现 ${concerns.value.length} 项需要优先关注的健康方向，已生成对应的健康随访计划。`
    : '当前已确认的数据整体平稳，建议保持良好生活习惯并按需完成健康随访。',
)
const statusOverview = computed(() => {
  const groups = [
    ['体重与代谢', ['GLUCOSE_METABOLISM']],
    ['心血管健康', ['LIPID_CARDIOVASCULAR']],
    ['肝肾基础状态', ['LIVER_METABOLIC', 'KIDNEY_ELECTROLYTE']],
    ['营养与饮食', ['NUTRITION_MICRONUTRIENT']],
    ['睡眠与恢复', ['HPA_ADRENAL']],
    ['运动与生活习惯', []],
  ] as const
  return groups.map(([label, codes]) => {
    const items = evaluated.value.filter((item) =>
      (codes as readonly string[]).includes(item.modelCode),
    )
    const high = items.some((item) => item.riskLevel === 'HIGH'),
      attention = items.some((item) => item.riskLevel === 'ATTENTION')
    return {
      label,
      level: high ? 'high' : attention ? 'attention' : 'good',
      text: high ? '重点关注' : attention ? '建议改善' : '当前平稳',
    }
  })
})
const evidenceItems = computed(() =>
  [
    ...new Set(
      concerns.value.flatMap((item) => item.evidence).filter((item) => !item.includes('未触发')),
    ),
  ].slice(0, 8),
)
const directions = computed(() => {
  const result = [...new Set(concerns.value.flatMap((item) => item.recommendations))].slice(0, 3)
  return result.length
    ? result
    : ['保持规律作息、均衡饮食与适量运动，并在有新的检验报告时进行复评。']
})
const load = async () => {
  if (!id.value) {
    error.value = '缺少健康报告编号'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    report.value = await getHealthReport(id.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '报告加载失败'
  } finally {
    loading.value = false
  }
}
onLoad((options) => {
  id.value = options?.id || ''
})
onShow(load)
const openLabReport = () => {
  if (!assessment.value?.reportId) return
  const root = isCustomer.value ? 'pages-customer' : 'pages-business'
  uni.navigateTo({ url: `/${root}/lab-report/detail?id=${assessment.value.reportId}` })
}
const openFollowup = () => uni.navigateTo({ url: '/pages-customer/followup/index' })
const download = async () => {
  if (!id.value) return
  downloading.value = true
  try {
    const { downloadUrl } = await getHealthReportDownloadUrl(id.value)
    // #ifdef H5
    globalThis.location.assign(downloadUrl)
    // #endif
    // #ifdef MP-WEIXIN
    uni.downloadFile({
      url: `${getApiBaseUrl()}/api/v1/health-reports/${id.value}/content`,
      header: getRequestHeaders(),
      success: ({ tempFilePath, statusCode }) =>
        statusCode === 200
          ? uni.openDocument({ filePath: tempFilePath, fileType: 'pdf', showMenu: true })
          : uni.showToast({ title: '下载失败，请稍后重试', icon: 'none' }),
      fail: () => uni.showToast({ title: '下载失败，请稍后重试', icon: 'none' }),
    })
    // #endif
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '下载失败', icon: 'none' })
  } finally {
    downloading.value = false
  }
}
</script>

<style scoped>
.report-page {
  padding-top: 24rpx;
}
.report-hero {
  padding: 34rpx 30rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #0b5547, #0d896d);
  color: #fff;
  box-shadow: 0 16rpx 38rpx rgba(12, 104, 82, 0.22);
}
.eyebrow {
  font-size: 20rpx;
  letter-spacing: 2rpx;
  font-weight: 750;
}
.hero-title {
  margin-top: 10rpx;
  font-size: 38rpx;
  font-weight: 760;
}
.hero-summary {
  margin-top: 16rpx;
  line-height: 1.65;
  font-size: 26rpx;
}
.hero-meta {
  margin-top: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  font-size: 20rpx;
}
.section-head {
  margin: 34rpx 8rpx 16rpx;
}
.section-head .title {
  margin-top: 6rpx;
  font-size: 31rpx;
}
.section-head .eyebrow {
  color: #0b8064;
}
.status-card {
  padding: 8rpx 28rpx;
}
.status-row {
  display: flex;
  justify-content: space-between;
  padding: 21rpx 0;
  border-bottom: 1rpx solid #edf2f0;
  color: #28493f;
  font-size: 25rpx;
}
.status-row:last-child {
  border-bottom: 0;
}
.status,
.pill {
  padding: 7rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}
.good {
  color: #08775d;
  background: #e4f7ef;
}
.attention {
  color: #a76200;
  background: #fff1d7;
}
.high {
  color: #b42318;
  background: #fee9e7;
}
.focus-card {
  margin-bottom: 18rpx;
  padding: 28rpx;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}
.focus-title {
  font-size: 29rpx;
  font-weight: 720;
  color: #1f463a;
}
.focus-description {
  margin-top: 17rpx;
  color: #557169;
  line-height: 1.65;
  font-size: 25rpx;
}
.focus-next {
  margin-top: 17rpx;
  padding: 16rpx;
  border-radius: 15rpx;
  background: #eef8f4;
  color: #176852;
  font-size: 23rpx;
  line-height: 1.55;
}
.good-card {
  color: #46685e;
  line-height: 1.7;
}
.evidence-card {
  padding: 16rpx 27rpx;
}
.evidence-item {
  padding: 17rpx 0;
  border-bottom: 1rpx solid #edf1ef;
  color: #47675d;
  font-size: 24rpx;
  line-height: 1.55;
}
.evidence-item:last-of-type {
  border-bottom: 0;
}
.data-action {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #e8f6f1;
  color: #08785e;
  text-align: center;
  font-size: 24rpx;
  font-weight: 650;
}
.direction-card {
  padding: 9rpx 27rpx;
}
.direction-item {
  display: flex;
  gap: 16rpx;
  padding: 19rpx 0;
  color: #365a4f;
  font-size: 24rpx;
  line-height: 1.55;
}
.direction-item text {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  background: #dff4ec;
  color: #0d755d;
  font-size: 20rpx;
}
.plan-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 30rpx;
  padding: 28rpx 30rpx;
  border-radius: 26rpx;
  background: #fff4da;
  border: 1rpx solid #f5db9d;
  color: #825d10;
}
.plan-kicker {
  font-size: 19rpx;
  letter-spacing: 1.5rpx;
}
.plan-title {
  margin-top: 8rpx;
  font-size: 29rpx;
  font-weight: 740;
}
.plan-copy {
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.5;
}
.plan-card > text {
  font-size: 42rpx;
}
.download-button {
  margin-top: 24rpx;
  border-radius: 16rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 28rpx;
}
</style>

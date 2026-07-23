<template>
  <view class="page trend-page">
    <view class="page-heading">
      <view
        ><view class="eyebrow">HEALTH TRENDS</view><view class="title">指标趋势</view
        ><view class="subtitle">基于已确认检验报告的同一指标历史变化</view></view
      >
    </view>
    <PageState :loading="loadingReports" :error="error" :empty="indicators.length === 0">
      <view class="indicator-selector">
        <view class="selector-main" @click="showSelector = !showSelector">
          <view>
            <view class="selector-label">当前查看指标</view>
            <view class="selector-value">{{ selectedName }}<text v-if="selectedUnit"> · {{ selectedUnit }}</text></view>
          </view>
          <view class="selector-action">{{ showSelector ? '收起' : '切换指标' }} <text>⌄</text></view>
        </view>
        <view v-if="showSelector" class="selector-options">
          <view class="option-tip">请选择要查看趋势的检验指标</view>
          <view class="option-grid">
            <view
              v-for="item in indicators"
              :key="item.code"
              class="indicator-option"
              :class="{ active: item.code === selectedCode }"
              @click="selectIndicator(item.code)"
            >
              <text>{{ item.name }}</text><text v-if="item.code === selectedCode">✓</text>
            </view>
          </view>
        </view>
      </view>
      <PageState :loading="loadingTrend" :empty="points.length === 0">
        <view class="card summary-card">
          <view
            ><view class="metric-label">最新值</view
            ><view class="metric-value"
              >{{ summary?.latestValue ?? '-' }}
              <text>{{ summary?.unit || selectedUnit }}</text></view
            ></view
          >
          <view
            ><view class="metric-label">变化趋势</view
            ><view
              class="trend-badge"
              :class="(summary?.trendDirection || 'STABLE').toLowerCase()"
              >{{ trendLabel }}</view
            ></view
          >
          <view
            ><view class="metric-label">有效记录</view
            ><view class="metric-value">{{ summary?.dataPoints || 0 }} <text>次</text></view></view
          >
        </view>
        <view class="card chart-card">
          <view class="section-title">{{ summary?.indicatorName || selectedName }} · 历史记录</view>
          <view class="chart">
            <view v-for="point in points" :key="point.reportId" class="column">
              <view class="bar-wrap"
                ><view
                  class="bar"
                  :class="
                    point.abnormalFlag === 'HIGH' || point.abnormalFlag === 'LOW' ? 'abnormal' : ''
                  "
                  :style="{ height: `${barHeight(point.value)}rpx` }"
              /></view>
              <text class="point-value">{{ point.value }}</text
              ><text class="point-date">{{ formatDate(point.reportDate) }}</text>
            </view>
          </view>
          <view class="chart-note"
            >绿色为参考范围内记录，橙色表示当次检验标记异常。趋势仅供健康管理参考。</view
          >
        </view>
      </PageState>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyLabReports } from '@/api/lab-report'
import { getIndicatorTrend, getIndicatorTrendSummary } from '@/api/trend'
import type { Indicator, LabReport, TrendPoint, TrendSummary } from '@/types/api'
import PageState from '@/components/PageState.vue'

const reports = ref<LabReport[]>([])
const selectedCode = ref('')
const points = ref<TrendPoint[]>([])
const summary = ref<TrendSummary | null>(null)
const loadingReports = ref(true)
const loadingTrend = ref(false)
const error = ref('')
const showSelector = ref(false)
const indicators = computed(() => {
  const seen = new Map<string, Indicator>()
  reports.value.forEach((report) => report.indicators.forEach((item) => seen.set(item.code, item)))
  return [...seen.values()]
})
const selectedIndicator = computed(() =>
  indicators.value.find((item) => item.code === selectedCode.value),
)
const selectedName = computed(() => selectedIndicator.value?.name || '检验指标')
const selectedUnit = computed(() => selectedIndicator.value?.unit || '')
const trendLabel = computed(
  () => ({ UP: '上升', DOWN: '下降', STABLE: '平稳' })[summary.value?.trendDirection || 'STABLE'],
)

const loadTrend = async () => {
  const report = reports.value.find((item) =>
    item.indicators.some((indicator) => indicator.code === selectedCode.value),
  )
  if (!report || !selectedCode.value) return
  loadingTrend.value = true
  try {
    const [trend, nextSummary] = await Promise.all([
      getIndicatorTrend(report.patientId, selectedCode.value),
      getIndicatorTrendSummary(report.patientId, selectedCode.value),
    ])
    points.value = trend
    summary.value = nextSummary
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '趋势加载失败'
  } finally {
    loadingTrend.value = false
  }
}
const selectIndicator = (code: string) => {
  if (code === selectedCode.value) return
  selectedCode.value = code
  showSelector.value = false
  void loadTrend()
}
const barHeight = (value: number) => {
  const maximum = Math.max(...points.value.map((point) => point.value), 1)
  return Math.max(32, Math.round((value / maximum) * 210))
}
const formatDate = (date: string) => date.slice(5, 10).replace('-', '/')

onShow(async () => {
  loadingReports.value = true
  error.value = ''
  try {
    reports.value = await getMyLabReports()
    if (!selectedCode.value && indicators.value[0]) selectedCode.value = indicators.value[0].code
    await loadTrend()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '检验报告加载失败'
  } finally {
    loadingReports.value = false
  }
})
</script>

<style scoped>
.trend-page {
  padding-top: 34rpx;
}
.page-heading {
  margin: 0 6rpx 28rpx;
}
.title {
  margin-top: 8rpx;
}
.indicator-selector {
  overflow: hidden;
  margin-bottom: 24rpx;
  border: 1rpx solid #dcebe5;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 10rpx 26rpx rgba(31, 74, 63, 0.05);
}
.selector-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 23rpx 25rpx;
}
.selector-label { color: #82948d; font-size: 21rpx; }
.selector-value { margin-top: 6rpx; color: #20483d; font-size: 29rpx; font-weight: 700; }
.selector-value text { color: #789087; font-size: 20rpx; font-weight: 400; }
.selector-action { padding: 12rpx 16rpx; border-radius: 999rpx; background: #e6f6f0; color: #0d765e; font-size: 22rpx; }
.selector-action text { margin-left: 6rpx; font-size: 20rpx; }
.selector-options { padding: 0 22rpx 23rpx; border-top: 1rpx solid #edf3f0; }
.option-tip { margin: 18rpx 0 14rpx; color: #82948d; font-size: 21rpx; }
.option-grid { display: flex; flex-wrap: wrap; gap: 14rpx; }
.indicator-option { display: flex; align-items: center; gap: 8rpx; padding: 14rpx 18rpx; border: 1rpx solid #dde9e5; border-radius: 16rpx; background: #f8fbfa; color: #62766f; font-size: 22rpx; }
.indicator-option.active { border-color: #0f7a62; background: #e2f5ed; color: #0c7159; font-weight: 650; }
.summary-card {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  text-align: center;
}
.metric-label {
  color: #83918c;
  font-size: 21rpx;
}
.metric-value {
  margin-top: 10rpx;
  color: #173e34;
  font-size: 33rpx;
  font-weight: 750;
}
.metric-value text {
  font-size: 20rpx;
  font-weight: 400;
}
.trend-badge {
  display: inline-block;
  margin-top: 12rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: #eaf0ee;
  color: #5f716a;
  font-size: 21rpx;
}
.trend-badge.up {
  background: #fff0e7;
  color: #c76a32;
}
.trend-badge.down {
  background: #e4f6f0;
  color: #0d765e;
}
.chart-card {
  margin-top: 22rpx;
}
.chart {
  display: flex;
  height: 310rpx;
  margin-top: 22rpx;
  align-items: flex-end;
  justify-content: space-around;
  gap: 12rpx;
}
.column {
  display: flex;
  min-width: 76rpx;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
}
.bar-wrap {
  display: flex;
  height: 220rpx;
  align-items: flex-end;
}
.bar {
  width: 42rpx;
  border-radius: 14rpx 14rpx 4rpx 4rpx;
  background: linear-gradient(#75bea8, #168066);
}
.bar.abnormal {
  background: linear-gradient(#f4ae77, #dc7242);
}
.point-value {
  margin-top: 10rpx;
  color: #24463b;
  font-size: 22rpx;
  font-weight: 650;
}
.point-date {
  margin-top: 8rpx;
  color: #98a39f;
  font-size: 19rpx;
}
.chart-note {
  margin-top: 18rpx;
  color: #899792;
  font-size: 21rpx;
  line-height: 1.6;
}
</style>

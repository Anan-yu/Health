<template>
  <view v-if="latest" id="health-scan-analysis" class="dashboard">
    <view class="primary-tabs">
      <view
        v-for="tab in primaryTabs"
        :key="tab.key"
        class="primary-tab"
        :class="{ active: activePrimaryTab === tab.key }"
        @tap="activePrimaryTab = tab.key"
      >
        {{ tab.label }}
      </view>
    </view>

    <view v-if="activePrimaryTab === 'result'" class="result-panel">
      <view class="metric-toolbar">
        <view class="metric-heading">
          <view class="metric-switch-icon">{{ selectedMetric.icon }}</view>
          <view class="metric-switch-name">{{ selectedMetric.name }}</view>
        </view>
        <view class="metric-time">{{ formatTime(latest.createdAt) }}</view>
      </view>

      <view class="secondary-tabs">
        <view
          v-for="tab in secondaryTabs"
          :key="tab.key"
          class="secondary-tab"
          :class="{ active: activeSecondaryTab === tab.key }"
          @tap="activeSecondaryTab = tab.key"
        >
          {{ tab.label }}
        </view>
      </view>

      <view v-if="activeSecondaryTab === 'value'" class="metric-result-card">
        <view class="result-card-head">
          <view>
            <view class="result-card-label">本次检测</view>
            <view class="result-card-time">{{ formatTime(latest.completedAt || latest.createdAt) }}</view>
          </view>
          <view class="metric-status" :class="`status-${selectedAssessment.tone}`">
            {{ selectedAssessment.status }}
          </view>
        </view>
        <view class="metric-value-row">
          <view class="metric-main-value">{{ selectedMetric.display(latest) }}</view>
          <view class="metric-unit">{{ selectedMetric.unit }}</view>
        </view>
        <view class="range-track">
          <view
            v-for="section in selectedMetric.ranges"
            :key="section.label"
            class="range-section"
            :style="{ background: section.color, flex: section.weight || 1 }"
          />
          <view class="range-marker" :style="{ left: `${selectedMarkerPosition}%` }">
            <view class="range-marker-tip" />
          </view>
        </view>
        <view class="range-labels">
          <view v-for="section in selectedMetric.ranges" :key="section.label" class="range-label">
            <view class="range-label-name">{{ section.label }}</view>
            <view class="range-threshold">{{ section.threshold }}</view>
          </view>
        </view>
        <view class="result-guidance">{{ selectedAssessment.guidance }}</view>
      </view>

      <view v-else-if="activeSecondaryTab === 'trend'" class="content-card">
        <view class="content-title-row">
          <view>
            <view class="content-title">近期变化</view>
            <view class="content-copy">最近 {{ selectedTrend.length }} 次有效检测记录</view>
          </view>
          <view class="trend-summary">{{ trendSummary }}</view>
        </view>
        <view v-if="selectedTrend.length > 1" class="trend-chart">
          <view v-for="point in selectedTrend" :key="point.id" class="trend-column">
            <view class="trend-value">{{ point.label }}</view>
            <view class="trend-bar-area">
              <view class="trend-bar" :style="{ height: `${point.height}%` }" />
            </view>
            <view class="trend-date">{{ point.date }}</view>
          </view>
        </view>
        <view v-else class="empty-trend">
          再完成一次健康检测后，即可查看该指标的变化趋势。
        </view>
      </view>

      <view v-else-if="activeSecondaryTab === 'explain'" class="content-card">
        <view class="content-title">指标解读</view>
        <view class="explain-copy">{{ selectedMetric.explanation }}</view>
        <view class="explain-block">
          <view class="explain-label">怎么看这个结果</view>
          <view class="explain-text">{{ selectedAssessment.interpretation }}</view>
        </view>
        <view class="explain-block">
          <view class="explain-label">日常建议</view>
          <view class="explain-text">{{ selectedAssessment.guidance }}</view>
        </view>
      </view>

      <view v-else class="content-card">
        <view class="content-title">异常风险</view>
        <view v-if="selectedAssessment.tone === 'normal'" class="risk-safe">
          <view class="risk-safe-icon">✓</view>
          <view>
            <view class="risk-safe-title">本次未见明显异常</view>
            <view class="risk-safe-copy">保持规律生活，并通过连续检测观察个人变化趋势。</view>
          </view>
        </view>
        <view v-else class="risk-warning">
          <view class="risk-warning-title">{{ selectedAssessment.riskTitle }}</view>
          <view class="risk-warning-copy">{{ selectedAssessment.risk }}</view>
          <view class="risk-action">{{ selectedAssessment.action }}</view>
        </view>
        <view class="result-note">单次面部检测会受光线、姿势和当时状态影响，仅作健康参考。</view>
      </view>
    </view>

    <view v-else class="assessment-panel">
      <view class="score-card">
        <view class="score-summary">
          <view class="score-caption-row">
            <view class="score-caption">健康指数</view>
            <view class="score-help" @tap.stop="scoreHelpOpen = !scoreHelpOpen">?</view>
          </view>
          <view class="score-value">{{ healthScore }}</view>
          <view class="score-status">{{ healthScoreStatus }}</view>
          <view class="peer-comparison">
            超过
            <text class="peer-percent">{{ peerPercentile }}%</text>
            的同龄用户
          </view>
        </view>
        <view class="score-gauge">
          <view class="score-gauge-shell">
            <view
              class="score-gauge-ring"
              :style="{ '--score-angle': `${healthScore * 1.8}deg` }"
            />
            <view class="score-gauge-center">
              <view class="score-gauge-label">本次状态</view>
              <view class="score-gauge-status">{{ healthScoreStatus.replace('状态', '') }}</view>
            </view>
          </view>
        </view>
      </view>
      <view v-if="scoreHelpOpen" class="score-help-panel">
        健康指数由本次有效体征综合计算，满分为 100 分。分数越高，表示本次检测的整体状态越平稳；请结合长期趋势持续观察。
      </view>

      <view class="assessment-card">
        <view class="assessment-title">本次状态概览</view>
        <view class="assessment-copy">{{ assessmentSummary }}</view>
        <view class="assessment-stats">
          <view>
            <view class="assessment-stat-value">{{ availableMetrics.length }}</view>
            <view class="assessment-stat-label">有效指标</view>
          </view>
          <view>
            <view class="assessment-stat-value warning">{{ abnormalMetrics.length }}</view>
            <view class="assessment-stat-label">需要关注</view>
          </view>
          <view>
            <view class="assessment-stat-value">{{ normalMetricCount }}</view>
            <view class="assessment-stat-label">状态平稳</view>
          </view>
        </view>
      </view>

      <view class="assessment-card">
        <view class="assessment-title">体征评估</view>
        <view
          v-for="metric in availableMetrics"
          :key="metric.code"
          class="assessment-metric"
          @tap="openMetric(metric.code)"
        >
          <view class="assessment-metric-icon">{{ metric.icon }}</view>
          <view class="assessment-metric-main">
            <view class="assessment-metric-name">{{ metric.name }}</view>
            <view class="assessment-metric-copy">参考：{{ metric.reference }}</view>
          </view>
          <view class="assessment-metric-value">
            <view class="assessment-metric-reading">
              <text class="assessment-metric-number">{{ metric.display(latest) }}</text>
              <text class="assessment-metric-unit">{{ metric.unit }}</text>
            </view>
            <view class="assessment-metric-meta">
              <view class="assessment-metric-status" :class="`status-${assessMetric(metric).tone}`">
                {{ assessMetric(metric).status }}
              </view>
              <view class="assessment-arrow">›</view>
            </view>
          </view>
        </view>
      </view>

      <view class="assessment-card">
        <view class="assessment-title">健康关注</view>
        <view v-if="abnormalMetrics.length === 0" class="all-normal">
          当前有效体征整体平稳，建议继续保持规律作息、适量运动并定期观察。
        </view>
        <view
          v-for="metric in abnormalMetrics"
          :key="metric.code"
          class="attention-item"
          @tap="openMetric(metric.code, 'risk')"
        >
          <view class="attention-index">!</view>
          <view class="attention-content">
            <view class="attention-title">{{ metric.name }}：{{ assessMetric(metric).status }}</view>
            <view class="attention-copy">{{ assessMetric(metric).risk }}</view>
          </view>
          <view class="assessment-arrow">›</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { HealthScanResult } from '@/types/api'

type PrimaryTab = 'result' | 'assessment'
type SecondaryTab = 'value' | 'trend' | 'explain' | 'risk'
type Tone = 'normal' | 'attention' | 'danger'

interface RangeSection {
  label: string
  threshold: string
  color: string
  weight?: number
}

interface MetricAssessment {
  status: string
  short: string
  tone: Tone
  guidance: string
  interpretation: string
  riskTitle: string
  risk: string
  action: string
}

interface MetricDefinition {
  code: string
  name: string
  icon: string
  unit: string
  reference: string
  explanation: string
  ranges: RangeSection[]
  available: (result: HealthScanResult) => boolean
  numeric: (result: HealthScanResult) => number | undefined
  display: (result: HealthScanResult) => string
  marker: (result: HealthScanResult) => number
  assess: (result: HealthScanResult) => MetricAssessment
}

const props = defineProps<{
  records: HealthScanResult[]
  selectedMetricCode?: string
}>()
const emit = defineEmits<{
  'update:selectedMetricCode': [code: string]
}>()

const primaryTabs: Array<{ key: PrimaryTab; label: string }> = [
  { key: 'result', label: '检测结果' },
  { key: 'assessment', label: '健康评估报告' },
]

const secondaryTabs: Array<{ key: SecondaryTab; label: string }> = [
  { key: 'value', label: '检测结果' },
  { key: 'trend', label: '数据趋势' },
  { key: 'explain', label: '指标解读' },
  { key: 'risk', label: '异常风险' },
]

const normalAssessment = (
  status: string,
  short: string,
  interpretation: string,
  guidance: string,
): MetricAssessment => ({
  status,
  short,
  tone: 'normal',
  guidance,
  interpretation,
  riskTitle: '本次状态平稳',
  risk: '本次检测未提示明显异常。',
  action: '保持当前健康习惯，结合后续检测观察变化。',
})

const attentionAssessment = (
  status: string,
  short: string,
  interpretation: string,
  guidance: string,
  riskTitle: string,
  risk: string,
  action: string,
  tone: Tone = 'attention',
): MetricAssessment => ({
  status,
  short,
  tone,
  guidance,
  interpretation,
  riskTitle,
  risk,
  action,
})

const metricDefinitions: MetricDefinition[] = [
  {
    code: 'heartRate',
    name: '静息心率',
    icon: '心',
    unit: '次/分钟',
    reference: '60–100 次/分钟',
    explanation:
      '静息心率是安静状态下每分钟心跳次数，会受年龄、情绪、运动、睡眠和咖啡因等因素影响。',
    ranges: [
      { label: '偏慢', threshold: '<60', color: '#70b8dc' },
      { label: '正常', threshold: '60–100', color: '#29b684', weight: 2 },
      { label: '偏快', threshold: '>100', color: '#ee9b48' },
    ],
    available: (result) => hasValue(result.heartRate),
    numeric: (result) => numberValue(result.heartRate),
    display: (result) => integer(result.heartRate),
    marker: (result) => normalize(numberValue(result.heartRate), 40, 120),
    assess: (result) => {
      const value = numberValue(result.heartRate) || 0
      if (value < 60) {
        return attentionAssessment(
          '偏慢',
          '本次静息心率偏慢',
          '安静状态下心率低于常用成人参考范围，运动人群也可能出现生理性偏慢。',
          '先安静休息后复测；如反复偏慢并伴头晕、乏力或胸闷，请及时就医。',
          '心率偏慢需观察',
          '持续偏慢可能影响身体供血，需结合症状和个人基础情况判断。',
          '建议使用可靠设备复测，并记录是否伴随不适。',
        )
      }
      if (value > 100) {
        return attentionAssessment(
          '偏快',
          '本次静息心率偏快',
          '紧张、运动后、睡眠不足、发热或饮用咖啡因饮品都可能使心率暂时升高。',
          '静坐 5 至 10 分钟后再次检测，近期减少熬夜和刺激性饮品。',
          '心率偏快需关注',
          '若安静状态下持续偏快，可能增加心脏负担。',
          '建议使用可靠设备复测；如伴胸痛、明显心悸或呼吸困难，请及时就医。',
        )
      }
      return normalAssessment(
        '正常',
        '静息心率处于常用参考范围',
        '本次静息心率处于常用成人参考范围内。',
        '保持规律作息、适量运动和稳定情绪，继续观察个人长期趋势。',
      )
    },
  },
  {
    code: 'bloodPressure',
    name: '血压',
    icon: '压',
    unit: '毫米汞柱',
    reference: '90/60–139/89 毫米汞柱',
    explanation:
      '血压包括收缩压和舒张压，反映心脏泵血时与心脏舒张时血液对血管壁产生的压力。',
    ranges: [
      { label: '偏低', threshold: '<90/60', color: '#70b8dc' },
      { label: '正常', threshold: '90/60–119/79', color: '#29b684' },
      { label: '正常偏高', threshold: '120/80–139/89', color: '#e6b84f' },
      { label: '偏高', threshold: '140/90–179/119', color: '#ee8a38' },
      { label: '明显偏高', threshold: '≥180/120', color: '#c7433b' },
    ],
    available: (result) =>
      hasValue(result.systolicBloodPressure) && hasValue(result.diastolicBloodPressure),
    numeric: (result) => numberValue(result.systolicBloodPressure),
    display: (result) =>
      `${integer(result.systolicBloodPressure)}/${integer(result.diastolicBloodPressure)}`,
    marker: (result) => normalize(numberValue(result.systolicBloodPressure), 70, 190),
    assess: (result) => {
      const systolic = numberValue(result.systolicBloodPressure) || 0
      const diastolic = numberValue(result.diastolicBloodPressure) || 0
      if (systolic >= 180 || diastolic >= 120) {
        return attentionAssessment(
          '明显偏高',
          '本次血压参考值明显偏高',
          '面部检测给出的血压为健康参考值，明显偏高时应使用经过验证的袖带式血压计复测。',
          '保持安静并规范复测；如复测仍明显偏高或伴剧烈头痛、胸痛等症状，请立即就医。',
          '血压明显偏高',
          '持续明显偏高会增加心脑血管负担。',
          '请尽快使用袖带式血压计复测并根据结果咨询医生。',
          'danger',
        )
      }
      if (systolic >= 140 || diastolic >= 90) {
        return attentionAssessment(
          '偏高',
          '本次血压参考值偏高',
          '情绪、活动、咖啡因和测量状态均可能影响血压。',
          '安静休息后使用袖带式血压计复测，减少高盐饮食并记录连续变化。',
          '血压偏高需复测',
          '持续偏高可能增加心脑血管风险。',
          '建议连续多日规范测量；若多次偏高，请咨询医生。',
        )
      }
      if (systolic < 90 || diastolic < 60) {
        return attentionAssessment(
          '偏低',
          '本次血压参考值偏低',
          '血压偏低可能与体质、饮水不足、用药或当前状态有关。',
          '补充水分、避免突然起身并使用袖带式血压计复测。',
          '血压偏低需观察',
          '若伴头晕、乏力或晕厥，需要进一步评估。',
          '建议规范复测；持续偏低或伴明显不适时请及时就医。',
        )
      }
      if (systolic >= 120 || diastolic >= 80) {
        return normalAssessment(
          '正常偏高',
          '本次血压参考值处于正常偏高范围',
          '本次血压参考值未达到偏高范围，但已接近上限，适合继续观察个人变化。',
          '保持少盐饮食和规律活动，并定期使用袖带式血压计规范测量。',
        )
      }
      return normalAssessment(
        '正常',
        '本次血压参考值处于常用范围',
        '本次收缩压和舒张压均处于常用成人参考范围。',
        '维持少盐饮食、规律运动，并定期使用袖带式血压计测量。',
      )
    },
  },
  {
    code: 'oxygenSaturation',
    name: '血氧饱和度',
    icon: '氧',
    unit: '%',
    reference: '95%–100%',
    explanation:
      '血氧饱和度反映血红蛋白携带氧气的比例，是观察身体供氧状态的重要参考指标。',
    ranges: [
      { label: '明显偏低', threshold: '<90%', color: '#d95b4b' },
      { label: '偏低', threshold: '90%–94%', color: '#ee9b48' },
      { label: '正常', threshold: '95%–100%', color: '#29b684', weight: 2 },
    ],
    available: (result) => hasValue(result.oxygenSaturation),
    numeric: (result) => numberValue(result.oxygenSaturation),
    display: (result) => integer(result.oxygenSaturation),
    marker: (result) => normalize(numberValue(result.oxygenSaturation), 85, 100),
    assess: (result) => {
      const value = numberValue(result.oxygenSaturation) || 0
      if (value < 90) {
        return attentionAssessment(
          '明显偏低',
          '本次血氧参考值明显偏低',
          '血氧降低可能提示当前供氧不足，也可能受到检测环境与采集质量影响。',
          '立即使用指夹式血氧仪复测；若仍偏低或伴呼吸困难、口唇发紫，请及时就医。',
          '血氧明显偏低',
          '持续低血氧可能影响重要器官供氧。',
          '请尽快用指夹式血氧仪核实，必要时立即就医。',
          'danger',
        )
      }
      if (value < 95) {
        return attentionAssessment(
          '偏低',
          '本次血氧参考值偏低',
          '轻度偏低需要排除手冷、活动、环境和采集不稳定等因素。',
          '保持安静、手部温暖，并使用指夹式血氧仪复测。',
          '血氧偏低需复测',
          '若连续测量仍偏低，需要进一步了解呼吸和循环状态。',
          '建议规范复测；持续偏低或伴不适时请咨询医生。',
        )
      }
      return normalAssessment(
        '正常',
        '本次血氧参考值处于常用范围',
        '本次血氧饱和度处于常用参考范围，当前供氧状态较平稳。',
        '保持通风、规律运动和充足睡眠，出现呼吸不适时及时复测。',
      )
    },
  },
  {
    code: 'respirationRate',
    name: '呼吸频率',
    icon: '呼',
    unit: '次/分钟',
    reference: '12–20 次/分钟',
    explanation:
      '呼吸频率是安静状态下每分钟呼吸次数，会受活动、情绪、发热和呼吸系统状态影响。',
    ranges: [
      { label: '偏慢', threshold: '<12', color: '#70b8dc' },
      { label: '正常', threshold: '12–20', color: '#29b684', weight: 2 },
      { label: '偏快', threshold: '>20', color: '#ee9b48' },
    ],
    available: (result) => hasValue(result.respirationRate),
    numeric: (result) => numberValue(result.respirationRate),
    display: (result) => integer(result.respirationRate),
    marker: (result) => normalize(numberValue(result.respirationRate), 6, 30),
    assess: (result) => {
      const value = numberValue(result.respirationRate) || 0
      if (value < 12 || value > 20) {
        const direction = value < 12 ? '偏慢' : '偏快'
        return attentionAssessment(
          direction,
          `本次呼吸频率${direction}`,
          '呼吸频率变化可能与刚刚活动、紧张、发热或呼吸系统状态有关。',
          '安静休息后重新检测并留意是否有气短、胸闷或发热。',
          `呼吸频率${direction}`,
          '持续异常需要结合血氧、体温和身体感受一起判断。',
          '如复测仍异常或伴呼吸困难，请及时就医。',
        )
      }
      return normalAssessment(
        '正常',
        '本次呼吸频率处于常用范围',
        '本次安静状态下的呼吸频率处于常用成人参考范围。',
        '保持适量活动和良好通风，出现呼吸不适时及时复测。',
      )
    },
  },
  {
    code: 'heartRateVariability',
    name: '心率变异性',
    icon: '变',
    unit: '毫秒',
    reference: '30–120 毫秒（结合个人基线）',
    explanation:
      '心率变异性反映相邻心跳间期的变化，与自主神经调节、疲劳、睡眠和压力状态有关。',
    ranges: [
      { label: '偏低', threshold: '<30', color: '#ee9b48' },
      { label: '常见范围', threshold: '30–120', color: '#29b684', weight: 2 },
      { label: '偏高', threshold: '>120', color: '#70b8dc' },
    ],
    available: (result) => hasValue(result.heartRateVariability),
    numeric: (result) => numberValue(result.heartRateVariability),
    display: (result) => integer(result.heartRateVariability),
    marker: (result) => normalize(numberValue(result.heartRateVariability), 10, 160),
    assess: (result) => {
      const value = numberValue(result.heartRateVariability) || 0
      if (value < 30) {
        return attentionAssessment(
          '偏低',
          '本次心率变异性偏低',
          '短期偏低常见于疲劳、睡眠不足、精神紧张或身体恢复不充分。',
          '优先保证睡眠、适度放松并避免过量运动，连续观察个人基线变化。',
          '恢复状态需要关注',
          '持续低于个人基线可能提示身体处于压力或恢复不足状态。',
          '建议结合睡眠和疲劳感受连续观察，而不是只看单次结果。',
        )
      }
      if (value > 120) {
        return normalAssessment(
          '偏高',
          '本次心率变异性高于常见范围',
          '心率变异性个体差异较大，本次值高于常见参考范围，需要结合个人长期基线理解。',
          '建议在相同时间、相近状态下继续检测，重点观察个人趋势而非单次高低。',
        )
      }
      return normalAssessment(
        '平稳',
        '本次心率变异性较平稳',
        '本次结果未见明显偏低，当前自主神经调节状态较平稳。',
        '继续保持充足睡眠、规律运动和适当放松。',
      )
    },
  },
  {
    code: 'stressHrv',
    name: '压力参考',
    icon: '压',
    unit: '',
    reference: '0–0.4 较低，0.4–0.7 中等，>0.7 较高',
    explanation:
      '压力参考值根据短时心率变化估算，用于观察当前紧张和恢复状态，适合与个人历史趋势对照。',
    ranges: [
      { label: '较低', threshold: '0–0.4', color: '#29b684' },
      { label: '中等', threshold: '0.4–0.7', color: '#e6b84f' },
      { label: '较高', threshold: '>0.7', color: '#d95b4b' },
    ],
    available: (result) => hasValue(result.stressHrv),
    numeric: (result) => numberValue(result.stressHrv),
    display: (result) => decimal(result.stressHrv),
    marker: (result) => normalize(numberValue(result.stressHrv), 0, 1),
    assess: (result) => {
      const value = numberValue(result.stressHrv) || 0
      if (value > 0.7) {
        return attentionAssessment(
          '较高',
          '本次压力参考值较高',
          '短时压力偏高可能与紧张、睡眠不足、疲劳或刚刚活动有关。',
          '做几分钟缓慢呼吸，减少连续久坐，今晚尽量保证充足睡眠。',
          '压力与恢复需要关注',
          '若长期处于较高水平，可能影响睡眠和身体恢复。',
          '建议结合情绪、睡眠和个人历史趋势连续观察。',
        )
      }
      if (value > 0.4) {
        return attentionAssessment(
          '中等',
          '本次压力参考值处于中等水平',
          '当前可能存在一定紧张或疲劳，建议结合当天感受判断。',
          '适当休息、补充水分并安排短时放松活动。',
          '压力状态可继续观察',
          '单次中等压力不代表异常，重点关注是否持续上升。',
          '连续检测并与睡眠、工作强度和情绪变化对照。',
        )
      }
      return normalAssessment(
        '较低',
        '本次压力参考值较低',
        '本次压力参考值较低，当前身心状态相对放松。',
        '保持规律作息和适度运动，继续观察个人趋势。',
      )
    },
  },
]

const activePrimaryTab = ref<PrimaryTab>('result')
const activeSecondaryTab = ref<SecondaryTab>('value')
const scoreHelpOpen = ref(false)
const selectedMetricCode = computed({
  get: () => props.selectedMetricCode || 'heartRate',
  set: (code: string) => emit('update:selectedMetricCode', code),
})

const succeededRecords = computed(() =>
  props.records.filter((record) => record.status === 'SUCCEEDED'),
)
const latest = computed(() => succeededRecords.value[0])
const availableMetrics = computed(() => {
  if (!latest.value) return []
  return metricDefinitions.filter((metric) => metric.available(latest.value as HealthScanResult))
})
const selectedMetric = computed(
  () =>
    availableMetrics.value.find((metric) => metric.code === selectedMetricCode.value) ||
    availableMetrics.value[0] ||
    metricDefinitions[0],
)
const selectedAssessment = computed(() => assessMetric(selectedMetric.value))
const selectedMarkerPosition = computed(() =>
  latest.value ? selectedMetric.value.marker(latest.value) : 50,
)
const abnormalMetrics = computed(() =>
  availableMetrics.value.filter((metric) => assessMetric(metric).tone !== 'normal'),
)
const normalMetricCount = computed(
  () => availableMetrics.value.length - abnormalMetrics.value.length,
)
const healthScore = computed(() => {
  if (latest.value?.healthScore !== undefined && latest.value.healthScore !== null) {
    return Number(latest.value.healthScore)
  }
  if (!availableMetrics.value.length) return 0
  const penalty = availableMetrics.value.reduce((sum, metric) => {
    const tone = assessMetric(metric).tone
    return sum + (tone === 'danger' ? 18 : tone === 'attention' ? 8 : 0)
  }, 0)
  return Math.max(45, Math.round(100 - penalty))
})
const peerPercentile = computed(() => {
  if (latest.value?.peerPercentile !== undefined && latest.value.peerPercentile !== null) {
    return Math.max(1, Math.min(99, Number(latest.value.peerPercentile)))
  }
  return Math.max(5, Math.min(95, Math.round(((healthScore.value - 50) * 82) / 45)))
})
const healthScoreStatus = computed(() => {
  if (healthScore.value >= 90) return '状态良好'
  if (healthScore.value >= 75) return '建议关注'
  return '需要重视'
})
const assessmentSummary = computed(() => {
  if (!latest.value) return ''
  if (!abnormalMetrics.value.length) {
    return `本次获得 ${availableMetrics.value.length} 项有效体征，整体处于常用参考范围。建议保持现有健康习惯并继续观察长期趋势。`
  }
  const names = abnormalMetrics.value.map((metric) => metric.name).join('、')
  return `本次获得 ${availableMetrics.value.length} 项有效体征，其中 ${names} 需要关注。建议先规范复测，并结合身体感受和历史变化综合判断。`
})
const selectedTrend = computed(() => {
  const metric = selectedMetric.value
  const points = succeededRecords.value
    .filter((record) => metric.available(record))
    .slice(0, 7)
    .reverse()
  const values = points
    .map((record) => metric.numeric(record))
    .filter((value): value is number => value !== undefined)
  const min = values.length ? Math.min(...values) : 0
  const max = values.length ? Math.max(...values) : 0
  return points.map((record) => {
    const value = metric.numeric(record) || 0
    const height = max === min ? 62 : 28 + ((value - min) / (max - min)) * 62
    return {
      id: record.id,
      label: metric.display(record),
      date: shortDate(record.completedAt || record.createdAt),
      height,
      value,
    }
  })
})
const trendSummary = computed(() => {
  const points = selectedTrend.value
  if (points.length < 2) return '等待更多数据'
  const first = points[0].value
  const last = points[points.length - 1].value
  if (Math.abs(last - first) < Math.max(0.01, Math.abs(first) * 0.03)) return '整体平稳'
  return last > first ? '近期上升' : '近期下降'
})

watch(
  availableMetrics,
  (metrics) => {
    if (metrics.length && !metrics.some((metric) => metric.code === selectedMetricCode.value)) {
      selectedMetricCode.value = metrics[0].code
    }
  },
  { immediate: true },
)
watch(
  () => props.selectedMetricCode,
  (code) => {
    if (code && availableMetrics.value.length && !availableMetrics.value.some((metric) => metric.code === code)) {
      emit('update:selectedMetricCode', availableMetrics.value[0].code)
    }
  },
  { immediate: true },
)

function assessMetric(metric: MetricDefinition) {
  if (!latest.value) {
    return normalAssessment('暂无数据', '暂无有效数据', '', '')
  }
  return metric.assess(latest.value)
}

function openMetric(code: string, tab: SecondaryTab = 'value') {
  selectedMetricCode.value = code
  activePrimaryTab.value = 'result'
  activeSecondaryTab.value = tab
  setTimeout(() => {
    uni.pageScrollTo({ selector: '#health-scan-analysis', offsetTop: -12, duration: 260 })
  }, 50)
}

function hasValue(value?: number) {
  return value !== undefined && value !== null && Number.isFinite(Number(value))
}

function numberValue(value?: number) {
  return hasValue(value) ? Number(value) : undefined
}

function integer(value?: number) {
  return hasValue(value) ? Number(value).toFixed(0) : '--'
}

function decimal(value?: number) {
  return hasValue(value) ? Number(value).toFixed(2).replace(/\.?0+$/, '') : '--'
}

function normalize(value: number | undefined, min: number, max: number) {
  if (value === undefined) return 50
  return Math.max(2, Math.min(98, ((value - min) / (max - min)) * 100))
}

function formatTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

function shortDate(value?: string) {
  if (!value) return ''
  const normalized = value.replace('T', ' ')
  return normalized.slice(5, 10).replace('-', '/')
}
</script>

<style scoped>
.dashboard {
  margin-top: 24rpx;
}
.primary-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 8rpx;
  border: 1rpx solid #d9e8e3;
  border-radius: 28rpx;
  background: rgba(232, 244, 240, 0.9);
}
.primary-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 74rpx;
  border-radius: 22rpx;
  color: #71837d;
  font-size: 27rpx;
  font-weight: 700;
}
.primary-tab.active {
  background: #fff;
  color: #08715b;
  box-shadow: 0 8rpx 24rpx rgba(25, 92, 74, 0.12);
}
.result-panel,
.assessment-panel {
  margin-top: 18rpx;
}
.metric-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 76rpx;
}
.metric-heading {
  display: flex;
  align-items: center;
  min-height: 66rpx;
  color: #173a32;
}
.metric-switch-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46rpx;
  height: 46rpx;
  border-radius: 15rpx;
  background: #def3eb;
  color: #08715b;
  font-size: 21rpx;
  font-weight: 760;
}
.metric-switch-name {
  margin-left: 12rpx;
  font-size: 28rpx;
  font-weight: 760;
}
.metric-time {
  color: #82918c;
  font-size: 21rpx;
}
.secondary-tabs {
  display: flex;
  overflow: hidden;
  margin-top: 16rpx;
  padding: 6rpx;
  border-radius: 24rpx;
  background: #eaf4f0;
}
.secondary-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 64rpx;
  border-radius: 19rpx;
  color: #758781;
  font-size: 23rpx;
  font-weight: 680;
}
.secondary-tab.active {
  background: #08715b;
  color: #fff;
  box-shadow: 0 6rpx 16rpx rgba(8, 113, 91, 0.18);
}
.metric-result-card,
.content-card,
.assessment-card {
  margin-top: 16rpx;
  padding: 30rpx;
  border: 1rpx solid #dae8e3;
  border-radius: 30rpx;
  background: #fff;
  box-shadow: 0 12rpx 30rpx rgba(29, 81, 67, 0.06);
}
.result-card-head,
.content-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.result-card-label,
.content-title,
.assessment-title {
  color: #173a32;
  font-size: 30rpx;
  font-weight: 760;
}
.content-copy {
  margin-top: 5rpx;
  color: #87958f;
  font-size: 21rpx;
}
.result-card-time {
  margin-top: 5rpx;
  color: #87958f;
  font-size: 21rpx;
}
.metric-status,
.assessment-metric-status {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 680;
}
.status-normal {
  background: #e1f6ed;
  color: #08715b;
}
.status-attention {
  background: #fff1d5;
  color: #b16b00;
}
.status-danger {
  background: #ffe4e2;
  color: #bf3f35;
}
.metric-value-row {
  display: flex;
  align-items: baseline;
  justify-content: center;
  margin: 34rpx 0 28rpx;
}
.metric-main-value {
  color: #103d32;
  font-size: 72rpx;
  font-weight: 820;
  letter-spacing: -3rpx;
}
.metric-unit {
  margin-left: 10rpx;
  color: #758781;
  font-size: 24rpx;
  font-weight: 650;
}
.range-track {
  position: relative;
  display: flex;
  gap: 5rpx;
  height: 18rpx;
  margin: 0 8rpx;
}
.range-section {
  height: 100%;
  border-radius: 999rpx;
}
.range-marker {
  position: absolute;
  top: -18rpx;
  width: 0;
  height: 52rpx;
}
.range-marker-tip {
  width: 0;
  height: 0;
  border-top: 16rpx solid #173a32;
  border-right: 10rpx solid transparent;
  border-left: 10rpx solid transparent;
  transform: translateX(-50%);
}
.range-labels {
  display: flex;
  justify-content: space-around;
  margin-top: 15rpx;
  color: #70847d;
  font-size: 19rpx;
}
.range-label {
  display: flex;
  align-items: center;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  text-align: center;
}
.range-label-name {
  color: #526e65;
  font-size: 20rpx;
  font-weight: 700;
}
.range-threshold {
  margin-top: 4rpx;
  color: #8b9994;
  font-size: 16rpx;
  line-height: 1.2;
  white-space: nowrap;
}
.result-guidance {
  margin-top: 26rpx;
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: #eef8f4;
  color: #31584d;
  font-size: 24rpx;
  line-height: 1.65;
}
.trend-summary {
  padding: 9rpx 14rpx;
  border-radius: 999rpx;
  background: #e7f5ef;
  color: #08715b;
  font-size: 21rpx;
  font-weight: 680;
}
.trend-chart {
  display: flex;
  align-items: flex-end;
  height: 330rpx;
  margin-top: 26rpx;
  padding: 20rpx 10rpx 0;
  border-radius: 22rpx;
  background: linear-gradient(180deg, #f5faf8, #fff);
}
.trend-column {
  display: flex;
  align-items: center;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}
.trend-value {
  color: #2f5449;
  font-size: 18rpx;
  font-weight: 720;
  white-space: nowrap;
}
.trend-bar-area {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  flex: 1;
  width: 100%;
  margin: 9rpx 0;
}
.trend-bar {
  width: 24rpx;
  min-height: 20rpx;
  border-radius: 999rpx 999rpx 6rpx 6rpx;
  background: linear-gradient(180deg, #69cfb0, #08715b);
  transition: height 0.25s ease;
}
.trend-date {
  color: #8a9994;
  font-size: 17rpx;
}
.empty-trend {
  margin-top: 24rpx;
  padding: 42rpx 20rpx;
  border-radius: 20rpx;
  background: #f4f8f6;
  color: #71837d;
  font-size: 23rpx;
  line-height: 1.6;
  text-align: center;
}
.explain-copy {
  margin-top: 18rpx;
  color: #526b64;
  font-size: 25rpx;
  line-height: 1.8;
}
.explain-block {
  margin-top: 22rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid #e4ece9;
}
.explain-label {
  color: #08715b;
  font-size: 24rpx;
  font-weight: 730;
}
.explain-text {
  margin-top: 8rpx;
  color: #536d65;
  font-size: 24rpx;
  line-height: 1.7;
}
.risk-safe,
.risk-warning {
  display: flex;
  margin-top: 20rpx;
  padding: 24rpx;
  border-radius: 22rpx;
}
.risk-safe {
  align-items: center;
  background: #eaf8f2;
}
.risk-safe-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 58rpx;
  height: 58rpx;
  margin-right: 16rpx;
  border-radius: 50%;
  background: #08715b;
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
}
.risk-safe-title,
.risk-warning-title {
  color: #173a32;
  font-size: 27rpx;
  font-weight: 760;
}
.risk-safe-copy,
.risk-warning-copy,
.risk-action {
  margin-top: 7rpx;
  color: #547069;
  font-size: 23rpx;
  line-height: 1.65;
}
.risk-warning {
  flex-direction: column;
  border: 1rpx solid #f0d89f;
  background: #fff8e8;
}
.risk-action {
  padding-top: 14rpx;
  border-top: 1rpx solid #f0dfb9;
  color: #875b14;
}
.result-note {
  margin-top: 20rpx;
  color: #87958f;
  font-size: 21rpx;
  line-height: 1.6;
  text-align: center;
}
.score-card {
  position: relative;
  display: grid;
  align-items: center;
  grid-template-columns: minmax(188rpx, 0.8fr) minmax(318rpx, 1.48fr);
  min-height: 250rpx;
  padding: 28rpx 24rpx 24rpx 30rpx;
  overflow: hidden;
  border-radius: 32rpx;
  border: 1rpx solid rgba(15, 129, 102, 0.14);
  background:
    radial-gradient(circle at 102% -8%, rgba(73, 202, 164, 0.16), transparent 42%),
    linear-gradient(145deg, #ffffff 0%, #f5fbf8 100%);
  color: #173a32;
  box-shadow: 0 18rpx 42rpx rgba(25, 94, 76, 0.1);
}
.score-summary {
  position: relative;
  z-index: 2;
  min-width: 0;
}
.score-caption-row {
  display: flex;
  align-items: center;
  gap: 9rpx;
}
.score-caption {
  color: #173c33;
  font-size: 28rpx;
  font-weight: 800;
}
.score-help {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34rpx;
  height: 34rpx;
  border: 2rpx solid #b3bab7;
  border-radius: 50%;
  color: #89928f;
  font-size: 19rpx;
  font-weight: 800;
}
.score-help-panel {
  margin-top: 12rpx;
  padding: 18rpx 22rpx;
  border: 1rpx solid rgba(13, 129, 101, 0.12);
  border-radius: 18rpx;
  background: #edf8f4;
  color: #496c62;
  font-size: 21rpx;
  line-height: 1.65;
}
.score-value {
  margin-top: 12rpx;
  color: #0b876c;
  font-size: 88rpx;
  font-weight: 850;
  line-height: 1;
  letter-spacing: -3rpx;
}
.score-status {
  display: inline-flex;
  margin-top: 12rpx;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #def5ec;
  color: #08715b;
  font-size: 22rpx;
  font-weight: 750;
}
.peer-comparison {
  margin-top: 18rpx;
  color: #6b817a;
  font-size: 20rpx;
  line-height: 1.55;
}
.peer-percent {
  margin: 0 3rpx;
  color: #0b876c;
  font-size: 25rpx;
  font-weight: 820;
}
.score-gauge {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  height: 166rpx;
  margin-top: 2rpx;
}
.score-gauge::before {
  position: absolute;
  top: 8rpx;
  bottom: 0;
  left: 0;
  width: 1rpx;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(15, 129, 102, 0.16) 22%,
    rgba(15, 129, 102, 0.16) 78%,
    transparent 100%
  );
  content: '';
}
.score-gauge-shell {
  position: relative;
  z-index: 1;
  width: 318rpx;
  height: 160rpx;
  overflow: hidden;
}
.score-gauge-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 318rpx;
  height: 318rpx;
  border-radius: 50%;
  background: conic-gradient(
    from 270deg,
    #08745d 0deg,
    #54d0ae var(--score-angle),
    #e2eeea var(--score-angle) 180deg,
    transparent 180deg 360deg
  );
  filter: drop-shadow(0 8rpx 12rpx rgba(11, 135, 108, 0.16));
}
.score-gauge-ring::after {
  position: absolute;
  top: 36rpx;
  left: 36rpx;
  width: 246rpx;
  height: 246rpx;
  border-radius: 50%;
  background: linear-gradient(180deg, #fbfefc 0%, #f5fbf8 100%);
  content: '';
}
.score-gauge-center {
  position: absolute;
  z-index: 3;
  top: 80rpx;
  left: 0;
  width: 100%;
  text-align: center;
}
.score-gauge-label {
  color: #82958e;
  font-size: 19rpx;
  line-height: 1.2;
}
.score-gauge-status {
  margin-top: 7rpx;
  color: #0a765f;
  font-size: 25rpx;
  font-weight: 800;
  line-height: 1.2;
}
.assessment-copy {
  margin-top: 16rpx;
  color: #536d65;
  font-size: 24rpx;
  line-height: 1.75;
}
.assessment-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 24rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid #e3ece9;
  text-align: center;
}
.assessment-stat-value {
  color: #08715b;
  font-size: 34rpx;
  font-weight: 790;
}
.assessment-stat-value.warning {
  color: #b06a00;
}
.assessment-stat-label {
  margin-top: 4rpx;
  color: #81918c;
  font-size: 20rpx;
}
.assessment-metric {
  display: grid;
  grid-template-columns: 58rpx minmax(0, 1fr) 184rpx;
  column-gap: 16rpx;
  align-items: center;
  min-height: 132rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #e5eeeb;
}
.assessment-metric:last-child {
  border-bottom: 0;
}
.assessment-metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 58rpx;
  height: 58rpx;
  border-radius: 18rpx;
  background: #e0f3ec;
  color: #08715b;
  font-size: 22rpx;
  font-weight: 780;
}
.assessment-metric-main {
  min-width: 0;
}
.assessment-metric-name {
  color: #193e35;
  font-size: 27rpx;
  font-weight: 760;
  line-height: 1.35;
}
.assessment-metric-copy {
  margin-top: 6rpx;
  color: #7a8d86;
  font-size: 19rpx;
  line-height: 1.45;
}
.assessment-metric-value {
  min-width: 0;
  padding: 4rpx 0 4rpx 18rpx;
  border-left: 1rpx solid #dce9e5;
  color: #183f35;
  text-align: right;
}
.assessment-metric-reading {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  min-height: 38rpx;
  white-space: nowrap;
}
.assessment-metric-number {
  font-size: 31rpx;
  font-weight: 820;
  letter-spacing: 0.2rpx;
  line-height: 1.15;
}
.assessment-metric-unit {
  margin-left: 5rpx;
  color: #758881;
  font-size: 16rpx;
  font-weight: 650;
  line-height: 1;
  white-space: nowrap;
}
.assessment-metric-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 38rpx;
  margin-top: 6rpx;
}
.assessment-metric-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  min-height: 30rpx;
  padding: 3rpx 12rpx;
  font-size: 16rpx;
  line-height: 1.25;
}
.assessment-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32rpx;
  height: 32rpx;
  margin-left: 10rpx;
  border-radius: 50%;
  background: #e7f4ef;
  color: #197c67;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 1;
}
.all-normal {
  margin-top: 18rpx;
  padding: 22rpx;
  border-radius: 20rpx;
  background: #ebf8f3;
  color: #45675e;
  font-size: 24rpx;
  line-height: 1.65;
}
.attention-item {
  display: flex;
  align-items: center;
  min-height: 112rpx;
  border-bottom: 1rpx solid #ece6d9;
}
.attention-item:last-child {
  border-bottom: 0;
}
.attention-index {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 54rpx;
  height: 54rpx;
  border-radius: 18rpx;
  background: #fff0d5;
  color: #aa6500;
  font-size: 26rpx;
  font-weight: 820;
}
.attention-content {
  min-width: 0;
  margin-left: 16rpx;
}
.attention-title {
  color: #473b2a;
  font-size: 25rpx;
  font-weight: 730;
}
.attention-copy {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 5rpx;
  color: #82725b;
  font-size: 20rpx;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
</style>

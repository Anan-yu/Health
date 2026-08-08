<template>
  <view class="page report-page elder-page">
    <PageState :loading="loading" :error="error" :empty="!report">
      <view v-if="report" class="report-hero">
        <view class="eyebrow">HEALTH REPORT</view>
        <view class="hero-title">本次健康评估报告</view>
        <view class="hero-summary">{{ overallSummary }}</view>
        <view class="hero-meta">由健康档案、检验报告、面部检测结果综合评估</view>
      </view>

      <CareFeedbackCard
        v-if="report"
        :title="reportFeedback.title"
        :message="reportFeedback.message"
        :detail="reportFeedback.detail"
        :icon="reportFeedback.icon"
        :tone="reportFeedback.tone"
      />

      <view v-if="isFallback" class="fallback-card">
        <view class="fallback-title">本次大模型综合解读未完成</view>
        <view class="fallback-copy">
          当前展示的是保守规则结果，不代表大模型已经完成综合分析。原始检查结果仍可正常查看。
        </view>
        <button
          v-if="isCustomer"
          class="retry-button"
          :loading="reassessing"
          :disabled="reassessing"
          @click="reassess"
        >
          {{ reassessing ? '正在重新生成…' : '重新生成 AI 解读' }}
        </button>
      </view>

      <view class="section-head"><view class="title">整体健康状态</view></view>
      <view class="card status-card">
        <view v-for="item in statusOverview" :key="item.label" class="status-row">
          <text>{{ item.label }}</text
          ><text :class="`status ${item.level}`">{{ item.text }}</text>
        </view>
      </view>

      <view class="section-head"
        ><view class="eyebrow">FOCUS</view><view class="title">本次重点发现</view></view
      >
      <view v-if="priorityItems.length" class="card evidence-card">
        <view v-for="item in priorityItems" :key="item" class="evidence-item">{{ item }}</view>
      </view>
      <view v-else-if="hasEffectiveData" class="card good-card">
        当前已确认的数据未提示需要优先改善的健康问题。</view
      >
      <view v-else class="card good-card">当前数据不足，请补充资料后再进行健康评估。</view>

      <view v-if="abnormalExplanations.length" class="section-head">
        <view class="eyebrow">EXPLAIN</view><view class="title">异常结果解释</view>
      </view>
      <view v-if="abnormalExplanations.length" class="card abnormal-card">
        <view
          v-for="item in abnormalExplanations"
          :key="`${item.title}-${item.explanation}`"
          class="abnormal-explanation"
        >
          <view class="abnormal-title">{{ item.title }}</view>
          <view v-if="item.finding" class="abnormal-finding">{{ item.finding }}</view>
          <view class="abnormal-copy"
            ><text class="abnormal-label">这说明什么：</text>{{ item.explanation }}</view
          >
          <view class="abnormal-copy"
            ><text class="abnormal-label">可能影响的器官或系统：</text
            >{{ item.possibleImpacts }}</view
          >
          <view class="abnormal-copy"
            ><text class="abnormal-label">下一步建议：</text>{{ item.nextStep }}</view
          >
        </view>
        <view class="abnormal-note">以上为健康管理和辅助参考，单次异常不能判断器官损害或确诊疾病。</view>
      </view>

      <view class="section-head"
        ><view class="eyebrow">REASON</view><view class="title">为什么需要关注</view></view
      >
      <view class="card evidence-card">
        <template v-if="whyItems.length">
          <view v-for="item in whyItems" :key="item" class="evidence-item">{{ item }}</view>
        </template>
        <view v-else class="muted">当前没有可进一步说明的异常依据。</view>
        <view class="data-action" @click="openLabReport">{{
          openingOriginal ? '正在打开…' : '查看原检验报告'
        }}</view>
      </view>

      <view v-if="interpretation?.diagnosticReferences?.length" class="section-head">
        <view class="eyebrow">REFERENCE</view><view class="title">疾病推断参考</view>
      </view>
      <view v-if="interpretation?.diagnosticReferences?.length" class="card evidence-card">
        <view
          v-for="item in interpretation.diagnosticReferences"
          :key="item.conditionName"
          class="diagnostic-reference"
        >
          <view class="diagnostic-line">
            <text>可能疾病：</text><text class="diagnostic-strong">{{ cleanHealthText(item.conditionName) }}</text>
          </view>
          <view class="diagnostic-copy">推断依据：{{ cleanHealthText(item.rationale) }}</view>
          <view v-if="item.recommendedDepartment" class="diagnostic-line">
            <text>建议咨询科室：</text
            ><text class="diagnostic-strong">{{ cleanHealthText(item.recommendedDepartment) }}</text>
          </view>
          <view class="diagnostic-plan">
            <view class="diagnostic-plan-title">疾病治疗方案</view>
            <view v-for="plan in treatmentPlanFor(item)" :key="plan" class="diagnostic-copy">• {{ plan }}</view>
          </view>
          <view class="diagnostic-plan integrated-treatment-plan">
            <view class="diagnostic-plan-title">中西医结合治疗建议</view>
            <view
              v-for="plan in integratedTreatmentFor(item)"
              :key="plan.label"
              class="diagnostic-copy"
            >
              <text class="integrated-treatment-label">{{ plan.label }}：</text>{{ plan.text }}
            </view>
          </view>
          <view class="diagnostic-plan">
            <view class="diagnostic-plan-title">营养干预修复方案</view>
            <view v-for="plan in nutritionPlanFor(item)" :key="plan" class="diagnostic-copy">• {{ plan }}</view>
          </view>
        </view>
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

      <view class="section-head"
        ><view class="eyebrow">MISSING</view><view class="title">需要补充的数据</view></view
      >
      <view class="card evidence-card">
        <template v-if="missingItems.length">
          <view v-for="item in missingItems" :key="item" class="evidence-item">{{ item }}</view>
        </template>
        <view v-else class="muted">当前没有额外的重点补充项。</view>
      </view>

      <view class="section-head"
        ><view class="eyebrow">LIMIT</view><view class="title">当前不能说明什么</view></view
      >
      <view class="card good-card">{{ uncertaintyText }}</view>

      <view class="section-head"
        ><view class="eyebrow">SOURCE</view><view class="title">数据来源与限制</view></view
      >
      <view class="card good-card">
        {{ sourceLimitText }}
        <view class="report-disclaimer">{{
          interpretation?.disclaimer || assessment?.disclaimer
        }}</view>
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
import { createAssessment } from '@/api/assessment'
import {
  getHealthReport,
  getHealthReports,
} from '@/api/health-report'
import { getReportFiles } from '@/api/lab-report'
import {
  downloadProtectedFileInBrowser,
  getApiBaseUrl,
  getRequestHeaders,
  openProtectedFileInBrowser,
} from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import type { Assessment, HealthReport } from '@/types/api'
import { cleanHealthText } from '@/utils/health-text'
import PageState from '@/components/PageState.vue'
import CareFeedbackCard from '@/components/CareFeedbackCard.vue'

type Focus = {
  code: string
  title: string
  level: 'attention' | 'high'
  description: string
  next: string
  evidence: string[]
  recommendations: string[]
}
type DiagnosticReference = {
  conditionName: string
  recommendedDepartment?: string
  treatmentPlan?: string[]
  nutritionInterventionPlan?: string[]
  westernMedicineApproach?: string[]
  traditionalChineseMedicineApproach?: string[]
  westernMedicineMedicationPlan?: string[]
  traditionalChineseMedicineMedicationPlan?: string[]
  integratedTreatmentNotes?: string[]
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
  reassessing = ref(false),
  openingOriginal = ref(false),
  error = ref('')
const auth = useAuthStore()
const isCustomer = computed(() => auth.currentWorkbench === 'CUSTOMER')
const assessment = computed<Assessment | undefined>(() => report.value?.assessment)
const interpretation = computed(() => assessment.value?.results?.interpretation)
const isFallback = computed(() => interpretation.value?.source === 'RULE_FALLBACK')
const allResults = computed(() => assessment.value?.results?.results || [])
const evaluated = computed(() => allResults.value.filter((item) => item.status === 'EVALUATED'))
const hasEffectiveData = computed(() => evaluated.value.length > 0)
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
const priorityItems = computed(() =>
  (interpretation.value?.priorityConcerns || []).map(cleanHealthText).filter(Boolean),
)
const abnormalExplanations = computed(() =>
  (interpretation.value?.abnormalExplanations || [])
    .map((item) => ({
      title: cleanHealthText(item.title),
      finding: cleanHealthText(item.finding || ''),
      explanation: cleanHealthText(item.explanation),
      possibleImpacts: cleanHealthText(item.possibleImpacts),
      nextStep: cleanHealthText(item.nextStep),
    }))
    .filter((item) => item.title && item.explanation && item.possibleImpacts && item.nextStep),
)
const overallSummary = computed(() => {
  const aiSummary = cleanHealthText(interpretation.value?.summary || '')
  if (aiSummary) return aiSummary
  if (concerns.value.length) return `本次有 ${concerns.value.length} 个健康方向需要关注。`
  return hasEffectiveData.value
    ? '当前已确认的数据未触发重点关注规则，建议按需复评。'
    : '当前数据不足以形成有效健康结论，请补充资料后复评。'
})
const reportFeedback = computed<{
  title: string
  message: string
  detail: string
  icon: string
  tone: 'life' | 'warm'
}>(() =>
  priorityItems.value.length || concerns.value.length
    ? {
        title: '看见需要关注的方向，是改善健康的第一步',
        message: '不必一次改变所有事情，先从最容易执行的一项行动开始。',
        detail: '持续记录与复评，才能更准确地看见身体状态的变化。',
        icon: '向',
        tone: 'warm',
      }
    : {
        title: '继续保持，让良好状态成为日常',
        message: '当前已确认的数据整体平稳，规律生活和持续记录同样重要。',
        detail: '愿每一次了解自己，都让未来的健康更从容。',
        icon: '心',
        tone: 'life',
      },
)
const statusOverview = computed(() => {
  const groups = [
    ['体重与代谢', ['BODY_COMPOSITION', 'GLUCOSE_METABOLISM']],
    ['心血管健康', ['LIPID_CARDIOVASCULAR']],
    ['肝肾基础状态', ['LIVER_METABOLIC', 'KIDNEY_ELECTROLYTE']],
    ['营养与饮食', ['NUTRITION_MICRONUTRIENT']],
    ['睡眠与恢复', ['HPA_ADRENAL']],
  ] as const
  return groups.map(([label, codes]) => {
    const matching = allResults.value.filter((item) =>
      (codes as readonly string[]).includes(item.modelCode),
    )
    const items = matching.filter((item) => item.status === 'EVALUATED')
    const high = items.some((item) => item.riskLevel === 'HIGH'),
      attention = items.some((item) => item.riskLevel === 'ATTENTION')
    if (!matching.length || !items.length) {
      return { label, level: 'insufficient', text: '数据不足' }
    }
    return {
      label,
      level: high ? 'high' : attention ? 'attention' : 'good',
      text: high ? '重点关注' : attention ? '建议改善' : '当前平稳',
    }
  })
})
const whyItems = computed(() => {
  const interpreted = (interpretation.value?.crossModelFindings || [])
    .map((item) => cleanHealthText(item.explanation))
    .filter(Boolean)
  const evidence = concerns.value.flatMap((item) => item.evidence)
  return [
    ...new Set([...interpreted, ...evidence].filter((item) => !item.includes('未触发'))),
  ].slice(0, 8)
})
const planItems = (plans?: string[]) => (plans || []).map(cleanHealthText).filter(Boolean)
const treatmentPlanFor = (item: DiagnosticReference) => {
  const plans = planItems(item.treatmentPlan)
  if (plans.length) return plans
  const department = cleanHealthText(item.recommendedDepartment || '') || '相关专科'
  return [`请由${department}结合症状、检查小结和必要复查明确后续诊疗路径，不要依据本报告自行用药或调整治疗。`]
}
const nutritionPlanFor = (item: DiagnosticReference) => {
  const plans = planItems(item.nutritionInterventionPlan)
  return plans.length
    ? plans
    : ['建议由临床营养师或相关专科结合体重、肝肾功能、过敏史、当前用药和复查结果制定个体化饮食方案。']
}
const isGenericTcmMedicationReference = (text: string) =>
  !text ||
  [
    '本次证据未支持',
    '未支持具体方药',
    '未支持具体药物',
    '具体方药由',
    '具体方药方向',
    '不自行购药或叠加中药',
  ].some((marker) => text.includes(marker))
const tcmMedicationReferenceFor = (condition: string, currentText: string) => {
  if (!isGenericTcmMedicationReference(currentText)) return currentText
  if (condition.includes('幽门螺杆菌') || condition.includes('胃炎') || condition.includes('消化不良')) {
    return '若中医辨证属于寒热互结之痞证，可与中医师讨论半夏泻心汤颗粒或半夏泻心汤类方；该方向用于胃脘痞满、脾胃不和等证候的中医调理，不替代幽门螺杆菌规范根除治疗，具体方药须由医师辨证处方并核对过敏史、当前用药。'
  }
  if (condition.includes('粥样硬化') || condition.includes('斑块') || condition.includes('血脂') || condition.includes('心血管')) {
    return '如辨证符合痰瘀阻滞且确需中成药辅助，可与心内科或中医师讨论血脂康胶囊等调脂类中成药；血脂康含天然他汀样成分，若正在使用他汀或存在肝酶、肌酶异常，不得自行叠加，须由医生先核对相互作用和复查指标。'
  }
  if (condition.includes('脂肪肝') || condition.includes('脂肪性肝病') || condition.includes('肝脏与代谢')) {
    return '如辨证属于湿热中阻且评估符合脂肪性肝病管理方向，可与肝病科或中医师讨论化滞柔肝颗粒等中成药；须先结合肝功能、饮酒、现用药和证候评估，由医生决定是否使用，不能自行购买。'
  }
  return currentText || '当前证据未支持可安全列出的具体中药名称，请先由中医师结合证候、过敏史和现用药辨证处方。'
}
const integratedTreatmentFor = (item: DiagnosticReference) => {
  const western = planItems(item.westernMedicineApproach)
  const traditionalChinese = planItems(item.traditionalChineseMedicineApproach)
  const westernMedications = planItems(item.westernMedicineMedicationPlan)
  const traditionalChineseMedications = planItems(item.traditionalChineseMedicineMedicationPlan)
  const condition = cleanHealthText(item.conditionName || '')
  const department = cleanHealthText(item.recommendedDepartment || '') || '相关专科'
  const isHelicobacter = condition.includes('幽门螺杆菌')
  const isAtherosclerosis = condition.includes('粥样硬化') || condition.includes('斑块')
  return [
    {
      label: '西医治疗思路',
      text:
        western.join('；') ||
        (isHelicobacter
          ? `请由${department}复核呼气试验、根除适应证、过敏史和既往抗菌药使用。`
          : isAtherosclerosis
            ? `请由${department}结合血脂、血压、糖代谢和整体心血管风险分层。`
            : `请由${department}结合检查结果、症状和复查结果评估西医治疗路径。`),
    },
    {
      label: '西医药物治疗参考',
      text:
        westernMedications.join('；') ||
        (isHelicobacter
          ? '如复核确认需要根除，医生通常在含铋四联方案中选择抗菌药、铋剂和抑酸药组合，具体药物与疗程必须由消化内科处方。'
          : isAtherosclerosis
            ? '医生可根据低密度脂蛋白胆固醇和总体风险评估是否需要他汀类降脂药；抗血小板药仅在明确适应证时考虑。'
            : '本次证据未支持具体药物名称，由相关专科结合诊断和禁忌证决定是否需要处方。'),
    },
    {
      label: '中医治疗思路',
      text:
        traditionalChinese.join('；') ||
        (isHelicobacter
          ? '先由中医师辨证判断脾胃湿热、脾胃虚弱等证候，再决定是否适合中医辅助调理。'
          : isAtherosclerosis
            ? '如需中医辅助管理，由中医师结合痰湿、血瘀等证候辨证评估。'
            : '如考虑中医干预，请由中医师辨证评估体质和症状后制定方案。'),
    },
    {
      label: '中医药物/治法参考',
      text:
        tcmMedicationReferenceFor(condition, traditionalChineseMedications.join('；')) ||
        (isHelicobacter
          ? '可围绕清热化湿或健脾和胃等治法选择药物方向，具体方药由中医师辨证开具。'
          : isAtherosclerosis
            ? '可围绕化痰祛瘀、调理脾胃等治法制定辅助方案，具体方药由中医师开具。'
            : '本次证据未支持具体方药方向，不自行购药或叠加中药。'),
    },
  ]
}
const directions = computed(() => {
  const generated = (interpretation.value?.recommendations || [])
    .map(cleanHealthText)
    .filter(Boolean)
  const result = [
    ...new Set(
      generated.length ? generated : concerns.value.flatMap((item) => item.recommendations),
    ),
  ].slice(0, 5)
  return result.length ? result : ['补充有效数据后，再制定与本次重点问题对应的健康行动。']
})
const missingItems = computed(() => [
  ...new Set((interpretation.value?.missingDataAdvice || []).map(cleanHealthText).filter(Boolean)),
])
const uncertaintyText = computed(
  () =>
    cleanHealthText(interpretation.value?.uncertainty || '') ||
    '本报告仅覆盖当前已提供的数据，不能据此诊断疾病或决定药物治疗。',
)
const sourceLimitText = computed(() =>
  assessment.value?.results?.interpretation
    ? '结论来自结构化检验报告、健康档案、问卷和辅助检测；健康拍摄像头估算仅供趋势参考，不能替代医疗设备测量。'
    : '当前仅使用已提供的结构化资料，缺失信息不会被推断为正常。',
)
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
const openLabReport = async () => {
  const reportId = assessment.value?.reportId
  if (!reportId || openingOriginal.value) return
  openingOriginal.value = true
  try {
    const files = await getReportFiles(reportId)
    const file = files[0]
    if (!file) {
      uni.showToast({ title: '未找到原检验报告文件', icon: 'none' })
      return
    }
    // #ifdef H5
    await openProtectedFileInBrowser(
      `/api/v1/lab-reports/${reportId}/files/${file.id}/content`,
      '_self',
    )
    // #endif
    // #ifdef MP-WEIXIN
    uni.showLoading({ title: '正在打开' })
    uni.downloadFile({
      url: `${getApiBaseUrl()}/api/v1/lab-reports/${reportId}/files/${file.id}/content`,
      header: getRequestHeaders(),
      success: ({ tempFilePath, statusCode }) => {
        if (statusCode !== 200) {
          uni.showToast({ title: '原检验报告打开失败', icon: 'none' })
          return
        }
        if (file.mimeType?.startsWith('image/')) {
          uni.previewImage({ urls: [tempFilePath], current: tempFilePath })
          return
        }
        uni.openDocument({
          filePath: tempFilePath,
          fileType: 'pdf',
          showMenu: true,
          fail: () => uni.showToast({ title: '文件打开失败', icon: 'none' }),
        })
      },
      fail: () => uni.showToast({ title: '原检验报告下载失败', icon: 'none' }),
      complete: () => uni.hideLoading(),
    })
    // #endif
  } catch (cause) {
    uni.showToast({
      title: cause instanceof Error ? cause.message : '原检验报告打开失败',
      icon: 'none',
    })
  } finally {
    openingOriginal.value = false
  }
}
const openFollowup = () => uni.navigateTo({ url: '/pages-customer/followup/index' })
const reassess = () => {
  const labReportId = assessment.value?.reportId
  if (!labReportId || !report.value || reassessing.value) return
  uni.showModal({
    title: '重新生成 AI 解读',
    content: '将使用当前健康档案、检查报告和面部健康检测重新评估，并生成一份新的健康报告。是否继续？',
    success: async ({ confirm }) => {
      if (!confirm || !report.value) return
      reassessing.value = true
      try {
        const generated = await createAssessment(labReportId)
        const reports = await getHealthReports(report.value.patientId)
        const generatedReport = reports.records.find(
          (item) => item.assessment?.id === generated.id,
        )
        if (!generatedReport) {
          uni.showToast({ title: '评估已生成，请稍后在报告列表查看', icon: 'none' })
          return
        }
        uni.redirectTo({ url: `/pages-customer/health-report/detail?id=${generatedReport.id}` })
      } catch (cause) {
        uni.showToast({
          title: cause instanceof Error ? cause.message : '重新生成失败，请稍后重试',
          icon: 'none',
        })
      } finally {
        reassessing.value = false
      }
    },
  })
}
const download = async () => {
  if (!id.value) return
  downloading.value = true
  try {
    // #ifdef H5
    await downloadProtectedFileInBrowser(
      `/api/v1/health-reports/${id.value}/content`,
      `健康评估报告-${id.value}.pdf`,
    )
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
.insufficient {
  color: #687a75;
  background: #eef2f1;
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
.report-disclaimer {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #e5ece9;
  color: #74857f;
  font-size: 22rpx;
}
.fallback-card {
  margin-top: 24rpx;
  padding: 26rpx 28rpx;
  border: 1rpx solid #f2cf91;
  border-radius: 22rpx;
  background: #fff8e9;
}
.fallback-title {
  color: #8c5200;
  font-size: 27rpx;
  font-weight: 720;
}
.fallback-copy {
  margin-top: 10rpx;
  color: #786445;
  font-size: 23rpx;
  line-height: 1.65;
}
.retry-button {
  margin-top: 20rpx;
  border: 0;
  border-radius: 16rpx;
  background: #0b8064;
  color: #fff;
  font-size: 24rpx;
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
.abnormal-card {
  padding: 12rpx 27rpx 18rpx;
  background: linear-gradient(145deg, #fff8ed, #ffffff 72%);
  border: 1rpx solid #f3dfbd;
}
.abnormal-explanation {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f2e8d8;
  color: #5d5b53;
  font-size: 24rpx;
  line-height: 1.65;
}
.abnormal-explanation:last-of-type {
  border-bottom: 0;
}
.abnormal-title {
  color: #8b5415;
  font-size: 27rpx;
  font-weight: 750;
}
.abnormal-copy {
  margin-top: 10rpx;
}
.abnormal-finding {
  margin-top: 6rpx;
  color: #8b5415;
  font-size: 23rpx;
  line-height: 1.55;
}
.abnormal-label {
  color: #35594e;
  font-weight: 700;
}
.abnormal-note {
  margin-top: 8rpx;
  color: #8c7d67;
  font-size: 21rpx;
  line-height: 1.55;
}
.diagnostic-reference {
  padding: 19rpx 0 22rpx;
  border-bottom: 1rpx solid #edf1ef;
  color: #47675d;
  font-size: 24rpx;
  line-height: 1.6;
}
.diagnostic-reference:last-child {
  border-bottom: 0;
  padding-bottom: 10rpx;
}
.diagnostic-line {
  color: #35594e;
}
.diagnostic-strong {
  color: #173f35;
  font-weight: 750;
}
.diagnostic-copy {
  margin-top: 9rpx;
  color: #58736a;
}
.diagnostic-plan {
  margin-top: 15rpx;
  padding: 14rpx 16rpx;
  border-radius: 14rpx;
  background: #f1f8f5;
}
.diagnostic-plan-title {
  color: #0b765d;
  font-weight: 700;
}
.integrated-treatment-plan {
  background: #fff8ed;
  border: 1rpx solid #f3dfbd;
}
.integrated-treatment-label {
  color: #35594e;
  font-weight: 700;
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
.report-page .download-button {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 100%;
  height: 94rpx;
  min-height: 94rpx;
  margin: 28rpx 0 0;
  padding: 0 28rpx;
  border: 0;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #11876c, #0b725c);
  color: #fff;
  font-size: 30rpx;
  line-height: 1;
  font-weight: 720;
  box-shadow: 0 14rpx 30rpx rgba(15, 122, 98, 0.18);
}
.report-page .download-button::after {
  display: none;
}
</style>

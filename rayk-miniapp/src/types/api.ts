export interface ApiResponse<T> {
  code: number
  message: string
  requestId: string
  timestamp: number
  data: T
}

export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface WorkbenchOption {
  code: Role
  name: string
}

export type Role = 'PLATFORM_ADMIN' | 'DOCTOR' | 'CUSTOMER'

export interface AuthData {
  accessToken: string
  tokenType: string
  expiresIn: number
  userId: string
  tenantId: string
  tenantName: string
  displayName: string
  roles: Role[]
  permissions: string[]
  availableWorkbenches: WorkbenchOption[]
  defaultWorkbench: Role
}

export interface WeChatBinding {
  userId: string
  appId: string
  openidMasked: string
  status: string
  boundAt: string
}

export interface HomeMetric {
  code: string
  label: string
  value: number
  route: string
}
export interface HomeSummary {
  workbench: Role
  greeting: string
  metrics: HomeMetric[]
  disclaimer: string
}

export interface Patient {
  id: string
  name: string
  gender: string
  birthDate?: string
  phoneMasked?: string
  status: string
  assignedDoctorId?: string
  assignedManagerId?: string
  createdAt: string
}

export interface HealthProfile {
  id: string
  patientId: string
  heightCm?: number
  weightKg?: number
  waistCm?: number
  recentWeightChangeKg?: number
  bmi?: number
  bloodType?: string
  lifestyleSummary?: string
  medicalHistory?: string
  familyHistory?: string
  allergyHistory?: string
  currentMedications?: string
  smokingStatus?: string
  alcoholStatus?: string
  exerciseFrequency?: string
  sleepQuality?: string
  sleepHours?: number
  stressLevel?: string
  moodStatus?: string
  fearLevel?: string
  dietaryPreference?: string
  recentDietaryPattern?: string
  diabetesStatus?: string
  hypertensionStatus?: string
  dyslipidemiaStatus?: string
  fattyLiverStatus?: string
  profileCompleteness: number
  updatedAt?: string
}

export interface HealthScanSession {
  taskId: string
  appId: string
  timestamp: number
  outUserId: string
  sign: string
  serverUrl?: string
  pluginProvider: string
  pluginVersion?: string
}

export interface HealthScanResult {
  id: string
  status: 'CREATED' | 'UPLOADING' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED'
  statusLabel: string
  vendorDetectId?: string
  heartRate?: number
  heartRateVariability?: number
  oxygenSaturation?: number
  respirationRate?: number
  systolicBloodPressure?: number
  diastolicBloodPressure?: number
  stressHrv?: number
  qualityScore?: number
  healthScore?: number
  peerPercentile?: number
  peerSampleSize?: number
  peerComparisonEstimated?: boolean
  failureMessage?: string
  createdAt: string
  completedAt?: string
}

export interface SupportTicket {
  id: string
  category: 'USAGE' | 'BUG' | 'SUGGESTION' | 'OTHER'
  content: string
  contact?: string
  status: 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'
  reply?: string
  createdAt: string
  updatedAt: string
}

export interface PlatformSupportTicket extends SupportTicket {
  tenantId: string
  submitterUserId: string
}

export interface PrivacyConsent {
  id: string
  consentType: string
  policyVersion: string
  consented: number
  consentedAt?: string
  revokedAt?: string
}

export interface Indicator {
  id?: string
  code: string
  name: string
  value: number
  unit: string
  referenceLow?: number
  referenceHigh?: number
  abnormalFlag?: string
  manuallyConfirmed?: boolean
}

export interface OcrFinding {
  section: string
  item: string
  result: string
}

export interface ImageAnalysisFinding {
  category: string
  item: string
  result: string
  unit?: string
  referenceRange?: string
  abnormalFlag?: string
  conclusion?: string
  note?: string
}

export interface ImageAnalysisPage {
  page: number
  pageSummary?: string
  findings: ImageAnalysisFinding[]
  uncertainties?: string[]
}

export interface ImageAnalysis {
  pages: ImageAnalysisPage[]
}

export interface LabReport {
  id: string
  patientId: string
  reportName: string
  reportDate: string
  status: string
  processingProgress?: number
  processingMessage?: string
  failureReason?: string
  sourceType: string
  indicators: Indicator[]
  findings?: OcrFinding[]
  hasImageFiles?: boolean
  imageAnalysis?: ImageAnalysis
  createdAt: string
}

export interface LabReportFile {
  id: string
  reportId: string
  originalName: string
  mimeType: string
  fileSize: number
  sha256: string
  status: string
  downloadUrl?: string
  downloadUrlExpiresAt?: string
  createdAt: string
}

export interface LabReportUpload {
  report: LabReport
  file: LabReportFile
  ocrTask: OcrTask
}

export interface OcrTask {
  id: string
  reportId: string
  fileId?: string
  taskCode: string
  status: string
  engine?: string
  confidence?: number
  attemptCount: number
  indicatorCount: number
  warnings: string[]
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  createdAt: string
}

export interface Assessment {
  id: string
  reportId: string
  patientId: string
  modelVersion: string
  status: string
  overallRiskLevel: string
  results: {
    results?: Array<{
      modelCode: string
      modelName: string
      modelVersion?: string
      status?: 'EVALUATED' | 'INSUFFICIENT_DATA'
      score: number | null
      riskLevel: string
      dataCompleteness?: number
      confidence?: 'HIGH' | 'MEDIUM' | 'LOW'
      evidence: string[]
      supportingIndicators?: string[]
      missingIndicators: string[]
      recommendations: string[]
      doctorEdited?: boolean
    }>
    interpretation?: {
      status: 'SUCCESS' | 'DISABLED' | 'FALLBACK'
      source: 'DEEPSEEK' | 'RULE_FALLBACK'
      model?: string
      generationAttempts?: number
      fallbackReason?: string
      summary: string
      priorityConcerns: string[]
      abnormalExplanations?: Array<{
        title: string
        finding?: string
        indicatorCodes?: string[]
        patientFactIds?: string[]
        evidenceIds?: string[]
        explanation: string
        possibleImpacts: string
        nextStep: string
      }>
      crossModelFindings: Array<{
        title: string
        indicatorCodes: string[]
        explanation: string
      }>
      diagnosticReferences?: Array<{
        conditionName: string
        assessment: 'RISK_SIGNAL' | 'POSSIBLE' | 'PRIORITY_REVIEW'
        rationale: string
        indicatorCodes: string[]
        supportingEvidence: string[]
        contradictingEvidence: string[]
        confirmationAdvice: string[]
        treatmentPlan?: string[]
        nutritionInterventionPlan?: string[]
        westernMedicineApproach?: string[]
        traditionalChineseMedicineApproach?: string[]
        westernMedicineMedicationPlan?: string[]
        traditionalChineseMedicineMedicationPlan?: string[]
        integratedTreatmentNotes?: string[]
        recommendedDepartment?: string
      }>
      recommendations: string[]
      missingDataAdvice: string[]
      followupQuestions: string[]
      redFlags: string[]
      uncertainty: string
      disclaimer: string
    }
  }
  disclaimer: string
  createdAt: string
}

export interface ReviewTask {
  id: string
  status: string
  reviewOpinion?: string
  patient: Patient
  assessment: Assessment
}
export interface HealthReport {
  id: string
  patientId: string
  patientName?: string
  title: string
  status: string
  summary: string
  doctorOpinion?: string
  disclaimer: string
  publishedAt: string
  assessment?: Assessment
}
export interface Followup {
  id: string
  patientId: string
  patientName?: string
  title: string
  content: string
  dueDate: string
  status: string
  feedback?: string
  feedbackDetail?: string
  completedAt?: string
  cycleNo?: number
  maxCycles?: number
  completionRate?: number
  decision?: 'CONTINUE' | 'ADJUST' | 'TERMINATE' | 'PAUSE'
  decisionReason?: string
  reminderCount?: number
  lastRemindedAt?: string
}

export interface FollowupPlan {
  id: string
  patientId: string
  planName: string
  startDate: string
  endDate: string
  status: string
  tasks: Followup[]
  createdAt: string
}

export interface TrendPoint {
  reportId: string
  reportDate: string
  value: number
  unit: string
  abnormalFlag?: string
}

export interface TrendSummary {
  indicatorCode: string
  indicatorName: string
  latestValue?: number
  minValue?: number
  maxValue?: number
  averageValue?: number
  unit?: string
  trendDirection: 'UP' | 'DOWN' | 'STABLE'
  dataPoints: number
}

export interface AuditLog {
  id: string
  tenantId: string
  operatorId: string
  operationType: string
  resourceType: string
  resourceId?: string
  requestId?: string
  result: string
  detailMasked?: string
  createdAt: string
}

export interface TenantProfile {
  id: string
  name: string
  status: string
  servicePlan: string
}

export interface UpdatePlatformTenantPayload {
  tenantName: string
  servicePlan: string
  status: 'ACTIVE' | 'DISABLED'
}

export interface CreatePlatformTenantPayload {
  tenantCode: string
  tenantName: string
  servicePlan: string
}

export interface TenantStaff {
  id: string
  username: string
  displayName: string
  phoneMasked?: string
  roles: Role[]
  status: string
}

export interface TenantSummary {
  id: string
  code: string
  name: string
  status: string
  servicePlan: string
  userCount: number
}

export interface PlatformOverview {
  tenantCount: number
  activeTenantCount: number
  userCount: number
  patientCount: number
  phoneCustomerCount: number
  pendingReviewCount: number
  pendingFollowupCount: number
  todayFollowupCount: number
  tenants: TenantSummary[]
  followups: PlatformFollowup[]
}

export interface PlatformCustomerMembership {
  userId: string
  displayName: string
  phoneMasked?: string
  membershipStatus: 'FREE' | 'ACTIVE'
  planName: string
  active: boolean
  expireAt?: string
}

export interface PlatformGoldBeanOverview {
  enabled: boolean
  developmentMode: boolean
  recordOnly: boolean
  registrationFeeYuan: number
  initialBeans: number
  dailyRewardBeans: number
  dailyRewardDays: number
  protectionDays: number
  limitedTradePercent: number
  firstRegistrationScope: string
  totalMemberCount: number
  registeredMemberCount: number
  pendingMemberCount: number
  platformFeeRegistrationCount: number
  referrerFeeRegistrationCount: number
  totalGoldBeanBalance: number
  digitalBankBalance: number
  tradingBalance: number
  dailyRewardBeansIssued: number
  activeRegionCount: number
  recentLedgers: PlatformGoldBeanLedger[]
}

export interface PlatformGoldBeanAccount {
  accountId: string
  tenantId: string
  tenantName: string
  userId: string
  displayName: string
  phoneMasked: string
  memberLevel: string
  memberLevelName: string
  historicalLevel: string
  historicalLevelName: string
  directReferralCount: number
  referrerId?: string
  referrerName: string
  referralCode?: string | null
  registrationFeeStatus: string
  feeRecipientType?: string
  feeRecipientName: string
  registrationFeeYuan: number
  registrationFeePaidAt?: string
  totalBalance: number
  digitalBankBalance: number
  tradingBalance: number
  tradeLimitPercent: number
  dailyRewardDays: number
  dailyRewardTotalDays: number
  dailyRewardRemainingDays: number
  protectionUntil?: string
  city?: string
  status: string
  createdAt: string
}

export interface PlatformGoldBeanReferral {
  id: string
  tenantId: string
  tenantName: string
  referrerId: string
  referrerName: string
  referredId: string
  referredName: string
  referralCode: string
  registrationFeeYuan: number
  feeRecipientType?: string
  feeRecipientName: string
  status: string
  registeredAt: string
}

export interface PlatformGoldBeanLedger {
  id: string
  tenantId: string
  tenantName: string
  userId: string
  displayName: string
  bucket: string
  direction: string
  amount: number
  eventType: string
  description: string
  createdAt: string
}

export interface PlatformGoldBeanOrder {
  orderNo: string
  tenantId: string
  tenantName: string
  customerId: string
  customerName: string
  orderType: string
  status: string
  amountCent: number
  paymentAmountCent: number
  goldBeanQuantity: number
  feeRecipientName: string
  paymentChannel?: string
  transactionIdMasked?: string
  createdAt: string
  paidAt?: string
  settlementStatus?: 'NOT_REQUIRED' | 'PENDING' | 'SETTLED' | 'FAILED' | string
  settlementFailureReason?: string
}

export interface PlatformGoldBeanInvite {
  id: string
  codeMasked: string
  status: 'AVAILABLE' | 'RESERVED' | 'CONSUMED' | 'REVOKED' | 'EXPIRED' | string
  boundPhoneMasked?: string
  reservedOrderNo?: string
  consumedOrderNo?: string
  expiresAt: string
  createdAt: string
  consumedAt?: string
  revokedAt?: string
}

export interface PlatformGoldBeanInviteCreated {
  id: string
  code: string
  codeMasked: string
  status: string
  boundPhoneMasked?: string
  expiresAt: string
}

export interface PlatformGoldBeanLegendary {
  id: string
  phoneMasked: string
  status: string
  note?: string
  matchedUserId?: string
  matchedDisplayName?: string
  matchedMemberLevelName?: string
  matchedRegistrationStatus?: string
  digitalBankBalance?: number | null
  createdAt: string
  updatedAt: string
}

export interface PlatformGoldRegion {
  id: string
  tenantId: string
  tenantName: string
  city: string
  depth: number
  parentRegionId?: string
  ownerUserId: string
  ownerName: string
  ownerPhoneMasked: string
  memberLevelName: string
  status: string
  createdAt: string
}

export interface PlatformGoldRegionProfitDistribution {
  recipientUserId: string
  recipientName: string
  recipientType: string
  ratePercent: number
  amount: number
}

export interface PlatformGoldRegionProfit {
  id: string
  tenantId: string
  tenantName: string
  regionId: string
  city: string
  depth: number
  ownerUserId: string
  ownerName: string
  amount: number
  ownerAmount: number
  retainedAmount: number
  status: string
  idempotencyKey: string
  settledAt: string
  distributions: PlatformGoldRegionProfitDistribution[]
}

export interface AiModelRuntimeConfig {
  id: string
  modelCode: string
  modelName: string
  provider: string
  modelVersion: string
  baseUrl: string
  contextLengthTokens: number
  maxOutputTokens: number
  thinkingSupported: boolean
  thinkingEnabled: boolean
  selected: boolean
  status: string
  updatedAt: string
}

export interface PlatformFollowup {
  id: string
  patientName: string
  title: string
  dueDate: string
  status: string
  feedback?: string
  completedAt?: string
}

export interface MallProduct {
  id: string
  productName: string
  subtitle?: string
  description?: string
  mainImageUrl?: string
  priceCent: number
  stock: number
  soldCount: number
  status: 'ACTIVE' | 'INACTIVE' | string
  sortOrder: number
}

export interface MallAddress {
  id: string
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: boolean
}

export interface MallOrderItem {
  productId: string
  productName: string
  mainImageUrl?: string
  unitPriceCent: number
  quantity: number
  totalCent: number
}

export interface MallOrder {
  orderNo: string
  status: 'PENDING_PAYMENT' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED' | string
  amountCent: number
  paymentChannel?: string
  transactionId?: string
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  expiresAt?: string
  createdAt: string
  paidAt?: string
  items: MallOrderItem[]
}

export interface MallPaymentParams {
  timeStamp: string
  nonceStr: string
  packageValue: string
  signType: string
  paySign: string
}

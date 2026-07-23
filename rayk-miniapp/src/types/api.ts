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

export interface LabReport {
  id: string
  patientId: string
  reportName: string
  reportDate: string
  status: string
  sourceType: string
  indicators: Indicator[]
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
      summary: string
      priorityConcerns: string[]
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
  title: string
  content: string
  dueDate: string
  status: string
  feedback?: string
  completedAt?: string
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
  pendingReviewCount: number
  pendingFollowupCount: number
  todayFollowupCount: number
  tenants: TenantSummary[]
  followups: PlatformFollowup[]
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

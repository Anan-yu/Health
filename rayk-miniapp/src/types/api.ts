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

export type Role = 'PLATFORM_ADMIN' | 'TENANT_ADMIN' | 'DOCTOR' | 'HEALTH_MANAGER' | 'CUSTOMER'

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
      score: number
      riskLevel: string
      evidence: string[]
      missingIndicators: string[]
      recommendations: string[]
    }>
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
}
export interface Followup {
  id: string
  patientId: string
  title: string
  content: string
  dueDate: string
  status: string
  feedback?: string
}

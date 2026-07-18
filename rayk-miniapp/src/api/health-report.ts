import type { HealthReport } from '@/types/api'
import { request } from '@/utils/request'
export const getMyHealthReports = () =>
  request<HealthReport[]>({ url: '/api/v1/me/health-reports', method: 'GET' })

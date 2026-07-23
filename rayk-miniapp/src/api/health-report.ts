import type { HealthReport, PageResponse } from '@/types/api'
import { request } from '@/utils/request'
export const getMyHealthReports = () =>
  request<HealthReport[]>({ url: '/api/v1/me/health-reports', method: 'GET' })

export const getHealthReports = (patientId?: string) =>
  request<PageResponse<HealthReport>>({
    url: `/api/v1/health-reports${patientId ? `?patientId=${encodeURIComponent(patientId)}` : ''}`,
    method: 'GET',
  })

export const getHealthReport = (id: string) =>
  request<HealthReport>({ url: `/api/v1/health-reports/${id}`, method: 'GET' })

export const getHealthReportDownloadUrl = (id: string) =>
  request<{ downloadUrl: string }>({ url: `/api/v1/health-reports/${id}/download`, method: 'GET' })

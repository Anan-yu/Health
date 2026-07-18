import type { Indicator, LabReport, PageResponse } from '@/types/api'
import { request } from '@/utils/request'

export const getLabReports = () =>
  request<PageResponse<LabReport>>({ url: '/api/v1/lab-reports', method: 'GET' })
export const getMyLabReports = () =>
  request<LabReport[]>({ url: '/api/v1/me/lab-reports', method: 'GET' })
export const getLabReport = (id: string) =>
  request<LabReport>({ url: `/api/v1/lab-reports/${id}`, method: 'GET' })
export const createLabReport = (patientId: string, reportName: string) =>
  request<LabReport>({
    url: '/api/v1/lab-reports',
    method: 'POST',
    data: { patientId, reportName, sourceType: 'SIMULATED_UPLOAD' },
  })
export const saveIndicators = (id: string, indicators: Indicator[]) =>
  request<LabReport>({
    url: `/api/v1/lab-reports/${id}/indicators`,
    method: 'PUT',
    data: { indicators },
  })
export const confirmIndicators = (id: string) =>
  request<LabReport>({ url: `/api/v1/lab-reports/${id}/confirm`, method: 'POST' })
export const submitAi = (id: string) =>
  request<unknown>({ url: `/api/v1/lab-reports/${id}/submit-ai`, method: 'POST' })

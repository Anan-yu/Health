import type { TrendPoint, TrendSummary } from '@/types/api'
import { request } from '@/utils/request'

export const getIndicatorTrend = (patientId: string, code: string, months = 6) =>
  request<TrendPoint[]>({
    url: `/api/v1/patients/${patientId}/indicators/${encodeURIComponent(code)}/trend`,
    method: 'GET',
    data: { months },
  })

export const getIndicatorTrendSummary = (patientId: string, code: string) =>
  request<TrendSummary>({
    url: `/api/v1/patients/${patientId}/indicators/${encodeURIComponent(code)}/summary`,
    method: 'GET',
  })

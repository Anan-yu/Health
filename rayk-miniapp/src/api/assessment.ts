import type { Assessment, PageResponse } from '@/types/api'
import { request } from '@/utils/request'

export const getAssessments = () =>
  request<PageResponse<Assessment>>({ url: '/api/v1/assessments', method: 'GET' })
export const getMyAssessments = () =>
  request<Assessment[]>({ url: '/api/v1/me/assessments', method: 'GET' })
export const getAssessment = (id: string) =>
  request<Assessment>({ url: `/api/v1/assessments/${id}`, method: 'GET' })

import type { HealthReport, PageResponse, ReviewTask } from '@/types/api'
import { request } from '@/utils/request'

export const getReviewTasks = () =>
  request<PageResponse<ReviewTask>>({ url: '/api/v1/reviews/tasks', method: 'GET' })
export const getReviewTask = (id: string) =>
  request<ReviewTask>({ url: `/api/v1/reviews/tasks/${id}`, method: 'GET' })
export const approveReview = (id: string, opinion: string) =>
  request<ReviewTask>({
    url: `/api/v1/reviews/tasks/${id}/approve`,
    method: 'POST',
    data: { opinion },
  })
export const rejectReview = (id: string, opinion: string) =>
  request<ReviewTask>({
    url: `/api/v1/reviews/tasks/${id}/reject`,
    method: 'POST',
    data: { opinion },
  })
export const publishReview = (id: string) =>
  request<HealthReport>({ url: `/api/v1/reviews/tasks/${id}/publish`, method: 'POST' })

export interface ReviewItemEdit {
  modelCode: string
  riskLevel: string
  evidence: string[]
  recommendations: string[]
}

export const editReview = (id: string, items: ReviewItemEdit[], overallOpinion: string) =>
  request<ReviewTask>({
    url: `/api/v1/reviews/tasks/${id}/edit`,
    method: 'PUT',
    data: { items, overallOpinion },
  })

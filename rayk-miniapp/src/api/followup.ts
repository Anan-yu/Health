import type { Followup, FollowupPlan, PageResponse } from '@/types/api'
import { request } from '@/utils/request'
export const getFollowups = () =>
  request<PageResponse<Followup>>({ url: '/api/v1/followups', method: 'GET' })
export const getMyFollowups = () =>
  request<Followup[]>({ url: '/api/v1/me/followups', method: 'GET' })
export const sendFeedback = (id: string, feedback: string) =>
  request<Followup>({
    url: `/api/v1/me/followups/${id}/feedback`,
    method: 'POST',
    data: { feedback },
  })

export const getFollowup = (id: string) =>
  request<Followup>({ url: `/api/v1/followups/${id}`, method: 'GET' })

export const createFollowup = (data: {
  patientId: number
  title: string
  content: string
  dueDate: string
  assigneeId?: number
}) => request<Followup>({ url: '/api/v1/followups', method: 'POST', data })

export const completeFollowup = (id: string) =>
  request<Followup>({ url: `/api/v1/followups/${id}/complete`, method: 'PUT' })

export const getFollowupPlans = (patientId: string) =>
  request<FollowupPlan[]>({
    url: `/api/v1/followup-plans?patientId=${encodeURIComponent(patientId)}`,
    method: 'GET',
  })

export const createFollowupPlan = (data: {
  patientId: number
  planName: string
  startDate: string
  intervalDays: number
  totalOccurrences: number
  title: string
  content?: string
  assigneeId?: number
}) => request<FollowupPlan>({ url: '/api/v1/followup-plans', method: 'POST', data })

export const activateFollowupPlan = (id: string) =>
  request<FollowupPlan>({ url: `/api/v1/followup-plans/${id}/activate`, method: 'POST' })

export const completeFollowupPlan = (id: string) =>
  request<FollowupPlan>({ url: `/api/v1/followup-plans/${id}/complete`, method: 'POST' })

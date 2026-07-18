import type { Followup, PageResponse } from '@/types/api'
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

import type { PlatformSupportTicket, SupportTicket } from '@/types/api'
import { request } from '@/utils/request'

export const getMySupportTickets = () =>
  request<SupportTicket[]>({ url: '/api/v1/me/support-tickets', method: 'GET' })

export const createSupportTicket = (data: {
  category: string
  content: string
  contact?: string
}) => request<SupportTicket>({ url: '/api/v1/me/support-tickets', method: 'POST', data })

export const getPlatformSupportTickets = () =>
  request<PlatformSupportTicket[]>({ url: '/api/v1/me/support-tickets/platform', method: 'GET' })

export const replyPlatformSupportTicket = (
  id: string,
  data: { reply: string; status?: 'PROCESSING' | 'RESOLVED' | 'CLOSED' },
) =>
  request<PlatformSupportTicket>({
    url: `/api/v1/me/support-tickets/platform/${id}/reply`,
    method: 'PUT',
    data,
  })

import type { SupportTicket } from '@/types/api'
import { request } from '@/utils/request'

export const getMySupportTickets = () =>
  request<SupportTicket[]>({ url: '/api/v1/me/support-tickets', method: 'GET' })

export const createSupportTicket = (data: {
  category: string
  content: string
  contact?: string
}) => request<SupportTicket>({ url: '/api/v1/me/support-tickets', method: 'POST', data })

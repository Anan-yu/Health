import type { HomeSummary, Role, WorkbenchOption } from '@/types/api'
import { request } from '@/utils/request'

export const getWorkbenches = () =>
  request<WorkbenchOption[]>({ url: '/api/v1/workbenches', method: 'GET' })
export const switchWorkbench = (code: Role) =>
  request<{ code: Role }>({ url: '/api/v1/workbenches/switch', method: 'POST', data: { code } })
export const getHomeSummary = () =>
  request<HomeSummary>({ url: '/api/v1/home/summary', method: 'GET' })

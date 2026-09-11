import type { HomeSummary, Role, WorkbenchOption } from '@/types/api'
import { request } from '@/utils/request'

export const getWorkbenches = () =>
  request<WorkbenchOption[]>({ url: '/api/v1/workbenches', method: 'GET' })
export const switchWorkbench = (code: Role) =>
  request<{ code: Role }>({ url: '/api/v1/workbenches/switch', method: 'POST', data: { code } })
export const getHomeSummary = () =>
  request<HomeSummary>({
    // The summary changes after a profile save; prevent a cached GET from
    // keeping the previous completeness percentage on the home card.
    url: `/api/v1/home/summary?refreshAt=${Date.now()}`,
    method: 'GET',
  })

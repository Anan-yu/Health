import type { PlatformOverview, TenantProfile, TenantStaff } from '@/types/api'
import { request } from '@/utils/request'

export const getTenantProfile = () =>
  request<TenantProfile>({ url: '/api/v1/tenant/profile', method: 'GET' })

export const getTenantStaff = () =>
  request<TenantStaff[]>({ url: '/api/v1/tenant/staff', method: 'GET' })

export const createTenantStaff = (data: {
  displayName: string
  phone: string
  roleCode: 'DOCTOR' | 'HEALTH_MANAGER'
}) => request<TenantStaff>({ url: '/api/v1/tenant/staff', method: 'POST', data })

export const getPlatformOverview = () =>
  request<PlatformOverview>({ url: '/api/v1/platform/overview', method: 'GET' })

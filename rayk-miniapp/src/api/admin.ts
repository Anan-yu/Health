import type {
  CreatePlatformTenantPayload,
  PlatformOverview,
  TenantProfile,
  TenantStaff,
  UpdatePlatformTenantPayload,
} from '@/types/api'
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

export const getPlatformTenant = (tenantId: string) =>
  request<TenantProfile>({ url: `/api/v1/platform/tenants/${tenantId}`, method: 'GET' })

export const createPlatformTenant = (data: CreatePlatformTenantPayload) =>
  request<TenantProfile>({ url: '/api/v1/platform/tenants', method: 'POST', data })

export const updatePlatformTenant = (tenantId: string, data: UpdatePlatformTenantPayload) =>
  request<TenantProfile>({ url: `/api/v1/platform/tenants/${tenantId}`, method: 'PUT', data })

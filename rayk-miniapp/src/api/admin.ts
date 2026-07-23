import type {
  CreatePlatformTenantPayload,
  PlatformOverview,
  TenantProfile,
  TenantStaff,
  UpdatePlatformTenantPayload,
} from '@/types/api'
import { request } from '@/utils/request'

export const getPlatformOverview = () =>
  request<PlatformOverview>({ url: '/api/v1/platform/overview', method: 'GET' })

export const getPlatformTenant = (tenantId: string) =>
  request<TenantProfile>({ url: `/api/v1/platform/tenants/${tenantId}`, method: 'GET' })

export const createPlatformTenant = (data: CreatePlatformTenantPayload) =>
  request<TenantProfile>({ url: '/api/v1/platform/tenants', method: 'POST', data })

export const updatePlatformTenant = (tenantId: string, data: UpdatePlatformTenantPayload) =>
  request<TenantProfile>({ url: `/api/v1/platform/tenants/${tenantId}`, method: 'PUT', data })

export const getPlatformDoctors = (tenantId: string) =>
  request<TenantStaff[]>({ url: `/api/v1/platform/tenants/${tenantId}/doctors`, method: 'GET' })

export const createPlatformDoctor = (
  tenantId: string,
  data: { displayName: string; phone: string },
) =>
  request<TenantStaff>({
    url: `/api/v1/platform/tenants/${tenantId}/doctors`,
    method: 'POST',
    data,
  })

export const updatePlatformDoctor = (
  tenantId: string,
  doctorId: string,
  data: { displayName: string; phone?: string },
) =>
  request<TenantStaff>({
    url: `/api/v1/platform/tenants/${tenantId}/doctors/${doctorId}`,
    method: 'PUT',
    data,
  })

export const deletePlatformDoctor = (tenantId: string, doctorId: string) =>
  request<void>({
    url: `/api/v1/platform/tenants/${tenantId}/doctors/${doctorId}`,
    method: 'DELETE',
  })

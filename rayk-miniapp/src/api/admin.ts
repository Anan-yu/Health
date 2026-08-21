import type {
  AiModelRuntimeConfig,
  CreatePlatformTenantPayload,
  PlatformOverview,
  TenantProfile,
  TenantStaff,
  UpdatePlatformTenantPayload,
} from '@/types/api'
import { request } from '@/utils/request'

export const getPlatformOverview = () =>
  request<PlatformOverview>({ url: '/api/v1/platform/overview', method: 'GET' })

export const getPlatformAdminProfile = () =>
  request<TenantStaff>({ url: '/api/v1/platform/admin-profile', method: 'GET' })

export const updatePlatformAdminPhone = (phone: string) =>
  request<TenantStaff>({
    url: '/api/v1/platform/admin-phone',
    method: 'PUT',
    data: { phone },
  })

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

export const createDoctorWeChatInvite = (tenantId: string, doctorId: string) =>
  request<{ code: string; expiresIn: number }>({
    url: `/api/v1/platform/tenants/${tenantId}/doctors/${doctorId}/wechat-invite`,
    method: 'POST',
  })

export const getAiModelRuntimeConfigs = () =>
  request<AiModelRuntimeConfig[]>({ url: '/api/v1/platform/ai-models', method: 'GET' })

export const switchAiModel = (modelCode: string) =>
  request<AiModelRuntimeConfig>({
    url: `/api/v1/platform/ai-models/${encodeURIComponent(modelCode)}`,
    method: 'PUT',
    data: { modelCode },
  })

export const switchAiThinking = (enabled: boolean) =>
  request<AiModelRuntimeConfig>({
    url: '/api/v1/platform/ai-models/thinking',
    method: 'PUT',
    data: { enabled },
  })

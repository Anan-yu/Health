import type { PageResponse, Patient } from '@/types/api'
import { request } from '@/utils/request'

export const getPatients = (keyword?: string) =>
  request<PageResponse<Patient>>({ url: `/api/v1/patients${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`, method: 'GET' })
export const getPatient = (id: string) =>
  request<Patient>({ url: `/api/v1/patients/${id}`, method: 'GET' })
export const updatePatientIdentity = (id: string, data: { name: string; phone?: string }) =>
  request<Patient>({ url: `/api/v1/patients/${id}/identity`, method: 'PUT', data })
export const createPatient = (data: object) =>
  request<Patient>({ url: '/api/v1/patients', method: 'POST', data })
export const getMyProfile = () =>
  request<Patient | null>({ url: '/api/v1/me/health-profile', method: 'GET' })
export const getHealthProfile = (patientId: string) =>
  request<import('@/types/api').HealthProfile>({
    url: `/api/v1/patients/${patientId}/profile`,
    method: 'GET',
  })
export const updateHealthProfile = (patientId: string, data: object) =>
  request<import('@/types/api').HealthProfile>({
    url: `/api/v1/patients/${patientId}/profile`,
    method: 'PUT',
    data,
  })

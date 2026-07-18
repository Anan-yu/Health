import type { PageResponse, Patient } from '@/types/api'
import { request } from '@/utils/request'

export const getPatients = () =>
  request<PageResponse<Patient>>({ url: '/api/v1/patients', method: 'GET' })
export const getPatient = (id: string) =>
  request<Patient>({ url: `/api/v1/patients/${id}`, method: 'GET' })
export const createPatient = (data: object) =>
  request<Patient>({ url: '/api/v1/patients', method: 'POST', data })
export const getMyProfile = () =>
  request<Patient | null>({ url: '/api/v1/me/health-profile', method: 'GET' })

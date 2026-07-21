import type { PrivacyConsent } from '@/types/api'
import { request } from '@/utils/request'

export const getPrivacyConsents = (patientId: string) =>
  request<PrivacyConsent[]>({ url: `/api/v1/patients/${patientId}/consents`, method: 'GET' })
export const grantPrivacyConsent = (patientId: string, consentType: string) =>
  request<PrivacyConsent>({
    url: `/api/v1/patients/${patientId}/consents`,
    method: 'POST',
    data: { consentType, policyVersion: '2026.07' },
  })
export const revokePrivacyConsent = (patientId: string, consentType: string) =>
  request<void>({
    url: `/api/v1/patients/${patientId}/consents/${encodeURIComponent(consentType)}`,
    method: 'DELETE',
  })

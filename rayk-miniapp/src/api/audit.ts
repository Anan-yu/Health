import type { AuditLog, PageResponse } from '@/types/api'
import { request } from '@/utils/request'

export const getAuditLogs = (tenantId?: string) =>
  request<PageResponse<AuditLog>>({
    url: '/api/v1/audit-logs',
    method: 'GET',
    data: { size: 50, ...(tenantId ? { tenantId } : {}) },
  })

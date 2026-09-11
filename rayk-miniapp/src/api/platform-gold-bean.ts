import type {
  PageResponse,
  PlatformGoldBeanAccount,
  PlatformGoldBeanInvite,
  PlatformGoldBeanInviteCreated,
  PlatformGoldBeanLegendary,
  PlatformGoldBeanLedger,
  PlatformGoldBeanOrder,
  PlatformGoldBeanOverview,
  PlatformGoldBeanReferral,
  PlatformGoldRegion,
  PlatformGoldRegionProfit,
} from '@/types/api'
import { request } from '@/utils/request'

const withQuery = (path: string, params: Record<string, string | undefined>) => {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value.trim() !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value || '')}`)
    .join('&')
  return query ? `${path}?${query}` : path
}

export const getPlatformGoldBeanOverview = () =>
  request<PlatformGoldBeanOverview>({ url: '/api/v1/platform/gold-bean/overview', method: 'GET' })

export const getPlatformGoldBeanAccounts = (params: {
  keyword?: string
  registrationStatus?: string
}) =>
  request<PageResponse<PlatformGoldBeanAccount>>({
    url: withQuery('/api/v1/platform/gold-bean/accounts', params),
    method: 'GET',
  })

export const getPlatformGoldBeanReferrals = (keyword?: string) =>
  request<PageResponse<PlatformGoldBeanReferral>>({
    url: withQuery('/api/v1/platform/gold-bean/referrals', { keyword }),
    method: 'GET',
  })

export const getPlatformGoldBeanLedger = (params: { keyword?: string; eventType?: string }) =>
  request<PageResponse<PlatformGoldBeanLedger>>({
    url: withQuery('/api/v1/platform/gold-bean/ledger', params),
    method: 'GET',
  })

export const getPlatformGoldBeanOrders = (params: {
  keyword?: string
  status?: string
  orderType?: string
}) =>
  request<PageResponse<PlatformGoldBeanOrder>>({
    url: withQuery('/api/v1/platform/gold-bean/orders', params),
    method: 'GET',
  })

export const getPlatformGoldBeanInvites = (status?: string) =>
  request<PageResponse<PlatformGoldBeanInvite>>({
    url: withQuery('/api/v1/platform/gold-bean/platform-invites', { status }),
    method: 'GET',
  })

export const createPlatformGoldBeanInvite = (data: { boundPhone?: string; validHours?: number }) =>
  request<PlatformGoldBeanInviteCreated>({
    url: '/api/v1/platform/gold-bean/platform-invites',
    method: 'POST',
    data,
  })

export const revokePlatformGoldBeanInvite = (inviteId: string) =>
  request<PlatformGoldBeanInvite>({
    url: `/api/v1/platform/gold-bean/platform-invites/${encodeURIComponent(inviteId)}/revoke`,
    method: 'POST',
  })

export const getPlatformGoldBeanLegendary = () =>
  request<PageResponse<PlatformGoldBeanLegendary>>({
    url: '/api/v1/platform/gold-bean/legendary',
    method: 'GET',
  })

export const createPlatformGoldBeanLegendary = (data: { phone: string; note?: string }) =>
  request<PlatformGoldBeanLegendary>({
    url: '/api/v1/platform/gold-bean/legendary',
    method: 'POST',
    data,
  })

export const revokePlatformGoldBeanLegendary = (id: string) =>
  request<PlatformGoldBeanLegendary>({
    url: `/api/v1/platform/gold-bean/legendary/${encodeURIComponent(id)}/revoke`,
    method: 'POST',
  })

export const getPlatformGoldRegions = () =>
  request<PageResponse<PlatformGoldRegion>>({
    url: '/api/v1/platform/gold-bean/regions',
    method: 'GET',
  })

export const getPlatformGoldRegionProfits = () =>
  request<PageResponse<PlatformGoldRegionProfit>>({
    url: '/api/v1/platform/gold-bean/region-profits',
    method: 'GET',
  })

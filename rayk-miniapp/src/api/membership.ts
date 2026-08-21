import { request } from '@/utils/request'

export interface MembershipBenefit {
  benefitCode: string
  benefitName: string
  description?: string
  unitType: string
  quota?: number | null
  used: number
  remaining?: number | null
  available: boolean
}

export interface MembershipSummary {
  membershipStatus: 'DISABLED' | 'FREE' | 'ACTIVE'
  planCode?: string
  planName?: string
  startAt?: string
  expireAt?: string
  remainingDays: number
  active: boolean
  paymentEnabled: boolean
  benefits: MembershipBenefit[]
}

export interface MembershipPlan {
  planCode: string
  planName: string
  durationDays: number
  priceCent: number
  originalPriceCent?: number
  description?: string
  benefits: string[]
}

export interface MembershipUsage {
  benefitCode: string
  benefitName?: string
  usageStatus: string
  bizType: string
  bizId?: string
  occurredAt: string
}

export interface MembershipOrder {
  orderNo: string
  planCode: string
  planName: string
  status: string
  amountCent: number
  simulated: boolean
  paymentEnabled: boolean
  createdAt: string
  paidAt?: string
  expireAt?: string
}

export interface MembershipPaymentParams {
  mode: 'short_series_goods' | 'short_series_coin' | string
  signData: string
  paySig: string
  signature: string
}

export const getMembershipSummary = () =>
  request<MembershipSummary>({ url: '/api/client/membership/summary', method: 'GET' })

export type MembershipDevelopmentTarget = 'FREE' | 'YEARLY'

export const switchMembershipForDevelopment = (target: MembershipDevelopmentTarget) =>
  request<MembershipSummary>({
    url: '/api/client/membership/dev/switch',
    method: 'POST',
    data: { target },
  })

export const getMembershipPlans = () =>
  request<MembershipPlan[]>({ url: '/api/client/membership/plans', method: 'GET' })

export const getMembershipBenefits = () =>
  request<MembershipBenefit[]>({ url: '/api/client/membership/benefits', method: 'GET' })

export const getMembershipUsage = () =>
  request<MembershipUsage[]>({ url: '/api/client/membership/usage', method: 'GET' })

export const createMembershipOrder = (planCode = 'AI_HEALTH_YEARLY') =>
  request<MembershipOrder>({
    url: '/api/client/membership/orders',
    method: 'POST',
    data: { planCode },
  })

export const payMembershipOrder = (orderNo: string) =>
  request<MembershipOrder>({
    url: `/api/client/membership/orders/${encodeURIComponent(orderNo)}/pay`,
    method: 'POST',
    data: { paymentChannel: 'SIMULATED' },
  })

export const createWechatMembershipPayment = (orderNo: string) =>
  request<MembershipPaymentParams>({
    url: `/api/client/membership/orders/${encodeURIComponent(orderNo)}/wechat-pay`,
    method: 'POST',
  })

export const getMembershipOrder = (orderNo: string) =>
  request<MembershipOrder>({
    url: `/api/client/membership/orders/${encodeURIComponent(orderNo)}`,
    method: 'GET',
  })

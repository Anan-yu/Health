import { request } from '@/utils/request'
import type { PageResponse } from '@/types/api'

export interface GoldBeanSummary {
  enabled: boolean
  userId: string
  referralCode?: string | null
  memberLevel: string
  memberLevelName: string
  historicalLevel: string
  historicalLevelName: string
  directReferralCount: number
  nextLevel?: string
  nextLevelName?: string
  nextLevelThreshold: number
  registrationFeeStatus: 'UNPAID' | 'PAID' | string
  registrationFeeRecipient?: 'PLATFORM' | 'REFERRER' | string
  totalBalance: number
  digitalBankBalance: number
  tradingBalance: number
  tradeLimitPercent: number
  dailyRewardDays: number
  dailyRewardTotalDays: number
  dailyRewardRemainingDays: number
  reminderText?: string
  protectionUntil?: string
  regionOpenAllowed: boolean
  regionId?: string
  regionCity?: string
  paymentEnabled: boolean
  registrationFeeCent: number
  platformRegistrationFeeCent?: number
  referralRegistrationFeeCent?: number
  virtualPaymentSurchargePercent: number
  /** 平台一级代理（以及服务端定义的其他合资格会员）可向平台购豆。 */
  goldBeanPurchaseEnabled: boolean
  goldBeanUnitPriceCent: number
  maxPurchaseQuantity: number
}

export interface GoldBeanReferralMember {
  displayName: string
  memberLevelName: string
}

export interface GoldBeanReferralBranch {
  displayName: string
  memberLevelName: string
  directReferralCount: number
  hiddenMemberCount: number
  members: GoldBeanReferralMember[]
}

export interface GoldBeanReferralTrendPoint {
  date: string
  directReferralCount: number
  levelName: string
}

export interface GoldBeanReferralNetwork {
  selfDisplayName: string
  selfLevelName: string
  directReferralCount: number
  teamMemberCount: number
  hiddenDirectCount: number
  branches: GoldBeanReferralBranch[]
  trend: GoldBeanReferralTrendPoint[]
}

export interface GoldBeanOrder {
  orderNo: string
  orderType: 'REGISTRATION_FEE' | 'GOLD_BEAN_PURCHASE' | 'LEGENDARY_BANK_PURCHASE' | string
  status: 'PENDING' | 'PAID' | 'CLOSED' | string
  amountCent: number
  paymentAmountCent: number
  goldBeanQuantity: number
  registrationFeeRecipient?: 'PLATFORM' | string
  paymentEnabled: boolean
  createdAt: string
  paidAt?: string
  settlementStatus?: 'NOT_REQUIRED' | 'PENDING' | 'SETTLED' | 'FAILED' | string
}

export interface GoldBeanReferralSettlement {
  orderNo: string
  amountCent: number
  settlementStatus: 'PENDING' | 'SETTLED' | 'FAILED' | string
  transferState?: string | null
  merchantId?: string | null
  appId?: string | null
  userConfirmationRequired: boolean
  packageInfo?: string | null
  createdAt: string
  settledAt?: string | null
  failureReason?: string | null
}

export interface GoldBeanReferralAuthorization {
  state: 'NOT_AUTHORIZED' | 'REQUESTING' | 'WAIT_USER_CONFIRM' | 'TAKING_EFFECT' | 'CLOSED' | string
  available: boolean
  authorized: boolean
  userConfirmationRequired: boolean
  merchantId?: string | null
  appId?: string | null
  packageInfo?: string | null
  authorizedAt?: string | null
  lastCheckedAt?: string | null
  failureReason?: string | null
}

export interface GoldBeanLegendarySummary {
  eligible: boolean
  registered: boolean
  purchaseEnabled: boolean
  userId: string
  digitalBankBalance: number
  goldBeanUnitPriceCent: number
  maxPurchaseQuantity: number
  message: string
}

export interface GoldRobotStatus {
  enabled: boolean
  registered: boolean
  canRedeem: boolean
  groupConfigured: boolean
  costGoldBeans: number
  digitalBankBalance: number
  redeemedCount: number
  entitlementName: string
}

export interface GoldRobotRedemption {
  redemptionId: string
  redemptionNo: string
  entitlementCode: string
  entitlementName: string
  goldBeanCost: number
  digitalBankBalance: number
  groupName: string
  groupQrImageUrl: string
  redeemedAt: string
}

export interface GoldBeanPaymentParams {
  mode: 'short_series_goods' | 'short_series_coin' | string
  signData: string
  paySig: string
  signature: string
}

export interface GoldBeanLedgerEntry {
  id: string
  bucket: 'DIGITAL_BANK' | 'TRADING' | string
  direction: 'CREDIT' | 'DEBIT' | string
  amount: number
  eventType: string
  description: string
  createdAt: string
}

export interface GoldBeanTradeListing {
  id: string
  bucket: 'DIGITAL_BANK' | 'TRADING' | string
  regionCity?: string | null
  quantity: number
  remainingQuantity: number
  unitPriceCent: number
  totalAmount: number
  status: 'OPEN' | 'FILLED' | 'CANCELLED' | string
  mine: boolean
  createdAt: string
}

export type GoldBeanTradeListingPage = PageResponse<GoldBeanTradeListing>

export interface GoldBeanTrade {
  tradeNo: string
  listingId: string
  bucket: 'DIGITAL_BANK' | 'TRADING' | string
  quantity: number
  unitPriceCent: number
  totalAmount: number
  paymentAmount: number
  status: string
  createdAt: string
}

export interface GoldBeanTradePayout {
  tradeNo: string
  quantity: number
  amountCent: number
  bucket: 'DIGITAL_BANK' | 'TRADING' | string
  settlementStatus: string
  transferState?: string | null
  merchantId?: string | null
  appId?: string | null
  userConfirmationRequired: boolean
  packageInfo?: string | null
  createdAt: string
  settledAt?: string | null
  failureReason?: string | null
}

export interface GoldRegion {
  id: string
  city: string
  depth: number
  parentRegionId?: string
  status: string
  createdAt: string
}

export const getGoldBeanSummary = () =>
  request<GoldBeanSummary>({ url: '/api/client/gold-bean/summary', method: 'GET' })

export const getGoldBeanReferralNetwork = () =>
  request<GoldBeanReferralNetwork>({ url: '/api/client/gold-bean/referral-network', method: 'GET' })

export const registerGoldBean = (data: {
  referralCode?: string
  platformInviteCode?: string
  city?: string
}) =>
  request<GoldBeanSummary>({ url: '/api/client/gold-bean/register', method: 'POST', data })

export const createGoldBeanRegistrationOrder = (data: {
  referralCode?: string
  platformInviteCode?: string
  city?: string
}) =>
  request<GoldBeanOrder>({ url: '/api/client/gold-bean/registration-orders', method: 'POST', data })

export const createGoldBeanPurchaseOrder = (quantity: number) =>
  request<GoldBeanOrder>({
    url: '/api/client/gold-bean/purchase-orders',
    method: 'POST',
    data: { quantity },
  })

export const getGoldBeanLegendarySummary = () =>
  request<GoldBeanLegendarySummary>({ url: '/api/client/gold-bean/legendary/summary', method: 'GET' })

export const getGoldRobotStatus = () =>
  request<GoldRobotStatus>({ url: '/api/client/gold-bean/robot/status', method: 'GET' })

export const redeemGoldRobot = (clientRequestId: string) =>
  request<GoldRobotRedemption>({
    url: '/api/client/gold-bean/robot/redeem',
    method: 'POST',
    data: { clientRequestId },
  })

export const createGoldBeanLegendaryBankPurchaseOrder = (quantity: number) =>
  request<GoldBeanOrder>({
    url: '/api/client/gold-bean/legendary/purchase-orders',
    method: 'POST',
    data: { quantity },
  })

export const getGoldBeanOrder = (orderNo: string) =>
  request<GoldBeanOrder>({
    url: `/api/client/gold-bean/orders/${encodeURIComponent(orderNo)}`,
    method: 'GET',
  })

export const createWechatGoldBeanPayment = (orderNo: string) =>
  request<GoldBeanPaymentParams>({
    url: `/api/client/gold-bean/orders/${encodeURIComponent(orderNo)}/wechat-pay`,
    method: 'POST',
  })

export const cancelGoldBeanOrder = (orderNo: string) =>
  request<GoldBeanOrder>({
    url: `/api/client/gold-bean/orders/${encodeURIComponent(orderNo)}/cancel`,
    method: 'POST',
  })

export const getGoldBeanReferralSettlements = () =>
  request<GoldBeanReferralSettlement[]>({
    url: '/api/client/gold-bean/referral-settlements',
    method: 'GET',
  })

export const syncGoldBeanReferralSettlement = (orderNo: string) =>
  request<GoldBeanReferralSettlement>({
    url: `/api/client/gold-bean/referral-settlements/${encodeURIComponent(orderNo)}/sync`,
    method: 'POST',
  })

export const getGoldBeanReferralAuthorization = () =>
  request<GoldBeanReferralAuthorization>({
    url: '/api/client/gold-bean/referral-receive-authorization',
    method: 'GET',
  })

export const beginGoldBeanReferralAuthorization = () =>
  request<GoldBeanReferralAuthorization>({
    url: '/api/client/gold-bean/referral-receive-authorization',
    method: 'POST',
  })

export const syncGoldBeanReferralAuthorization = () =>
  request<GoldBeanReferralAuthorization>({
    url: '/api/client/gold-bean/referral-receive-authorization/sync',
    method: 'POST',
  })

export const getGoldBeanLedger = () =>
  request<GoldBeanLedgerEntry[]>({ url: '/api/client/gold-bean/ledger', method: 'GET' })

const withQuery = (path: string, params: { page?: number; size?: number }) => {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  return query ? `${path}?${query}` : path
}

export const getGoldBeanTradeMarket = (params: { page?: number; size?: number } = {}) =>
  request<GoldBeanTradeListingPage>({ url: withQuery('/api/client/gold-bean/trade/market', params), method: 'GET' })

export const getMyGoldBeanTradeListings = (params: { page?: number; size?: number } = {}) =>
  request<GoldBeanTradeListingPage>({ url: withQuery('/api/client/gold-bean/trade/listings/mine', params), method: 'GET' })

export const createGoldBeanTradeListing = (data: { quantity: number; bucket: 'DIGITAL_BANK' | 'TRADING' }) =>
  request<GoldBeanTradeListing>({ url: '/api/client/gold-bean/trade/listings', method: 'POST', data })

export const buyGoldBeanTradeListing = (listingId: string, quantity: number) =>
  request<GoldBeanTrade>({
    url: `/api/client/gold-bean/trade/listings/${encodeURIComponent(listingId)}/buy`,
    method: 'POST',
    data: { quantity },
  })

export const cancelGoldBeanTradeListing = (listingId: string) =>
  request<GoldBeanTradeListing>({
    url: `/api/client/gold-bean/trade/listings/${encodeURIComponent(listingId)}/cancel`,
    method: 'POST',
  })

export const getGoldBeanTrade = (tradeNo: string) =>
  request<GoldBeanTrade>({
    url: `/api/client/gold-bean/trade/orders/${encodeURIComponent(tradeNo)}`,
    method: 'GET',
  })

export const createWechatGoldBeanTradePayment = (tradeNo: string) =>
  request<GoldBeanPaymentParams>({
    url: `/api/client/gold-bean/trade/orders/${encodeURIComponent(tradeNo)}/wechat-pay`,
    method: 'POST',
  })

export const cancelGoldBeanTrade = (tradeNo: string) =>
  request<GoldBeanTrade>({
    url: `/api/client/gold-bean/trade/orders/${encodeURIComponent(tradeNo)}/cancel`,
    method: 'POST',
  })

export const getGoldBeanTradePayouts = () =>
  request<GoldBeanTradePayout[]>({
    url: '/api/client/gold-bean/trade/payouts',
    method: 'GET',
  })

export const syncGoldBeanTradePayout = (tradeNo: string) =>
  request<GoldBeanTradePayout>({
    url: `/api/client/gold-bean/trade/payouts/${encodeURIComponent(tradeNo)}/sync`,
    method: 'POST',
  })

export const openGoldRegion = (data: { city: string }) =>
  request<GoldRegion>({ url: '/api/client/gold-bean/regions', method: 'POST', data })

export const getMyGoldRegion = () =>
  request<GoldRegion | null>({ url: '/api/client/gold-bean/regions/mine', method: 'GET' })

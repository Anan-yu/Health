import { request } from '@/utils/request'

export interface GoldBeanSummary {
  enabled: boolean
  userId: string
  referralCode: string
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

export const registerGoldBean = (data: { referralCode?: string; city?: string }) =>
  request<GoldBeanSummary>({ url: '/api/client/gold-bean/register', method: 'POST', data })

export const getGoldBeanLedger = () =>
  request<GoldBeanLedgerEntry[]>({ url: '/api/client/gold-bean/ledger', method: 'GET' })

export const openGoldRegion = (data: { city: string; parentRegionId?: string }) =>
  request<GoldRegion>({ url: '/api/client/gold-bean/regions', method: 'POST', data })

export const getMyGoldRegion = () =>
  request<GoldRegion | null>({ url: '/api/client/gold-bean/regions/mine', method: 'GET' })

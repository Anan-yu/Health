<template>
  <view class="page gold-bean-admin-page">
    <view class="gold-bean-hero">
      <view class="hero-top">
        <view>
          <view class="hero-eyebrow">GOLD BEAN OPERATIONS</view>
          <view class="hero-title">金豆会员运营</view>
          <view class="hero-copy">查看会员、支付归属、授权码与金豆流水</view>
        </view>
        <view class="refresh-button" :class="{ disabled: loading }" @click="load">
          {{ loading ? '加载中' : '刷新' }}
        </view>
      </view>
      <view class="hero-footer">
        <view class="dev-badge">开发环境 · 权限受控</view>
        <view class="hero-note">支付订单、平台收款归属与金豆到账状态均以服务端回调为准。</view>
      </view>
    </view>

    <PageState :loading="loading" :error="error" :empty="!overview">
      <view v-if="overview" class="console-content">
        <view class="card invite-card">
          <view class="card-heading">
            <view>
              <view class="card-title">平台首会员授权</view>
              <view class="card-subtitle">客户没有推荐码时，必须使用一次性授权码；完整码只显示一次</view>
            </view>
            <view class="invite-count">可用 {{ availableInviteCount }} 个</view>
          </view>
          <view class="invite-policy">授权码只允许一个客户成功注册平台首会员。支付取消或订单过期会释放授权码，支付回调确认后自动核销。</view>
          <view class="field-label">绑定手机号（可选）</view>
          <input v-model="inviteBoundPhone" class="invite-input" type="number" maxlength="11" placeholder="绑定后仅该手机号对应的客户可使用" />
          <view class="field-helper">不绑定手机号时，可由任意已登录客户使用；建议正式发放时绑定手机号。</view>
          <view class="field-label">有效期（小时）</view>
          <input v-model.number="inviteValidHours" class="invite-input" type="number" maxlength="3" placeholder="1-168，默认24小时" />
          <view class="invite-actions">
            <view class="invite-primary" :class="{ disabled: inviteIssuing }" @tap="issueInvite">{{ inviteIssuing ? '生成中…' : '生成一次性授权码' }}</view>
            <view class="invite-secondary" :class="{ disabled: inviteLoading }" @tap="loadInvites">{{ inviteLoading ? '刷新中…' : '刷新授权码' }}</view>
          </view>
          <view v-if="issuedInvite" class="issued-invite">
            <view class="issued-title">授权码已生成，请立即复制</view>
            <view class="issued-code">{{ issuedInvite.code }}</view>
            <view class="issued-note">出于安全考虑，离开当前页面后不能再次查看完整授权码。</view>
            <view class="invite-copy" @tap="copyInviteCode">复制授权码</view>
          </view>
          <view
            class="invite-list-heading invite-list-toggle"
            role="button"
            :aria-expanded="inviteListExpanded"
            :aria-label="inviteListExpanded ? '收起最近授权码' : '展开最近授权码'"
            @tap="toggleInviteList"
          >
            <view>最近授权码 <text v-if="invites.length" class="invite-list-count">（{{ invites.length }}）</text></view>
            <view class="invite-list-toggle-label">{{ inviteListExpanded ? '收起' : '展开' }} <text>{{ inviteListExpanded ? '⌃' : '⌄' }}</text></view>
          </view>
          <view v-if="inviteListExpanded">
            <view v-if="inviteLoading && !invites.length" class="list-empty">正在加载授权码…</view>
            <view v-else-if="!invites.length" class="list-empty">暂无授权码</view>
            <view v-else class="invite-list">
              <view v-for="invite in invites" :key="invite.id" class="invite-row">
                <view class="invite-row-main">
                  <view class="invite-code-mask">{{ invite.codeMasked }}</view>
                  <view class="invite-row-meta">{{ invite.boundPhoneMasked || '未绑定手机号' }} · 到期 {{ formatDate(invite.expiresAt) }}</view>
                  <view v-if="invite.reservedOrderNo" class="invite-row-meta">已占用订单 {{ invite.reservedOrderNo }}</view>
                  <view v-if="invite.consumedOrderNo" class="invite-row-meta">已核销订单 {{ invite.consumedOrderNo }}</view>
                </view>
                <view class="invite-row-side">
                  <view class="invite-status" :class="`invite-status-${invite.status.toLowerCase()}`">{{ inviteStatusLabel(invite.status) }}</view>
                  <view v-if="invite.status === 'AVAILABLE'" class="invite-revoke" @tap="revokeInvite(invite)">撤销</view>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="card legendary-admin-card">
          <view class="card-heading">
            <view>
              <view class="card-title">传奇人物资格</view>
              <view class="card-subtitle">提前录入手机号，匹配成功后可查看数字银行余额与购豆资格</view>
            </view>
            <view class="invite-count">已启用 {{ activeLegendaryCount }} 人</view>
          </view>
          <view class="invite-policy">传奇人物不是新的登录角色，只是平台管理员授予的购豆资格；手机号仅以掩码展示，撤销后立即不能创建新订单。</view>
          <view class="field-label">手机号</view>
          <input v-model="legendaryPhone" class="invite-input" type="number" maxlength="11" placeholder="请输入传奇人物登录手机号" />
          <view class="field-label">备注（可选）</view>
          <input v-model="legendaryNote" class="invite-input" maxlength="255" placeholder="例如：区域负责人" />
          <view class="invite-actions">
            <view class="invite-primary" :class="{ disabled: legendaryCreating }" @tap="createLegendary">{{ legendaryCreating ? '录入中…' : '录入传奇人物' }}</view>
            <view class="invite-secondary" :class="{ disabled: legendaryLoading }" @tap="loadLegendary">{{ legendaryLoading ? '刷新中…' : '刷新名单' }}</view>
          </view>
          <view class="invite-list-heading">已录入手机号</view>
          <view v-if="legendaryLoading && !legendary.length" class="list-empty">正在加载传奇人物名单…</view>
          <view v-else-if="!legendary.length" class="list-empty">暂无传奇人物手机号</view>
          <view v-else class="invite-list">
            <view v-for="person in legendary" :key="person.id" class="invite-row">
              <view class="invite-row-main">
                <view class="invite-code-mask">{{ person.phoneMasked }}</view>
                <view class="invite-row-meta">{{ person.matchedDisplayName || '尚未匹配登录用户' }} · {{ person.note || '无备注' }}</view>
                <view v-if="person.matchedRegistrationStatus" class="invite-row-meta">注册状态：{{ feeStatusLabel(person.matchedRegistrationStatus) }} · {{ person.matchedMemberLevelName || '普通会员' }}</view>
                <view class="invite-row-balance">数字银行余额：{{ legendaryBankBalance(person.digitalBankBalance) }}</view>
              </view>
              <view class="invite-row-side">
                <view class="invite-status" :class="person.status === 'ACTIVE' ? 'invite-status-available' : 'invite-status-revoked'">{{ person.status === 'ACTIVE' ? '已启用' : '已撤销' }}</view>
                <view v-if="person.status === 'ACTIVE'" class="invite-revoke" @tap="revokeLegendary(person)">撤销</view>
              </view>
            </view>
          </view>
        </view>

        <view class="metric-grid">
          <view class="metric-card">
            <view class="metric-label">会员账户</view>
            <view class="metric-value">{{ overview.totalMemberCount }}</view>
            <view class="metric-note">已建档账户</view>
          </view>
          <view class="metric-card metric-card-warm">
            <view class="metric-label">已记录注册</view>
            <view class="metric-value">{{ overview.registeredMemberCount }}</view>
            <view class="metric-note">待记录 {{ overview.pendingMemberCount }}</view>
          </view>
            <view class="metric-card metric-card-blue">
            <view class="metric-label">平台收款记录</view>
            <view class="metric-value">{{ overview.platformFeeRegistrationCount }}</view>
            <view class="metric-note">推荐人收取 {{ overview.referrerFeeRegistrationCount }}</view>
          </view>
          <view class="metric-card metric-card-purple">
            <view class="metric-label">金豆总余额</view>
            <view class="metric-value">{{ formatBalance(overview.totalGoldBeanBalance) }}</view>
            <view class="metric-note">银行 {{ formatBalance(overview.digitalBankBalance) }}</view>
          </view>
        </view>

        <view class="section-header">
          <view>
            <view class="section-title">运营数据</view>
            <view class="section-subtitle">支持按用户昵称或手机号核对记录，最多展示 200 条</view>
          </view>
          <view class="region-count">活跃区域 {{ overview.activeRegionCount }}</view>
        </view>

        <view class="tab-row">
          <view
            v-for="tab in tabs"
            :key="tab.value"
            class="tab-item"
            :class="{ active: activeTab === tab.value }"
            @click="switchTab(tab.value)"
          >
            <view class="tab-icon">{{ tab.icon }}</view>
            <view>{{ tab.label }}</view>
          </view>
        </view>

        <view class="card filter-card">
          <view class="filter-caption search-label">搜索用户昵称或手机号</view>
          <view class="filter-row">
            <input
              v-model="keyword"
              class="keyword-input"
              placeholder="请输入用户昵称或手机号"
              aria-label="搜索用户昵称或手机号"
              confirm-type="search"
              @confirm="searchCurrent"
            />
            <view class="search-button" :class="{ disabled: listLoading }" @click="searchCurrent">
              搜索
            </view>
          </view>
          <view v-if="activeTab === 'accounts'" class="picker-row">
            <view class="filter-caption">注册状态</view>
            <picker
              class="filter-picker"
              :range="registrationStatusOptions"
              range-key="label"
              :value="registrationStatusIndex"
              @change="onRegistrationStatusChange"
            >
              <view class="picker-value">{{ registrationStatusOptions[registrationStatusIndex].label }} <text>⌄</text></view>
            </picker>
          </view>
          <view v-if="activeTab === 'ledger'" class="picker-row">
            <view class="filter-caption">流水类型</view>
            <picker
              class="filter-picker"
              :range="ledgerEventOptions"
              range-key="label"
              :value="ledgerEventIndex"
              @change="onLedgerEventChange"
            >
              <view class="picker-value">{{ ledgerEventOptions[ledgerEventIndex].label }} <text>⌄</text></view>
            </picker>
          </view>
          <view v-if="activeTab === 'orders'" class="picker-row">
            <view class="filter-caption">订单状态</view>
            <picker
              class="filter-picker"
              :range="orderStatusOptions"
              range-key="label"
              :value="orderStatusIndex"
              @change="onOrderStatusChange"
            >
              <view class="picker-value">{{ orderStatusOptions[orderStatusIndex].label }} <text>⌄</text></view>
            </picker>
          </view>
          <view v-if="activeTab === 'orders'" class="picker-row">
            <view class="filter-caption">订单类型</view>
            <picker
              class="filter-picker"
              :range="orderTypeOptions"
              range-key="label"
              :value="orderTypeIndex"
              @change="onOrderTypeChange"
            >
              <view class="picker-value">{{ orderTypeOptions[orderTypeIndex].label }} <text>⌄</text></view>
            </picker>
          </view>
        </view>

        <view v-if="listLoading" class="list-loading">正在更新列表…</view>

        <view v-else-if="activeTab === 'accounts'" class="record-list">
          <view v-if="accounts.length === 0" class="list-empty">暂无匹配的会员账户</view>
          <view v-for="account in accounts" :key="account.accountId" class="card record-card">
            <view class="record-head">
              <view class="identity-wrap">
                <view class="record-icon level-icon">{{ account.memberLevelName.slice(0, 1) }}</view>
                <view class="identity-copy">
                  <view class="record-name">{{ account.displayName }}</view>
                  <view class="record-meta">{{ account.phoneMasked }} · {{ account.tenantName }}</view>
                </view>
              </view>
              <view class="level-badge">{{ account.memberLevelName }}</view>
            </view>
            <view class="record-code-row">
              <text>推荐码 {{ account.referralCode || '未生成' }}</text>
              <text>直推 {{ account.directReferralCount }} 人</text>
            </view>
            <view class="account-grid">
              <view class="account-item">
                <view class="item-label">注册费记录</view>
                <view class="item-value">{{ account.registrationFeeYuan }} 元 · {{ feeStatusLabel(account.registrationFeeStatus) }}</view>
              </view>
              <view class="account-item">
                <view class="item-label">费用归属</view>
                <view class="item-value">{{ account.feeRecipientName }}</view>
              </view>
              <view class="account-item">
                <view class="item-label">金豆余额</view>
                <view class="item-value emphasis">{{ formatBalance(account.totalBalance) }} 豆</view>
              </view>
              <view class="account-item">
                <view class="item-label">每日奖励</view>
                <view class="item-value">{{ account.dailyRewardDays }}/{{ account.dailyRewardTotalDays }} 天</view>
              </view>
            </view>
            <view class="record-footer">
              <text>银行 {{ formatBalance(account.digitalBankBalance) }} · 可交易 {{ formatBalance(account.tradingBalance) }}</text>
              <text>{{ account.city || '未设置城市' }} · 限额 {{ account.tradeLimitPercent }}%</text>
            </view>
          </view>
        </view>

        <view v-else-if="activeTab === 'orders'" class="record-list">
          <view v-if="orders.length === 0" class="list-empty">暂无匹配的支付订单</view>
          <view v-for="order in orders" :key="order.orderNo" class="card record-card order-card">
            <view class="record-head">
              <view class="identity-wrap">
                <view class="record-icon order-icon">付</view>
                <view class="identity-copy">
                  <view class="record-name">{{ order.customerName }}</view>
                  <view class="record-meta">{{ order.tenantName }} · {{ order.orderNo }}</view>
                </view>
              </view>
              <view class="level-badge" :class="{ 'order-paid': order.status === 'PAID' }">{{ orderStatusLabel(order.status) }}</view>
            </view>
            <view class="order-summary">
              <view><text class="item-label">订单类型</text><text class="detail-value">{{ orderTypeLabel(order.orderType) }}</text></view>
              <view><text class="item-label">用户支付</text><text class="detail-value emphasis">¥{{ ((order.paymentAmountCent ?? order.amountCent) / 100).toFixed(2) }}</text></view>
              <view><text class="item-label">业务/结算金额</text><text class="detail-value">¥{{ (order.amountCent / 100).toFixed(2) }}</text></view>
              <view><text class="item-label">金豆数量</text><text class="detail-value">{{ order.goldBeanQuantity ? `${formatNumber(order.goldBeanQuantity)} 豆` : '注册权益' }}</text></view>
              <view><text class="item-label">收款归属</text><text class="detail-value">{{ order.feeRecipientName || '—' }}</text></view>
              <view v-if="order.orderType === 'REGISTRATION_FEE' && order.feeRecipientName !== '平台'"><text class="item-label">结算状态</text><text class="detail-value">{{ settlementStatusLabel(order.settlementStatus) }}</text></view>
            </view>
            <view class="record-footer">
              <text>{{ order.paymentChannel || '未支付' }} · 交易号 {{ order.transactionIdMasked || '—' }}</text>
              <text>{{ order.paidAt ? formatDate(order.paidAt) : formatDate(order.createdAt) }}</text>
            </view>
            <view v-if="order.settlementFailureReason" class="order-settlement-error">{{ order.settlementFailureReason }}</view>
          </view>
        </view>

        <view v-else-if="activeTab === 'referrals'" class="record-list">
          <view v-if="referrals.length === 0" class="list-empty">暂无匹配的推荐关系</view>
          <view v-for="referral in referrals" :key="referral.id" class="card record-card referral-card">
            <view class="referral-route">
              <view class="referral-person">
                <view class="record-icon referrer-icon">荐</view>
                <view>
                  <view class="record-name">{{ referral.referrerName }}</view>
                  <view class="person-caption">推荐人</view>
                </view>
              </view>
              <view class="route-arrow">→</view>
              <view class="referral-person referred-person">
                <view>
                  <view class="record-name">{{ referral.referredName }}</view>
                  <view class="person-caption">新入会员</view>
                </view>
                <view class="record-icon referred-icon">会</view>
              </view>
            </view>
            <view class="record-detail-grid">
              <view><text class="item-label">机构</text><text class="detail-value">{{ referral.tenantName }}</text></view>
              <view><text class="item-label">推荐码</text><text class="detail-value">{{ referral.referralCode || '—' }}</text></view>
              <view><text class="item-label">费用归属</text><text class="detail-value">{{ referral.feeRecipientName }}</text></view>
              <view><text class="item-label">记录时间</text><text class="detail-value">{{ formatDate(referral.registeredAt) }}</text></view>
            </view>
          </view>
        </view>

        <view v-else class="record-list">
          <view v-if="ledger.length === 0" class="list-empty">暂无匹配的金豆流水</view>
          <view v-for="entry in ledger" :key="entry.id" class="card record-card ledger-card">
            <view class="record-head">
              <view class="identity-wrap">
                <view class="record-icon ledger-icon">豆</view>
                <view class="identity-copy">
                  <view class="record-name">{{ eventTypeLabel(entry.eventType) }}</view>
                  <view class="record-meta">{{ entry.displayName }} · {{ entry.tenantName }}</view>
                </view>
              </view>
              <view class="ledger-amount" :class="{ debit: entry.direction === 'DEBIT' }">
                {{ entry.direction === 'DEBIT' ? '-' : '+' }}{{ formatNumber(entry.amount) }} 豆
              </view>
            </view>
            <view class="ledger-detail">
              <view class="ledger-description">{{ entry.description || '—' }}</view>
              <view class="ledger-tags">
                <view class="ledger-tag">{{ bucketLabel(entry.bucket) }}</view>
                <view class="ledger-time">{{ formatDate(entry.createdAt) }}</view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import PageState from '@/components/PageState.vue'
import { formatGoldBeanAmount, formatGoldBeanBalance } from '@/utils/gold-bean-amount'
import {
  getPlatformGoldBeanAccounts,
  getPlatformGoldBeanLedger,
  getPlatformGoldBeanOrders,
  getPlatformGoldBeanInvites,
  getPlatformGoldBeanOverview,
  getPlatformGoldBeanReferrals,
  createPlatformGoldBeanInvite,
  revokePlatformGoldBeanInvite,
  createPlatformGoldBeanLegendary,
  getPlatformGoldBeanLegendary,
  revokePlatformGoldBeanLegendary,
} from '@/api/platform-gold-bean'
import type {
  PlatformGoldBeanAccount,
  PlatformGoldBeanInvite,
  PlatformGoldBeanInviteCreated,
  PlatformGoldBeanLedger,
  PlatformGoldBeanOrder,
  PlatformGoldBeanOverview,
  PlatformGoldBeanReferral,
  PlatformGoldBeanLegendary,
} from '@/types/api'

type AdminTab = 'accounts' | 'orders' | 'referrals' | 'ledger'

const overview = ref<PlatformGoldBeanOverview | null>(null)
const accounts = ref<PlatformGoldBeanAccount[]>([])
const orders = ref<PlatformGoldBeanOrder[]>([])
const referrals = ref<PlatformGoldBeanReferral[]>([])
const ledger = ref<PlatformGoldBeanLedger[]>([])
const invites = ref<PlatformGoldBeanInvite[]>([])
const loading = ref(true)
const listLoading = ref(false)
const inviteLoading = ref(false)
const inviteIssuing = ref(false)
const error = ref('')
const keyword = ref('')
const activeTab = ref<AdminTab>('accounts')
const registrationStatus = ref('ALL')
const ledgerEventType = ref('ALL')
const inviteBoundPhone = ref('')
const inviteValidHours = ref(24)
const issuedInvite = ref<PlatformGoldBeanInviteCreated | null>(null)
const inviteListExpanded = ref(false)
const legendary = ref<PlatformGoldBeanLegendary[]>([])
const legendaryLoading = ref(false)
const legendaryCreating = ref(false)
const legendaryPhone = ref('')
const legendaryNote = ref('')

const tabs: Array<{ label: string; value: AdminTab; icon: string }> = [
  { label: '会员账户', value: 'accounts', icon: '会' },
  { label: '支付订单', value: 'orders', icon: '付' },
  { label: '推荐关系', value: 'referrals', icon: '荐' },
  { label: '金豆流水', value: 'ledger', icon: '豆' },
]
const registrationStatusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '已记录', value: 'PAID' },
  { label: '待记录', value: 'UNPAID' },
]
const ledgerEventOptions = [
  { label: '全部流水', value: 'ALL' },
  { label: '初始金豆', value: 'INITIAL_GRANT' },
  { label: '每日奖励', value: 'DAILY_REWARD' },
  { label: '直推奖励', value: 'REFERRAL_DIRECT' },
  { label: '下游推荐奖励', value: 'REFERRAL_DOWNLINE' },
  { label: '历史二级推荐奖励', value: 'REFERRAL_SECOND_LEVEL' },
  { label: '等级奖励', value: 'LEVEL_REWARD' },
  { label: '区域代理返利', value: 'REGION_REWARD' },
  { label: '平台购买金豆', value: 'PLATFORM_PURCHASE' },
  { label: '传奇人物购豆', value: 'LEGENDARY_BANK_PURCHASE' },
  { label: '机器人权益兑换', value: 'ROBOT_REDEEM' },
  { label: '集市买入', value: 'TRADE_SETTLEMENT_BUY' },
  { label: '集市卖出', value: 'TRADE_SETTLEMENT_SELL' },
]
const orderStatusOptions = [
  { label: '全部订单', value: 'ALL' },
  { label: '待支付', value: 'PENDING' },
  { label: '已支付', value: 'PAID' },
  { label: '已关闭', value: 'CLOSED' },
]
const orderTypeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '平台注册费', value: 'REGISTRATION_FEE' },
  { label: '平台购买金豆', value: 'GOLD_BEAN_PURCHASE' },
  { label: '传奇人物数字银行购豆', value: 'LEGENDARY_BANK_PURCHASE' },
]
const registrationStatusIndex = computed(() =>
  Math.max(0, registrationStatusOptions.findIndex((item) => item.value === registrationStatus.value)),
)
const ledgerEventIndex = computed(() =>
  Math.max(0, ledgerEventOptions.findIndex((item) => item.value === ledgerEventType.value)),
)
const orderStatus = ref('ALL')
const orderType = ref('ALL')
const orderStatusIndex = computed(() =>
  Math.max(0, orderStatusOptions.findIndex((item) => item.value === orderStatus.value)),
)
const orderTypeIndex = computed(() => Math.max(0, orderTypeOptions.findIndex((item) => item.value === orderType.value)))

const formatNumber = (value: number) => formatGoldBeanAmount(value)
const legendaryBankBalance = (value?: number | null) =>
  value == null ? '未建档' : `${formatBalance(value)} 豆`
const formatBalance = (value: number) => formatGoldBeanBalance(value)
const formatDate = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '—')
const inviteStatusLabel = (value: string) => ({
  AVAILABLE: '可使用',
  RESERVED: '支付中',
  CONSUMED: '已核销',
  REVOKED: '已撤销',
  EXPIRED: '已过期',
}[value] || value || '未知')
const availableInviteCount = computed(() => invites.value.filter((item) => item.status === 'AVAILABLE').length)
const activeLegendaryCount = computed(() => legendary.value.filter((item) => item.status === 'ACTIVE').length)
const feeStatusLabel = (value: string) => (value === 'PAID' ? '已记录' : value === 'UNPAID' ? '待记录' : value || '未知')
const ledgerEventLabels: Record<string, string> = {
  INITIAL_GRANT: '初始金豆',
  DAILY_REWARD: '每日奖励',
  REFERRAL_DIRECT: '直推奖励',
  REFERRAL_DOWNLINE: '下游推荐奖励',
  REFERRAL_SECOND_LEVEL: '历史二级推荐奖励',
  LEVEL_REWARD: '等级奖励',
  REGION_PROFIT: '区域代理返利',
  PLATFORM_PURCHASE: '平台购买金豆',
  LEGENDARY_BANK_PURCHASE: '传奇人物购豆',
  ROBOT_REDEEM: '机器人权益兑换',
  TRADE_SETTLEMENT_BUY: '集市买入',
  TRADE_SETTLEMENT_SELL: '集市卖出',
}
const eventTypeLabel = (value: string) =>
  ledgerEventLabels[value] || ledgerEventOptions.find((item) => item.value === value)?.label || (value ? '其他金豆流水' : '未分类流水')
const bucketLabel = (value: string) =>
  value === 'DIGITAL_BANK' ? '数字银行' : value === 'TRADING' ? '可交易' : value || '未分桶'
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const [nextOverview, accountPage, referralPage, ledgerPage, orderPage, invitePage, legendaryPage] = await Promise.all([
      getPlatformGoldBeanOverview(),
      getPlatformGoldBeanAccounts({ registrationStatus: registrationStatus.value }),
      getPlatformGoldBeanReferrals(),
      getPlatformGoldBeanLedger({ eventType: ledgerEventType.value }),
      getPlatformGoldBeanOrders({ status: orderStatus.value, orderType: orderType.value }),
      getPlatformGoldBeanInvites(),
      getPlatformGoldBeanLegendary(),
    ])
    overview.value = nextOverview
    accounts.value = accountPage.records
    referrals.value = referralPage.records
    ledger.value = ledgerPage.records
    orders.value = orderPage.records
    invites.value = invitePage.records
    legendary.value = legendaryPage.records
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '金豆运营数据加载失败'
  } finally {
    loading.value = false
  }
}

const loadLegendary = async () => {
  if (legendaryLoading.value) return
  legendaryLoading.value = true
  try {
    legendary.value = (await getPlatformGoldBeanLegendary()).records
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '传奇人物名单加载失败', icon: 'none' })
  } finally {
    legendaryLoading.value = false
  }
}

const createLegendary = async () => {
  if (legendaryCreating.value) return
  const phone = legendaryPhone.value.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    uni.showToast({ title: '请输入正确的11位手机号', icon: 'none' })
    return
  }
  legendaryCreating.value = true
  try {
    await createPlatformGoldBeanLegendary({ phone, note: legendaryNote.value.trim() || undefined })
    legendaryPhone.value = ''
    legendaryNote.value = ''
    await loadLegendary()
    uni.showToast({ title: '传奇人物资格已录入', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '传奇人物资格录入失败', icon: 'none' })
  } finally {
    legendaryCreating.value = false
  }
}

const revokeLegendary = (person: PlatformGoldBeanLegendary) => {
  uni.showModal({
    title: '撤销传奇资格',
    content: `撤销 ${person.phoneMasked} 的购豆资格吗？已支付订单不受影响。`,
    success: (result) => {
      if (!result.confirm) return
      void (async () => {
        try {
          await revokePlatformGoldBeanLegendary(person.id)
          await loadLegendary()
          uni.showToast({ title: '传奇人物资格已撤销', icon: 'success' })
        } catch (cause) {
          uni.showToast({ title: cause instanceof Error ? cause.message : '传奇人物资格撤销失败', icon: 'none' })
        }
      })()
    },
  })
}

const loadInvites = async () => {
  if (inviteLoading.value) return
  inviteLoading.value = true
  try {
    invites.value = (await getPlatformGoldBeanInvites()).records
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '授权码加载失败', icon: 'none' })
  } finally {
    inviteLoading.value = false
  }
}

const toggleInviteList = () => {
  inviteListExpanded.value = !inviteListExpanded.value
}

const issueInvite = async () => {
  if (inviteIssuing.value) return
  const validHours = Math.floor(Number(inviteValidHours.value) || 0)
  if (validHours < 1 || validHours > 168) {
    uni.showToast({ title: '有效期需为1-168小时', icon: 'none' })
    return
  }
  inviteIssuing.value = true
  try {
    issuedInvite.value = await createPlatformGoldBeanInvite({
      boundPhone: inviteBoundPhone.value.trim() || undefined,
      validHours,
    })
    invites.value = (await getPlatformGoldBeanInvites()).records
    uni.showToast({ title: '授权码已生成，请及时复制', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '授权码生成失败', icon: 'none' })
  } finally {
    inviteIssuing.value = false
  }
}

const copyInviteCode = () => {
  if (!issuedInvite.value) return
  uni.setClipboardData({
    data: issuedInvite.value.code,
    success: () => uni.showToast({ title: '授权码已复制', icon: 'success' }),
  })
}

const revokeInvite = (invite: PlatformGoldBeanInvite) => {
  uni.showModal({
    title: '撤销授权码',
    content: '撤销后该授权码不能再使用，确定继续吗？',
    success: (result) => {
      if (!result.confirm) return
      void (async () => {
        try {
          await revokePlatformGoldBeanInvite(invite.id)
          invites.value = (await getPlatformGoldBeanInvites()).records
          uni.showToast({ title: '授权码已撤销', icon: 'success' })
        } catch (cause) {
          uni.showToast({ title: cause instanceof Error ? cause.message : '授权码撤销失败', icon: 'none' })
        }
      })()
    },
  })
}

const loadCurrentList = async () => {
  listLoading.value = true
  try {
    if (activeTab.value === 'accounts') {
      accounts.value = (
        await getPlatformGoldBeanAccounts({
          keyword: keyword.value.trim(),
          registrationStatus: registrationStatus.value,
        })
      ).records
    } else if (activeTab.value === 'referrals') {
      referrals.value = (await getPlatformGoldBeanReferrals(keyword.value.trim())).records
    } else if (activeTab.value === 'orders') {
      orders.value = (await getPlatformGoldBeanOrders({
        keyword: keyword.value.trim(),
        status: orderStatus.value,
        orderType: orderType.value,
      })).records
    } else {
      ledger.value = (
        await getPlatformGoldBeanLedger({
          keyword: keyword.value.trim(),
          eventType: ledgerEventType.value,
        })
      ).records
    }
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '列表加载失败', icon: 'none' })
  } finally {
    listLoading.value = false
  }
}

const switchTab = (tab: AdminTab) => {
  if (activeTab.value === tab || listLoading.value) return
  activeTab.value = tab
  void loadCurrentList()
}
const searchCurrent = () => {
  if (!listLoading.value) void loadCurrentList()
}
const onRegistrationStatusChange = (event: { detail: { value: string } }) => {
  registrationStatus.value = registrationStatusOptions[Number(event.detail.value)].value
  void loadCurrentList()
}
const onLedgerEventChange = (event: { detail: { value: string } }) => {
  ledgerEventType.value = ledgerEventOptions[Number(event.detail.value)].value
  void loadCurrentList()
}
const onOrderStatusChange = (event: { detail: { value: string } }) => {
  orderStatus.value = orderStatusOptions[Number(event.detail.value)].value
  void loadCurrentList()
}
const onOrderTypeChange = (event: { detail: { value: string } }) => {
  orderType.value = orderTypeOptions[Number(event.detail.value)].value
  void loadCurrentList()
}
const orderStatusLabel = (value: string) =>
  orderStatusOptions.find((item) => item.value === value)?.label || value || '未知'
const orderTypeLabel = (value: string) =>
  orderTypeOptions.find((item) => item.value === value)?.label || value || '未知订单'
const settlementStatusLabel = (value?: string) => ({
  NOT_REQUIRED: '无需结算',
  PENDING: '结算中',
  SETTLED: '已结算',
  FAILED: '结算失败',
}[value || ''] || value || '待确认')

defineExpose({ refresh: load })
</script>

<style scoped>
.gold-bean-admin-page {
  padding-top: 28rpx;
  padding-bottom: 48rpx;
}
.gold-bean-hero {
  position: relative;
  overflow: hidden;
  padding: 32rpx 30rpx 28rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #0b6b57, #159579);
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(12, 102, 82, 0.2);
}
.gold-bean-hero::after {
  position: absolute;
  top: -128rpx;
  right: -68rpx;
  width: 300rpx;
  height: 300rpx;
  border: 34rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  content: '';
}
.hero-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}
.hero-eyebrow {
  color: rgba(232, 255, 248, 0.72);
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}
.hero-title {
  margin-top: 9rpx;
  font-size: 42rpx;
  font-weight: 760;
}
.hero-copy {
  margin-top: 8rpx;
  color: rgba(238, 255, 249, 0.9);
  font-size: 25rpx;
  line-height: 1.5;
}
.refresh-button {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 128rpx;
  min-height: 76rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.3);
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
}
.refresh-button.disabled,
.search-button.disabled {
  opacity: 0.55;
  pointer-events: none;
}
.hero-footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 24rpx;
  padding-top: 19rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.2);
}
.dev-badge {
  flex: 0 0 auto;
  padding: 9rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: #e5fff6;
  font-size: 21rpx;
  font-weight: 700;
}
.hero-note {
  color: rgba(238, 255, 249, 0.78);
  font-size: 21rpx;
  line-height: 1.45;
}
.console-content {
  padding-top: 24rpx;
}
.record-icon,
.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 58rpx;
  height: 58rpx;
  border-radius: 18rpx;
  font-size: 25rpx;
  font-weight: 750;
}
.card.invite-card {
  margin-top: 20rpx;
  padding: 26rpx;
  border-color: #c9e9dc;
  background: linear-gradient(145deg, #f4fdf9, #fff);
}
.card.legendary-admin-card {
  margin-top: 20rpx;
  padding: 26rpx;
  border-color: #d8d0ed;
  background: linear-gradient(145deg, #faf8ff, #fff);
}
.invite-count {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  color: #0d8067;
  background: #e2f7ed;
  font-size: 20rpx;
  font-weight: 700;
  white-space: nowrap;
}
.invite-policy {
  margin-top: 18rpx;
  padding: 16rpx 18rpx;
  border-radius: 16rpx;
  color: #55776c;
  background: #edf9f4;
  font-size: 21rpx;
  line-height: 1.55;
}
.invite-card .field-label {
  margin-top: 18rpx;
  color: #48665e;
  font-size: 23rpx;
  font-weight: 700;
}
.invite-card .field-helper {
  margin-top: 8rpx;
  color: #81958e;
  font-size: 20rpx;
  line-height: 1.4;
}
.invite-input {
  width: 100%;
  height: 76rpx;
  margin-top: 10rpx;
  padding: 0 20rpx;
  box-sizing: border-box;
  border: 2rpx solid #cfe8de;
  border-radius: 16rpx;
  color: #294b41;
  background: #fff;
  font-size: 24rpx;
}
.invite-actions {
  display: flex;
  gap: 14rpx;
  margin-top: 20rpx;
}
.invite-primary,
.invite-secondary,
.invite-copy,
.invite-revoke {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 76rpx;
  border-radius: 16rpx;
  font-size: 23rpx;
  font-weight: 700;
}
.invite-primary {
  flex: 1;
  color: #fff;
  background: #0d8568;
}
.invite-secondary {
  flex: 0 0 160rpx;
  border: 1rpx solid #bfe5d8;
  color: #0d8067;
  background: #f0fbf6;
}
.invite-primary.disabled,
.invite-secondary.disabled {
  opacity: 0.55;
  pointer-events: none;
}
.issued-invite {
  margin-top: 20rpx;
  padding: 20rpx;
  border: 2rpx solid #f1d48e;
  border-radius: 18rpx;
  background: #fff9e9;
}
.issued-title {
  color: #8a641b;
  font-size: 23rpx;
  font-weight: 750;
}
.issued-code {
  margin-top: 14rpx;
  padding: 18rpx 12rpx;
  border-radius: 14rpx;
  color: #096f56;
  background: #fff;
  font-family: monospace;
  font-size: 29rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
  text-align: center;
  word-break: break-all;
}
.issued-note {
  margin-top: 10rpx;
  color: #96794e;
  font-size: 20rpx;
  line-height: 1.45;
}
.invite-copy {
  min-height: 64rpx;
  margin-top: 14rpx;
  color: #fff;
  background: #c48a28;
}
.invite-list-heading {
  margin-top: 26rpx;
  color: #31554b;
  font-size: 24rpx;
  font-weight: 750;
}
.invite-list-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72rpx;
  margin-right: -8rpx;
  padding: 0 8rpx;
  border-radius: 14rpx;
}
.invite-list-toggle > view:first-child {
  min-width: 0;
}
.invite-list-toggle:active {
  background: #f2faf6;
}
.invite-list-count,
.invite-list-toggle-label {
  flex: 0 0 auto;
  color: #0d8067;
  font-size: 21rpx;
  font-weight: 700;
  white-space: nowrap;
}
.invite-list-toggle-label text {
  margin-left: 4rpx;
  font-size: 25rpx;
}
.invite-list {
  margin-top: 10rpx;
}
.invite-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #edf3f0;
}
.invite-row:last-child {
  border-bottom: 0;
}
.invite-row-main {
  min-width: 0;
}
.invite-code-mask {
  color: #31554b;
  font-family: monospace;
  font-size: 24rpx;
  font-weight: 750;
}
.invite-row-meta {
  margin-top: 6rpx;
  overflow: hidden;
  color: #91a39d;
  font-size: 19rpx;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.invite-row-balance {
  margin-top: 8rpx;
  color: #0d8067;
  font-size: 21rpx;
  font-weight: 750;
  line-height: 1.4;
}
.invite-row-side {
  display: flex;
  flex: 0 0 auto;
  align-items: flex-end;
  flex-direction: column;
  gap: 10rpx;
}
.invite-status {
  padding: 7rpx 12rpx;
  border-radius: 999rpx;
  color: #6e8980;
  background: #f1f6f3;
  font-size: 19rpx;
  white-space: nowrap;
}
.invite-status-available {
  color: #0b8065;
  background: #e4f8ef;
}
.invite-status-reserved {
  color: #a36e18;
  background: #fff4da;
}
.invite-status-consumed {
  color: #4276a0;
  background: #e8f4ff;
}
.invite-status-revoked,
.invite-status-expired {
  color: #8d7770;
  background: #f4efed;
}
.invite-revoke {
  min-height: 54rpx;
  padding: 0 14rpx;
  border: 1rpx solid #f0c9c0;
  color: #b15e50;
  background: #fff7f5;
  font-size: 20rpx;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 20rpx;
}
.metric-card {
  min-height: 150rpx;
  padding: 22rpx;
  box-sizing: border-box;
  border: 1rpx solid #d7eee6;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 8rpx 22rpx rgba(28, 102, 82, 0.06);
}
.metric-card-warm {
  border-color: #f4dfb2;
  background: #fffaf0;
}
.metric-card-blue {
  border-color: #d3e8f5;
  background: #f5fbff;
}
.metric-card-purple {
  border-color: #e3d8f2;
  background: #fbf8ff;
}
.metric-label {
  color: #71847e;
  font-size: 22rpx;
}
.metric-value {
  margin-top: 10rpx;
  color: #0c8067;
  font-size: 38rpx;
  font-weight: 760;
  line-height: 1.1;
}
.metric-card-warm .metric-value {
  color: #bd7a16;
}
.metric-card-blue .metric-value {
  color: #317ea8;
}
.metric-card-purple .metric-value {
  color: #7257a5;
  font-size: 32rpx;
}
.metric-note {
  margin-top: 8rpx;
  color: #9aaba5;
  font-size: 20rpx;
}
.card {
  border: 1rpx solid #e0eee9;
  border-radius: 26rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(28, 102, 82, 0.06);
}
.card-heading,
.record-head,
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}
.card-title,
.section-title {
  color: #244c41;
  font-size: 29rpx;
  font-weight: 730;
}
.card-subtitle,
.section-subtitle {
  margin-top: 7rpx;
  color: #91a39d;
  font-size: 21rpx;
}
.region-count,
.level-badge {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #e9f8f2;
  color: #0c8067;
  font-size: 20rpx;
  font-weight: 700;
  white-space: nowrap;
}
.item-label {
  color: #91a39d;
  font-size: 20rpx;
}
.section-header {
  align-items: center;
  margin: 30rpx 2rpx 16rpx;
}
.region-count {
  background: #f4f8f6;
  color: #71847e;
  font-weight: 600;
}
.tab-row {
  display: flex;
  gap: 12rpx;
  padding: 8rpx;
  border-radius: 24rpx;
  background: #eaf5f1;
}
.tab-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  flex: 1;
  min-height: 82rpx;
  border-radius: 18rpx;
  color: #6e8980;
  font-size: 23rpx;
  font-weight: 650;
}
.tab-item.active {
  background: #fff;
  color: #0c8067;
  box-shadow: 0 6rpx 16rpx rgba(28, 102, 82, 0.08);
}
.tab-icon {
  width: 42rpx;
  height: 42rpx;
  border-radius: 13rpx;
  background: #d9f2e9;
  color: #0b8066;
  font-size: 20rpx;
}
.filter-card {
  margin-top: 16rpx;
  padding: 18rpx 20rpx;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.search-label {
  margin-bottom: 10rpx;
}
.keyword-input {
  flex: 1;
  min-width: 0;
  height: 76rpx;
  padding: 0 20rpx;
  box-sizing: border-box;
  border: 2rpx solid #dcece6;
  border-radius: 16rpx;
  background: #f8fcfa;
  color: #294b41;
  font-size: 24rpx;
}
.search-button {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 116rpx;
  height: 76rpx;
  border-radius: 16rpx;
  background: #0e8067;
  color: #fff;
  font-size: 23rpx;
  font-weight: 700;
}
.picker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72rpx;
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #edf4f1;
}
.filter-caption {
  color: #849991;
  font-size: 21rpx;
}
.filter-picker {
  min-width: 220rpx;
}
.picker-value {
  color: #285347;
  font-size: 23rpx;
  text-align: right;
}
.picker-value text {
  margin-left: 8rpx;
  color: #0e8067;
}
.list-loading,
.list-empty {
  padding: 74rpx 24rpx;
  color: #94a49f;
  font-size: 23rpx;
  text-align: center;
}
.record-list {
  margin-top: 16rpx;
}
.record-card {
  margin-bottom: 16rpx;
  padding: 24rpx;
}
.identity-wrap,
.referral-person {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 14rpx;
}
.record-icon {
  background: #e8f7f1;
  color: #0d8067;
}
.level-icon {
  background: #fff1d5;
  color: #b97a17;
}
.identity-copy {
  min-width: 0;
}
.record-name {
  overflow: hidden;
  color: #294b41;
  font-size: 26rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.record-meta,
.person-caption {
  margin-top: 6rpx;
  overflow: hidden;
  color: #91a19c;
  font-size: 20rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.level-badge {
  background: #f8f2e6;
  color: #aa711c;
}
.order-icon {
  background: #e4f2ff;
  color: #317ea8;
}
.order-paid {
  background: #e7f8ef;
  color: #0c8067;
}
.order-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx 20rpx;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #eef4f1;
}
.order-summary > view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  min-width: 0;
}
.detail-value.emphasis {
  color: #0c8067;
  font-size: 24rpx;
  font-weight: 730;
}
.record-code-row,
.record-footer {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
  color: #789088;
  font-size: 20rpx;
}
.record-code-row {
  padding: 13rpx 16rpx;
  border-radius: 14rpx;
  background: #f6fbf8;
}
.account-grid,
.record-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx 20rpx;
  margin-top: 20rpx;
}
.account-item {
  min-width: 0;
}
.item-value {
  margin-top: 6rpx;
  overflow: hidden;
  color: #425f56;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-value.emphasis {
  color: #0c8067;
  font-size: 25rpx;
  font-weight: 730;
}
.record-footer {
  padding-top: 16rpx;
  border-top: 1rpx solid #eef4f1;
}
.order-settlement-error {
  margin-top: 14rpx;
  padding: 12rpx 14rpx;
  border-radius: 12rpx;
  color: #a45d43;
  background: #fff3ed;
  font-size: 21rpx;
  line-height: 1.45;
}
.referral-route {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.referrer-icon {
  background: #e8f1ff;
  color: #4d78bc;
}
.referred-icon {
  background: #fff1d8;
  color: #b8791d;
}
.referred-person {
  justify-content: flex-end;
  text-align: right;
}
.route-arrow {
  flex: 0 0 auto;
  color: #9ab4aa;
  font-size: 32rpx;
}
.record-detail-grid {
  padding-top: 18rpx;
  border-top: 1rpx solid #eef4f1;
}
.record-detail-grid > view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  min-width: 0;
}
.detail-value {
  overflow: hidden;
  color: #405f55;
  font-size: 21rpx;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ledger-amount {
  flex: 0 0 auto;
  color: #0c8067;
  font-size: 25rpx;
  font-weight: 760;
}
.ledger-amount.debit {
  color: #bc655a;
}
.ledger-detail {
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #eef4f1;
}
.ledger-description {
  color: #526e64;
  font-size: 22rpx;
  line-height: 1.5;
}
.ledger-tags {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 12rpx;
}
.ledger-tag {
  padding: 6rpx 12rpx;
  border-radius: 10rpx;
  background: #f0f8f5;
  color: #5f8175;
  font-size: 19rpx;
}
.ledger-time {
  color: #96a7a1;
  font-size: 20rpx;
}
@media screen and (max-width: 360px) {
  .hero-copy,
  .hero-note {
    font-size: 20rpx;
  }
  .refresh-button {
    flex-basis: 110rpx;
  }
  .metric-value {
    font-size: 34rpx;
  }
  .tab-item {
    gap: 4rpx;
    font-size: 21rpx;
  }
  .tab-icon {
    display: none;
  }
}
</style>

<template>
  <view class="page support-center">
    <view class="support-hero">
      <view class="hero-glow hero-glow-top" />
      <view class="hero-glow hero-glow-bottom" />
      <view class="hero-top">
        <view>
          <view class="hero-eyebrow">PLATFORM SERVICE DESK</view>
          <view class="hero-title">反馈服务台</view>
          <view class="hero-copy">统一受理各角色的问题与建议</view>
        </view>
        <view class="refresh-button" @click="load">刷新</view>
      </view>
      <view class="hero-summary">
        <view class="summary-item">
          <text class="summary-value">{{ tickets.length }}</text>
          <text class="summary-label">全部工单</text>
        </view>
        <view class="summary-divider" />
        <view class="summary-item">
          <text class="summary-value">{{ pendingCount }}</text>
          <text class="summary-label">待优先处理</text>
        </view>
        <view class="summary-note">回复后，提交人可在“我的反馈”查看处理结果</view>
      </view>
    </view>

    <view class="filter-row">
      <view
        v-for="item in filters"
        :key="item.value"
        class="filter-chip"
        :class="{ active: activeFilter === item.value }"
        @click="activeFilter = item.value"
      >
        {{ item.label }}
      </view>
    </view>

    <PageState :loading="loading" :error="error" :empty="visibleTickets.length === 0">
      <view v-for="ticket in visibleTickets" :key="ticket.id" class="card ticket-card">
        <view class="ticket-head">
          <view class="ticket-category">
            <view class="category-icon" :class="`category-${ticket.category.toLowerCase()}`">
              {{ categoryIcons[ticket.category] }}
            </view>
            <view>
              <view class="ticket-title">{{ categoryLabels[ticket.category] }}</view>
              <view class="ticket-meta"
                >机构 {{ ticket.tenantId }} · 用户 {{ ticket.submitterUserId }}</view
              >
            </view>
          </view>
          <view :class="`status ${ticket.status.toLowerCase()}`">{{
            statusLabels[ticket.status]
          }}</view>
        </view>

        <view class="ticket-content">{{ ticket.content }}</view>
        <view class="ticket-detail-row">
          <text>{{ formatTime(ticket.createdAt) }}</text>
          <text v-if="ticket.contact">联系：{{ ticket.contact }}</text>
        </view>

        <view v-if="ticket.reply && activeReplyId !== ticket.id" class="reply-preview">
          <view class="reply-label">{{
            ticket.status === 'PROCESSING' ? '平台处理中' : '平台已回复'
          }}</view>
          <view class="reply-text">{{ ticket.reply }}</view>
        </view>

        <view v-if="activeReplyId === ticket.id" class="reply-editor">
          <view class="editor-label">处理回复</view>
          <textarea
            v-model="drafts[ticket.id]"
            maxlength="1000"
            auto-height
            placeholder="请说明处理结果、下一步或需要补充的信息"
          />
          <view class="editor-footer">
            <text>{{ (drafts[ticket.id] || '').length }}/1000</text>
            <view class="editor-actions">
              <view class="text-action" @click="closeEditor">取消</view>
              <view class="processing-action" @click="reply(ticket.id, 'PROCESSING')"
                >保存处理中</view
              >
              <view class="resolve-action" @click="reply(ticket.id, 'RESOLVED')">回复并解决</view>
            </view>
          </view>
        </view>

        <view v-else class="ticket-footer">
          <view class="ticket-time">
            工单
            {{
              ticket.status === 'PROCESSING'
                ? '平台处理中，可继续跟进'
                : ticket.reply
                  ? '已处理，可继续跟进'
                  : '等待平台受理'
            }}
          </view>
          <view class="reply-trigger" @click="openEditor(ticket.id)">
            {{ ticket.reply ? '更新回复' : '开始处理' }} <text>›</text>
          </view>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPlatformSupportTickets, replyPlatformSupportTicket } from '@/api/support'
import PageState from '@/components/PageState.vue'
import type { PlatformSupportTicket } from '@/types/api'

type TicketStatus = PlatformSupportTicket['status']
type FilterStatus = TicketStatus | 'ALL'

const tickets = ref<PlatformSupportTicket[]>([])
const drafts = ref<Record<string, string>>({})
const activeReplyId = ref('')
const activeFilter = ref<FilterStatus>('ALL')
const loading = ref(true)
const error = ref('')
const filters: Array<{ label: string; value: FilterStatus }> = [
  { label: '全部', value: 'ALL' },
  { label: '待处理', value: 'OPEN' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已回复', value: 'RESOLVED' },
]
const categoryLabels: Record<PlatformSupportTicket['category'], string> = {
  USAGE: '使用咨询',
  BUG: '功能异常',
  SUGGESTION: '产品建议',
  OTHER: '其他问题',
}
const categoryIcons: Record<PlatformSupportTicket['category'], string> = {
  USAGE: '问',
  BUG: '修',
  SUGGESTION: '荐',
  OTHER: '他',
}
const statusLabels: Record<TicketStatus, string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已回复',
  CLOSED: '已关闭',
}
const pendingCount = computed(
  () => tickets.value.filter((ticket) => ticket.status === 'OPEN').length,
)
const visibleTickets = computed(() =>
  activeFilter.value === 'ALL'
    ? tickets.value
    : tickets.value.filter((ticket) => ticket.status === activeFilter.value),
)
const formatTime = (value: string) => value.replace('T', ' ').slice(0, 16)
const openEditor = (id: string) => {
  activeReplyId.value = id
}
const closeEditor = () => {
  activeReplyId.value = ''
}
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    tickets.value = await getPlatformSupportTickets()
    drafts.value = Object.fromEntries(tickets.value.map((item) => [item.id, item.reply || '']))
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '反馈列表加载失败'
  } finally {
    loading.value = false
  }
}
const reply = async (id: string, status: 'PROCESSING' | 'RESOLVED') => {
  const replyText = drafts.value[id]?.trim()
  if (!replyText) {
    uni.showToast({ title: '请先填写回复内容', icon: 'none' })
    return
  }
  try {
    await replyPlatformSupportTicket(id, { reply: replyText, status })
    uni.showToast({
      title: status === 'RESOLVED' ? '已回复提交人' : '已保存处理中',
      icon: 'success',
    })
    closeEditor()
    await load()
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '回复失败', icon: 'none' })
  }
}
onShow(load)
</script>

<style scoped>
.support-center {
  padding-top: 88rpx;
}
.support-hero {
  position: relative;
  overflow: hidden;
  padding: 34rpx 30rpx 26rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #0b6956, #118870);
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(12, 102, 82, 0.22);
}
.hero-glow {
  position: absolute;
  border: 34rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}
.hero-glow-top {
  top: -130rpx;
  right: -72rpx;
  width: 260rpx;
  height: 260rpx;
}
.hero-glow-bottom {
  right: 190rpx;
  bottom: -205rpx;
  width: 300rpx;
  height: 300rpx;
}
.hero-top,
.hero-summary,
.ticket-head,
.ticket-footer,
.editor-footer,
.editor-actions {
  display: flex;
  align-items: center;
}
.hero-top,
.ticket-head,
.ticket-footer,
.editor-footer {
  justify-content: space-between;
}
.hero-eyebrow {
  position: relative;
  color: rgba(225, 255, 246, 0.72);
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}
.hero-title {
  position: relative;
  margin-top: 9rpx;
  font-size: 40rpx;
  font-weight: 760;
  letter-spacing: -1rpx;
}
.hero-copy {
  position: relative;
  margin-top: 7rpx;
  color: rgba(238, 255, 249, 0.8);
  font-size: 23rpx;
}
.refresh-button {
  position: relative;
  padding: 13rpx 21rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.25);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.13);
  color: #fff;
  font-size: 22rpx;
}
.hero-summary {
  position: relative;
  gap: 25rpx;
  margin-top: 28rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.16);
}
.summary-item {
  display: flex;
  flex-direction: column;
}
.summary-value {
  font-size: 32rpx;
  font-weight: 760;
}
.summary-label,
.summary-note {
  margin-top: 4rpx;
  color: rgba(235, 255, 248, 0.67);
  font-size: 19rpx;
}
.summary-divider {
  width: 1rpx;
  height: 44rpx;
  background: rgba(255, 255, 255, 0.2);
}
.summary-note {
  flex: 1;
  margin: 0 0 0 auto;
  text-align: right;
  line-height: 1.45;
}
.filter-row {
  display: flex;
  gap: 13rpx;
  overflow-x: auto;
  margin: 26rpx 2rpx 20rpx;
  white-space: nowrap;
}
.filter-chip {
  flex: 0 0 auto;
  padding: 13rpx 22rpx;
  border: 1rpx solid #deebe6;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.82);
  color: #6e817a;
  font-size: 22rpx;
}
.filter-chip.active {
  border-color: #0f7a62;
  background: #0f7a62;
  color: #fff;
  box-shadow: 0 8rpx 18rpx rgba(15, 122, 98, 0.16);
}
.ticket-card {
  padding: 28rpx;
  margin-bottom: 18rpx;
}
.ticket-category {
  display: flex;
  align-items: center;
  min-width: 0;
}
.category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 62rpx;
  height: 62rpx;
  margin-right: 16rpx;
  border-radius: 20rpx;
  background: #e7f5f0;
  color: #08745c;
  font-size: 23rpx;
  font-weight: 750;
}
.category-bug {
  background: #fff1df;
  color: #ab6714;
}
.category-suggestion {
  background: #eeeaff;
  color: #7050ad;
}
.category-other {
  background: #edf1f0;
  color: #62736c;
}
.ticket-title {
  font-size: 28rpx;
  font-weight: 720;
}
.ticket-meta,
.ticket-detail-row,
.ticket-time {
  color: #84948e;
  font-size: 20rpx;
}
.ticket-meta {
  margin-top: 6rpx;
}
.status {
  flex: 0 0 auto;
  margin-left: 16rpx;
  padding: 9rpx 15rpx;
  border-radius: 999rpx;
  background: #fff4de;
  color: #a66a10;
  font-size: 20rpx;
  font-weight: 650;
}
.status.processing {
  background: #eaf1ff;
  color: #426fb2;
}
.status.resolved,
.status.closed {
  background: #e4f6f0;
  color: #0f7a62;
}
.ticket-content {
  margin-top: 22rpx;
  color: #30483f;
  font-size: 25rpx;
  line-height: 1.65;
}
.ticket-detail-row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 16rpx;
  padding-top: 15rpx;
  border-top: 1rpx solid #edf2f0;
}
.ticket-detail-row text:last-child {
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reply-preview {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #edf8f4;
}
.reply-label {
  color: #16806a;
  font-size: 20rpx;
  font-weight: 700;
}
.reply-text {
  margin-top: 8rpx;
  color: #286756;
  font-size: 23rpx;
  line-height: 1.55;
}
.ticket-footer {
  margin-top: 21rpx;
}
.reply-trigger {
  color: #0d765e;
  font-size: 23rpx;
  font-weight: 700;
}
.reply-trigger text {
  margin-left: 4rpx;
  font-size: 32rpx;
  vertical-align: -2rpx;
}
.reply-editor {
  margin-top: 20rpx;
  padding: 20rpx;
  border: 1rpx solid #cfe6dd;
  border-radius: 20rpx;
  background: #f6fbf9;
}
.editor-label {
  color: #2f6657;
  font-size: 22rpx;
  font-weight: 700;
}
.reply-editor textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 130rpx;
  margin-top: 12rpx;
  padding: 15rpx 0;
  color: #315249;
  font-size: 24rpx;
  line-height: 1.55;
}
.editor-footer {
  margin-top: 8rpx;
  color: #92a29c;
  font-size: 19rpx;
}
.editor-actions {
  gap: 12rpx;
}
.text-action,
.processing-action,
.resolve-action {
  padding: 12rpx 16rpx;
  border-radius: 14rpx;
  font-size: 20rpx;
  font-weight: 650;
}
.text-action {
  color: #71817b;
}
.processing-action {
  background: #e3f3ee;
  color: #0e765e;
}
.resolve-action {
  background: #0f7a62;
  color: #fff;
}
</style>

<template>
  <view class="page support-center">
    <view class="page-heading">
      <view>
        <view class="eyebrow">PLATFORM SUPPORT</view>
        <view class="title">反馈中心</view>
        <view class="subtitle">统一受理并回复来自各角色的问题反馈</view>
      </view>
      <view class="refresh" @click="load">刷新</view>
    </view>

    <PageState :loading="loading" :error="error" :empty="tickets.length === 0">
      <view v-for="ticket in tickets" :key="ticket.id" class="card ticket-card">
        <view class="ticket-head">
          <view>
            <view class="ticket-title">{{ categoryLabels[ticket.category] }}</view>
            <view class="meta">机构 {{ ticket.tenantId }} · 提交用户 {{ ticket.submitterUserId }}</view>
          </view>
          <view :class="`status ${ticket.status.toLowerCase()}`">{{ statusLabels[ticket.status] }}</view>
        </view>
        <view class="content">{{ ticket.content }}</view>
        <view v-if="ticket.contact" class="contact">联系方式：{{ ticket.contact }}</view>
        <view v-if="ticket.reply" class="existing-reply">已回复：{{ ticket.reply }}</view>
        <textarea v-model="drafts[ticket.id]" maxlength="1000" placeholder="输入处理回复" />
        <view class="actions">
          <view class="processing" @click="reply(ticket.id, 'PROCESSING')">处理中</view>
          <view class="resolve" @click="reply(ticket.id, 'RESOLVED')">回复并解决</view>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPlatformSupportTickets, replyPlatformSupportTicket } from '@/api/support'
import PageState from '@/components/PageState.vue'
import type { PlatformSupportTicket } from '@/types/api'

const tickets = ref<PlatformSupportTicket[]>([])
const drafts = ref<Record<string, string>>({})
const loading = ref(true)
const error = ref('')
const categoryLabels: Record<PlatformSupportTicket['category'], string> = {
  USAGE: '使用咨询',
  BUG: '功能异常',
  SUGGESTION: '产品建议',
  OTHER: '其他问题',
}
const statusLabels: Record<PlatformSupportTicket['status'], string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已回复',
  CLOSED: '已关闭',
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
    uni.showToast({ title: status === 'RESOLVED' ? '已回复' : '已更新状态', icon: 'success' })
    await load()
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '回复失败', icon: 'none' })
  }
}
onShow(load)
</script>

<style scoped>
.support-center { padding-top: 34rpx; }
.page-heading, .ticket-head, .actions { display: flex; justify-content: space-between; }
.page-heading { align-items: flex-end; margin: 0 6rpx 24rpx; }
.refresh, .processing, .resolve { padding: 13rpx 20rpx; border-radius: 16rpx; font-size: 22rpx; }
.refresh, .processing { background: #e4f6f0; color: #0d765e; }
.ticket-card { margin-bottom: 18rpx; }
.ticket-title { font-size: 28rpx; font-weight: 700; }
.meta, .contact { margin-top: 8rpx; color: #789087; font-size: 21rpx; }
.content, .existing-reply { margin-top: 18rpx; color: #40584f; line-height: 1.6; font-size: 24rpx; }
.existing-reply { padding: 16rpx; border-radius: 14rpx; background: #edf7f3; color: #176b57; }
textarea { width: 100%; min-height: 150rpx; margin-top: 20rpx; font-size: 24rpx; }
.actions { gap: 16rpx; margin-top: 16rpx; }
.resolve { background: #0f7a62; color: #fff; }
.status { padding: 6rpx 14rpx; border-radius: 999rpx; color: #a06a0b; background: #fff4de; font-size: 20rpx; }
.status.processing { color: #426fb2; background: #eaf1ff; }
.status.resolved, .status.closed { color: #0f7a62; background: #e4f6f0; }
</style>

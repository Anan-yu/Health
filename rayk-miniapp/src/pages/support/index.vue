<template>
  <view class="page support-page">
    <view class="title">帮助与反馈</view>
    <view class="subtitle page-copy">查看常见操作说明，或把问题直接反馈给平台。</view>

    <view class="section-title">常见问题</view>
    <view class="card faq-card">
      <view
        v-for="item in faqs"
        :key="item.question"
        class="faq-item"
        @click="toggle(item.question)"
      >
        <view class="faq-head"
          ><text>{{ item.question }}</text
          ><text>{{ expanded === item.question ? '−' : '+' }}</text></view
        >
        <view v-if="expanded === item.question" class="faq-answer">{{ item.answer }}</view>
      </view>
    </view>

    <view class="section-title feedback-title">提交反馈</view>
    <view class="card form-card">
      <view class="field">
        <text>问题类型</text>
        <picker :range="categories" range-key="label" @change="selectCategory">
          <view class="picker-value">{{ selectedCategory.label }} ›</view>
        </picker>
      </view>
      <view class="field">
        <text>问题描述</text>
        <textarea
          v-model="content"
          maxlength="1000"
          placeholder="请描述操作步骤、异常表现和期望结果"
        />
        <view class="count">{{ content.length }}/1000</view>
      </view>
      <view class="field last">
        <text>联系方式（选填）</text>
        <input v-model="contact" maxlength="100" placeholder="手机号或微信号" />
      </view>
    </view>
    <button class="primary-button" :loading="submitting" :disabled="submitting" @click="submit">
      提交反馈
    </button>

    <view v-if="tickets.length" class="section-title history-title">我的反馈</view>
    <view v-for="ticket in tickets" :key="ticket.id" class="card ticket-card">
      <view class="ticket-head"
        ><text>{{ categoryLabels[ticket.category] }}</text
        ><text :class="`status ${ticket.status.toLowerCase()}`">{{
          statusLabels[ticket.status]
        }}</text></view
      >
      <view class="ticket-content">{{ ticket.content }}</view>
      <view v-if="ticket.reply" class="reply">平台回复：{{ ticket.reply }}</view>
      <view class="ticket-time">{{ formatTime(ticket.createdAt) }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createSupportTicket, getMySupportTickets } from '@/api/support'
import type { SupportTicket } from '@/types/api'

const faqs = [
  {
    question: '如何切换不同身份？',
    answer: '进入“我的”或“工作台”，点击“切换工作台”，系统会按微信绑定账号自动展示可用身份。',
  },
  {
    question: '上传报告后为什么没有评估？',
    answer: '报告需要先完成 OCR 识别和人工指标确认，之后才会进入 AI 评估与医生审核流程。',
  },
  {
    question: '修改档案后数据何时更新？',
    answer: '保存成功后，档案页和首页会重新读取最新数据；下拉刷新也可立即更新。',
  },
]
const categories = [
  { label: '使用咨询', value: 'USAGE' },
  { label: '功能异常', value: 'BUG' },
  { label: '产品建议', value: 'SUGGESTION' },
  { label: '其他问题', value: 'OTHER' },
] as const
const categoryLabels: Record<SupportTicket['category'], string> = {
  USAGE: '使用咨询',
  BUG: '功能异常',
  SUGGESTION: '产品建议',
  OTHER: '其他问题',
}
const statusLabels: Record<SupportTicket['status'], string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已回复',
  CLOSED: '已关闭',
}
const categoryIndex = ref(0)
const content = ref('')
const contact = ref('')
const submitting = ref(false)
const tickets = ref<SupportTicket[]>([])
const expanded = ref('')
const selectedCategory = computed(() => categories[categoryIndex.value] || categories[0])
const toggle = (question: string) => (expanded.value = expanded.value === question ? '' : question)
const selectCategory = (event: { detail: { value: string | number } }) => {
  categoryIndex.value = Number(event.detail.value) || 0
}
const formatTime = (value: string) => value.replace('T', ' ').slice(0, 16)
const load = async () => {
  try {
    tickets.value = await getMySupportTickets()
  } catch {
    tickets.value = []
  }
}
const submit = async () => {
  const description = content.value.trim()
  if (!description) {
    uni.showToast({ title: '请填写问题描述', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await createSupportTicket({
      category: selectedCategory.value.value,
      content: description,
      contact: contact.value.trim() || undefined,
    })
    content.value = ''
    contact.value = ''
    await load()
    uni.showToast({ title: '反馈已提交', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
onShow(load)
</script>

<style scoped>
.page-copy {
  margin-bottom: 28rpx;
}
.faq-card,
.form-card {
  padding: 0 28rpx;
}
.faq-item,
.field {
  padding: 26rpx 0;
  border-bottom: 1rpx solid #e9f0ed;
}
.faq-item:last-child,
.field.last {
  border-bottom: 0;
}
.faq-head,
.ticket-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 650;
}
.faq-answer {
  margin-top: 16rpx;
  color: #687b74;
  font-size: 23rpx;
  line-height: 1.7;
}
.feedback-title,
.history-title {
  margin-top: 34rpx;
}
.field > text {
  display: block;
  margin-bottom: 14rpx;
  color: #29483e;
  font-size: 24rpx;
}
.field textarea {
  width: 100%;
  min-height: 180rpx;
  font-size: 24rpx;
}
.field input,
.picker-value {
  font-size: 24rpx;
  color: #3d574e;
}
.count {
  color: #9aa8a3;
  text-align: right;
  font-size: 20rpx;
}
.primary-button {
  margin-top: 24rpx;
  border-radius: 18rpx;
  background: #0f7a62;
  color: #fff;
}
.ticket-card {
  margin-bottom: 18rpx;
}
.ticket-content {
  margin-top: 18rpx;
  color: #40584f;
  font-size: 24rpx;
  line-height: 1.65;
}
.reply {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #edf7f3;
  color: #176b57;
  font-size: 22rpx;
}
.ticket-time {
  margin-top: 15rpx;
  color: #9aa8a3;
  font-size: 20rpx;
}
.status {
  color: #a06a0b;
  font-size: 21rpx;
}
.status.resolved,
.status.closed {
  color: #0f7a62;
}
.status.processing {
  color: #426fb2;
}
</style>

<template>
  <view class="page detail-page">
    <PageState :loading="loading" :error="error" :empty="!task">
      <template v-if="task">
        <view class="hero">
          <view class="hero-pattern" />
          <view class="hero-label">HEALTH FOLLOW-UP</view>
          <view class="hero-main">
            <view>
              <view class="patient-name">{{ task.patientName || '未命名客户' }}</view>
              <view class="task-title">{{ task.title }}</view>
            </view>
            <StatusTag :status="statusText(task.status)" />
          </view>
          <view class="hero-date">计划完成日期 {{ task.dueDate }}</view>
        </view>

        <view class="card overview-card">
          <view class="overview-item">
            <text class="overview-label">任务状态</text>
            <text class="overview-value">{{ statusText(task.status) }}</text>
          </view>
          <view class="overview-divider" />
          <view class="overview-item">
            <text class="overview-label">完成时间</text>
            <text class="overview-value">{{
              task.completedAt ? formatDateTime(task.completedAt) : '尚未完成'
            }}</text>
          </view>
        </view>

        <view v-if="task.completionRate !== undefined || task.decisionReason" class="card decision-card">
          <view class="decision-top">
            <view>
              <view class="decision-label">本期执行结果</view>
              <view class="decision-value">{{ task.completionRate ?? 0 }}%</view>
            </view>
            <view class="decision-badge">{{ decisionText(task.decision) }}</view>
          </view>
          <view v-if="task.cycleNo" class="decision-cycle">
            当前第 {{ task.cycleNo }} 期，最多 {{ task.maxCycles || 4 }} 期
          </view>
          <view v-if="task.decisionReason" class="decision-reason">{{ task.decisionReason }}</view>
        </view>

        <view class="section-heading">健康行动计划</view>
        <view class="card plan-card">
          <view
            v-for="section in planSections(task.content)"
            :key="section.title"
            class="plan-section"
          >
            <view class="plan-section-title">
              <text class="section-icon">{{ sectionIcon(section.title) }}</text>
              <text>{{ section.title }}</text>
            </view>
            <view v-for="action in section.actions" :key="action" class="plan-action">
              <text class="bullet">•</text>
              <text>{{ action }}</text>
            </view>
          </view>
        </view>

        <view class="section-heading">客户执行反馈</view>
        <view class="card feedback-card" :class="{ pending: !task.feedback }">
          <view class="feedback-icon">{{ task.feedback ? '✓' : '待' }}</view>
          <view class="feedback-content">
            <view class="feedback-title">{{ task.feedback ? '客户已提交反馈' : '等待客户反馈' }}</view>
            <view class="feedback-copy">{{
              task.feedback || '客户完成健康行动后，反馈内容会展示在这里。'
            }}</view>
          </view>
        </view>
        <view v-if="feedbackItems.length" class="card feedback-list">
          <view
            v-for="(item, index) in feedbackItems"
            :key="`${item.section}-${index}`"
            class="feedback-item"
          >
            <view class="feedback-item-top">
              <view class="feedback-action">{{ item.action }}</view>
              <view class="feedback-status" :class="item.status.toLowerCase()">
                {{ actionStatusText(item.status) }}
              </view>
            </view>
            <view v-if="item.note" class="feedback-note">说明：{{ item.note }}</view>
          </view>
        </view>
      </template>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getFollowup } from '@/api/followup'
import type { Followup } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const task = ref<Followup | null>(null)
const loading = ref(true)
const error = ref('')
type FeedbackItem = {
  section: string
  action: string
  status: 'COMPLETED' | 'PARTIAL' | 'NOT_COMPLETED'
  note?: string
}
const feedbackItems = computed<FeedbackItem[]>(() => {
  if (!task.value?.feedbackDetail) return []
  try {
    const parsed = JSON.parse(task.value.feedbackDetail)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

type PlanSection = { title: string; actions: string[] }
const sectionTitles = new Set(['本周重点', '饮食行动', '运动行动', '作息行动', '监测行动', '健康行动'])
const introToHide = '本计划根据本次健康报告自动生成。'

function planSections(content: string): PlanSection[] {
  const sections: PlanSection[] = []
  let current: PlanSection | undefined
  const startSection = (title: string) => {
    current = { title, actions: [] }
    sections.push(current)
  }
  for (const rawLine of (content || '').split('\n')) {
    const line = rawLine.trim()
    if (!line || line === introToHide) continue
    const title = line.replace(/[：:]$/, '')
    if (sectionTitles.has(title)) {
      startSection(title)
      continue
    }
    const legacySection = line.match(/^(本周重点|建议执行)[：:]\s*(.*)$/)
    if (legacySection) {
      startSection(legacySection[1] === '建议执行' ? '健康行动' : legacySection[1])
      current?.actions.push(
        ...legacySection[2]
          .replace(/[。；]$/, '')
          .split('；')
          .map((value) => value.trim())
          .filter(Boolean),
      )
      continue
    }
    if (!current) startSection('健康行动')
    current?.actions.push(line.replace(/^[-•]\s*/, ''))
  }
  return sections.filter((section) => section.title !== '本周重点' && section.actions.length)
}

function sectionIcon(title: string) {
  const icons: Record<string, string> = {
    饮食行动: '食',
    运动行动: '动',
    作息行动: '眠',
    监测行动: '记',
    健康行动: '行',
  }
  return icons[title] || '行'
}

const statusText = (status: string) => {
  const labels: Record<string, string> = {
    PENDING: '待完成',
    COMPLETED: '已完成',
    PAUSED: '已暂停',
    CANCELLED: '已结束',
  }
  return labels[status] || status
}
const decisionText = (decision?: string) => {
  const labels: Record<string, string> = {
    CONTINUE: '继续随访',
    ADJUST: '调整计划',
    TERMINATE: '本轮结束',
    PAUSE: '暂停随访',
  }
  return decision ? labels[decision] || decision : '等待反馈'
}
const actionStatusText = (status: string) => {
  const labels: Record<string, string> = {
    COMPLETED: '已完成',
    PARTIAL: '部分完成',
    NOT_COMPLETED: '未完成',
  }
  return labels[status] || status
}
const formatDateTime = (value: string) => value.replace('T', ' ').slice(0, 16)

onLoad(async (query) => {
  const id = query?.id
  if (!id) {
    error.value = '缺少随访任务编号，请从随访动态重新进入'
    loading.value = false
    return
  }
  try {
    task.value = await getFollowup(String(id))
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '随访任务详情加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-page {
  padding-top: 26rpx;
}
.hero {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 32rpx;
  background: linear-gradient(145deg, #075744 0%, #0c7960 58%, #22a07e 100%);
  color: #fff;
  box-shadow: 0 22rpx 44rpx rgba(8, 91, 72, 0.2);
}
.hero-pattern {
  position: absolute;
  top: -100rpx;
  right: -60rpx;
  width: 300rpx;
  height: 300rpx;
  border: 42rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 50%;
}
.hero-label {
  position: relative;
  color: rgba(255, 255, 255, 0.7);
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}
.hero-main {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 18rpx;
}
.patient-name {
  font-size: 38rpx;
  font-weight: 760;
}
.task-title {
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.8);
  font-size: 23rpx;
}
.hero-date {
  position: relative;
  margin-top: 28rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.8);
  font-size: 22rpx;
}
.overview-card {
  display: flex;
  align-items: stretch;
  margin-top: 22rpx;
  padding: 28rpx 20rpx;
}
.decision-card {
  margin-top: 22rpx;
  padding: 28rpx;
  background: linear-gradient(145deg, #f2fbf8, #ffffff);
}
.decision-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.decision-label {
  color: #71837d;
  font-size: 22rpx;
}
.decision-value {
  margin-top: 4rpx;
  color: #08765d;
  font-size: 46rpx;
  font-weight: 800;
}
.decision-badge {
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background: #dff5ed;
  color: #08765d;
  font-size: 23rpx;
  font-weight: 700;
}
.decision-cycle {
  margin-top: 18rpx;
  color: #657a73;
  font-size: 23rpx;
}
.decision-reason {
  margin-top: 12rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #edf7f4;
  color: #28564b;
  font-size: 23rpx;
  line-height: 1.6;
}
.overview-item {
  display: flex;
  flex: 1;
  align-items: center;
  flex-direction: column;
  gap: 8rpx;
  text-align: center;
}
.overview-label {
  color: #84938e;
  font-size: 21rpx;
}
.overview-value {
  color: #173f36;
  font-size: 24rpx;
  font-weight: 700;
}
.overview-divider {
  width: 1rpx;
  margin: 0 12rpx;
  background: #e5eeeb;
}
.section-heading {
  margin: 34rpx 8rpx 16rpx;
  color: #153e35;
  font-size: 29rpx;
  font-weight: 730;
}
.plan-card {
  padding: 8rpx 30rpx;
}
.plan-section {
  padding: 25rpx 0;
  border-bottom: 1rpx solid #e8efec;
}
.plan-section:last-child {
  border-bottom: 0;
}
.plan-section-title {
  display: flex;
  align-items: center;
  gap: 13rpx;
  margin-bottom: 15rpx;
  color: #173f36;
  font-size: 27rpx;
  font-weight: 700;
}
.section-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
  border-radius: 15rpx;
  background: #e2f5ee;
  color: #08775d;
  font-size: 21rpx;
}
.plan-action {
  display: flex;
  gap: 12rpx;
  padding: 7rpx 0 7rpx 6rpx;
  color: #506d65;
  font-size: 23rpx;
  line-height: 1.65;
}
.bullet {
  flex: 0 0 auto;
  color: #13a17b;
  font-weight: 800;
}
.feedback-card {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 28rpx;
}
.feedback-card.pending {
  background: #fbfcfc;
}
.feedback-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 54rpx;
  height: 54rpx;
  border-radius: 17rpx;
  background: #ddf5ec;
  color: #0d795f;
  font-size: 22rpx;
  font-weight: 750;
}
.pending .feedback-icon {
  background: #fff1d7;
  color: #a56a00;
}
.feedback-content {
  min-width: 0;
}
.feedback-title {
  color: #173f36;
  font-size: 25rpx;
  font-weight: 700;
}
.feedback-copy {
  margin-top: 10rpx;
  color: #678078;
  font-size: 23rpx;
  line-height: 1.65;
}
.feedback-list {
  margin-top: 16rpx;
  padding: 8rpx 26rpx;
}
.feedback-item {
  padding: 22rpx 0;
  border-bottom: 1rpx solid #e8efed;
}
.feedback-item:last-child {
  border-bottom: 0;
}
.feedback-item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}
.feedback-action {
  flex: 1;
  color: #24483f;
  font-size: 23rpx;
  line-height: 1.6;
}
.feedback-status {
  flex: 0 0 auto;
  padding: 7rpx 13rpx;
  border-radius: 999rpx;
  background: #dff5ed;
  color: #08765d;
  font-size: 20rpx;
}
.feedback-status.partial {
  background: #fff2d7;
  color: #996000;
}
.feedback-status.not_completed {
  background: #ffebe9;
  color: #b33a31;
}
.feedback-note {
  margin-top: 10rpx;
  padding: 14rpx;
  border-radius: 12rpx;
  background: #f5f8f7;
  color: #70817b;
  font-size: 21rpx;
}
</style>

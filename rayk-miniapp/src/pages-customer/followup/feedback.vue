<template>
  <view class="page feedback-page">
    <view class="title">随访反馈</view>
    <PageState :loading="loading" :error="loadError" :empty="!task">
      <template v-if="task">
        <view class="intro-card">
          <view class="intro-title">{{ task.title }}</view>
          <view class="intro-copy">请逐项对照健康行动，选择真实完成情况。</view>
          <view class="cycle-tag">第 {{ task.cycleNo || 1 }} 期</view>
        </view>

        <view v-for="section in sections" :key="section.title" class="section-card">
          <view class="section-title">{{ section.title }}</view>
          <view
            v-for="(item, index) in section.actions"
            :key="`${section.title}-${index}`"
            class="action-item"
          >
            <view class="action-number">{{ index + 1 }}</view>
            <view class="action-content">
              <view class="action-text">{{ item.action }}</view>
              <view class="status-row">
                <view
                  v-for="option in statusOptions"
                  :key="option.value"
                  class="status-option"
                  :class="{ active: item.status === option.value, [option.value.toLowerCase()]: true }"
                  @click="item.status = option.value"
                >
                  {{ option.label }}
                </view>
              </view>
              <textarea
                v-if="item.status && item.status !== 'COMPLETED'"
                v-model="item.note"
                class="note-input"
                :maxlength="300"
                :placeholder="
                  item.status === 'PARTIAL'
                    ? '可说明已完成部分或遇到的困难（选填）'
                    : '可说明未完成原因（选填）'
                "
              />
            </view>
          </view>
        </view>

        <view class="card summary-card">
          <view class="summary-title">补充说明（选填）</view>
          <textarea
            v-model="feedback"
            class="summary-input"
            :maxlength="1000"
            placeholder="可补充本期身体感受、睡眠、饮食或其他变化"
          />
          <view class="count">{{ feedback.length }}/1000</view>
          <view v-if="submitError" class="error">{{ submitError }}</view>
          <button class="primary submit-button" :loading="submitting" :disabled="submitting" @click="submit">
            提交本期反馈
          </button>
        </view>
      </template>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getFollowup, sendFeedback } from '@/api/followup'
import type { FollowupActionFeedback } from '@/api/followup'
import type { Followup } from '@/types/api'
import PageState from '@/components/PageState.vue'

type EditableAction = {
  section: string
  action: string
  status: FollowupActionFeedback['status'] | ''
  note?: string
}
type EditableSection = { title: string; actions: EditableAction[] }

const task = ref<Followup | null>(null)
const sections = ref<EditableSection[]>([])
const feedback = ref('')
const loading = ref(true)
const submitting = ref(false)
const loadError = ref('')
const submitError = ref('')

const statusOptions: Array<{ value: FollowupActionFeedback['status']; label: string }> = [
  { value: 'COMPLETED', label: '已完成' },
  { value: 'PARTIAL', label: '部分完成' },
  { value: 'NOT_COMPLETED', label: '未完成' },
]

const sectionTitles = new Set([
  '饮食行动',
  '运动行动',
  '作息行动',
  '监测行动',
  '健康行动',
])

function parsePlan(content: string): EditableSection[] {
  const result: EditableSection[] = []
  let current: EditableSection | undefined
  const start = (title: string) => {
    current = { title, actions: [] }
    result.push(current)
  }
  for (const raw of (content || '').split('\n')) {
    const line = raw.trim()
    if (!line || line === '本周重点' || line.startsWith('本计划根据')) continue
    const title = line.replace(/[：:]$/, '')
    if (sectionTitles.has(title)) {
      start(title)
      continue
    }
    if (!current) start('健康行动')
    current?.actions.push({
      section: current.title,
      action: line.replace(/^[-•·]\s*/, ''),
      status: '',
      note: '',
    })
  }
  return result.filter((section) => section.actions.length)
}

const allActions = computed(() => sections.value.flatMap((section) => section.actions))

onLoad(async (query) => {
  const id = String(query?.id || '')
  if (!id) {
    loadError.value = '缺少随访任务编号，请从随访列表重新进入。'
    loading.value = false
    return
  }
  try {
    task.value = await getFollowup(id)
    sections.value = parsePlan(task.value.content)
    if (!sections.value.length) {
      loadError.value = '该随访任务暂无可反馈的健康行动。'
    }
  } catch (cause) {
    loadError.value = cause instanceof Error ? cause.message : '随访任务加载失败'
  } finally {
    loading.value = false
  }
})

async function submit() {
  submitError.value = ''
  if (!task.value) return
  if (allActions.value.some((item) => !item.status)) {
    submitError.value = '请为每一项健康行动选择完成情况。'
    return
  }
  submitting.value = true
  try {
    await sendFeedback(task.value.id, {
      feedback: feedback.value.trim() || undefined,
      actions: allActions.value.map((item) => ({
        section: item.section,
        action: item.action,
        status: item.status as FollowupActionFeedback['status'],
        note: item.note?.trim() || undefined,
      })),
    })
    uni.showToast({ title: '反馈已提交', icon: 'success' })
    globalThis.setTimeout(
      () => uni.redirectTo({ url: '/pages-customer/followup/index' }),
      600,
    )
  } catch (cause) {
    submitError.value = cause instanceof Error ? cause.message : '反馈提交失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.feedback-page {
  padding-bottom: 44rpx;
}
.intro-card {
  position: relative;
  margin-bottom: 22rpx;
  padding: 30rpx;
  border-radius: 28rpx;
  background: linear-gradient(145deg, #075c49, #139071);
  color: #fff;
  box-shadow: 0 18rpx 40rpx rgba(5, 98, 76, 0.18);
}
.intro-title {
  padding-right: 100rpx;
  font-size: 32rpx;
  font-weight: 800;
}
.intro-copy {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 24rpx;
}
.cycle-tag {
  position: absolute;
  top: 28rpx;
  right: 26rpx;
  padding: 8rpx 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.3);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.12);
  font-size: 21rpx;
}
.section-card {
  margin-bottom: 22rpx;
  padding: 28rpx;
  border: 1rpx solid #dce9e5;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 12rpx 30rpx rgba(15, 70, 57, 0.07);
}
.section-title {
  margin-bottom: 8rpx;
  color: #0b6f58;
  font-size: 29rpx;
  font-weight: 800;
}
.action-item {
  display: flex;
  gap: 18rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #edf2f0;
}
.action-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}
.action-number {
  display: flex;
  flex: 0 0 42rpx;
  align-items: center;
  justify-content: center;
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  background: #e2f5ef;
  color: #08765d;
  font-size: 22rpx;
  font-weight: 800;
}
.action-content {
  flex: 1;
  min-width: 0;
}
.action-text {
  color: #173f36;
  font-size: 25rpx;
  line-height: 1.65;
}
.status-row {
  display: flex;
  gap: 12rpx;
  margin-top: 18rpx;
}
.status-option {
  flex: 1;
  padding: 14rpx 6rpx;
  border: 1rpx solid #d9e5e1;
  border-radius: 16rpx;
  background: #f7faf9;
  color: #6f817b;
  text-align: center;
  font-size: 22rpx;
}
.status-option.active {
  border-color: #149170;
  background: #e4f7f0;
  color: #08765d;
  font-weight: 700;
}
.status-option.active.partial {
  border-color: #e2aa42;
  background: #fff4dc;
  color: #9b6400;
}
.status-option.active.not_completed {
  border-color: #e28a82;
  background: #fff0ee;
  color: #b33a31;
}
.note-input {
  box-sizing: border-box;
  width: 100%;
  min-height: 108rpx;
  margin-top: 16rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #f4f8f6;
  color: #23473f;
  font-size: 23rpx;
}
.summary-card {
  padding: 28rpx;
}
.summary-title {
  margin-bottom: 16rpx;
  font-size: 27rpx;
  font-weight: 800;
}
.summary-input {
  box-sizing: border-box;
  width: 100%;
  min-height: 190rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #f4f8f6;
  font-size: 24rpx;
}
.count {
  margin-top: 8rpx;
  color: #8a9893;
  text-align: right;
  font-size: 20rpx;
}
.error {
  margin: 12rpx 0;
  color: #b42318;
  font-size: 23rpx;
}
.submit-button {
  width: 100%;
  min-height: 92rpx;
  margin-top: 18rpx;
  font-size: 28rpx;
  font-weight: 800;
}
</style>

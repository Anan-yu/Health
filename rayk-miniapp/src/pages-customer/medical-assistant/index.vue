<template>
  <view class="assistant-page elder-page">
    <view class="assistant-topbar">
      <view class="topbar-back" @tap="goBack">‹</view>
      <view class="topbar-title">健康助手</view>
      <view class="topbar-spacer" aria-hidden="true" />
    </view>

    <scroll-view
      class="conversation-scroll"
      scroll-y
      :scroll-into-view="scrollIntoView"
      scroll-with-animation
    >
      <view class="assistant-hero">
        <view class="hero-orbit orbit-one" />
        <view class="hero-orbit orbit-two" />
        <image class="hero-mark" :src="assistantAvatar" mode="aspectFill" aria-hidden="true" />
        <view class="hero-eyebrow">健康管理陪伴</view>
        <view class="hero-title">把健康问题说清楚，<br />一起找到下一步</view>
        <view class="hero-copy">
          结合健康档案、健康报告和健康拍，帮你理清重点，找到下一步。
        </view>
        <view class="context-row">
          <view class="context-chip"><text class="chip-dot mint" />健康档案</view>
          <view class="context-chip"><text class="chip-dot blue" />健康报告</view>
          <view class="context-chip"><text class="chip-dot violet" />健康拍</view>
        </view>
      </view>

      <view v-if="loading" class="loading-card">
        <view class="loading-dot" />正在准备你的健康资料…
      </view>

      <view v-else-if="error" class="error-card">
        <view class="error-title">暂时无法打开助手</view>
        <view class="error-copy">{{ error }}</view>
        <button class="retry-button" @tap="loadConversation">重新打开</button>
      </view>

      <view v-else class="chat-area">
        <view class="conversation-tools">
          <view class="conversation-tools-copy">
            <view class="conversation-tools-title">对话记录</view>
            <view class="conversation-tools-hint">切换会话，不会带入其他对话内容</view>
          </view>
          <button class="history-entry" aria-label="查看对话记录" @tap="toggleHistory">
            <text>查看记录</text>
            <text class="history-entry-arrow">›</text>
          </button>
        </view>
        <view class="date-note">本次对话仅用于本人健康管理</view>

        <view v-if="messages.length === 0" class="welcome-card">
          <view class="welcome-title">从一个问题开始</view>
          <view class="welcome-copy">你可以问我报告里的指标、近期需要优先关注什么，或如何准备下一次就医沟通。</view>
          <view class="suggestion-list">
            <view
              v-for="suggestion in suggestions"
              :key="suggestion"
              class="suggestion-item"
              @tap="sendSuggestion(suggestion)"
            >
              <text>{{ suggestion }}</text><text class="suggestion-arrow">›</text>
            </view>
          </view>
        </view>

        <view v-for="message in messages" :id="`message-${message.id}`" :key="message.id" class="message-row" :class="message.role === 'USER' ? 'is-user' : 'is-assistant'">
          <image v-if="message.role === 'ASSISTANT'" class="assistant-avatar" :src="assistantAvatar" mode="aspectFill" aria-hidden="true" />
          <view class="message-column">
            <view class="message-bubble" :class="{ 'urgent-bubble': message.emergency }">
              <view v-if="sending && !message.content" class="typing-bubble inline-typing">
                <view /><view /><view />
              </view>
              <view v-else class="message-text">
                <text
                  v-for="(part, partIndex) in messageParts(message.content)"
                  :key="`${message.id}-part-${partIndex}`"
                  :class="{ 'message-bold': part.bold }"
                >{{ part.text }}</text>
              </view>
            </view>
            <view v-if="message.emergency" class="urgent-card">
              <view class="urgent-title">需要立即处理</view>
              <view class="urgent-copy">{{ message.recommendedAction }}</view>
            </view>
            <view v-if="message.role === 'ASSISTANT' && message.followupQuestions.length" class="followup-list">
              <view v-for="question in message.followupQuestions" :key="question" class="followup-item">
                <text>{{ question }}</text>
              </view>
            </view>
          </view>
        </view>

        <view id="conversation-bottom" class="conversation-bottom" />
      </view>
    </scroll-view>

    <view v-if="historyOpen" class="history-overlay" @tap="closeHistory">
      <view class="history-drawer" @tap.stop>
        <view class="history-drawer-head">
          <view>
            <view class="history-drawer-title">对话记录</view>
            <view class="history-drawer-subtitle">每个对话独立保存上下文</view>
          </view>
          <view class="history-close" aria-label="关闭对话记录" @tap="closeHistory">×</view>
        </view>

        <button class="new-conversation-button" :disabled="historyLoading || sending" @tap="createNewConversation">
          <text class="new-conversation-plus">＋</text>
          <text>新对话</text>
        </button>

        <scroll-view class="history-list" scroll-y>
          <view v-if="historyLoading" class="history-empty">正在加载对话记录…</view>
          <view v-else-if="conversations.length === 0" class="history-empty">还没有历史对话</view>
          <view v-else class="history-items">
            <view
              v-for="item in conversations"
              :key="item.id"
              class="history-item"
              :class="{ active: item.id === conversationId }"
              @tap="selectConversation(item.id)"
            >
              <view class="history-item-main">
                <view class="history-item-title">{{ displayConversationTitle(item) }}</view>
                <view class="history-item-time">{{ formatConversationTime(item.updatedAt) }}</view>
              </view>
              <view class="history-item-actions">
                <view v-if="item.id === conversationId" class="history-active-mark">当前</view>
                <view class="history-delete" aria-label="删除对话" @tap.stop="deleteConversation(item)">删除</view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="composer-shell">
      <view class="composer-row">
        <textarea
          v-model="draft"
          class="composer-input"
          :maxlength="4000"
          :disabled="loading || sending || !conversationId"
          auto-height
          cursor-spacing="16"
          :show-confirm-bar="false"
          confirm-type="send"
          @confirm="sendMessage"
        />
        <button class="send-button" :class="{ ready: canSend }" :disabled="!canSend" @tap="sendMessage">
          <text v-if="sending" class="send-loading">···</text>
          <template v-else>
            <text class="send-label">发送</text>
            <text class="send-glyph">↑</text>
          </template>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ApiError } from '@/utils/request'
import {
  createMedicalAssistantConversation,
  deleteMedicalAssistantConversation,
  getMedicalAssistantConversation,
  listMedicalAssistantConversations,
  normalizeMedicalAssistantText,
  streamMedicalAssistantMessage,
  type MedicalAssistantConversation,
  type MedicalAssistantMessage,
} from '@/api/medical-assistant'

const conversation = ref<MedicalAssistantConversation>()
const conversations = ref<MedicalAssistantConversation[]>([])
const draft = ref('')
const loading = ref(true)
const historyLoading = ref(false)
const historyOpen = ref(false)
const switchingConversation = ref(false)
const sending = ref(false)
const error = ref('')
const scrollIntoView = ref('conversation-bottom')
const streamAbort = ref<(() => void)>()
const MEDICAL_ASSISTANT_QUOTA_EXHAUSTED = 60401

const suggestions = [
  '帮我解释最近一次健康报告',
  '我现在最需要先关注哪些指标？',
  '结合我的档案，下一步该准备什么？',
]

const assistantAvatar = '/pages-customer/static/assistant/sheep-avatar.jpg'

type MessageTextPart = { text: string; bold: boolean }

const conversationId = computed(() => conversation.value?.id || '')
const messages = computed<MedicalAssistantMessage[]>(() => conversation.value?.messages || [])
const canSend = computed(() => Boolean(conversationId.value && draft.value.trim() && !sending.value && !switchingConversation.value))

onShow(() => {
  uni.setNavigationBarTitle({ title: '健康助手' })
  if (!conversation.value) void loadConversation()
})

async function loadConversation() {
  loading.value = true
  error.value = ''
  try {
    const conversationList = await listMedicalAssistantConversations()
    conversations.value = conversationList
    const first = conversationList[0]
    if (first) {
      conversation.value = await getMedicalAssistantConversation(first.id)
    } else {
      const created = await createMedicalAssistantConversation()
      conversation.value = created
      conversations.value = [created]
    }
    await scrollToBottom()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '健康助手加载失败'
  } finally {
    loading.value = false
  }
}

async function sendMessage(contentOverride?: string) {
  const isDirectSuggestion = typeof contentOverride === 'string'
  const content = isDirectSuggestion ? contentOverride.trim() : draft.value.trim()
  if (!conversationId.value || !content || sending.value || switchingConversation.value) return
  draft.value = ''
  sending.value = true
  error.value = ''
  const baseConversation = conversation.value
  const baseMessages = [...messages.value]
  const temporaryUserId = `draft-user-${Date.now()}`
  const temporaryAssistantId = `draft-assistant-${Date.now()}`
  if (baseConversation) {
    const now = new Date().toISOString()
    const emptyAssistant: MedicalAssistantMessage = {
      id: temporaryAssistantId,
      role: 'ASSISTANT',
      content: '',
      emergency: false,
      citations: [],
      usedContext: [],
      followupQuestions: [],
      createdAt: now,
    }
    conversation.value = {
      ...baseConversation,
      messages: [
        ...baseMessages,
        {
          id: temporaryUserId,
          role: 'USER',
          content,
          emergency: false,
          citations: [],
          usedContext: [],
          followupQuestions: [],
          createdAt: now,
        },
        emptyAssistant,
      ],
    }
    await scrollToBottom()
  }
  try {
    await new Promise<void>((resolve, reject) => {
      streamAbort.value = streamMedicalAssistantMessage(conversationId.value, content, {
        onDelta: (delta) => {
          if (!conversation.value) return
          conversation.value = {
            ...conversation.value,
            messages: conversation.value.messages.map((message) =>
              message.id === temporaryAssistantId
                ? { ...message, content: normalizeMedicalAssistantText(message.content + delta) }
                : message,
            ),
          }
          void scrollToBottom()
        },
        onDone: (nextConversation) => {
          conversation.value = nextConversation
          rememberConversation(nextConversation)
          resolve()
        },
        onError: reject,
      }).abort
    })
  } catch (cause) {
    try {
      conversation.value = await getMedicalAssistantConversation(conversationId.value)
    } catch {
      if (baseConversation) conversation.value = { ...baseConversation, messages: baseMessages }
    }
    if (!isDirectSuggestion) draft.value = content
    handleSendError(cause)
  } finally {
    streamAbort.value = undefined
    sending.value = false
  }
}

function sendSuggestion(value: string) {
  void sendMessage(value)
}

function handleSendError(cause: unknown) {
  if (cause instanceof ApiError && cause.code === MEDICAL_ASSISTANT_QUOTA_EXHAUSTED) {
    uni.showModal({
      title: '健康助手次数已用完',
      content: '免费客户可使用 3 次健康助手对话，开通年度健康会员后可继续使用。',
      confirmText: '开通会员',
      cancelText: '暂不',
      success: (result) => {
        if (result.confirm) uni.navigateTo({ url: '/pages-customer/member/subscribe' })
      },
    })
    return
  }
  uni.showToast({ title: cause instanceof Error ? cause.message : '发送失败，请稍后重试', icon: 'none' })
}

function messageParts(content: string): MessageTextPart[] {
  const parts: MessageTextPart[] = []
  const pattern = /\*\*([\s\S]+?)\*\*/g
  let cursor = 0
  let match: RegExpExecArray | null

  while ((match = pattern.exec(content)) !== null) {
    if (match.index > cursor) {
      const normalText = content.slice(cursor, match.index).replace(/\*\*/g, '')
      if (normalText) parts.push({ text: normalText, bold: false })
    }
    if (match[1]) parts.push({ text: match[1], bold: true })
    cursor = match.index + match[0].length
  }

  const trailingText = content.slice(cursor).replace(/\*\*/g, '')
  if (trailingText) parts.push({ text: trailingText, bold: false })

  return parts.length ? parts : [{ text: content.replace(/\*\*/g, ''), bold: false }]
}

function rememberConversation(nextConversation: MedicalAssistantConversation) {
  conversations.value = [
    { ...nextConversation, messages: [] },
    ...conversations.value.filter((item) => item.id !== nextConversation.id),
  ]
}

function displayConversationTitle(item: MedicalAssistantConversation) {
  return item.title === '新的健康咨询' ? '新对话' : item.title || '新对话'
}

function formatConversationTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (part: number) => String(part).padStart(2, '0')
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return `今天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

function toggleHistory() {
  if (loading.value || switchingConversation.value) return
  historyOpen.value = !historyOpen.value
}

function closeHistory() {
  historyOpen.value = false
}

async function selectConversation(id: string) {
  if (id === conversationId.value || sending.value || switchingConversation.value) {
    closeHistory()
    return
  }
  switchingConversation.value = true
  try {
    conversation.value = await getMedicalAssistantConversation(id)
    draft.value = ''
    closeHistory()
    await scrollToBottom()
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '对话打开失败，请稍后重试', icon: 'none' })
  } finally {
    switchingConversation.value = false
  }
}

async function createNewConversation() {
  if (sending.value || historyLoading.value) return
  historyLoading.value = true
  try {
    const created = await createMedicalAssistantConversation()
    conversation.value = created
    rememberConversation(created)
    draft.value = ''
    closeHistory()
    await scrollToBottom()
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '新对话创建失败，请稍后重试', icon: 'none' })
  } finally {
    historyLoading.value = false
  }
}

async function deleteConversation(item: MedicalAssistantConversation) {
  if (sending.value || switchingConversation.value || historyLoading.value) return
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '删除对话',
      content: '删除后将不再显示这段对话记录，确定删除吗？',
      confirmText: '删除',
      confirmColor: '#c95a47',
      success: (result) => resolve(Boolean(result.confirm)),
      fail: () => resolve(false),
    })
  })
  if (!confirmed) return

  historyLoading.value = true
  try {
    await deleteMedicalAssistantConversation(item.id)
    const remaining = conversations.value.filter((conversationItem) => conversationItem.id !== item.id)
    conversations.value = remaining
    if (item.id === conversationId.value) {
      if (remaining[0]) {
        conversation.value = await getMedicalAssistantConversation(remaining[0].id)
      } else {
        const created = await createMedicalAssistantConversation()
        conversation.value = created
        conversations.value = [created]
      }
      draft.value = ''
      await scrollToBottom()
    }
    uni.showToast({ title: '对话已删除', icon: 'none' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '删除失败，请稍后重试', icon: 'none' })
  } finally {
    historyLoading.value = false
  }
}

function goBack() {
  uni.navigateBack({
    delta: 1,
    fail: () => uni.switchTab({ url: '/pages/home/index' }),
  })
}

async function scrollToBottom() {
  await nextTick()
  scrollIntoView.value = ''
  await nextTick()
  scrollIntoView.value = 'conversation-bottom'
}
</script>

<style scoped>
.assistant-page { display: flex; flex-direction: column; height: 100vh; min-height: 100vh; padding: 0; background: #f3faf7; }
.assistant-topbar { display: flex; align-items: center; height: calc(96rpx + var(--status-bar-height)); padding: var(--status-bar-height) 28rpx 0; border-bottom: 1rpx solid #e1eee9; background: rgba(248, 253, 251, .96); box-sizing: border-box; }
.topbar-back { width: 54rpx; color: #173c33; font-size: 58rpx; font-weight: 300; line-height: 1; }
.topbar-title { flex: 1; color: #173c33; text-align: center; font-size: 34rpx; font-weight: 800; }
.topbar-spacer { width: 54rpx; }
.conversation-scroll { flex: 1; min-height: 0; box-sizing: border-box; }
.assistant-hero { position: relative; overflow: hidden; margin: 20rpx 24rpx 16rpx; padding: 30rpx 30rpx 24rpx; border-radius: 28rpx; color: #f1fffb; background: linear-gradient(135deg, #0b624f 0%, #10a080 100%); box-shadow: 0 18rpx 34rpx rgba(15, 114, 91, .18); }
.hero-orbit { position: absolute; border: 2rpx solid rgba(188, 255, 228, .18); border-radius: 50%; pointer-events: none; }
.orbit-one { width: 240rpx; height: 240rpx; right: -16rpx; top: -28rpx; }
.orbit-two { width: 360rpx; height: 360rpx; right: -76rpx; top: -88rpx; }
.hero-mark { position: absolute; z-index: 1; top: 36rpx; right: 48rpx; display: block; overflow: hidden; width: 112rpx; height: 112rpx; border: 3rpx solid rgba(255,255,255,.62); border-radius: 32rpx; background: #dbf8ec; box-shadow: 0 10rpx 20rpx rgba(2, 77, 60, .16); box-sizing: border-box; }
.hero-eyebrow, .hero-title, .hero-copy { position: relative; z-index: 2; max-width: calc(100% - 162rpx); }
.hero-eyebrow { margin-top: 0; color: #a6e9d2; font-size: 27rpx; font-weight: 800; line-height: 1.4; letter-spacing: 1rpx; }
.hero-title { margin-top: 12rpx; font-size: 42rpx; font-weight: 800; line-height: 1.32; }
.hero-copy { margin-top: 16rpx; color: rgba(240, 255, 249, .82); font-size: 27rpx; line-height: 1.55; }
.context-row { position: relative; z-index: 2; display: flex; flex-wrap: wrap; gap: 10rpx; margin-top: 18rpx; }
.context-chip { display: flex; align-items: center; gap: 8rpx; padding: 10rpx 16rpx; border: 1rpx solid rgba(255,255,255,.2); border-radius: 999rpx; color: rgba(255,255,255,.92); background: rgba(255,255,255,.1); font-size: 24rpx; }
.chip-dot { width: 12rpx; height: 12rpx; border-radius: 50%; }
.chip-dot.mint { background: #8ff0c8; }.chip-dot.blue { background: #8bd9ff; }.chip-dot.violet { background: #c7b4ff; }
.history-overlay { position: fixed; z-index: 30; top: calc(96rpx + var(--status-bar-height)); right: 0; bottom: 0; left: 0; background: rgba(13, 55, 45, .22); }
.history-drawer { display: flex; flex-direction: column; width: 78%; max-width: 640rpx; height: 100%; padding: 30rpx 22rpx calc(26rpx + env(safe-area-inset-bottom)); border-right: 1rpx solid #dceee7; border-radius: 0 30rpx 30rpx 0; background: #f8fcfa; box-shadow: 20rpx 0 48rpx rgba(18, 75, 61, .16); box-sizing: border-box; }
.history-drawer-head { display: flex; align-items: flex-start; justify-content: space-between; padding: 2rpx 8rpx 22rpx; }
.history-drawer-title { color: #173f35; font-size: 34rpx; font-weight: 800; }
.history-drawer-subtitle { margin-top: 8rpx; color: #86a199; font-size: 22rpx; }
.history-close { display: flex; align-items: center; justify-content: center; width: 52rpx; height: 52rpx; border-radius: 50%; color: #4b7569; background: #e8f5ef; font-size: 38rpx; font-weight: 300; line-height: 1; }
.new-conversation-button { display: flex; align-items: center; justify-content: center; width: 100%; height: 80rpx; margin: 0; padding: 0; border: 0; border-radius: 22rpx; color: #fff; background: linear-gradient(135deg, #1ab487, #087e65); box-shadow: 0 12rpx 22rpx rgba(14, 142, 107, .18); font-size: 28rpx; font-weight: 800; line-height: 80rpx; }
.new-conversation-button[disabled] { opacity: .55; }
.new-conversation-plus { margin-right: 8rpx; font-size: 34rpx; font-weight: 400; }
.history-list { flex: 1; min-height: 0; margin-top: 20rpx; }
.history-item { display: flex; align-items: center; justify-content: space-between; min-height: 84rpx; margin-bottom: 10rpx; padding: 14rpx 16rpx; border: 1rpx solid transparent; border-radius: 20rpx; background: #fff; box-sizing: border-box; }
.history-item.active { border-color: #75d2b5; background: #eaf9f2; box-shadow: inset 5rpx 0 #18a77f; }
.history-item:active { background: #e7f7f0; }
.history-item-main { min-width: 0; }
.history-item-title { overflow: hidden; color: #285347; font-size: 26rpx; font-weight: 700; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }
.history-item-time { margin-top: 6rpx; color: #9aafa8; font-size: 21rpx; }
.history-item-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 10rpx; margin-left: 12rpx; }
.history-active-mark { padding: 6rpx 10rpx; border-radius: 999rpx; color: #168667; background: #d6f3e6; font-size: 20rpx; }
.history-delete { display: flex; align-items: center; justify-content: center; min-width: 68rpx; min-height: 56rpx; padding: 0 10rpx; border: 1rpx solid #f0d3cd; border-radius: 16rpx; color: #b05d4f; background: #fff7f5; font-size: 22rpx; line-height: 56rpx; }
.history-delete:active { color: #923f31; background: #ffebe7; }
.history-empty { padding: 54rpx 20rpx; color: #90a69e; text-align: center; font-size: 24rpx; line-height: 1.5; }
.loading-card, .error-card, .welcome-card { margin: 0 24rpx 22rpx; padding: 30rpx; border: 1rpx solid #e0eee9; border-radius: 28rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(26, 91, 76, .05); }
.loading-card { display: flex; align-items: center; gap: 14rpx; color: #638078; font-size: 26rpx; }
.loading-dot { width: 16rpx; height: 16rpx; border-radius: 50%; background: #18aa82; box-shadow: 0 0 0 8rpx #dff8ed; }
.error-title, .welcome-title { color: #183e35; font-size: 34rpx; font-weight: 800; }
.error-copy, .welcome-copy { margin-top: 12rpx; color: #78918a; font-size: 27rpx; line-height: 1.55; }
.retry-button { height: 76rpx; margin: 22rpx 0 0; border: 0; border-radius: 38rpx; color: #fff; background: #0d8e70; font-size: 28rpx; line-height: 76rpx; }
.chat-area { padding: 0 24rpx 32rpx; }
.conversation-tools { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; margin: 0 0 18rpx; padding: 20rpx 20rpx 20rpx 24rpx; border: 1rpx solid #dceee7; border-radius: 24rpx; background: rgba(255, 255, 255, .92); box-shadow: 0 8rpx 20rpx rgba(26, 91, 76, .04); }
.conversation-tools-copy { min-width: 0; }
.conversation-tools-title { color: #285347; font-size: 28rpx; font-weight: 800; line-height: 1.35; }
.conversation-tools-hint { overflow: hidden; margin-top: 6rpx; color: #8da49c; font-size: 22rpx; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }
.history-entry { display: flex; flex: 0 0 auto; align-items: center; justify-content: center; min-width: 160rpx; height: 72rpx; margin: 0; padding: 0 16rpx; border: 1rpx solid #8bd8bd; border-radius: 18rpx; color: #117d64; background: #edfaf4; font-size: 24rpx; font-weight: 800; line-height: 72rpx; }
.history-entry:active { transform: scale(.97); background: #ddf5e9; }
.history-entry-arrow { margin-left: 7rpx; color: #32a985; font-size: 31rpx; font-weight: 400; line-height: 1; }
.date-note { margin: 4rpx 0 18rpx; color: #9aaaA5; text-align: center; font-size: 22rpx; }
.suggestion-list { margin-top: 22rpx; }
.suggestion-item { position: relative; display: flex; align-items: center; justify-content: center; min-height: 76rpx; margin: 10rpx 0 0; padding: 12rpx 56rpx 12rpx 24rpx; border: 1rpx solid #d7eee7; border-radius: 18rpx; box-sizing: border-box; color: #236456; background: #f6fcf9; text-align: center; font-size: 27rpx; line-height: 1.45; }
.suggestion-arrow { position: absolute; right: 20rpx; color: #37ad8d; font-size: 34rpx; line-height: 1; }
.message-row { display: flex; align-items: flex-start; gap: 14rpx; margin: 24rpx 0; }
.message-row.is-user { justify-content: flex-end; }
.assistant-avatar { display: block; flex: 0 0 auto; overflow: hidden; width: 72rpx; height: 72rpx; border: 2rpx solid #d3eee4; border-radius: 22rpx; background: #eefaf5; box-sizing: border-box; }
.message-column { max-width: 82%; }
.is-user .message-column { display: flex; flex-direction: column; align-items: flex-end; max-width: 84%; }
.message-label { margin: 2rpx 0 8rpx; color: #79938b; font-size: 22rpx; }
.message-bubble { padding: 22rpx 24rpx; border: 1rpx solid #dcece7; border-radius: 8rpx 26rpx 26rpx 26rpx; color: #284b42; background: #fff; box-shadow: 0 8rpx 20rpx rgba(31, 87, 72, .04); }
.is-user .message-bubble { border: 0; border-radius: 26rpx 8rpx 26rpx 26rpx; color: #fff; background: linear-gradient(135deg, #149b7b, #0b8066); }
.urgent-bubble { border-color: #f0c987; background: #fff8e9; color: #66491d; }
.message-text { display: block; white-space: pre-wrap; word-break: break-word; font-size: 30rpx; line-height: 1.65; }.message-bold { color: inherit; font-weight: 800; }
.urgent-card { margin-top: 12rpx; padding: 18rpx; border-left: 6rpx solid #e19a27; border-radius: 4rpx 16rpx 16rpx 4rpx; background: #fff4d9; }
.urgent-title { color: #8c5d13; font-size: 26rpx; font-weight: 800; }.urgent-copy { margin-top: 6rpx; color: #876b3e; font-size: 24rpx; line-height: 1.45; }
.followup-list { margin-top: 10rpx; }.followup-item { display: flex; align-items: center; justify-content: flex-start; min-height: 72rpx; margin: 8rpx 0 0; padding: 12rpx 22rpx; border: 1rpx solid #bfe8dc; border-radius: 18rpx; box-sizing: border-box; color: #11846a; background: #f4fcf8; text-align: left; font-size: 27rpx; line-height: 1.45; }.followup-item text { display: block; width: 100%; }
.typing-bubble { display: flex; align-items: center; gap: 8rpx; padding: 24rpx; border-radius: 8rpx 26rpx 26rpx 26rpx; background: #fff; }.typing-bubble.inline-typing { padding: 4rpx 2rpx; background: transparent; }.typing-bubble view { width: 10rpx; height: 10rpx; border-radius: 50%; background: #62b9a0; animation: typing-pulse 1.1s infinite ease-in-out; }.typing-bubble view:nth-child(2) { animation-delay: .16s; }.typing-bubble view:nth-child(3) { animation-delay: .32s; }
.conversation-bottom { height: 2rpx; }
.composer-shell { padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #dcece6; background: rgba(255,255,255,.98); box-shadow: 0 -8rpx 24rpx rgba(24, 90, 73, .05); }
.composer-row { display: flex; align-items: flex-end; gap: 12rpx; }.composer-input { flex: 1; min-height: 72rpx; max-height: 180rpx; padding: 16rpx 18rpx; border: 1rpx solid #d4e9e2; border-radius: 22rpx; box-sizing: border-box; color: #254d42; background: #f7fcfa; font-size: 28rpx; line-height: 1.45; }
.send-button { display: flex; align-items: center; justify-content: center; flex: 0 0 auto; width: 128rpx; min-width: 0; height: 72rpx; min-height: 0; margin: 0; padding: 0 12rpx; border: 0; border-radius: 22rpx; box-sizing: border-box; color: #a4b5b0; background: #e7f0ed; font-size: 24rpx; line-height: 1; transition: transform .16s ease, box-shadow .16s ease; }.send-button.ready { color: #fff; background: linear-gradient(135deg, #19ad86 0%, #087e65 100%); box-shadow: 0 8rpx 16rpx rgba(12, 137, 104, .2); }.send-button.ready:active { transform: scale(.96); box-shadow: 0 4rpx 10rpx rgba(12, 137, 104, .16); }.send-button[disabled] { opacity: 1; }.send-label { font-weight: 800; letter-spacing: 1rpx; }.send-glyph { margin-left: 6rpx; font-size: 30rpx; font-weight: 700; line-height: 1; }.send-loading { min-width: 42rpx; font-size: 30rpx; font-weight: 800; letter-spacing: 4rpx; }
@keyframes typing-pulse { 0%, 70%, 100% { opacity: .35; transform: translateY(0); } 35% { opacity: 1; transform: translateY(-4rpx); } }
@media (prefers-reduced-motion: reduce) { .typing-bubble view, .send-button { animation: none; transition: none; } }
</style>

<template>
  <PlatformAdminShell>
    <view class="page model-page">
    <view class="model-hero">
      <view class="hero-eyebrow">AI MODEL CONTROL</view>
      <view class="hero-title">AI模型管理</view>
      <view class="hero-copy">仅平台管理员可切换健康评估使用的 DeepSeek 模型</view>
      <view class="hero-note">切换将在下一次生成评估时生效，当前报告不会被改写。</view>
    </view>

    <view v-if="selectedModel" class="card thinking-control">
      <view class="thinking-copy">
        <view class="thinking-title">思考模式</view>
        <view class="thinking-description">
          开启后模型会进行更深度推理，可能增加 token 消耗和响应时间
        </view>
        <view class="thinking-status">当前：{{ thinkingEnabled ? '已开启' : '已关闭' }}</view>
      </view>
      <view
        class="thinking-switch"
        :class="{ enabled: thinkingEnabled, disabled: !selectedModel.thinkingSupported || switchingThinking }"
        @click.stop="toggleThinking"
      >
        <view class="thinking-knob" />
      </view>
    </view>

    <PageState :loading="loading" :error="error" :empty="models.length === 0">
      <view class="model-list">
        <view
          v-for="model in models"
          :key="model.modelCode"
          class="card model-card"
          :class="{ selected: model.selected }"
          @click="selectModel(model)"
        >
          <view class="model-card-head">
            <view class="model-card-title-wrap">
              <view class="model-icon">AI</view>
              <view>
                <view class="model-card-title">{{ model.modelName }}</view>
                <view class="model-code">{{ model.modelCode }}</view>
              </view>
            </view>
            <view class="model-status" :class="{ active: model.selected }">
              {{ model.selected ? '当前使用' : '可切换' }}
            </view>
          </view>

          <view class="model-version">{{ model.modelVersion }}</view>
          <view class="model-meta-grid">
            <view class="meta-item">
              <view class="meta-label">上下文长度</view>
              <view class="meta-value">{{ formatTokens(model.contextLengthTokens) }}</view>
            </view>
            <view class="meta-item">
              <view class="meta-label">输出上限</view>
              <view class="meta-value">{{ formatTokens(model.maxOutputTokens) }}</view>
            </view>
            <view class="meta-item">
              <view class="meta-label">思考模式</view>
              <view class="meta-value">{{ model.thinkingSupported ? '支持' : '不支持' }}</view>
            </view>
          </view>
          <view v-if="model.selected" class="current-hint">新生成的AI综合评估将使用此模型</view>
        </view>
      </view>
    </PageState>
    </view>
  </PlatformAdminShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getAiModelRuntimeConfigs, switchAiModel, switchAiThinking } from '@/api/admin'
import PageState from '@/components/PageState.vue'
import PlatformAdminShell from '@/components/PlatformAdminShell.vue'
import type { AiModelRuntimeConfig } from '@/types/api'

const models = ref<AiModelRuntimeConfig[]>([])
const loading = ref(true)
const error = ref('')
const switching = ref(false)
const switchingThinking = ref(false)

const selectedModel = computed(() => models.value.find((model) => model.selected) ?? null)
const thinkingEnabled = computed(() => selectedModel.value?.thinkingEnabled ?? false)

const formatTokens = (value: number) => {
  if (value >= 1_000_000) return `${Math.round(value / 1_000_000)}M`
  if (value >= 1_000) return `${Math.round(value / 1_000)}K`
  return String(value)
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    models.value = await getAiModelRuntimeConfigs()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '模型配置加载失败'
  } finally {
    loading.value = false
  }
}

const selectModel = async (model: AiModelRuntimeConfig) => {
  if (model.selected || switching.value) return
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '切换AI模型',
      content: `确定切换为${model.modelName}吗？下一次生成评估时生效。`,
      confirmText: '确认切换',
      cancelText: '取消',
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    })
  })
  if (!confirmed) return
  switching.value = true
  try {
    const updated = await switchAiModel(model.modelCode)
    models.value = models.value.map((item) => ({ ...item, selected: item.modelCode === updated.modelCode }))
    uni.showToast({ title: '模型已切换', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '模型切换失败', icon: 'none' })
  } finally {
    switching.value = false
  }
}

const toggleThinking = async () => {
  const model = selectedModel.value
  if (!model || !model.thinkingSupported || switchingThinking.value) return
  const nextValue = !model.thinkingEnabled
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '思考模式',
      content: nextValue
        ? '开启后可能增加 token 消耗和响应时间，确定开启吗？'
        : '关闭后将使用普通响应模式，确定关闭吗？',
      confirmText: '确定',
      cancelText: '取消',
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    })
  })
  if (!confirmed) return
  switchingThinking.value = true
  try {
    const updated = await switchAiThinking(nextValue)
    models.value = models.value.map((item) => ({
      ...item,
      thinkingEnabled: item.selected ? updated.thinkingEnabled : item.thinkingEnabled,
    }))
    uni.showToast({ title: nextValue ? '已开启思考模式' : '已关闭思考模式', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '思考模式设置失败', icon: 'none' })
  } finally {
    switchingThinking.value = false
  }
}

onShow(load)
</script>

<style scoped>
.model-page {
  padding-top: 34rpx;
}
.model-hero {
  padding: 32rpx 30rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #0b6b57, #149577);
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(12, 102, 82, 0.2);
}
.hero-eyebrow {
  color: rgba(232, 255, 248, 0.72);
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}
.hero-title {
  margin-top: 10rpx;
  font-size: 40rpx;
  font-weight: 760;
}
.hero-copy {
  margin-top: 8rpx;
  color: rgba(238, 255, 249, 0.88);
  font-size: 23rpx;
  line-height: 1.5;
}
.hero-note {
  margin-top: 18rpx;
  padding-top: 17rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.2);
  color: rgba(238, 255, 249, 0.72);
  font-size: 20rpx;
  line-height: 1.5;
}
.model-list {
  margin-top: 24rpx;
}
.thinking-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
  padding: 26rpx 28rpx;
}
.thinking-copy {
  flex: 1;
  min-width: 0;
  margin-right: 20rpx;
}
.thinking-title {
  color: #183d33;
  font-size: 29rpx;
  font-weight: 740;
}
.thinking-description {
  margin-top: 8rpx;
  color: #71817b;
  font-size: 20rpx;
  line-height: 1.5;
}
.thinking-status {
  margin-top: 9rpx;
  color: #0f7a62;
  font-size: 20rpx;
}
.thinking-switch {
  display: flex;
  align-items: center;
  width: 92rpx;
  height: 52rpx;
  padding: 5rpx;
  box-sizing: border-box;
  border-radius: 999rpx;
  background: #d6e3df;
  transition: background 0.2s ease;
}
.thinking-switch.enabled {
  background: #0f7a62;
}
.thinking-switch.disabled {
  opacity: 0.55;
}
.thinking-knob {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 3rpx 8rpx rgba(24, 61, 51, 0.14);
  transition: transform 0.2s ease;
}
.thinking-switch.enabled .thinking-knob {
  transform: translateX(40rpx);
}
.model-card {
  margin-bottom: 18rpx;
  padding: 28rpx;
  border: 2rpx solid transparent;
}
.model-card.selected {
  border-color: #35b697;
  box-shadow: 0 14rpx 30rpx rgba(15, 122, 98, 0.13);
}
.model-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}
.model-card-title-wrap {
  display: flex;
  align-items: center;
  min-width: 0;
}
.model-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  margin-right: 16rpx;
  border-radius: 19rpx;
  background: #e3f6ef;
  color: #0e7962;
  font-size: 22rpx;
  font-weight: 760;
}
.model-card-title {
  color: #183d33;
  font-size: 29rpx;
  font-weight: 740;
}
.model-code {
  margin-top: 5rpx;
  color: #879a93;
  font-size: 20rpx;
}
.model-status {
  flex: 0 0 auto;
  padding: 9rpx 14rpx;
  border-radius: 999rpx;
  background: #f1f4f3;
  color: #71817b;
  font-size: 20rpx;
}
.model-status.active {
  background: #e1f6ee;
  color: #0f7a62;
}
.model-version {
  margin-top: 20rpx;
  padding: 13rpx 16rpx;
  border-radius: 14rpx;
  background: #f3faf7;
  color: #417466;
  font-size: 21rpx;
}
.model-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 20rpx;
}
.meta-item {
  padding: 14rpx 10rpx;
  border-radius: 14rpx;
  background: #f7faf9;
  text-align: center;
}
.meta-label {
  color: #8a9a94;
  font-size: 19rpx;
}
.meta-value {
  margin-top: 6rpx;
  color: #24584a;
  font-size: 23rpx;
  font-weight: 700;
}
.current-hint {
  margin-top: 17rpx;
  color: #0e7a61;
  font-size: 21rpx;
}
</style>

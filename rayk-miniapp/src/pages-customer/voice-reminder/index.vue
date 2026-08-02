<template>
  <view class="page reminder-page elder-page">
    <view class="hero-card">
      <view class="hero-icon">音</view>
      <view class="hero-content">
        <view class="hero-title">健康语音提醒</view>
        <view class="hero-copy">按时吃饭，好好睡觉，让健康多一点陪伴</view>
      </view>
    </view>

    <view class="warm-card">
      <view class="warm-title">每天两次，刚刚好的关心</view>
      <view class="warm-copy">
        提醒文案会自然变化，{{ setting.voiceDescription || '并根据性别选择温暖音色' }}。
      </view>
    </view>

    <view class="setting-card">
      <view class="setting-head">
        <view class="setting-icon meal">餐</view>
        <view class="setting-main">
          <view class="setting-title">吃饭提醒</view>
          <view class="setting-copy">到了饭点，温柔提醒您及时补充能量</view>
        </view>
        <switch :checked="setting.mealEnabled" color="#11856c" @change="toggleMeal" />
      </view>
      <picker mode="time" :value="shortTime(setting.mealTime)" @change="changeMealTime">
        <view class="time-row" :class="{ disabled: !setting.mealEnabled }">
          <view>
            <view class="time-label">提醒时间</view>
            <view class="time-hint">点击可修改</view>
          </view>
          <view class="time-value">{{ shortTime(setting.mealTime) }} <text>›</text></view>
        </view>
      </picker>
      <button
        class="preview-button"
        :disabled="previewing !== '' || !setting.serviceAvailable"
        @click="preview('MEAL')"
      >
        {{ previewing === 'MEAL' ? '正在生成语音…' : '试听吃饭提醒' }}
      </button>
    </view>

    <view class="setting-card">
      <view class="setting-head">
        <view class="setting-icon sleep">眠</view>
        <view class="setting-main">
          <view class="setting-title">睡觉提醒</view>
          <view class="setting-copy">夜深时提醒您放下忙碌，早点休息</view>
        </view>
        <switch :checked="setting.sleepEnabled" color="#11856c" @change="toggleSleep" />
      </view>
      <picker mode="time" :value="shortTime(setting.sleepTime)" @change="changeSleepTime">
        <view class="time-row" :class="{ disabled: !setting.sleepEnabled }">
          <view>
            <view class="time-label">提醒时间</view>
            <view class="time-hint">点击可修改</view>
          </view>
          <view class="time-value">{{ shortTime(setting.sleepTime) }} <text>›</text></view>
        </view>
      </picker>
      <button
        class="preview-button"
        :disabled="previewing !== '' || !setting.serviceAvailable"
        @click="preview('SLEEP')"
      >
        {{ previewing === 'SLEEP' ? '正在生成语音…' : '试听睡觉提醒' }}
      </button>
    </view>

    <view v-if="lastPreviewText" class="preview-card">
      <view class="preview-label">刚刚为您生成</view>
      <view class="preview-text">“{{ lastPreviewText }}”</view>
    </view>

    <view v-if="!setting.serviceAvailable && !loading" class="service-tip">
      语音服务尚未启用，请联系管理员完成配置。
    </view>

    <button class="save-button" :disabled="saving || loading" @click="save">
      {{ saving ? '正在保存…' : '保存提醒设置' }}
    </button>
    <view class="bottom-tip">提醒时间已保存；微信关闭时的准时通知需在微信中授权消息提醒。</view>
  </view>
</template>

<script setup lang="ts">
import { onShow, onUnload } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import {
  createVoiceReminderPreview,
  getVoiceReminderSetting,
  updateVoiceReminderSetting,
  type VoiceReminderSetting,
} from '@/api/voice-reminder'
import { getApiBaseUrl, getRequestHeaders } from '@/utils/request'

const defaultSetting: VoiceReminderSetting = {
  mealEnabled: true,
  mealTime: '11:30:00',
  sleepEnabled: true,
  sleepTime: '21:30:00',
  timezone: 'Asia/Shanghai',
  voiceDescription: '',
  serviceAvailable: false,
}

const setting = reactive<VoiceReminderSetting>({ ...defaultSetting })
const loading = ref(false)
const saving = ref(false)
const previewing = ref<'' | 'MEAL' | 'SLEEP'>('')
const lastPreviewText = ref('')
let audio: ReturnType<typeof uni.createInnerAudioContext> | undefined

const shortTime = (value: string) => (value || '00:00').slice(0, 5)

const load = async () => {
  loading.value = true
  try {
    Object.assign(setting, await getVoiceReminderSetting())
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提醒设置读取失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const toggleMeal = (event: unknown) => {
  setting.mealEnabled = (event as { detail: { value: boolean } }).detail.value
}

const toggleSleep = (event: unknown) => {
  setting.sleepEnabled = (event as { detail: { value: boolean } }).detail.value
}

const changeMealTime = (event: { detail: { value: string | number } }) => {
  setting.mealTime = `${String(event.detail.value)}:00`
}

const changeSleepTime = (event: { detail: { value: string | number } }) => {
  setting.sleepTime = `${String(event.detail.value)}:00`
}

const save = async () => {
  saving.value = true
  try {
    Object.assign(
      setting,
      await updateVoiceReminderSetting({
        mealEnabled: setting.mealEnabled,
        mealTime: setting.mealTime,
        sleepEnabled: setting.sleepEnabled,
        sleepTime: setting.sleepTime,
      }),
    )
    uni.showToast({ title: '提醒设置已保存', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

const playAudio = (audioUrl: string) => {
  uni.downloadFile({
    url: `${getApiBaseUrl()}${audioUrl}`,
    header: getRequestHeaders(),
    success: (response) => {
      if (response.statusCode !== 200) {
        uni.showToast({ title: '语音下载失败', icon: 'none' })
        return
      }
      audio?.destroy()
      audio = uni.createInnerAudioContext()
      audio.autoplay = true
      audio.src = response.tempFilePath
      audio.onError(() => uni.showToast({ title: '语音播放失败', icon: 'none' }))
    },
    fail: () => uni.showToast({ title: '语音下载失败，请检查网络', icon: 'none' }),
  })
}

const preview = async (type: 'MEAL' | 'SLEEP') => {
  previewing.value = type
  try {
    const result = await createVoiceReminderPreview(type)
    lastPreviewText.value = result.text
    playAudio(result.audioUrl)
  } catch (error) {
    setting.serviceAvailable = false
    uni.showToast({ title: error instanceof Error ? error.message : '试听生成失败', icon: 'none' })
  } finally {
    previewing.value = ''
  }
}

onShow(load)
onUnload(() => audio?.destroy())
</script>

<style scoped>
.reminder-page {
  padding: 24rpx 24rpx 80rpx;
  background: linear-gradient(180deg, #e7f8f2 0, #f5faf8 520rpx);
  min-height: 100vh;
  color: #173c35;
}

.hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 32rpx;
  border-radius: 32rpx;
  color: #fff;
  background: linear-gradient(135deg, #086a57, #18a37f);
  box-shadow: 0 18rpx 36rpx rgba(8, 106, 87, 0.18);
}

.hero-icon {
  width: 92rpx;
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border: 2rpx solid rgba(255, 255, 255, 0.45);
  border-radius: 26rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 38rpx;
  font-weight: 800;
}

.hero-title { font-size: 40rpx; font-weight: 800; }
.hero-copy { margin-top: 12rpx; font-size: 28rpx; line-height: 1.55; opacity: 0.9; }

.warm-card,
.setting-card,
.preview-card {
  margin-top: 24rpx;
  padding: 30rpx;
  border: 1rpx solid #d9e9e3;
  border-radius: 30rpx;
  background: #fff;
  box-shadow: 0 12rpx 28rpx rgba(36, 75, 64, 0.07);
}

.warm-card { background: linear-gradient(135deg, #fff9e9, #fffdf6); border-color: #f1dfad; }
.warm-title { font-size: 32rpx; font-weight: 800; color: #7c5a0a; }
.warm-copy { margin-top: 10rpx; font-size: 27rpx; line-height: 1.65; color: #7b6d4d; }

.setting-head { display: flex; align-items: center; gap: 18rpx; }
.setting-icon {
  width: 74rpx; height: 74rpx; display: flex; align-items: center; justify-content: center;
  border-radius: 22rpx; font-size: 28rpx; font-weight: 800;
}
.setting-icon.meal { color: #a56b00; background: #fff0d0; }
.setting-icon.sleep { color: #5a52a4; background: #eceaff; }
.setting-main { flex: 1; min-width: 0; }
.setting-title { font-size: 34rpx; font-weight: 800; }
.setting-copy { margin-top: 6rpx; color: #70827c; font-size: 25rpx; line-height: 1.45; }

.time-row {
  margin-top: 26rpx; padding: 24rpx 26rpx; display: flex; align-items: center; justify-content: space-between;
  border-radius: 22rpx; background: #f2f8f6;
}
.time-row.disabled { opacity: 0.5; }
.time-label { font-size: 28rpx; font-weight: 700; }
.time-hint { margin-top: 4rpx; font-size: 23rpx; color: #8b9994; }
.time-value { color: #08745e; font-size: 38rpx; font-weight: 800; }
.time-value text { margin-left: 8rpx; color: #8caaa1; }

.preview-button,
.save-button {
  margin: 22rpx 0 0; height: 92rpx; display: flex; align-items: center; justify-content: center;
  border: 0; border-radius: 24rpx; font-size: 30rpx; font-weight: 800;
}
.preview-button { color: #08745e; background: #e4f5ef; }
.save-button { color: #fff; background: linear-gradient(135deg, #109476, #08745e); box-shadow: 0 12rpx 26rpx rgba(8, 116, 94, 0.2); }
button::after { border: 0; }
button[disabled] { opacity: 0.55; }
.preview-label { color: #0b7962; font-size: 25rpx; font-weight: 700; }
.preview-text { margin-top: 12rpx; font-size: 29rpx; line-height: 1.7; }
.service-tip { margin-top: 22rpx; padding: 22rpx; border-radius: 20rpx; color: #a66700; background: #fff1d8; text-align: center; font-size: 26rpx; }
.bottom-tip { margin-top: 18rpx; padding: 0 18rpx; color: #87958f; text-align: center; font-size: 23rpx; line-height: 1.55; }
</style>

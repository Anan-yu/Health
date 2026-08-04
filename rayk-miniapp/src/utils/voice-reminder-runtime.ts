import {
  createVoiceReminderPreview,
  getVoiceReminderSetting,
  type VoiceReminderSetting,
} from '@/api/voice-reminder'
import { getApiBaseUrl, getRequestHeaders } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

type ReminderType = 'MEAL' | 'SLEEP'

const POLL_INTERVAL_MS = 15_000
const SETTING_REFRESH_INTERVAL_MS = 60_000

let timer: ReturnType<typeof globalThis.setInterval> | undefined
let setting: VoiceReminderSetting | undefined
let settingLoadedAt = 0
let audio: ReturnType<typeof uni.createInnerAudioContext> | undefined
const lastTriggered = new Map<ReminderType, string>()
const pending = new Set<ReminderType>()

const currentMinute = (timezone: string) => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone || 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(new Date())
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]))
  const hour = values.hour === '24' ? '00' : values.hour
  return {
    date: `${values.year}-${values.month}-${values.day}`,
    time: `${hour}:${values.minute}`,
  }
}

const shortTime = (value: string) => (value || '').slice(0, 5)

const loadSetting = async () => {
  const auth = useAuthStore()
  if (!auth.isLoggedIn || auth.currentWorkbench !== 'CUSTOMER') {
    setting = undefined
    settingLoadedAt = 0
    return
  }
  if (setting && Date.now() - settingLoadedAt < SETTING_REFRESH_INTERVAL_MS) return
  try {
    setting = await getVoiceReminderSetting()
    settingLoadedAt = Date.now()
  } catch {
    // Page-level requests surface authentication and network errors to the user.
    // The background loop stays quiet and retries on the next interval.
    setting = undefined
    settingLoadedAt = 0
  }
}

const playScheduledAudio = (type: ReminderType) => {
  if (pending.has(type)) return
  pending.add(type)
  void createVoiceReminderPreview(type)
    .then((result) => {
      uni.downloadFile({
        url: `${getApiBaseUrl()}${result.audioUrl}`,
        header: getRequestHeaders(),
        success: (response) => {
          if (response.statusCode !== 200) return
          audio?.destroy()
          audio = uni.createInnerAudioContext()
          audio.autoplay = true
          audio.src = response.tempFilePath
          audio.onError(() => undefined)
        },
      })
    })
    .catch(() => undefined)
    .finally(() => pending.delete(type))
}

const check = async () => {
  await loadSetting()
  if (!setting) return
  const now = currentMinute(setting.timezone)
  const candidates: Array<[ReminderType, boolean, string]> = [
    ['MEAL', setting.mealEnabled, shortTime(setting.mealTime)],
    ['SLEEP', setting.sleepEnabled, shortTime(setting.sleepTime)],
  ]
  for (const [type, enabled, time] of candidates) {
    if (!enabled || time !== now.time) continue
    const triggerKey = `${now.date} ${time}`
    if (lastTriggered.get(type) === triggerKey) continue
    lastTriggered.set(type, triggerKey)
    playScheduledAudio(type)
  }
}

export const startVoiceReminderRuntime = () => {
  if (timer) return
  void check()
  timer = globalThis.setInterval(() => void check(), POLL_INTERVAL_MS)
}

export const refreshVoiceReminderRuntime = () => {
  settingLoadedAt = 0
  void check()
}

export const stopVoiceReminderRuntime = () => {
  if (timer) globalThis.clearInterval(timer)
  timer = undefined
  setting = undefined
  settingLoadedAt = 0
  pending.clear()
  audio?.destroy()
  audio = undefined
}

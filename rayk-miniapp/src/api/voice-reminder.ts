import { request } from '@/utils/request'

export interface VoiceReminderSetting {
  mealEnabled: boolean
  mealTime: string
  sleepEnabled: boolean
  sleepTime: string
  timezone: string
  voiceDescription: string
  serviceAvailable: boolean
}

export interface VoiceReminderPreview {
  id: number
  type: 'MEAL' | 'SLEEP'
  text: string
  voiceName: string
  audioUrl: string
}

export const getVoiceReminderSetting = () =>
  request<VoiceReminderSetting>({
    url: '/api/v1/me/voice-reminders/settings',
    method: 'GET',
  })

export const updateVoiceReminderSetting = (data: {
  mealEnabled: boolean
  mealTime: string
  sleepEnabled: boolean
  sleepTime: string
}) =>
  request<VoiceReminderSetting>({
    url: '/api/v1/me/voice-reminders/settings',
    method: 'PUT',
    data,
  })

export const createVoiceReminderPreview = (type: 'MEAL' | 'SLEEP') =>
  request<VoiceReminderPreview>({
    url: '/api/v1/me/voice-reminders/preview',
    method: 'POST',
    data: { type },
  })

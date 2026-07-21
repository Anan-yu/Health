<template>
  <view class="page"
    ><view class="title">完善健康档案</view
    ><view class="subtitle page-copy">健康资料仅用于已授权的健康管理服务，请如实填写。</view
    ><PageState :loading="loading" :error="error" :empty="!patient"
      ><view v-if="!collectionAuthorized" class="consent-warning"
        ><text>保存健康档案前，请先授权“健康数据采集”。</text
        ><button size="mini" @click="openPrivacy">去授权</button></view
      ><view class="card form-card"
        ><view class="field"
          ><text>身高（cm）</text
          ><input v-model="form.heightCm" type="digit" placeholder="例如 168" /></view
        ><view class="field"
          ><text>体重（kg）</text
          ><input v-model="form.weightKg" type="digit" placeholder="例如 60" /></view
        ><view class="field"
          ><text>血型</text><input v-model="form.bloodType" placeholder="例如 A 型" /></view
        ><view class="field"
          ><text>既往病史</text
          ><textarea v-model="form.medicalHistory" placeholder="如无，请填写无" /></view
        ><view class="field"
          ><text>过敏史</text
          ><textarea v-model="form.allergyHistory" placeholder="如无，请填写无" /></view
        ><view class="field"
          ><text>当前用药</text
          ><textarea v-model="form.currentMedications" placeholder="如无，请填写无" /></view
        ><view class="field"
          ><text>吸烟情况</text
          ><picker
            :range="smokingOptions"
            range-key="label"
            @change="selectOption('smokingStatus', smokingOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(smokingOptions, form.smokingStatus) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>饮酒情况</text
          ><picker
            :range="alcoholOptions"
            range-key="label"
            @change="selectOption('alcoholStatus', alcoholOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(alcoholOptions, form.alcoholStatus) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>运动频率</text
          ><picker
            :range="exerciseOptions"
            range-key="label"
            @change="selectOption('exerciseFrequency', exerciseOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(exerciseOptions, form.exerciseFrequency) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>睡眠质量</text
          ><picker
            :range="qualityOptions"
            range-key="label"
            @change="selectOption('sleepQuality', qualityOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(qualityOptions, form.sleepQuality) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>压力水平</text
          ><picker
            :range="stressOptions"
            range-key="label"
            @change="selectOption('stressLevel', stressOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(stressOptions, form.stressLevel) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>饮食偏好</text
          ><input v-model="form.dietaryPreference" placeholder="例如：均衡、低盐、素食" /></view
        ><view class="field"
          ><text>生活方式概述</text
          ><textarea
            v-model="form.lifestyleSummary"
            placeholder="饮食、运动、作息等"
          /></view></view
      ><button
        class="primary-button"
        :loading="saving"
        :disabled="!collectionAuthorized || saving"
        @click="save"
      >
        保存健康档案
      </button></PageState
    ></view
  >
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getHealthProfile, getMyProfile, updateHealthProfile } from '@/api/patient'
import { getPrivacyConsents } from '@/api/privacy'
import type { HealthProfile, Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'

const patient = ref<Patient | null>(null),
  loading = ref(true),
  saving = ref(false),
  collectionAuthorized = ref(false),
  error = ref('')
const form = reactive<Record<string, string>>({
  heightCm: '',
  weightKg: '',
  bloodType: '',
  medicalHistory: '',
  allergyHistory: '',
  currentMedications: '',
  smokingStatus: '',
  alcoholStatus: '',
  exerciseFrequency: '',
  sleepQuality: '',
  stressLevel: '',
  dietaryPreference: '',
  lifestyleSummary: '',
})
type SelectOption = { label: string; value: string }
type PickerChangeEvent = { detail: { value: string | number } }
const smokingOptions: SelectOption[] = [
  { label: '请选择', value: '' },
  { label: '从不吸烟', value: 'NEVER' },
  { label: '已经戒烟', value: 'FORMER' },
  { label: '目前吸烟', value: 'CURRENT' },
]
const alcoholOptions: SelectOption[] = [
  { label: '请选择', value: '' },
  { label: '不饮酒', value: 'NONE' },
  { label: '偶尔饮酒', value: 'OCCASIONAL' },
  { label: '经常饮酒', value: 'REGULAR' },
]
const exerciseOptions: SelectOption[] = [
  { label: '请选择', value: '' },
  { label: '基本不运动', value: 'RARELY' },
  { label: '每周 1–2 次', value: '1_2_PER_WEEK' },
  { label: '每周 3–5 次', value: '3_5_PER_WEEK' },
  { label: '几乎每天', value: 'DAILY' },
]
const qualityOptions: SelectOption[] = [
  { label: '请选择', value: '' },
  { label: '较差', value: 'POOR' },
  { label: '一般', value: 'FAIR' },
  { label: '良好', value: 'GOOD' },
]
const stressOptions: SelectOption[] = [
  { label: '请选择', value: '' },
  { label: '较低', value: 'LOW' },
  { label: '中等', value: 'MEDIUM' },
  { label: '较高', value: 'HIGH' },
]
const optionLabel = (options: SelectOption[], value: string) =>
  options.find((item) => item.value === value)?.label || '请选择'
const selectOption = (field: string, options: SelectOption[], event: PickerChangeEvent) => {
  form[field] = options[Number(event.detail.value)]?.value || ''
}
const assign = (profile: HealthProfile) =>
  Object.keys(form).forEach((key) => {
    form[key] = String(profile[key as keyof HealthProfile] || '')
  })
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    patient.value = await getMyProfile()
    if (patient.value) {
      const [profile, consents] = await Promise.all([
        getHealthProfile(patient.value.id),
        getPrivacyConsents(patient.value.id),
      ])
      assign(profile)
      collectionAuthorized.value = consents.some(
        (item) => item.consentType === 'DATA_COLLECTION' && item.consented === 1,
      )
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '档案加载失败'
  } finally {
    loading.value = false
  }
})
const save = async () => {
  if (!patient.value) return
  if (!collectionAuthorized.value) {
    uni.showToast({ title: '请先授权健康数据采集', icon: 'none' })
    return
  }
  const height = Number(form.heightCm)
  const weight = Number(form.weightKg)
  if (form.heightCm && (!Number.isFinite(height) || height < 50 || height > 250)) {
    uni.showToast({ title: '身高应在 50–250 cm', icon: 'none' })
    return
  }
  if (form.weightKg && (!Number.isFinite(weight) || weight < 10 || weight > 500)) {
    uni.showToast({ title: '体重应在 10–500 kg', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await updateHealthProfile(patient.value.id, {
      ...form,
      heightCm: form.heightCm ? height : null,
      weightKg: form.weightKg ? weight : null,
    })
    uni.showToast({ title: '已保存', icon: 'success' })
    globalThis.setTimeout(() => uni.navigateBack(), 500)
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
const openPrivacy = () => uni.navigateTo({ url: '/pages-customer/privacy/index' })
</script>

<style scoped>
.page-copy {
  margin-bottom: 24rpx;
}
.field {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #edf1ef;
}
.field text {
  display: block;
  margin-bottom: 14rpx;
  color: #29483e;
  font-size: 25rpx;
}
.field input,
.field textarea,
.picker-value {
  width: 100%;
  box-sizing: border-box;
  color: #334b43;
  font-size: 25rpx;
}
.picker-value {
  min-height: 42rpx;
  color: #526b63;
}
.field textarea {
  min-height: 110rpx;
}
.primary-button {
  margin-top: 26rpx;
  border-radius: 16rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 28rpx;
}
.consent-warning {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 20rpx;
  padding: 22rpx;
  border-radius: 18rpx;
  background: #fff4dc;
  color: #7b590f;
  font-size: 23rpx;
}
.consent-warning button {
  flex: 0 0 auto;
  color: #7b590f;
  background: #ffe4a8;
}
</style>

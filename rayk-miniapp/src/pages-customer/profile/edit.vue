<template>
  <view class="page"
    ><view class="title">健康档案与问卷</view
    ><view class="subtitle page-copy">请认真作答，作答内容将作为健康报告评估依据。</view
    ><PageState :loading="loading" :error="error" :empty="!patient"
      ><view v-if="!collectionAuthorized" class="consent-warning"
        ><text>保存健康档案前，请先授权“健康数据采集”。</text
        ><button size="mini" @click="openPrivacy">去授权</button></view
      ><view class="questionnaire-note"><text>健康问卷</text><view>身高体重、既往情况、饮食、睡眠和情绪信息将与检验报告共同用于本次健康评估。</view></view
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
          ><text>家族病史</text
          ><textarea v-model="form.familyHistory" placeholder="如父母或兄弟姐妹的糖尿病、高血压、心脑血管病、肿瘤等；如无请填写无" /></view
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
          ><text>近两周平均睡眠时长（小时）</text
          ><input v-model="form.sleepHours" type="digit" placeholder="例如 7.5" /></view
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
          ><text>近期心情</text
          ><picker :range="moodOptions" range-key="label" @change="selectOption('moodStatus', moodOptions, $event)"
            ><view class="picker-value">{{ optionLabel(moodOptions, form.moodStatus) }} ›</view></picker></view
        ><view class="field"
          ><text>近期恐惧或焦虑感受</text
          ><picker :range="fearOptions" range-key="label" @change="selectOption('fearLevel', fearOptions, $event)"
            ><view class="picker-value">{{ optionLabel(fearOptions, form.fearLevel) }} ›</view></picker></view
        ><view class="field"
          ><text>饮食偏好</text
          ><input v-model="form.dietaryPreference" placeholder="例如：均衡、低盐、素食" /></view
        ><view class="field"
          ><text>近三周饮食结构</text
          ><textarea v-model="form.recentDietaryPattern" placeholder="例如：蔬果、全谷、奶豆、鱼肉、外卖、甜饮和夜宵的频率" /></view
        ><view class="field"
          ><text>糖尿病既往诊断</text><picker :range="diseaseOptions" range-key="label" @change="selectOption('diabetesStatus', diseaseOptions, $event)"
            ><view class="picker-value">{{ optionLabel(diseaseOptions, form.diabetesStatus) }} ›</view></picker></view
        ><view class="field"
          ><text>高血压既往诊断</text><picker :range="diseaseOptions" range-key="label" @change="selectOption('hypertensionStatus', diseaseOptions, $event)"
            ><view class="picker-value">{{ optionLabel(diseaseOptions, form.hypertensionStatus) }} ›</view></picker></view
        ><view class="field"
          ><text>血脂异常既往诊断</text><picker :range="diseaseOptions" range-key="label" @change="selectOption('dyslipidemiaStatus', diseaseOptions, $event)"
            ><view class="picker-value">{{ optionLabel(diseaseOptions, form.dyslipidemiaStatus) }} ›</view></picker></view
        ><view class="field"
          ><text>脂肪肝既往诊断</text><picker :range="diseaseOptions" range-key="label" @change="selectOption('fattyLiverStatus', diseaseOptions, $event)"
            ><view class="picker-value">{{ optionLabel(diseaseOptions, form.fattyLiverStatus) }} ›</view></picker></view
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
  familyHistory: '',
  allergyHistory: '',
  currentMedications: '',
  smokingStatus: '',
  alcoholStatus: '',
  exerciseFrequency: '',
  sleepQuality: '',
  sleepHours: '',
  stressLevel: '',
  moodStatus: '',
  fearLevel: '',
  dietaryPreference: '',
  recentDietaryPattern: '',
  diabetesStatus: '',
  hypertensionStatus: '',
  dyslipidemiaStatus: '',
  fattyLiverStatus: '',
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
const moodOptions: SelectOption[] = [
  { label: '请选择', value: '' }, { label: '愉快平稳', value: 'GOOD' },
  { label: '一般', value: 'FAIR' }, { label: '低落或易烦躁', value: 'POOR' },
]
const fearOptions: SelectOption[] = [
  { label: '请选择', value: '' }, { label: '无或很少', value: 'LOW' },
  { label: '偶有', value: 'MEDIUM' }, { label: '经常或明显', value: 'HIGH' },
]
const diseaseOptions: SelectOption[] = [
  { label: '请选择', value: '' }, { label: '无', value: 'NO' },
  { label: '有，已确诊', value: 'YES' }, { label: '不清楚', value: 'UNKNOWN' },
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
  const sleepHours = Number(form.sleepHours)
  if (form.heightCm && (!Number.isFinite(height) || height < 50 || height > 250)) {
    uni.showToast({ title: '身高应在 50–250 cm', icon: 'none' })
    return
  }
  if (form.weightKg && (!Number.isFinite(weight) || weight < 10 || weight > 500)) {
    uni.showToast({ title: '体重应在 10–500 kg', icon: 'none' })
    return
  }
  if (form.sleepHours && (!Number.isFinite(sleepHours) || sleepHours < 0 || sleepHours > 24)) {
    uni.showToast({ title: '睡眠时长应在 0–24 小时', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await updateHealthProfile(patient.value.id, {
      ...form,
      heightCm: form.heightCm ? height : null,
      weightKg: form.weightKg ? weight : null,
      sleepHours: form.sleepHours ? sleepHours : null,
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
.questionnaire-note { margin-bottom:20rpx; padding:24rpx 26rpx; border-radius:20rpx; background:#eaf8f3; color:#42685d; font-size:23rpx; line-height:1.65; }
.questionnaire-note text { display:block; margin-bottom:6rpx; color:#0e755d; font-size:27rpx; font-weight:720; }
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

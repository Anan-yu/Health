<template>
  <view class="page"
    ><view class="title">健康档案与问卷</view
    ><view class="subtitle page-copy">请认真作答，作答内容将作为健康报告评估依据。</view
    ><PageState :loading="loading" :error="error" :empty="!patient"
      ><view class="questionnaire-note"
        ><text>健康问卷</text
        ><view
          >身高体重、既往情况、饮食、睡眠和情绪信息将与检验报告共同用于本次健康评估。</view
        ></view
      ><view class="completion-preview"
        ><view class="completion-header"
          ><text>档案完整度</text><text>{{ liveCompleteness }}%</text></view
        ><progress :percent="liveCompleteness" active-color="#176b57"
      /></view>
      <view class="card form-card"
        ><view class="identity-note">姓名和手机号用于医院医生按姓名或手机号查询您的健康报告。</view
        ><view class="field"
          ><text>姓名</text><input v-model="identity.name" placeholder="请输入真实姓名" /></view
        ><view class="field"
          ><text>性别</text
          ><picker :range="genderOptions" range-key="label" @change="selectGender"
            ><view class="picker-value"
              >{{ optionLabel(genderOptions, identity.gender) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>出生日期</text
          ><picker
            mode="date"
            :value="identity.birthDate"
            :start="minBirthDate"
            :end="today"
            @change="selectBirthDate"
            ><view class="picker-value"
              >{{ identity.birthDate || '请选择出生日期' }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>手机号</text
          ><input
            v-model="identity.phone"
            type="number"
            maxlength="11"
            :placeholder="patient?.phoneMasked || '请输入 11 位手机号'" /></view
        ><view class="field"
          ><text>身高（cm）</text
          ><input v-model="form.heightCm" type="digit" placeholder="例如 168" /></view
        ><view class="field"
          ><text>体重（kg）</text
          ><input v-model="form.weightKg" type="digit" placeholder="例如 60" /></view
        ><view class="field"
          ><text>腰围（cm）</text
          ><input v-model="form.waistCm" type="digit" placeholder="例如 78" /></view
        ><view class="field"
          ><text>近三个月体重变化（kg）</text
          ><input
            v-model="form.recentWeightChangeKg"
            placeholder="例如减轻 2 kg 填 -2，无变化填 0" /></view
        ><view class="field"
          ><text>血型</text><input v-model="form.bloodType" placeholder="例如 A 型" /></view
        ><view class="field"
          ><text>既往病史</text
          ><textarea v-model="form.medicalHistory" placeholder="如无，请填写无" /></view
        ><view class="field"
          ><text>家族病史</text
          ><textarea
            v-model="form.familyHistory"
            placeholder="如父母或兄弟姐妹的糖尿病、高血压、心脑血管病、肿瘤等；如无请填写无"
          /></view
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
          ><picker
            :range="moodOptions"
            range-key="label"
            @change="selectOption('moodStatus', moodOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(moodOptions, form.moodStatus) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>近期恐惧或焦虑感受</text
          ><picker
            :range="fearOptions"
            range-key="label"
            @change="selectOption('fearLevel', fearOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(fearOptions, form.fearLevel) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>饮食偏好</text
          ><input v-model="form.dietaryPreference" placeholder="例如：均衡、低盐、素食" /></view
        ><view class="field"
          ><text>近三周饮食结构</text
          ><textarea
            v-model="form.recentDietaryPattern"
            placeholder="例如：蔬果、全谷、奶豆、鱼肉、外卖、甜饮和夜宵的频率"
          /></view
        ><view class="field"
          ><text>糖尿病既往诊断</text
          ><picker
            :range="diseaseOptions"
            range-key="label"
            @change="selectOption('diabetesStatus', diseaseOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(diseaseOptions, form.diabetesStatus) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>高血压既往诊断</text
          ><picker
            :range="diseaseOptions"
            range-key="label"
            @change="selectOption('hypertensionStatus', diseaseOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(diseaseOptions, form.hypertensionStatus) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>血脂异常既往诊断</text
          ><picker
            :range="diseaseOptions"
            range-key="label"
            @change="selectOption('dyslipidemiaStatus', diseaseOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(diseaseOptions, form.dyslipidemiaStatus) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>脂肪肝既往诊断</text
          ><picker
            :range="diseaseOptions"
            range-key="label"
            @change="selectOption('fattyLiverStatus', diseaseOptions, $event)"
            ><view class="picker-value"
              >{{ optionLabel(diseaseOptions, form.fattyLiverStatus) }} ›</view
            ></picker
          ></view
        ><view class="field"
          ><text>生活方式概述</text
          ><textarea
            v-model="form.lifestyleSummary"
            placeholder="饮食、运动、作息等"
          /></view></view
      ><button class="primary-button" :loading="saving" :disabled="saving" @click="save">
        保存健康档案
      </button></PageState
    ></view
  >
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getHealthProfile,
  getMyProfile,
  updateHealthProfile,
  updatePatientIdentity,
} from '@/api/patient'
import type { HealthProfile, Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'

const patient = ref<Patient | null>(null),
  loading = ref(true),
  saving = ref(false),
  error = ref('')
const identity = reactive({ name: '', gender: '', birthDate: '', phone: '' })
const form = reactive<Record<string, string>>({
  heightCm: '',
  weightKg: '',
  waistCm: '',
  recentWeightChangeKg: '',
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
const questionnaireFields = [
  'heightCm',
  'weightKg',
  'waistCm',
  'recentWeightChangeKg',
  'bloodType',
  'lifestyleSummary',
  'medicalHistory',
  'familyHistory',
  'allergyHistory',
  'currentMedications',
  'smokingStatus',
  'alcoholStatus',
  'exerciseFrequency',
  'sleepQuality',
  'sleepHours',
  'stressLevel',
  'moodStatus',
  'fearLevel',
  'dietaryPreference',
  'recentDietaryPattern',
  'diabetesStatus',
  'hypertensionStatus',
  'dyslipidemiaStatus',
  'fattyLiverStatus',
] as const
const liveCompleteness = computed(() => {
  const answered = questionnaireFields.filter((field) => form[field].trim() !== '').length
  return Math.round((answered / questionnaireFields.length) * 100)
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
  { label: '请选择', value: '' },
  { label: '愉快平稳', value: 'GOOD' },
  { label: '一般', value: 'FAIR' },
  { label: '低落或易烦躁', value: 'POOR' },
]
const fearOptions: SelectOption[] = [
  { label: '请选择', value: '' },
  { label: '无或很少', value: 'LOW' },
  { label: '偶有', value: 'MEDIUM' },
  { label: '经常或明显', value: 'HIGH' },
]
const diseaseOptions: SelectOption[] = [
  { label: '请选择', value: '' },
  { label: '无', value: 'NO' },
  { label: '有，已确诊', value: 'YES' },
  { label: '不清楚', value: 'UNKNOWN' },
]
const genderOptions: SelectOption[] = [
  { label: '请选择', value: '' },
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
]
const optionLabel = (options: SelectOption[], value: string) =>
  options.find((item) => item.value === value)?.label || '请选择'
const selectOption = (field: string, options: SelectOption[], event: PickerChangeEvent) => {
  form[field] = options[Number(event.detail.value)]?.value || ''
}
const selectGender = (event: PickerChangeEvent) => {
  identity.gender = genderOptions[Number(event.detail.value)]?.value || ''
}
const formatDate = (value: Date) => {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
const today = formatDate(new Date())
const minBirthDate = `${new Date().getFullYear() - 120}-01-01`
const selectBirthDate = (event: PickerChangeEvent) => {
  identity.birthDate = String(event.detail.value || '')
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
      identity.name = patient.value.name || ''
      identity.gender = ['MALE', 'FEMALE'].includes(patient.value.gender)
        ? patient.value.gender
        : ''
      identity.birthDate = patient.value.birthDate || ''
      identity.phone = ''
      const profile = await getHealthProfile(patient.value.id)
      assign(profile)
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '档案加载失败'
  } finally {
    loading.value = false
  }
})
const save = async () => {
  if (!patient.value) return
  if (!identity.name.trim()) {
    uni.showToast({ title: '请填写姓名', icon: 'none' })
    return
  }
  if (!identity.gender) {
    uni.showToast({ title: '请选择性别', icon: 'none' })
    return
  }
  if (!identity.birthDate) {
    uni.showToast({ title: '请选择出生日期', icon: 'none' })
    return
  }
  if (identity.phone && !/^1[3-9]\d{9}$/.test(identity.phone)) {
    uni.showToast({ title: '请输入正确的 11 位手机号', icon: 'none' })
    return
  }
  const height = Number(form.heightCm)
  const weight = Number(form.weightKg)
  const waist = Number(form.waistCm)
  const weightChange = Number(form.recentWeightChangeKg)
  const sleepHours = Number(form.sleepHours)
  if (form.heightCm && (!Number.isFinite(height) || height < 50 || height > 250)) {
    uni.showToast({ title: '身高应在 50–250 cm', icon: 'none' })
    return
  }
  if (form.weightKg && (!Number.isFinite(weight) || weight < 10 || weight > 500)) {
    uni.showToast({ title: '体重应在 10–500 kg', icon: 'none' })
    return
  }
  if (form.waistCm && (!Number.isFinite(waist) || waist < 30 || waist > 250)) {
    uni.showToast({ title: '腰围应在 30–250 cm', icon: 'none' })
    return
  }
  if (
    form.recentWeightChangeKg &&
    (!Number.isFinite(weightChange) || weightChange < -100 || weightChange > 100)
  ) {
    uni.showToast({ title: '请正确填写近三个月体重变化', icon: 'none' })
    return
  }
  if (form.sleepHours && (!Number.isFinite(sleepHours) || sleepHours < 0 || sleepHours > 24)) {
    uni.showToast({ title: '睡眠时长应在 0–24 小时', icon: 'none' })
    return
  }
  saving.value = true
  try {
    patient.value = await updatePatientIdentity(patient.value.id, {
      name: identity.name.trim(),
      gender: identity.gender,
      birthDate: identity.birthDate,
      ...(identity.phone ? { phone: identity.phone } : {}),
    })
    const questionnairePayload = Object.fromEntries(
      Object.entries(form).map(([key, value]) => [key, value.trim() || null]),
    )
    await updateHealthProfile(patient.value.id, {
      ...questionnairePayload,
      heightCm: form.heightCm ? height : null,
      weightKg: form.weightKg ? weight : null,
      waistCm: form.waistCm ? waist : null,
      recentWeightChangeKg: form.recentWeightChangeKg ? weightChange : null,
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
</script>

<style scoped>
.page-copy {
  margin-bottom: 24rpx;
}
.questionnaire-note {
  margin-bottom: 20rpx;
  padding: 24rpx 26rpx;
  border-radius: 20rpx;
  background: #eaf8f3;
  color: #42685d;
  font-size: 23rpx;
  line-height: 1.65;
}
.questionnaire-note text {
  display: block;
  margin-bottom: 6rpx;
  color: #0e755d;
  font-size: 27rpx;
  font-weight: 720;
}
.completion-preview {
  margin-bottom: 20rpx;
  padding: 24rpx 26rpx;
  border: 1rpx solid #dbe9e4;
  border-radius: 20rpx;
  background: #fff;
}
.completion-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14rpx;
  color: #24483d;
  font-size: 25rpx;
  font-weight: 700;
}
.completion-header text:last-child {
  color: #0f7a62;
}
.identity-note {
  margin-bottom: 4rpx;
  color: #55746a;
  font-size: 22rpx;
  line-height: 1.6;
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
</style>

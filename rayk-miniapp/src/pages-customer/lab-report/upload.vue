<template>
  <view class="page upload-page elder-page">
    <view class="upload-hero">
      <view class="upload-hero-icon">传</view>
      <view>
        <view class="hero-title">上传检验报告</view>
        <view class="hero-copy">安全存储报告，为指标识别与健康评估提供依据</view>
      </view>
    </view>

    <view class="stepper">
      <view class="step active"><text>1</text><view>填写信息</view></view>
      <view class="step-line" :class="{ active: state !== 'IDLE' }" />
      <view class="step" :class="{ active: state !== 'IDLE' }"
        ><text>2</text><view>上传文件</view></view
      >
      <view class="step-line" :class="{ active: state === 'OCR_PROCESSING' }" />
      <view class="step" :class="{ active: state === 'OCR_PROCESSING' }"
        ><text>3</text><view>自动评估</view></view
      >
    </view>

    <view class="card form-card">
      <view class="form-heading"><text class="form-index">01</text><text>报告信息</text></view>
      <view class="field-label">报告所属人</view>
      <view class="input owner-field owner-selector" @click="editOwner">
        <view class="owner-info">
          <text class="owner-name">{{ patient?.name || '正在识别当前账号…' }}</text>
          <text v-if="patient" class="owner-meta">{{ ownerAge }} 岁 · {{ ownerGender }}</text>
        </view>
        <text class="owner-edit">修改健康档案 ›</text>
      </view>
      <view class="field-label">报告名称</view>
      <input v-model="reportName" class="input" placeholder="例如：生化检验报告" />
      <view class="field-label">报告日期</view>
      <picker mode="date" :value="reportDate" @change="changeDate">
        <view class="input date-input"
          ><text>{{ reportDate }}</text
          ><text>选择日期 ›</text></view
        >
      </picker>
    </view>

    <view class="card file-card">
      <view class="form-heading"><text class="form-index">02</text><text>报告文件</text></view>
      <view class="upload-zone" @click="choose">
        <view class="file-icon" :class="{ selected: files.length }">{{ files.length ? '✓' : '+' }}</view>
        <view class="file-title">{{ files.length ? `已选择 ${files.length} 个文件` : '选择检验报告文件' }}</view>
        <view class="file-copy">
          {{
            files.length
              ? '可继续分批拍摄或添加文件，数量不限'
              : '支持 PDF、JPG、PNG 格式，可上传多页，支持分批选择'
          }}
        </view>
      </view>
      <view v-if="files.length" class="selected-files">
        <view v-for="(file, index) in files" :key="file.path" class="selected-file">
          <view class="selected-file-info">
            <text class="selected-file-name">{{ file.name }}</text>
            <text class="selected-file-size">{{ formatSize(file.size) }}</text>
          </view>
          <text class="remove-file" @click.stop="removeFile(index)">删除</text>
        </view>
        <button class="add-more-button" @click.stop="choose">继续添加照片或文件</button>
      </view>
      <view v-if="progress > 0" class="progress-box">
        <view class="row"
          ><text>{{ state === 'OCR_PROCESSING' ? '上传完成，已进入识别队列' : '正在上传' }}</text
          ><text>{{ progress }}%</text></view
        >
        <progress :percent="progress" active-color="#0f7a62" background-color="#e3eeea" />
      </view>
    </view>

    <view class="privacy-tip"
      ><view class="shield">安</view
      ><view class="privacy-content"><text>安全上传</text><text>文件将加密传输</text></view></view
    >
    <view v-if="error" class="error">{{ error }} <text @click="submit">重新尝试</text></view>
    <button class="primary submit-button" :loading="loading" @click="submit">安全上传报告</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  appendLabReportFile,
  completeLabReportUpload,
  uploadLabReport,
} from '@/api/lab-report'
import { getMyProfile } from '@/api/patient'
import type { Patient } from '@/types/api'

const today = new Date()
type SelectedFile = { name: string; path: string; size: number }

const patient = ref<Patient | null>(null),
  patientId = ref(''),
  reportName = ref('生化检验报告'),
  reportDate = ref(
    `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`,
  ),
  files = ref<SelectedFile[]>([]),
  progress = ref(0),
  state = ref('IDLE'),
  loading = ref(false),
  error = ref('')

const ownerAge = computed(() => {
  const birthDate = patient.value?.birthDate
  if (!birthDate) return '年龄未填写'
  const birthday = new Date(birthDate)
  if (Number.isNaN(birthday.getTime())) return '年龄未填写'
  const now = new Date()
  let age = now.getFullYear() - birthday.getFullYear()
  const birthdayNotReached =
    now.getMonth() < birthday.getMonth() ||
    (now.getMonth() === birthday.getMonth() && now.getDate() < birthday.getDate())
  if (birthdayNotReached) age -= 1
  return age >= 0 ? age : '年龄未填写'
})
const ownerGender = computed(() => {
  const gender = patient.value?.gender
  return gender === 'MALE' ? '男' : gender === 'FEMALE' ? '女' : '性别未填写'
})

onShow(async () => {
  try {
    patient.value = await getMyProfile()
    patientId.value = patient.value?.id || ''
    if (!patientId.value) {
      error.value = '当前账号尚未关联客户档案'
      return
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '客户档案加载失败'
  }
})

function choose() {
  uni.showActionSheet({
    itemList: ['拍照上传', '从手机文件选择'],
    success: ({ tapIndex }) => {
      if (tapIndex === 0) {
        chooseFromCamera()
        return
      }
      chooseFromFiles()
    },
  })
}

const acceptFile = (
  file: { name?: string; path: string; size?: number },
  fallbackName = 'report.pdf',
) => {
  const pathName = file.path.split('/').pop() || ''
  const name = file.name || (pathName.includes('.') ? pathName : fallbackName)
  const extension = name.split('.').pop()?.toLowerCase()
  if (!extension || !['pdf', 'jpg', 'jpeg', 'png'].includes(extension)) {
    error.value = '仅支持 PDF、JPG、PNG 文件'
    return
  }
  if (files.value.some((item) => item.path === file.path)) return
  files.value.push({ name, path: file.path, size: file.size || 0 })
  progress.value = 0
  state.value = 'SELECTED'
  error.value = ''
}

function chooseFromCamera() {
  uni.chooseImage({
    // The platform picker may return at most nine at once; users can keep
    // tapping “继续添加” without an application-level limit.
    count: 9,
    sourceType: ['camera'],
    success: (result) => {
      const selectedFiles = (Array.isArray(result.tempFiles) ? result.tempFiles : []) as Array<{
        path?: string
        size?: number
      }>
      const paths = Array.isArray(result.tempFilePaths)
        ? result.tempFilePaths
        : typeof result.tempFilePaths === 'string'
          ? [result.tempFilePaths]
          : []
      paths.forEach((path, index) => {
        if (typeof path !== 'string') return
        const selected = selectedFiles[index]
        acceptFile(
          {
            name: selected?.path?.split('/').pop(),
            path,
            size: selected?.size,
          },
          'camera-report.jpg',
        )
      })
    },
  })
}

function chooseFromFiles() {
  // #ifdef MP-WEIXIN
  uni.chooseMessageFile({
    // WeChat allows up to 100 files per picker invocation. There is no
    // application-level cap: users can keep adding another batch later.
    count: 100,
    type: 'all',
    extension: ['pdf', 'jpg', 'jpeg', 'png'],
    success: (result) => {
      result.tempFiles.forEach((file) =>
        acceptFile({ name: file.name, path: file.path, size: file.size }),
      )
    },
  })
  // #endif
  // #ifdef H5
  uni.chooseFile({
    // Keep the web picker aligned with its 100-file default; the selected
    // list itself remains unbounded and can be filled in multiple batches.
    count: 100,
    extension: ['.pdf', '.jpg', '.jpeg', '.png'],
    success: (result) => {
      const selectedFiles = (Array.isArray(result.tempFiles) ? result.tempFiles : []) as Array<{
        name?: string
        path?: string
        size?: number
      }>
      const paths = Array.isArray(result.tempFilePaths)
        ? result.tempFilePaths
        : typeof result.tempFilePaths === 'string'
          ? [result.tempFilePaths]
          : []
      paths.forEach((path, index) => {
        if (typeof path !== 'string') return
        const selected = selectedFiles[index]
        acceptFile({ name: selected?.name, path, size: selected?.size })
      })
    },
  })
  // #endif
}

async function submit() {
  if (!patientId.value) {
    error.value = '当前账号尚未关联客户档案，请联系机构管理员'
    return
  }
  if (!reportName.value.trim() || !files.value.length) {
    error.value = '请填写报告信息并至少选择一个文件'
    return
  }
  loading.value = true
  error.value = ''
  state.value = 'UPLOADING'
  try {
    const total = files.value.length
    const first = await uploadLabReport(
      files.value[0].path,
      patientId.value,
      reportName.value.trim(),
      reportDate.value,
      (value) => setOverallProgress(0, value, total),
      total === 1,
    )
    const reportId = first.report.id
    for (let index = 1; index < total; index += 1) {
      await appendLabReportFile(reportId, files.value[index].path, (value) =>
        setOverallProgress(index, value, total),
      )
    }
    if (total > 1) {
      await completeLabReportUpload(reportId)
    }
    state.value = 'OCR_PROCESSING'
    uni.showToast({ title: '报告已提交，正在整理', icon: 'success' })
    setTimeout(
      () =>
        uni.navigateTo({
          url: `/pages-customer/lab-report/detail?id=${reportId}&autoReturn=1`,
        }),
      500,
    )
  } catch (e) {
    state.value = 'FAILED'
    error.value = e instanceof Error ? e.message : '上传失败'
  } finally {
    loading.value = false
  }
}

const editOwner = () => uni.navigateTo({ url: '/pages-customer/profile/edit' })

function removeFile(index: number) {
  files.value.splice(index, 1)
  if (!files.value.length) state.value = 'IDLE'
}

function setOverallProgress(index: number, value: number, total: number) {
  progress.value = Math.min(100, Math.round(((index + value / 100) / total) * 100))
}

function changeDate(event: { detail: { value: string } }) {
  reportDate.value = event.detail.value
}

function formatSize(size: number) {
  if (!size) return ''
  return size >= 1024 * 1024
    ? `${(size / 1024 / 1024).toFixed(1)} MB`
    : `${Math.ceil(size / 1024)} KB`
}
</script>

<style scoped>
.upload-page {
  padding-top: 24rpx;
}
.upload-hero {
  display: flex;
  align-items: center;
  padding: 30rpx 32rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #e4f7f0, #f4fbf8);
  border: 1rpx solid #d6ebe3;
}
.upload-hero-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 82rpx;
  height: 82rpx;
  margin-right: 24rpx;
  border-radius: 27rpx;
  background: linear-gradient(135deg, #138269, #0b6d58);
  color: #fff;
  font-size: 26rpx;
  font-weight: 750;
  box-shadow: 0 10rpx 22rpx rgba(15, 122, 98, 0.18);
}
.hero-title {
  font-size: 35rpx;
  font-weight: 740;
}
.hero-copy {
  margin-top: 8rpx;
  color: #6e827b;
  font-size: 27rpx;
  line-height: 1.5;
}
.stepper {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  margin: 34rpx 18rpx 30rpx;
}
.step {
  display: flex;
  align-items: center;
  width: 110rpx;
  flex-direction: column;
  color: #a1ada9;
  font-size: 24rpx;
  white-space: nowrap;
}
.step text {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44rpx;
  height: 44rpx;
  margin-bottom: 9rpx;
  border-radius: 50%;
  background: #e1e8e6;
  color: #87938f;
  font-size: 21rpx;
  font-weight: 700;
}
.step.active {
  color: #0f7a62;
  font-weight: 650;
}
.step.active text {
  background: #0f7a62;
  color: #fff;
  box-shadow: 0 6rpx 14rpx rgba(15, 122, 98, 0.18);
}
.step-line {
  width: 74rpx;
  height: 4rpx;
  margin: 20rpx -15rpx 0;
  background: #dde6e3;
}
.step-line.active {
  background: #58b79d;
}
.form-card,
.file-card {
  padding: 32rpx;
}
.form-heading {
  display: flex;
  align-items: center;
  margin-bottom: 26rpx;
  font-size: 34rpx;
  font-weight: 700;
}
.form-index {
  margin-right: 14rpx;
  color: #0f7a62;
  font-size: 22rpx;
  font-weight: 760;
}
.field-label {
  margin: 22rpx 2rpx 2rpx;
  color: #587068;
  font-size: 28rpx;
  font-weight: 620;
}
.date-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #33463f;
}
.owner-field {
  color: #33463f;
  background: #f4f8f6;
}
.owner-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.owner-info {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}
.owner-name {
  font-size: 31rpx;
}
.owner-meta {
  margin-top: 6rpx;
  color: #81918b;
  font-size: 26rpx;
}
.owner-edit {
  flex: 0 0 auto;
  color: #0f7a62;
  font-size: 27rpx;
  font-weight: 650;
}
.date-input text:last-child {
  color: #91a09b;
  font-size: 22rpx;
}
.upload-zone {
  display: flex;
  align-items: center;
  flex-direction: column;
  min-height: 250rpx;
  padding: 52rpx 24rpx;
  text-align: center;
  border: 2rpx dashed #a8cfc3;
  border-radius: 26rpx;
  background: #f7fbf9;
  color: #527067;
}
.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 82rpx;
  height: 82rpx;
  border-radius: 27rpx;
  background: #dff3ec;
  color: #0f7a62;
  font-size: 42rpx;
  font-weight: 400;
}
.file-icon.selected {
  background: #0f7a62;
  color: #fff;
  font-size: 30rpx;
}
.file-title {
  max-width: 560rpx;
  overflow: hidden;
  margin-top: 20rpx;
  color: #294a41;
  font-size: 31rpx;
  font-weight: 660;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-copy {
  margin-top: 9rpx;
  color: #879690;
  font-size: 27rpx;
}
.selected-files {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 20rpx;
  background: #f2f8f6;
}
.selected-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 14rpx 4rpx;
  border-bottom: 1rpx solid #dcebe5;
}
.selected-file-info {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}
.selected-file-name {
  overflow: hidden;
  color: #31544a;
  font-size: 25rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.selected-file-size {
  margin-top: 4rpx;
  color: #8a9c95;
  font-size: 21rpx;
}
.remove-file {
  flex: 0 0 auto;
  padding: 8rpx 10rpx;
  color: #c2574d;
  font-size: 22rpx;
}
.add-more-button {
  height: 68rpx;
  margin: 16rpx 0 0;
  padding: 0;
  border: 0;
  border-radius: 16rpx;
  background: #dff3ec;
  color: #0f7a62;
  font-size: 25rpx;
  line-height: 68rpx;
}
.add-more-button::after {
  border: 0;
}
.file-chip {
  margin-top: 18rpx;
  padding: 7rpx 16rpx;
  border-radius: 999rpx;
  background: #e4f2ed;
  color: #56776c;
  font-size: 20rpx;
}
.progress-box {
  margin-top: 24rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #f2f8f6;
  color: #527168;
  font-size: 22rpx;
}
.progress-box progress {
  margin-top: 14rpx;
}
.privacy-tip {
  display: flex;
  align-items: center;
  margin: 6rpx 4rpx 26rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: #fff8ea;
}
.shield {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58rpx;
  height: 58rpx;
  margin-right: 18rpx;
  border-radius: 19rpx;
  background: #ffe9b9;
  color: #8f6208;
  font-size: 21rpx;
  font-weight: 740;
}
.privacy-content {
  display: flex;
  flex-direction: column;
}
.privacy-tip text:first-child {
  color: #72531a;
  font-size: 23rpx;
  font-weight: 650;
}
.privacy-tip text:last-child {
  margin-top: 4rpx;
  color: #9b824f;
  font-size: 20rpx;
}
.submit-button {
  height: 106rpx;
  line-height: 106rpx;
}
.error {
  padding: 20rpx;
  color: #b42318;
  text-align: center;
  font-size: 23rpx;
}
.error text {
  margin-left: 10rpx;
  text-decoration: underline;
}
</style>

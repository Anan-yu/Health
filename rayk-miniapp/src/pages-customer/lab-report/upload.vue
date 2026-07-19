<template>
  <view class="page upload-page">
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
      <view class="step-line" :class="{ active: state === 'WAITING_CONFIRMATION' }" />
      <view class="step" :class="{ active: state === 'WAITING_CONFIRMATION' }"
        ><text>3</text><view>确认指标</view></view
      >
    </view>

    <view class="card form-card">
      <view class="form-heading"><text class="form-index">01</text><text>报告信息</text></view>
      <view class="field-label">客户档案 ID</view>
      <input v-model="patientId" class="input" placeholder="请输入客户档案ID" />
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
        <view class="file-icon" :class="{ selected: fileName }">{{ fileName ? '✓' : '+' }}</view>
        <view class="file-title">{{ fileName || '选择检验报告文件' }}</view>
        <view class="file-copy">
          {{ fileName ? '文件已准备好，可重新选择' : '支持 PDF、JPG、PNG 格式，最大 20MB' }}
        </view>
        <view v-if="fileSize" class="file-chip">{{ formatSize(fileSize) }}</view>
      </view>
      <view v-if="progress > 0" class="progress-box">
        <view class="row"
          ><text>{{ state === 'WAITING_CONFIRMATION' ? '上传完成' : '正在上传' }}</text
          ><text>{{ progress }}%</text></view
        >
        <progress :percent="progress" active-color="#0f7a62" background-color="#e3eeea" />
      </view>
    </view>

    <view class="privacy-tip"
      ><view class="shield">安</view
      ><view><text>隐私安全保护</text><text>文件将加密传输并保存至私有存储空间</text></view></view
    >
    <view v-if="error" class="error">{{ error }} <text @click="submit">重新尝试</text></view>
    <button class="primary submit-button" :loading="loading" @click="submit">安全上传报告</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { uploadLabReport } from '@/api/lab-report'
import { getMyProfile } from '@/api/patient'

const today = new Date()
const patientId = ref(''),
  reportName = ref('生化检验报告'),
  reportDate = ref(
    `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`,
  ),
  fileName = ref(''),
  filePath = ref(''),
  fileSize = ref(0),
  progress = ref(0),
  state = ref('IDLE'),
  loading = ref(false),
  error = ref('')

onLoad(async (query) => {
  patientId.value = String(query?.patientId || '')
  if (!patientId.value) {
    try {
      const profile = await getMyProfile()
      patientId.value = profile?.id || ''
    } catch {
      // B端用户可手工填写客户档案 ID。
    }
  }
})

function choose() {
  const acceptFile = (file: { name?: string; path: string; size?: number }) => {
    const name = file.name || file.path.split('/').pop() || 'report.pdf'
    const extension = name.split('.').pop()?.toLowerCase()
    if (!extension || !['pdf', 'jpg', 'jpeg', 'png'].includes(extension)) {
      error.value = '仅支持 PDF、JPG、PNG 文件'
      return
    }
    if ((file.size || 0) > 20 * 1024 * 1024) {
      error.value = '文件不能超过 20MB'
      return
    }
    fileName.value = name
    filePath.value = file.path
    fileSize.value = file.size || 0
    progress.value = 0
    state.value = 'SELECTED'
    error.value = ''
  }
  // #ifdef MP-WEIXIN
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['pdf', 'jpg', 'jpeg', 'png'],
    success: (result) => acceptFile(result.tempFiles[0]),
  })
  // #endif
  // #ifdef H5
  uni.chooseFile({
    count: 1,
    extension: ['.pdf', '.jpg', '.jpeg', '.png'],
    success: (result) => {
      const selected = Array.isArray(result.tempFiles) ? result.tempFiles[0] : result.tempFiles
      const path = Array.isArray(result.tempFilePaths)
        ? result.tempFilePaths[0]
        : result.tempFilePaths
      acceptFile({
        name: 'name' in selected ? selected.name : undefined,
        path,
        size: selected.size,
      })
    },
  })
  // #endif
}

async function submit() {
  if (!patientId.value || !reportName.value || !filePath.value) {
    error.value = '请填写报告信息并选择文件'
    return
  }
  loading.value = true
  error.value = ''
  state.value = 'UPLOADING'
  try {
    const result = await uploadLabReport(
      filePath.value,
      patientId.value,
      reportName.value,
      reportDate.value,
      (value) => (progress.value = value),
    )
    state.value = 'WAITING_CONFIRMATION'
    uni.showToast({ title: '上传成功' })
    setTimeout(
      () => uni.navigateTo({ url: `/pages-customer/lab-report/confirm?id=${result.report.id}` }),
      500,
    )
  } catch (e) {
    state.value = 'FAILED'
    error.value = e instanceof Error ? e.message : '上传失败'
  } finally {
    loading.value = false
  }
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
  font-size: 22rpx;
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
  font-size: 20rpx;
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
  font-size: 30rpx;
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
  font-size: 23rpx;
  font-weight: 620;
}
.date-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #33463f;
}
.date-input text:last-child {
  color: #91a09b;
  font-size: 22rpx;
}
.upload-zone {
  display: flex;
  align-items: center;
  flex-direction: column;
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
  font-size: 27rpx;
  font-weight: 660;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-copy {
  margin-top: 9rpx;
  color: #879690;
  font-size: 22rpx;
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
.privacy-tip > view:last-child {
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
  height: 92rpx;
  line-height: 92rpx;
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

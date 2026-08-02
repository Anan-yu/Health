<template>
  <view class="page scan-page elder-page">
    <view class="hero-card">
      <view>
        <view class="hero-title">面部健康检测</view>
        <view class="hero-copy">自然注视镜头约 20 秒，了解当前身体状态</view>
      </view>
      <view class="hero-badge">{{ stageLabel }}</view>
    </view>

    <view v-if="scanning" id="health-scan-camera" class="camera-card">
      <!-- #ifdef MP-WEIXIN -->
      <camera
        class="camera"
        device-position="front"
        resolution="low"
        frame-size="small"
        :flash="'off'"
        @initdone="onCameraReady"
        @error="onCameraError"
      />
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <view class="h5-camera-placeholder">
        <view class="face-symbol">人</view>
        <view>请在微信小程序中完成面部检测</view>
      </view>
      <!-- #endif -->
      <view class="face-guide">
        <view class="corner corner-tl" />
        <view class="corner corner-tr" />
        <view class="corner corner-bl" />
        <view class="corner corner-br" />
      </view>
      <view class="camera-tip">{{ cameraTip }}</view>
      <view v-if="progress > 0" class="progress-track">
        <view class="progress-value" :style="{ width: `${progress * 100}%` }" />
      </view>
      <view v-if="remainingTime > 0" class="remaining">还需约 {{ remainingTime }} 秒</view>
    </view>

    <view v-else class="intro-card">
      <view class="intro-title">一次检测，多项体征</view>
      <view class="intro-copy">检测结果将与健康档案、问卷和体检报告共同完善健康画像。</view>
      <view class="indicator-grid">
        <view v-for="indicator in indicators" :key="indicator.name" class="indicator-item">
          <view class="indicator-icon">{{ indicator.icon }}</view>
          <view class="indicator-name">{{ indicator.name }}</view>
        </view>
      </view>
    </view>

    <HealthScanResultDashboard v-if="hasSucceededResult" :records="scanRecords" />

    <view v-else-if="latestResult" class="result-card">
      <view class="result-head">
        <view>
          <view class="result-title">最近一次检测</view>
          <view class="result-time">{{ formatTime(latestResult.createdAt) }}</view>
        </view>
        <view class="status-pill" :class="`status-${latestResult.status.toLowerCase()}`">
          {{ latestResult.statusLabel }}
        </view>
      </view>
      <view v-if="latestResult.failureMessage" class="failure-copy">
        {{ latestResult.failureMessage }}
      </view>
      <view v-else class="processing-copy">健康数据正在分析，请稍后刷新查看。</view>
    </view>

    <view class="ready-card">
      <view class="ready-title">检测前请确认</view>
      <view v-for="item in readyItems" :key="item" class="ready-item">
        <view class="ready-check">✓</view>
        <view>{{ item }}</view>
      </view>
    </view>

    <button
      v-if="!scanning"
      class="start-button"
      :loading="starting"
      :disabled="starting || uploading"
      @tap="startScan"
    >
      {{ startButtonText }}
    </button>
    <button v-else class="cancel-button" @tap="cancelScan">退出本次检测</button>
    <view v-if="uploading" class="upload-copy">视频正在安全上传 {{ uploadProgress }}%</view>
    <view class="privacy-note">检测结果可为医护人员提供诊断参考，但不作为临床诊断依据。</view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import HealthScanResultDashboard from '@/components/health-scan/HealthScanResultDashboard.vue'
import {
  createHealthScanSession,
  getMyHealthScans,
  uploadHealthScanVideo,
} from '@/api/health-scan'
import type { HealthScanResult, HealthScanSession } from '@/types/api'

declare const requirePlugin: (name: string) => any
declare const wx: any

const scanning = ref(false)
const starting = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const progress = ref(0)
const remainingTime = ref(0)
const cameraTip = ref('请将面部完整放入取景框')
const latestResult = ref<HealthScanResult>()
const scanRecords = ref<HealthScanResult[]>([])
const activeSession = ref<HealthScanSession>()

let sampler: any
let cameraListener: any

const indicators = [
  { icon: '心', name: '心率' },
  { icon: '压', name: '血压' },
  { icon: '氧', name: '血氧' },
  { icon: '呼', name: '呼吸' },
  { icon: '变', name: '心率变异性' },
  { icon: '压', name: '压力参考' },
]

const hasSucceededResult = computed(() =>
  scanRecords.value.some((record) => record.status === 'SUCCEEDED'),
)

const readyItems = [
  '保持环境光线均匀，避免强光和逆光',
  '取下口罩、帽子和有色眼镜，保持面部无遮挡',
  '采集时自然注视镜头，请勿说话或大幅移动',
]

const stageLabel = computed(() => {
  if (uploading.value) return '上传中'
  if (scanning.value) return '检测中'
  return '约 20 秒'
})

const startButtonText = computed(() => {
  if (uploading.value) return `正在上传 ${uploadProgress.value}%`
  if (starting.value) return '正在连接检测服务'
  return '开始健康检测'
})

onShow(() => loadHistory())
onBeforeUnmount(() => releaseSampler())

async function loadHistory() {
  try {
    const records = await getMyHealthScans()
    scanRecords.value = records
    latestResult.value = records[0]
  } catch {
    // 页面仍可展示接入说明，网络错误由全局请求层处理。
  }
}

async function startScan() {
  // #ifndef MP-WEIXIN
  uni.showModal({
    title: '请在微信小程序中使用',
    content: '面部健康检测需要调用微信摄像头和健康拍插件，网页端仅展示检测结果。',
    showCancel: false,
  })
  return
  // #endif

  // #ifdef MP-WEIXIN
  // eslint-disable-next-line no-unreachable
  starting.value = true
  try {
    const session = await createHealthScanSession()
    activeSession.value = session
    const plugin = requirePlugin('vitals-plugin')
    await plugin.init({
      appId: session.appId,
      timestamp: session.timestamp,
      outUserId: session.outUserId,
      sign: session.sign,
      ...(session.serverUrl ? { serverUrl: session.serverUrl } : {}),
    })
    const detectable = await plugin.checkDetectable()
    const detectableCode =
      typeof detectable === 'number'
        ? detectable
        : typeof detectable?.code === 'number'
          ? detectable.code
          : undefined
    if (detectable === false || (detectableCode !== undefined && detectableCode !== 0)) {
      throw new Error('当前设备暂不支持面部健康检测')
    }
    scanning.value = true
    cameraTip.value = '正在启动摄像头'
    await scrollToCamera()
  } catch (error) {
    const message =
      typeof (error as { message?: unknown })?.message === 'string'
        ? String((error as { message: string }).message)
        : '检测服务连接失败，请稍后重试'
    uni.showModal({ title: '暂时无法开始检测', content: message, showCancel: false })
  } finally {
    starting.value = false
  }
  // #endif
}

async function scrollToCamera() {
  await nextTick()
  setTimeout(() => {
    uni.pageScrollTo({
      selector: '#health-scan-camera',
      offsetTop: -16,
      duration: 320,
    })
  }, 80)
}

function onCameraReady() {
  // #ifdef MP-WEIXIN
  try {
    const plugin = requirePlugin('vitals-plugin')
    const { FaceState, Frame, SampleState, createSampler } = plugin
    const cameraContext = wx.createCameraContext()
    sampler = createSampler({ cameraContext })
    bindSamplerEvents(sampler, FaceState, SampleState)
    sampler.create()
    cameraListener = cameraContext.onCameraFrame((frame: any) => {
      sampler?.postFrame(new Frame(frame.data, frame.width, frame.height))
    })
    cameraListener.start({
      success: () => sampler?.start(),
      fail: () => handleScanError('摄像头画面读取失败，请重新进入后再试'),
    })
  } catch {
    handleScanError('健康检测插件初始化失败')
  }
  // #endif
}

function bindSamplerEvents(instance: any, FaceState: any, SampleState: any) {
  instance.on('stateChange', ({ newState }: any) => {
    if (newState !== SampleState.RECORDING) progress.value = 0
  })
  instance.on('faceStateChange', ({ faceState }: any) => {
    cameraTip.value = faceStateTip(faceState, FaceState)
  })
  instance.on('progressChange', (event: any) => {
    progress.value = event.progress || 0
    remainingTime.value = Math.ceil((event.remainingTime || 0) / 1000)
  })
  instance.on('finish', async ({ tempVideoPath }: any) => {
    progress.value = 1
    cameraTip.value = '采集完成，正在安全上传'
    releaseSampler()
    await uploadVideo(tempVideoPath)
  })
  instance.on('error', () => handleScanError('采集未完成，请调整光线和位置后重试'))
}

function faceStateTip(faceState: any, FaceState: any) {
  const tips: Array<[any, string]> = [
    [FaceState.NO_FACE, '请面向镜头'],
    [FaceState.OUT_OF_FRAME, '请将面部完整放入取景框'],
    [FaceState.FAR, '请靠近一些'],
    [FaceState.DARK, '环境光线太暗'],
    [FaceState.LIGHT, '环境光线太亮'],
    [FaceState.OCCLUDE, '请勿遮挡面部'],
    [FaceState.UNSTEADY, '请保持身体和手机稳定'],
    [FaceState.OK, '位置合适，请保持自然注视'],
  ]
  return tips.find(([state]) => state === faceState)?.[1] || '请保持面部居中'
}

async function uploadVideo(filePath: string) {
  if (!activeSession.value) return
  uploading.value = true
  uploadProgress.value = 0
  try {
    const result = await uploadHealthScanVideo(
      activeSession.value.taskId,
      filePath,
      (value) => (uploadProgress.value = value),
    )
    latestResult.value = result
    scanRecords.value = [result, ...scanRecords.value.filter((record) => record.id !== result.id)]
    uni.showToast({
      title: result.status === 'SUCCEEDED' ? '检测完成' : '已提交分析',
      icon: 'success',
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : '上传失败，请稍后重试'
    uni.showModal({ title: '检测未完成', content: message, showCancel: false })
  } finally {
    uploading.value = false
    scanning.value = false
    activeSession.value = undefined
    await loadHistory()
  }
}

function onCameraError() {
  handleScanError('无法使用前置摄像头，请检查微信摄像头权限')
}

function handleScanError(message: string) {
  releaseSampler()
  scanning.value = false
  uni.showModal({ title: '检测未完成', content: message, showCancel: false })
}

function cancelScan() {
  releaseSampler()
  scanning.value = false
  activeSession.value = undefined
}

function releaseSampler() {
  try {
    cameraListener?.stop?.()
    sampler?.stop?.()
    sampler?.release?.()
  } catch {
    // 插件已释放时无需重复处理。
  }
  cameraListener = undefined
  sampler = undefined
}

function formatTime(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.scan-page {
  padding: 24rpx 24rpx calc(50rpx + env(safe-area-inset-bottom));
}
.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 36rpx 32rpx;
  border-radius: 34rpx;
  background: linear-gradient(135deg, #07594a, #0c8b70);
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(7, 88, 72, 0.18);
}
.hero-title {
  font-size: 38rpx;
  font-weight: 760;
}
.hero-copy {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 25rpx;
}
.hero-badge {
  flex: 0 0 auto;
  margin-left: 18rpx;
  padding: 12rpx 18rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.28);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.12);
  font-size: 22rpx;
}
.camera-card {
  position: relative;
  overflow: hidden;
  height: 720rpx;
  margin-top: 24rpx;
  border-radius: 34rpx;
  background: #092f29;
}
.camera {
  width: 100%;
  height: 100%;
}
.h5-camera-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  width: 100%;
  height: 100%;
  color: #d7eee7;
  font-size: 28rpx;
}
.face-symbol {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 180rpx;
  height: 230rpx;
  margin-bottom: 30rpx;
  border: 4rpx solid #59ddb7;
  border-radius: 48%;
  font-size: 70rpx;
}
.face-guide {
  position: absolute;
  top: 120rpx;
  left: 50%;
  width: 430rpx;
  height: 470rpx;
  transform: translateX(-50%);
}
.corner {
  position: absolute;
  width: 70rpx;
  height: 70rpx;
  border-color: #62e4bd;
  border-style: solid;
}
.corner-tl {
  top: 0;
  left: 0;
  border-width: 6rpx 0 0 6rpx;
  border-radius: 24rpx 0 0;
}
.corner-tr {
  top: 0;
  right: 0;
  border-width: 6rpx 6rpx 0 0;
  border-radius: 0 24rpx 0 0;
}
.corner-bl {
  bottom: 0;
  left: 0;
  border-width: 0 0 6rpx 6rpx;
  border-radius: 0 0 0 24rpx;
}
.corner-br {
  right: 0;
  bottom: 0;
  border-width: 0 6rpx 6rpx 0;
  border-radius: 0 0 24rpx;
}
.camera-tip {
  position: absolute;
  right: 28rpx;
  bottom: 90rpx;
  left: 28rpx;
  color: #fff;
  font-size: 28rpx;
  font-weight: 650;
  text-align: center;
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.5);
}
.progress-track {
  position: absolute;
  right: 42rpx;
  bottom: 55rpx;
  left: 42rpx;
  height: 12rpx;
  overflow: hidden;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.24);
}
.progress-value {
  height: 100%;
  border-radius: 999rpx;
  background: #5ee7bd;
  transition: width 0.2s ease;
}
.remaining {
  position: absolute;
  top: 30rpx;
  right: 30rpx;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 22rpx;
}
.intro-card,
.result-card,
.ready-card {
  margin-top: 24rpx;
  padding: 32rpx;
  border: 1rpx solid #dceae5;
  border-radius: 32rpx;
  background: #fff;
  box-shadow: 0 12rpx 30rpx rgba(29, 81, 67, 0.06);
}
.intro-title,
.ready-title,
.result-title {
  color: #173a32;
  font-size: 32rpx;
  font-weight: 740;
}
.intro-copy {
  margin-top: 8rpx;
  color: #72847e;
  font-size: 24rpx;
  line-height: 1.55;
}
.indicator-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 26rpx;
}
.indicator-item {
  display: flex;
  align-items: center;
  min-height: 82rpx;
  padding: 0 18rpx;
  border-radius: 22rpx;
  background: #f0f8f5;
}
.indicator-item:last-child:nth-child(odd) {
  grid-column: 1 / -1;
}
.indicator-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50rpx;
  height: 50rpx;
  margin-right: 14rpx;
  border-radius: 16rpx;
  background: #d8f1e8;
  color: #08715b;
  font-size: 22rpx;
  font-weight: 750;
}
.indicator-name {
  color: #27463e;
  font-size: 26rpx;
  font-weight: 680;
}
.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.result-time {
  margin-top: 6rpx;
  color: #87958f;
  font-size: 22rpx;
}
.status-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #eef3f1;
  color: #5d6f69;
  font-size: 22rpx;
}
.status-succeeded {
  background: #e2f5ee;
  color: #08715b;
}
.status-failed {
  background: #ffebeb;
  color: #b33d3d;
}
.vitals-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}
.result-summary {
  margin: 24rpx 0 18rpx;
  color: #657a73;
  font-size: 25rpx;
  line-height: 1.5;
}
.vital-item {
  padding: 24rpx 18rpx;
  border-radius: 22rpx;
  background: #f4f9f7;
  text-align: center;
}
.vital-value {
  color: #08715b;
  font-size: 38rpx;
  font-weight: 760;
}
.vital-label {
  margin-top: 8rpx;
  color: #788983;
  font-size: 21rpx;
}
.failure-copy,
.processing-copy {
  margin-top: 24rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #f3f7f5;
  color: #667872;
  font-size: 24rpx;
  line-height: 1.5;
}
.ready-item {
  display: flex;
  align-items: center;
  min-height: 82rpx;
  border-bottom: 1rpx solid #e7efec;
  color: #3d554e;
  font-size: 25rpx;
}
.ready-item:last-child {
  border-bottom: 0;
}
.ready-check {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 42rpx;
  height: 42rpx;
  margin-right: 16rpx;
  border-radius: 50%;
  background: #e2f4ed;
  color: #08715b;
  font-size: 22rpx;
  font-weight: 760;
}
.start-button,
.cancel-button {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 98rpx;
  margin-top: 28rpx;
  border: 0;
  border-radius: 28rpx;
  font-size: 30rpx;
  font-weight: 730;
}
.start-button {
  background: linear-gradient(135deg, #11886c, #086550);
  color: #fff;
  box-shadow: 0 15rpx 32rpx rgba(11, 111, 87, 0.2);
}
.cancel-button {
  border: 1rpx solid #d7e5e0;
  background: #fff;
  color: #49615a;
}
.start-button::after,
.cancel-button::after {
  display: none;
}
.upload-copy,
.privacy-note {
  margin-top: 16rpx;
  color: #778983;
  font-size: 22rpx;
  line-height: 1.5;
  text-align: center;
}
</style>

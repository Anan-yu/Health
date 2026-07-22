<template>
  <view class="page">
    <view class="title">健康报告详情</view>
    <PageState :loading="loading" :error="error" :empty="!report">
      <view class="card">
        <view class="row"
          ><view class="section-title">{{ report?.title }}</view
          ><StatusTag :status="report?.status || ''"
        /></view>
        <view class="subtitle">{{ report?.summary }}</view>
        <view class="report-row"
          ><text>医生意见</text><text>{{ report?.doctorOpinion || '暂无补充意见' }}</text></view
        >
        <view class="report-row"
          ><text>发布时间</text><text>{{ report?.publishedAt || '-' }}</text></view
        >
      </view>
      <view class="card disclaimer">{{ report?.disclaimer }}</view>
      <button class="download-button" :loading="downloading" @click="download">
        下载 PDF 健康报告
      </button>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getHealthReport, getHealthReportDownloadUrl } from '@/api/health-report'
import { getApiBaseUrl, getRequestHeaders } from '@/utils/request'
import type { HealthReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const id = ref('')
const report = ref<HealthReport | null>(null)
const loading = ref(true)
const downloading = ref(false)
const error = ref('')

const load = async () => {
  if (!id.value) {
    error.value = '缺少健康报告编号'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    report.value = await getHealthReport(id.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '报告加载失败'
  } finally {
    loading.value = false
  }
}

onLoad((options) => {
  id.value = options?.id || ''
})
onShow(load)

const download = async () => {
  if (!id.value) return
  downloading.value = true
  try {
    const { downloadUrl } = await getHealthReportDownloadUrl(id.value)
    // #ifdef H5
    globalThis.location.assign(downloadUrl)
    // #endif
    // #ifdef MP-WEIXIN
    uni.downloadFile({
      url: `${getApiBaseUrl()}/api/v1/health-reports/${id.value}/content`,
      header: getRequestHeaders(),
      success: ({ tempFilePath, statusCode }) => {
        if (statusCode !== 200) {
          uni.showToast({ title: '下载失败，请稍后重试', icon: 'none' })
          return
        }
        uni.openDocument({ filePath: tempFilePath, fileType: 'pdf', showMenu: true })
      },
      fail: () => uni.showToast({ title: '下载失败，请稍后重试', icon: 'none' }),
    })
    // #endif
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '下载失败', icon: 'none' })
  } finally {
    downloading.value = false
  }
}
</script>

<style scoped>
.report-row {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding-top: 22rpx;
  color: #70817b;
  font-size: 24rpx;
}
.report-row text:last-child {
  color: #334b43;
  text-align: right;
}
.disclaimer {
  color: #8a7a53;
  font-size: 22rpx;
  line-height: 1.65;
}
.download-button {
  margin-top: 28rpx;
  border-radius: 16rpx;
  background: #0f7a62;
  color: #fff;
  font-size: 28rpx;
}
</style>

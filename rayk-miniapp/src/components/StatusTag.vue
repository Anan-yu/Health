<template>
  <text class="tag" :class="status.toLowerCase()"><text class="dot" />{{ label }}</text>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()
const labels: Record<string, string> = {
  ACTIVE: '正常',
  NORMAL: '正常',
  INACTIVE: '已停用',
  UPLOADED: '已上传',
  OCR_PENDING: '排队中',
  STORED: '已存储',
  OCR_PROCESSING: '识别中',
  OCR_FAILED: '识别失败',
  WAITING_CONFIRMATION: '待确认',
  CONFIRMED: '已确认',
  PUBLISHED: '已发布',
  SUCCESS: '成功',
  COMPLETED: '已完成',
  APPROVED: '已通过',
  FAILED: '失败',
  REJECTED: '已退回',
  HIGH: '偏高',
  LOW: '偏低',
  ATTENTION: '需关注',
  INSUFFICIENT_DATA: '数据不足',
  REVIEWING: '审核中',
  WAITING_REVIEW: '待审核',
  PENDING: '待处理',
}
const label = computed(() => labels[props.status] || props.status)
</script>

<style scoped>
.tag {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  padding: 8rpx 15rpx;
  border-radius: 999rpx;
  background: #edf2f1;
  color: #5e6d68;
  font-size: 21rpx;
  line-height: 1;
}
.dot {
  width: 10rpx;
  height: 10rpx;
  margin-right: 8rpx;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.7;
}
.active,
.normal,
.stored,
.confirmed,
.published,
.success,
.completed,
.approved {
  background: #e4f7ef;
  color: #14744c;
}
.failed,
.ocr_failed,
.rejected,
.high,
.low {
  background: #ffebeb;
  color: #b42318;
}
.attention,
.insufficient_data,
.reviewing,
.waiting_review,
.waiting_confirmation,
.pending {
  background: #fff3d8;
  color: #8c5d08;
}
.uploaded,
.ocr_pending,
.ocr_processing {
  background: #eaf1ff;
  color: #416ba8;
}
</style>

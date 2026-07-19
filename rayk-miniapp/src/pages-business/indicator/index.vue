<template>
  <view class="page editor-page">
    <OcrIndicatorEditor :report-id="id" assessment-route="/pages-business/assessment/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import OcrIndicatorEditor from '@/components/OcrIndicatorEditor.vue'

const id = ref('')
onLoad((query) => (id.value = String(query?.id || '')))
</script>

<style scoped>
.editor-page {
  padding-top: 24rpx;
}
</style>

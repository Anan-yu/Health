<template>
  <view class="page editor-page">
    <OcrIndicatorEditor
      v-if="id"
      :report-id="id"
      assessment-route="/pages-customer/assessment/index"
    />
    <PageState v-else error="缺少检验报告编号，请从报告列表重新进入" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import OcrIndicatorEditor from '@/components/OcrIndicatorEditor.vue'
import PageState from '@/components/PageState.vue'

const id = ref('')
onLoad((query) => (id.value = String(query?.id || '')))
</script>

<style scoped>
.editor-page {
  padding-top: 24rpx;
}
</style>

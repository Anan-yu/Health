<template>
  <view class="page"
    ><PageState :loading="loading" :error="error"
      ><view class="card"
        ><view class="title">{{ summary?.greeting }}</view
        ><view class="subtitle">当前工作台：{{ summary?.workbench }}</view
        ><button size="mini" @click="goSwitch">切换工作台</button></view
      ><view class="grid"
        ><view
          v-for="item in summary?.metrics"
          :key="item.code"
          class="card metric-card"
          @click="open(item.route)"
          ><view class="metric">{{ item.value }}</view
          ><view>{{ item.label }}</view></view
        ></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import PageState from '@/components/PageState.vue'
import { getHomeSummary } from '@/api/workbench'
import type { HomeSummary } from '@/types/api'
const summary = ref<HomeSummary>()
const loading = ref(true),
  error = ref('')
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    summary.value = await getHomeSummary()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
const open = (url: string) => uni.navigateTo({ url })
const goSwitch = () => uni.navigateTo({ url: '/pages/switch-workbench/index' })
</script>
<style scoped>
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}
.metric-card {
  margin: 0 0 4rpx;
}
</style>

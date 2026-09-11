<template>
  <GoldBeanMarketPanel ref="panel" />
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import GoldBeanMarketPanel from '@/components/GoldBeanMarketPanel.vue'

type RefreshablePanel = { refresh: () => Promise<void> }

const panel = ref<RefreshablePanel | null>(null)

async function refresh() {
  await nextTick()
  await panel.value?.refresh()
}

defineExpose({ refresh })
onShow(() => void refresh())
onPullDownRefresh(() => {
  void refresh().finally(() => uni.stopPullDownRefresh())
})
</script>

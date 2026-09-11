<template>
  <GoldBeanPanel ref="panel" />
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import GoldBeanPanel from '@/components/GoldBeanPanel.vue'

type RefreshablePanel = { refresh: () => Promise<void> }

const panel = ref<RefreshablePanel | null>(null)

async function refresh() {
  await nextTick()
  await panel.value?.refresh()
}

defineExpose({ refresh })
onShow(() => void refresh())
</script>

<template>
  <PlatformAdminShell>
    <GoldBeanAdminPanel ref="adminPanel" />
  </PlatformAdminShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import GoldBeanAdminPanel from '@/components/GoldBeanAdminPanel.vue'
import PlatformAdminShell from '@/components/PlatformAdminShell.vue'

type RefreshablePanel = { refresh: () => Promise<void> }

const adminPanel = ref<RefreshablePanel | null>(null)

onShow(() => void adminPanel.value?.refresh())
</script>

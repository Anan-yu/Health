<template>
  <view v-if="loading" class="state">加载中…</view
  ><view v-else-if="error" class="state error">{{ error }}</view
  ><view v-else-if="empty" class="state">暂无数据</view><slot v-else />
</template>
<script setup lang="ts">
defineProps<{ loading?: boolean; error?: string; empty?: boolean }>()
</script>
<style scoped>
.state {
  padding: 100rpx 24rpx;
  text-align: center;
  color: #7b8794;
}
.error {
  color: #b42318;
}
</style>

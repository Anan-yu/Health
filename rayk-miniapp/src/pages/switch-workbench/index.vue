<template>
  <view class="page"
    ><view class="title">切换工作台</view
    ><view class="subtitle">切换仅改变当前操作身份，不改变真实账号。</view
    ><view
      v-for="item in auth.user?.availableWorkbenches"
      :key="item.code"
      class="card option"
      :class="{ active: auth.currentWorkbench === item.code }"
      @click="change(item.code)"
      ><view
        ><view class="section-title">{{ item.name }}</view
        ><view class="muted">{{ item.code }}</view></view
      ><text>{{ auth.currentWorkbench === item.code ? '当前' : '切换' }}</text></view
    ></view
  >
</template>
<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types/api'
const auth = useAuthStore()
async function change(code: Role) {
  if (code === auth.currentWorkbench) return
  uni.showLoading({ title: '切换中' })
  try {
    await auth.changeWorkbench(code)
    uni.switchTab({ url: '/pages/home/index' })
  } finally {
    uni.hideLoading()
  }
}
</script>
<style scoped>
.option {
  border: 2rpx solid transparent;
}
.option.active {
  border-color: #176b57;
  background: #edf8f4;
}
.section-title {
  margin: 0 0 8rpx;
}
</style>

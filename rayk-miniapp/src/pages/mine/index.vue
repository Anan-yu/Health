<template>
  <view class="page"
    ><view class="card"
      ><view class="title">{{ auth.user?.displayName }}</view
      ><view class="subtitle">{{ auth.user?.tenantName }} · {{ auth.currentWorkbench }}</view></view
    ><view class="card" @click="goSwitch">切换工作台</view
    ><view class="card">隐私授权与个人设置</view
    ><button class="danger" @click="signOut">退出登录</button></view
  >
</template>
<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const goSwitch = () => uni.navigateTo({ url: '/pages/switch-workbench/index' })
async function signOut() {
  await auth.signOut()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

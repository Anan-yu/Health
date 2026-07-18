<template>
  <view class="page login-page">
    <view class="brand"
      ><view class="logo">A1</view><view class="title">RayK A1</view
      ><view class="subtitle">功能医学 AI 健康管理系统</view></view
    >
    <view v-if="expired" class="notice">登录已失效，请重新登录。</view>
    <view class="card">
      <view class="section-title">选择模拟身份</view>
      <view class="roles">
        <view
          v-for="item in accounts"
          :key="item.username"
          class="role"
          :class="{ active: username === item.username }"
          @click="username = item.username"
        >
          <text class="role-name">{{ item.name }}</text
          ><text class="muted">{{ item.description }}</text>
        </view>
      </view>
      <input v-model="password" class="input" password placeholder="测试密码" />
      <button class="primary" :loading="loading" @click="handleLogin">进入系统</button>
      <view v-if="error" class="error">{{ error }}</view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
const accounts = [
  { username: 'platform_admin', name: '平台管理员', description: '平台基础查看' },
  { username: 'tenant_admin', name: '机构管理员', description: '机构与人员管理' },
  { username: 'doctor', name: '医生', description: '评估审核与发布' },
  { username: 'health_manager', name: '健康管理师', description: '客户、报告与随访' },
  { username: 'customer', name: '普通客户', description: '个人健康中心' },
]
const username = ref('doctor'),
  password = ref('RayK@123456'),
  loading = ref(false),
  error = ref(''),
  expired = ref(false)
const auth = useAuthStore()
onLoad((query) => {
  expired.value = query?.expired === '1'
})
async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    uni.switchTab({ url: '/pages/home/index' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.login-page {
  min-height: 100vh;
}
.brand {
  text-align: center;
  padding: 70rpx 0 30rpx;
}
.logo {
  width: 100rpx;
  height: 100rpx;
  line-height: 100rpx;
  margin: 0 auto 20rpx;
  border-radius: 30rpx;
  background: #176b57;
  color: #fff;
  font-size: 40rpx;
  font-weight: 800;
}
.roles {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.role {
  display: flex;
  flex-direction: column;
  padding: 20rpx;
  border: 1px solid #dde5e2;
  border-radius: 16rpx;
}
.role.active {
  border-color: #176b57;
  background: #edf8f4;
}
.role-name {
  font-weight: 650;
  margin-bottom: 8rpx;
}
.notice,
.error {
  padding: 18rpx;
  color: #b42318;
  text-align: center;
}
.notice {
  background: #ffeded;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}
</style>

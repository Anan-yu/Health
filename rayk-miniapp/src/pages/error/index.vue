<template>
  <view class="page"
    ><view class="card state"
      ><view class="title">网络连接失败</view
      ><view class="subtitle">请检查 Docker 服务和网络后重试。</view
      ><button class="primary" @click="back">返回首页</button></view
    ></view
  >
</template>
<script setup lang="ts">
const back = () => uni.switchTab({ url: '/pages/home/index' })
</script>
<style scoped>
.state {
  text-align: center;
  margin-top: 120rpx;
}
</style>

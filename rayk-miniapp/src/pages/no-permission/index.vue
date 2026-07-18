<template>
  <view class="page"
    ><view class="card state"
      ><view class="title">无访问权限</view
      ><view class="subtitle">当前工作台没有此页面或操作权限。请切换身份，或联系机构管理员。</view
      ><button class="primary" @click="back">返回工作台</button></view
    ></view
  >
</template>
<script setup lang="ts">
const back = () => uni.switchTab({ url: '/pages/workbench/index' })
</script>
<style scoped>
.state {
  text-align: center;
  margin-top: 120rpx;
}
</style>

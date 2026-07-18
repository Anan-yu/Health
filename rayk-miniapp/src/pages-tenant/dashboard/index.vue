<template>
  <view class="page"
    ><view class="title">基础数据概览</view
    ><view class="grid"
      ><view v-for="item in metrics" :key="item.name" class="card"
        ><view class="metric">{{ item.value }}</view
        ><view>{{ item.name }}</view></view
      ></view
    ><view class="card"
      ><view class="subtitle"
        >平台管理员一期仅提供机构、平台用户和审计记录的基础查看能力。</view
      ></view
    ></view
  >
</template>
<script setup lang="ts">
const metrics = [
  { name: '机构数', value: 1 },
  { name: '客户总量', value: 1 },
  { name: '待审核', value: 1 },
  { name: '待随访', value: 1 },
]
</script>
<style scoped>
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}
</style>

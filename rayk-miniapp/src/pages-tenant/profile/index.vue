<template>
  <view class="page"
    ><view class="title">机构信息</view
    ><view class="card"
      ><view class="row"><text>机构名称</text><text>RayK测试健康中心</text></view
      ><view class="row"><text>机构状态</text><StatusTag status="ACTIVE" /></view
      ><view class="row"><text>服务套餐</text><text>DEVELOPMENT</text></view></view
    ></view
  >
</template>
<script setup lang="ts">
import StatusTag from '@/components/StatusTag.vue'
</script>

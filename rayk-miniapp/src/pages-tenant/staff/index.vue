<template>
  <view class="page"
    ><view class="title">机构员工</view
    ><view v-for="item in staff" :key="item.id" class="card"
      ><view class="row"
        ><view
          ><view class="section-title">{{ item.name }}</view
          ><view class="muted">{{ item.role }}</view></view
        ><StatusTag status="ACTIVE" /></view></view
  ></view>
</template>
<script setup lang="ts">
import StatusTag from '@/components/StatusTag.vue'
const staff = [
  { id: '10002', name: '机构管理员', role: 'TENANT_ADMIN' },
  { id: '10003', name: '测试医生', role: 'DOCTOR' },
  { id: '10004', name: '测试健康管理师', role: 'HEALTH_MANAGER' },
]
</script>

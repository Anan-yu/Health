<template>
  <view class="page"
    ><view class="row"
      ><view class="title">客户列表</view
      ><button size="mini" @click="create">新建客户</button></view
    ><PageState :loading="loading" :error="error" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card" @click="detail(item.id)"
        ><view class="row"
          ><view
            ><view class="section-title">{{ item.name }}</view
            ><view class="muted">{{ item.gender }} · {{ item.phoneMasked }}</view></view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle"
          >医生 {{ item.assignedDoctorId || '未分配' }} · 健康管理师
          {{ item.assignedManagerId || '未分配' }}</view
        ></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPatients } from '@/api/patient'
import type { Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<Patient[]>([]),
  loading = ref(true),
  error = ref('')
onShow(async () => {
  loading.value = true
  try {
    items.value = (await getPatients()).records
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
const detail = (id: string) => uni.navigateTo({ url: `/pages-business/patient/detail?id=${id}` })
const create = () => uni.navigateTo({ url: '/pages-business/patient/create' })
</script>

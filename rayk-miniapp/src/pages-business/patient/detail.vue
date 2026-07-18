<template>
  <view class="page"
    ><PageState :loading="loading" :empty="!item"
      ><view class="card"
        ><view class="row"
          ><view class="title">{{ item?.name }}</view
          ><StatusTag :status="item?.status || ''" /></view
        ><view class="row"
          ><text>客户ID</text><text>{{ item?.id }}</text></view
        ><view class="row"
          ><text>性别</text><text>{{ item?.gender }}</text></view
        ><view class="row"
          ><text>出生日期</text><text>{{ item?.birthDate }}</text></view
        ><view class="row"
          ><text>联系方式</text><text>{{ item?.phoneMasked }}</text></view
        ></view
      ><view class="card"
        ><view class="section-title">业务操作</view><button @click="report">创建检验报告</button
        ><button @click="followup">查看随访</button></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getPatient } from '@/api/patient'
import type { Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const item = ref<Patient>(),
  loading = ref(true)
onLoad(async (q) => {
  try {
    item.value = await getPatient(String(q?.id || ''))
  } finally {
    loading.value = false
  }
})
const report = () => uni.navigateTo({ url: `/pages-customer/lab-report/upload` })
const followup = () => uni.navigateTo({ url: '/pages-business/followup/index' })
</script>

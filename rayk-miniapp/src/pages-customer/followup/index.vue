<template>
  <view class="page"
    ><view class="title">我的随访</view
    ><PageState :loading="loading" :error="error" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card"
        ><view class="row"
          ><view class="section-title">{{ item.title }}</view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle">{{ item.content }}</view
        ><view class="muted">计划完成：{{ item.dueDate }}</view
        ><button v-if="item.status !== 'COMPLETED'" size="mini" @click="feedback(item.id)">
          填写反馈
        </button></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyFollowups } from '@/api/followup'
import type { Followup } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<Followup[]>([]),
  loading = ref(true),
  error = ref('')
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    items.value = await getMyFollowups()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '随访任务加载失败'
  } finally {
    loading.value = false
  }
})
const feedback = (id: string) =>
  uni.navigateTo({ url: `/pages-customer/followup/feedback?id=${id}` })
</script>

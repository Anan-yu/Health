<template>
  <view class="page"
    ><view class="title">随访任务</view
    ><PageState :loading="loading" :empty="items.length === 0"
      ><view v-for="item in items" :key="item.id" class="card"
        ><view class="row"
          ><view class="section-title">{{ item.title }}</view
          ><StatusTag :status="item.status" /></view
        ><view class="subtitle">客户 {{ item.patientId }} · {{ item.content }}</view
        ><view class="muted">截止 {{ item.dueDate }}</view
        ><view v-if="item.feedback" class="feedback">客户反馈：{{ item.feedback }}</view></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getFollowups } from '@/api/followup'
import type { Followup } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<Followup[]>([]),
  loading = ref(true)
onShow(async () => {
  try {
    items.value = (await getFollowups()).records
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.feedback {
  margin-top: 18rpx;
  padding: 18rpx;
  background: #edf8f4;
  border-radius: 14rpx;
}
</style>

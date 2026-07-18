<template>
  <view class="page"
    ><view class="title">我的健康档案</view
    ><PageState :loading="loading" :error="error" :empty="!profile"
      ><view class="card"
        ><view class="row"
          ><text>姓名</text><text>{{ profile?.name }}</text></view
        ><view class="row"
          ><text>性别</text><text>{{ profile?.gender }}</text></view
        ><view class="row"
          ><text>出生日期</text><text>{{ profile?.birthDate || '待完善' }}</text></view
        ><view class="row"
          ><text>联系方式</text><text>{{ profile?.phoneMasked || '待完善' }}</text></view
        ><view class="row"
          ><text>档案状态</text><StatusTag :status="profile?.status || ''" /></view></view
      ><view class="card"
        ><view class="section-title">档案完整度 80%</view
        ><progress percent="80" active-color="#176b57" /><view class="subtitle"
          >一期展示基础档案；生活方式、过敏史和授权信息将在后续扩展。</view
        ></view
      ></PageState
    >
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyProfile } from '@/api/patient'
import type { Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const profile = ref<Patient | null>(null),
  loading = ref(true),
  error = ref('')
onShow(async () => {
  try {
    profile.value = await getMyProfile()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

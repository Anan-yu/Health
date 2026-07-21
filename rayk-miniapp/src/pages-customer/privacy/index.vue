<template>
  <view class="page"
    ><view class="title">隐私授权</view
    ><view class="subtitle page-copy"
      >你可以管理健康数据的使用范围；撤回后，新的相关处理将停止。</view
    ><PageState :loading="loading" :error="error" :empty="!patient"
      ><view v-for="item in options" :key="item.type" class="card consent-card"
        ><view
          ><view class="section-title">{{ item.name }}</view
          ><view class="subtitle">{{ item.description }}</view
          ><view class="muted">{{ enabled(item.type) ? '已授权' : '未授权' }}</view></view
        ><switch
          :checked="enabled(item.type)"
          :disabled="pending.includes(item.type)"
          color="#0f7a62"
          @change="change(item.type, $event)" /></view></PageState
  ></view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyProfile } from '@/api/patient'
import { getPrivacyConsents, grantPrivacyConsent, revokePrivacyConsent } from '@/api/privacy'
import type { Patient, PrivacyConsent } from '@/types/api'
import PageState from '@/components/PageState.vue'

const patient = ref<Patient | null>(null),
  consents = ref<PrivacyConsent[]>([]),
  pending = ref<string[]>([]),
  loading = ref(true),
  error = ref('')
const options = [
  {
    type: 'DATA_COLLECTION',
    name: '健康数据采集',
    description: '保存你主动提交的健康档案和检验报告。',
  },
  {
    type: 'HEALTH_ASSESSMENT',
    name: '健康评估服务',
    description: '使用已确认数据生成健康管理参考。',
  },
  {
    type: 'DATA_SHARING',
    name: '专业人员协作',
    description: '允许已获授权的医生和健康管理师查看服务所需资料。',
  },
  { type: 'MARKETING', name: '服务资讯', description: '接收产品和健康管理服务资讯，可随时关闭。' },
]
const enabled = (type: string) =>
  consents.value.some((item) => item.consentType === type && item.consented === 1)
onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    patient.value = await getMyProfile()
    if (patient.value) consents.value = await getPrivacyConsents(patient.value.id)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '授权记录加载失败'
  } finally {
    loading.value = false
  }
})
type SwitchChangeEvent = { detail: { value: boolean } }
const change = async (type: string, event: unknown) => {
  if (!patient.value) return
  const checked = (event as SwitchChangeEvent).detail.value
  if (pending.value.includes(type)) return
  pending.value = [...pending.value, type]
  try {
    if (checked) await grantPrivacyConsent(patient.value.id, type)
    else await revokePrivacyConsent(patient.value.id, type)
    consents.value = await getPrivacyConsents(patient.value.id)
  } catch (cause) {
    consents.value = await getPrivacyConsents(patient.value.id).catch(() => consents.value)
    uni.showToast({ title: cause instanceof Error ? cause.message : '操作失败', icon: 'none' })
  } finally {
    pending.value = pending.value.filter((item) => item !== type)
  }
}
</script>

<style scoped>
.page-copy {
  margin-bottom: 24rpx;
}
.consent-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}
.consent-card .subtitle {
  margin-top: 8rpx;
  line-height: 1.55;
}
.consent-card .muted {
  margin-top: 12rpx;
}
</style>

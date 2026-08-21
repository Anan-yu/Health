<template>
  <view class="page"
    ><PageState :loading="loading" :empty="!item"
      ><view class="card"
        ><view class="row"
          ><view class="title">{{ item?.reportName }}</view
          ><StatusTag :status="item?.status || ''" /></view
        ><view class="subtitle">客户 {{ item?.patientId }} · {{ item?.reportDate }}</view></view
      ><view v-for="indicator in item?.indicators" :key="indicator.id" class="card row"
        ><view>{{ indicator.name }}</view
        ><view
          ><text class="metric">{{ indicator.value }}</text> {{ indicator.unit }}</view
        ></view
      ><view v-for="group in imageGroups" :key="group.key" class="card"
        ><view class="row"><view class="title">{{ group.section }}</view></view
        ><view v-for="(finding, index) in group.findings" :key="`${finding.item}-${index}`" class="row"
          ><view>{{ finding.item }}</view
          ><view class="subtitle"
            >{{ finding.result }}{{ finding.unit ? ` ${finding.unit}` : ''
            }}<text v-if="finding.abnormalFlag"> · {{ finding.abnormalFlag }}</text
            ><text v-if="finding.referenceRange"> · 参考 {{ finding.referenceRange }}</text></view
          ></view
        ></view
      ><view v-if="awaitingCustomerConfirmation" class="card notice">
        客户正在核对 OCR 识别数据；确认后系统将自动提交 AI 初评。
      </view></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getLabReport } from '@/api/lab-report'
import type { ImageAnalysisFinding, LabReport } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const item = ref<LabReport>(),
  loading = ref(true),
  id = ref('')
onLoad(async (q) => {
  id.value = String(q?.id || '')
  try {
    item.value = await getLabReport(id.value)
  } finally {
    loading.value = false
  }
})
const awaitingCustomerConfirmation = computed(() => item.value?.status === 'WAITING_CONFIRMATION')
const imageGroups = computed(() => {
  const groups: Array<{ key: string; section: string; findings: ImageAnalysisFinding[] }> = []
  for (const page of item.value?.imageAnalysis?.pages || []) {
    for (const finding of page.findings || []) {
      const section = finding.category?.trim() || '体检结果'
      let group = groups[groups.length - 1]
      if (!group || group.section !== section) {
        group = { key: `${groups.length}-${section}`, section, findings: [] }
        groups.push(group)
      }
      group.findings.push(finding)
    }
  }
  return groups
})
</script>

<style scoped>
.notice {
  margin-top: 20rpx;
  color: #397267;
  line-height: 1.7;
}
</style>

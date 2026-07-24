<template>
  <view class="page"
    ><view class="title">我的随访</view
    ><view class="filter-row"
      ><view
        v-for="filter in filters"
        :key="filter.code"
        class="filter"
        :class="{ active: activeFilter === filter.code }"
        @click="activeFilter = filter.code"
        >{{ filter.label }}</view
      ></view
    ><PageState :loading="loading" :error="error" :empty="filteredItems.length === 0"
      ><view v-for="item in filteredItems" :key="item.id" class="card followup-card"
        ><view class="row"
          ><view class="section-title">{{ item.title }}</view
          ><StatusTag :status="item.status" /></view
        ><view class="plan-sections">
          <view
            v-for="section in planSections(item.content)"
            :key="section.title"
            class="plan-section"
          >
            <view class="plan-section-title"
              ><text class="section-icon">{{ sectionIcon(section.title) }}</text
              ><text>{{ section.title }}</text></view
            >
            <view v-for="action in section.actions" :key="action" class="plan-action">
              <text class="bullet">•</text><text>{{ action }}</text>
            </view>
          </view>
        </view>
        <view class="due-row"
          ><text>计划完成日期</text><text>{{ item.dueDate }}</text></view
        ><button
          v-if="item.status === 'PENDING'"
          class="feedback-button"
          @click.stop="feedback(item.id)"
        >
          填写反馈
        </button></view
      ></PageState
    ></view
  >
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyFollowups } from '@/api/followup'
import type { Followup } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'
const items = ref<Followup[]>([]),
  loading = ref(true),
  error = ref('')
const filters = [
  { code: 'ALL' as const, label: '全部' },
  { code: 'PENDING' as const, label: '待处理' },
  { code: 'COMPLETED' as const, label: '已完成' },
]
const activeFilter = ref<(typeof filters)[number]['code']>('ALL')
const filteredItems = computed(() =>
  activeFilter.value === 'ALL'
    ? items.value
    : items.value.filter((item) => item.status === activeFilter.value),
)
type PlanSection = { title: string; actions: string[] }
const sectionTitles = new Set([
  '本周重点',
  '饮食行动',
  '运动行动',
  '作息行动',
  '监测行动',
  '健康行动',
])
const introToHide = '本计划根据本次健康报告自动生成。'

function planSections(content: string): PlanSection[] {
  const sections: PlanSection[] = []
  let current: PlanSection | undefined
  const startSection = (title: string) => {
    current = { title, actions: [] }
    sections.push(current)
  }
  for (const rawLine of (content || '').split('\n')) {
    const line = rawLine.trim()
    if (!line || line === introToHide) continue
    const title = line.replace(/[：:]$/, '')
    if (sectionTitles.has(title)) {
      startSection(title)
      continue
    }
    const legacySection = line.match(/^(本周重点|建议执行)[：:]\s*(.*)$/)
    if (legacySection) {
      startSection(legacySection[1] === '建议执行' ? '健康行动' : legacySection[1])
      const actions = legacySection[2]
        .replace(/[。；]$/, '')
        .split('；')
        .map((value) => value.trim())
        .filter(Boolean)
      current?.actions.push(...actions)
      continue
    }
    if (!current) startSection('健康行动')
    current?.actions.push(line.replace(/^[-•]\s*/, ''))
  }
  return sections.filter((section) => section.title !== '本周重点' && section.actions.length)
}

function sectionIcon(title: string) {
  const icons: Record<string, string> = {
    饮食行动: '食',
    运动行动: '动',
    作息行动: '眠',
    监测行动: '记',
    健康行动: '行',
  }
  return icons[title] || '行'
}

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

<style scoped>
.filter-row {
  display: flex;
  gap: 14rpx;
  margin: 24rpx 0;
}
.filter {
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: #e9efed;
  color: #73827d;
  font-size: 22rpx;
}
.filter.active {
  background: #dff3ec;
  color: #0d745c;
  font-weight: 650;
}
.followup-card {
  padding: 30rpx;
}
.plan-sections {
  margin-top: 18rpx;
}
.plan-section {
  padding: 22rpx 0;
  border-top: 1rpx solid #e8efec;
}
.plan-section-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 14rpx;
  color: #173f36;
  font-size: 27rpx;
  font-weight: 700;
}
.section-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46rpx;
  height: 46rpx;
  border-radius: 14rpx;
  color: #08775d;
  background: #e2f5ee;
  font-size: 21rpx;
}
.plan-action {
  display: flex;
  gap: 12rpx;
  padding: 7rpx 0 7rpx 6rpx;
  color: #4d6961;
  font-size: 24rpx;
  line-height: 1.65;
}
.bullet {
  flex: 0 0 auto;
  color: #0d8468;
  font-weight: 800;
}
.due-row {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  padding: 20rpx 0;
  border-top: 1rpx solid #e8efec;
  color: #71837d;
  font-size: 23rpx;
}
.feedback-button {
  width: 100%;
  height: 88rpx;
  margin-top: 8rpx;
  border-radius: 18rpx;
  color: #fff;
  background: #0d8065;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 88rpx;
}
.feedback-button::after {
  border: 0;
}
</style>

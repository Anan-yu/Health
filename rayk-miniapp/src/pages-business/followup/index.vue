<template>
  <view class="page followup-page">
    <view class="followup-hero">
      <view>
        <view class="eyebrow-light">CONTINUOUS CARE</view>
        <view class="hero-title">随访任务</view>
        <view class="hero-copy">创建计划、跟进执行并查看客户反馈</view>
      </view>
      <view class="create-button" @click="showCreator = !showCreator">
        {{ showCreator ? '收起' : '＋ 新建' }}
      </view>
    </view>

    <view v-if="showCreator" class="card creator-card">
      <view class="creator-tabs">
        <view :class="{ active: createMode === 'TASK' }" @click="createMode = 'TASK'"
          >单次任务</view
        >
        <view :class="{ active: createMode === 'PLAN' }" @click="createMode = 'PLAN'"
          >周期计划</view
        >
      </view>
      <picker :range="patientNames" :value="patientIndex" @change="selectPatient">
        <view class="input">客户：{{ selectedPatient?.name || '请选择客户' }}</view>
      </picker>
      <input
        v-if="createMode === 'PLAN'"
        v-model="form.planName"
        class="input"
        placeholder="计划名称"
      />
      <input v-model="form.title" class="input" placeholder="随访主题" />
      <textarea v-model="form.content" class="textarea" placeholder="随访内容与执行要求" />
      <input
        v-model="form.startDate"
        class="input"
        :placeholder="createMode === 'PLAN' ? '开始日期 YYYY-MM-DD' : '截止日期 YYYY-MM-DD'"
      />
      <view v-if="createMode === 'PLAN'" class="number-grid">
        <input v-model="form.intervalDays" class="input" type="number" placeholder="间隔天数" />
        <input v-model="form.totalOccurrences" class="input" type="number" placeholder="随访次数" />
      </view>
      <button class="primary" :loading="submitting" @click="submit">
        {{ createMode === 'PLAN' ? '创建草稿计划' : '创建随访任务' }}
      </button>
    </view>

    <view class="summary-strip">
      <view
        ><text>{{ pendingCount }}</text
        ><text>待处理</text></view
      >
      <view
        ><text>{{ completedCount }}</text
        ><text>已完成</text></view
      >
      <view
        ><text>{{ feedbackCount }}</text
        ><text>有反馈</text></view
      >
    </view>

    <view v-if="plans.length" class="section-head">
      <view><view class="eyebrow">PLANS</view><view class="section-title">随访计划</view></view>
    </view>
    <view v-for="plan in plans" :key="plan.id" class="card plan-card">
      <view class="row">
        <view>
          <view class="section-title">{{ plan.planName }}</view>
          <view class="muted"
            >{{ plan.startDate }} 至 {{ plan.endDate }} · {{ plan.tasks.length }} 次</view
          >
        </view>
        <StatusTag :status="plan.status" />
      </view>
      <view class="plan-actions">
        <button v-if="plan.status === 'DRAFT'" class="mini primary" @click="activatePlan(plan.id)">
          启用计划
        </button>
        <button v-if="plan.status === 'ACTIVE'" class="mini secondary" @click="finishPlan(plan.id)">
          完成计划
        </button>
      </view>
    </view>

    <view class="section-head">
      <view><view class="eyebrow">TASKS</view><view class="section-title">执行任务</view></view>
    </view>
    <PageState :loading="loading" :error="error" :empty="visibleItems.length === 0">
      <view v-for="item in visibleItems" :key="item.id" class="card task-card">
        <view class="row">
          <view class="section-title">{{ item.title }}</view>
          <StatusTag :status="item.status" />
        </view>
        <view class="subtitle">客户 {{ patientName(item.patientId) }} · {{ item.content }}</view>
        <view class="muted">计划完成：{{ item.dueDate }}</view>
        <view v-if="item.feedback" class="feedback">客户反馈：{{ item.feedback }}</view>
        <button
          v-if="item.status === 'PENDING'"
          class="mini secondary complete-button"
          @click="completeTask(item.id)"
        >
          标记已完成
        </button>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  activateFollowupPlan,
  completeFollowup,
  completeFollowupPlan,
  createFollowup,
  createFollowupPlan,
  getFollowupPlans,
  getFollowups,
} from '@/api/followup'
import { getPatients } from '@/api/patient'
import { useAuthStore } from '@/stores/auth'
import type { Followup, FollowupPlan, Patient } from '@/types/api'
import PageState from '@/components/PageState.vue'
import StatusTag from '@/components/StatusTag.vue'

const auth = useAuthStore()
const items = ref<Followup[]>([])
const plans = ref<FollowupPlan[]>([])
const patients = ref<Patient[]>([])
const selectedPatientId = ref('')
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const showCreator = ref(false)
const createMode = ref<'TASK' | 'PLAN'>('TASK')
const localToday = () => {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
}

const form = reactive({
  planName: '',
  title: '',
  content: '',
  startDate: localToday(),
  intervalDays: '7',
  totalOccurrences: '4',
})

const patientNames = computed(() => patients.value.map((item) => item.name))
const patientIndex = computed(() =>
  Math.max(
    patients.value.findIndex((item) => item.id === selectedPatientId.value),
    0,
  ),
)
const selectedPatient = computed(() =>
  patients.value.find((item) => item.id === selectedPatientId.value),
)
const visibleItems = computed(() =>
  selectedPatientId.value
    ? items.value.filter((item) => item.patientId === selectedPatientId.value)
    : items.value,
)
const pendingCount = computed(
  () => visibleItems.value.filter((item) => item.status === 'PENDING').length,
)
const completedCount = computed(
  () => visibleItems.value.filter((item) => item.status === 'COMPLETED').length,
)
const feedbackCount = computed(() => visibleItems.value.filter((item) => item.feedback).length)

onLoad((query) => {
  selectedPatientId.value = String(query?.patientId || '')
})

onShow(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [followupPage, patientPage] = await Promise.all([getFollowups(), getPatients()])
    items.value = followupPage.records
    patients.value = patientPage.records
    if (!selectedPatientId.value && patients.value.length)
      selectedPatientId.value = patients.value[0].id
    await loadPlans()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '随访任务加载失败'
  } finally {
    loading.value = false
  }
}

async function loadPlans() {
  plans.value = selectedPatientId.value ? await getFollowupPlans(selectedPatientId.value) : []
}

function selectPatient(event: { detail: { value: string | number } }) {
  const patient = patients.value[Number(event.detail.value)]
  selectedPatientId.value = patient?.id || ''
  void loadPlans()
}

function patientName(id: string) {
  return patients.value.find((item) => item.id === id)?.name || `#${id}`
}

function validateForm() {
  if (!selectedPatient.value) return '请选择客户'
  if (!form.title.trim() || !form.content.trim()) return '请填写随访主题和内容'
  if (!/^\d{4}-\d{2}-\d{2}$/.test(form.startDate)) return '日期格式应为 YYYY-MM-DD'
  if (createMode.value === 'PLAN' && !form.planName.trim()) return '请填写计划名称'
  if (
    createMode.value === 'PLAN' &&
    (Number(form.intervalDays) < 1 || Number(form.totalOccurrences) < 1)
  ) {
    return '间隔天数和随访次数必须大于 0'
  }
  return ''
}

async function submit() {
  const message = validateForm()
  if (message) {
    uni.showToast({ title: message, icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const assignee = selectedPatient.value?.assignedManagerId || auth.user?.userId
    if (createMode.value === 'PLAN') {
      await createFollowupPlan({
        patientId: Number(selectedPatientId.value),
        planName: form.planName.trim(),
        startDate: form.startDate,
        intervalDays: Number(form.intervalDays),
        totalOccurrences: Number(form.totalOccurrences),
        title: form.title.trim(),
        content: form.content.trim(),
        assigneeId: assignee ? Number(assignee) : undefined,
      })
    } else {
      await createFollowup({
        patientId: Number(selectedPatientId.value),
        title: form.title.trim(),
        content: form.content.trim(),
        dueDate: form.startDate,
        assigneeId: assignee ? Number(assignee) : undefined,
      })
    }
    showCreator.value = false
    form.planName = ''
    form.title = ''
    form.content = ''
    await load()
    uni.showToast({ title: '创建成功' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '创建失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

async function completeTask(id: string) {
  await completeFollowup(id)
  await load()
  uni.showToast({ title: '任务已完成' })
}

async function activatePlan(id: string) {
  await activateFollowupPlan(id)
  await load()
  uni.showToast({ title: '计划已启用' })
}

async function finishPlan(id: string) {
  await completeFollowupPlan(id)
  await load()
  uni.showToast({ title: '计划已完成' })
}
</script>

<style scoped>
.followup-page {
  padding-top: 24rpx;
}
.followup-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 34rpx;
  border-radius: 34rpx;
  background: linear-gradient(140deg, #173f37, #0b715a);
  color: #fff;
  box-shadow: 0 18rpx 40rpx rgba(15, 94, 75, 0.18);
}
.eyebrow-light {
  color: #91d9c4;
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}
.hero-title {
  margin-top: 10rpx;
  font-size: 38rpx;
  font-weight: 750;
}
.hero-copy {
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.68);
  font-size: 21rpx;
}
.create-button {
  padding: 14rpx 20rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.25);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.13);
  font-size: 22rpx;
  font-weight: 650;
}
.creator-card {
  margin-top: 22rpx;
}
.creator-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8rpx;
  margin-bottom: 20rpx;
  padding: 7rpx;
  border-radius: 18rpx;
  background: #edf3f1;
}
.creator-tabs view {
  padding: 15rpx;
  border-radius: 14rpx;
  color: #778780;
  text-align: center;
  font-size: 23rpx;
}
.creator-tabs .active {
  background: #fff;
  color: #0f765f;
  font-weight: 680;
  box-shadow: 0 4rpx 14rpx rgba(31, 72, 61, 0.08);
}
.creator-card .input,
.creator-card .textarea {
  margin-top: 16rpx;
}
.number-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
.summary-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 25rpx;
  background: #e7f5f0;
}
.summary-strip view {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.summary-strip text:first-child {
  color: #0e735b;
  font-size: 32rpx;
  font-weight: 750;
}
.summary-strip text:last-child {
  color: #789087;
  font-size: 20rpx;
}
.plan-card,
.task-card {
  margin-top: 18rpx;
}
.plan-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 18rpx;
}
.feedback {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #edf8f4;
  color: #45675d;
  font-size: 22rpx;
}
.mini {
  width: auto;
  margin: 0;
  padding: 0 24rpx;
  font-size: 22rpx;
}
.complete-button {
  margin-top: 18rpx;
  margin-left: auto;
}
</style>

<template>
  <view class="page member-page" :class="{ 'yearly-page': isActive }">
    <view class="hero" :class="isActive ? 'hero-yearly' : 'hero-free'">
      <view class="hero-copy">
        <view class="eyebrow">HEALTH MEMBERSHIP</view>
        <view class="hero-title">{{ isActive ? '年度健康会员' : '健康会员中心' }}</view>
        <view class="hero-subtitle">
          {{ isActive ? '已解锁 AI 健康管理、持续随访、健康拍与专属提醒服务' : '保留全部历史健康数据，按需解锁智能服务' }}
        </view>
        <view v-if="isActive" class="hero-meta">
          <view>有效期至 {{ formatDate(summary?.expireAt) }}</view>
          <view>连续守护中 {{ Math.max(summary?.remainingDays || 0, 0) }} 天</view>
        </view>
      </view>
      <view class="hero-badge">{{ isActive ? '已开通' : '免费客户' }}</view>
      <image class="hero-art" :src="isActive ? yearlyHero : freeHero" mode="aspectFit" />
    </view>

    <PageState :loading="loading" :error="error" :empty="false">
      <view class="dashboard-card">
        <view class="section-heading">
          <view class="heading-mark">{{ isActive ? '◇' : '♕' }}</view>
          <view class="section-heading-copy">
            <view>{{ isActive ? '会员专属仪表盘' : '会员权益总览' }}</view>
            <view class="section-heading-note">{{ isActive ? '当前会员权益与使用概况' : '当前可用权益与健康评估状态' }}</view>
          </view>
        </view>
        <view class="dashboard-metrics">
          <view class="metric-card metric-benefit">
            <view class="metric-topline">
              <view class="metric-icon">♕</view>
              <view class="metric-kicker">权益状态</view>
            </view>
            <view class="metric-main">
              <text class="metric-value">{{ isActive ? activeAvailableCount : freeRemainingCount }}</text>
              <text class="metric-unit">项</text>
            </view>
            <view class="metric-label">{{ isActive ? '可用会员权益' : '可用基础权益' }}</view>
            <view class="metric-progress"><view class="metric-progress-fill" :style="{ width: `${isActive ? activeCoverage : freeCoverage}%` }" /></view>
            <view class="metric-foot">{{ isActive ? `已使用 ${activeUsedCount} 次` : `基础权益共 ${benefits.length} 项` }}</view>
          </view>
          <view class="metric-card metric-score">
            <view class="metric-topline">
              <view class="metric-icon">♡</view>
              <view class="metric-kicker">健康评估</view>
            </view>
            <view class="metric-main">
              <text class="metric-value" :class="{ 'metric-value-placeholder': latestHealthScore === null }">{{ latestHealthScore === null ? '待评估' : latestHealthScore }}</text>
              <text v-if="latestHealthScore !== null" class="metric-unit">分</text>
            </view>
            <view class="metric-label">{{ latestHealthScore === null ? '完成评估后显示' : '最近一次健康评估' }}</view>
            <view class="metric-progress metric-progress-score"><view class="metric-progress-fill" :style="{ width: `${latestHealthScore ?? 0}%` }" /></view>
            <view class="metric-foot">{{ latestHealthScore === null ? '等待有效评估结果' : '满分 100 分' }}</view>
          </view>
        </view>
        <view class="dashboard-stats">
          <view class="stat-item">
            <view class="stat-value">{{ isActive ? activeAvailableCount : freeEffectiveCount }}</view>
            <view class="stat-label">已解锁权益</view>
          </view>
          <view class="stat-item">
            <view class="stat-value warning">{{ isActive ? activeLockedCount : freeAttentionCount }}</view>
            <view class="stat-label">待解锁权益</view>
          </view>
          <view class="stat-item">
            <view class="stat-value">{{ isActive ? activeCoverage : freeCoverage }}%</view>
            <view class="stat-label">权益覆盖率</view>
          </view>
        </view>
      </view>

      <view class="section-label"><view class="section-line" />{{ isActive ? '会员专属服务' : '我的权益' }}</view>
      <view class="benefit-grid" :class="{ 'yearly-grid': isActive }">
        <view v-for="tile in visibleTiles" :key="tile.title" class="benefit-tile" :class="`tone-${tile.tone}`">
          <image class="benefit-icon" :src="tile.icon" mode="aspectFit" />
          <view class="benefit-copy">
            <view class="benefit-title">{{ tile.title }}</view>
            <view class="benefit-description">{{ tile.description }}</view>
            <view class="benefit-status">{{ tile.status }}</view>
          </view>
        </view>
      </view>

      <view v-if="!isActive" class="unlock-card">
        <view class="unlock-title">解锁更多健康管理服务</view>
        <view class="unlock-description">年度会员提供持续评估、随访、健康拍额度和 AI 健康报告。</view>
        <button class="primary-button" @click="subscribe">查看会员方案</button>
      </view>
      <view v-else class="renew-area">
        <button class="primary-button" @click="subscribe">续费会员</button>
      </view>

      <view v-if="devMembershipEnabled" class="dev-card">
        <view class="dev-title">开发调试</view>
        <view class="dev-description">仅开发包可用，用于切换会员状态验证权益。</view>
        <view class="dev-actions">
          <button class="dev-button" :disabled="switching" @click="switchMembership('FREE')">恢复免费客户</button>
          <button class="dev-button" :disabled="switching" @click="switchMembership('YEARLY')">模拟年度会员</button>
        </view>
      </view>

      <view class="disclaimer">本服务仅为健康管理参考，不替代临床诊断与医生面诊</view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import PageState from '@/components/PageState.vue'
import { getMyAssessments } from '@/api/assessment'
import type { Assessment } from '@/types/api'
import {
  getMembershipSummary,
  switchMembershipForDevelopment,
  type MembershipBenefit,
  type MembershipDevelopmentTarget,
  type MembershipSummary,
} from '@/api/membership'
const memberAssetRoot = '/pages-customer/static/member'
const freeHero = `${memberAssetRoot}/free-member/member-free-hero-shield.svg`
const freeAssessment = `${memberAssetRoot}/common/member-ai-assessment.svg`
const freeFollowup = `${memberAssetRoot}/common/member-health-followup-camera.svg`
const freeMeal = `${memberAssetRoot}/common/member-meal-reminder.svg`
const freeSleep = `${memberAssetRoot}/common/member-sleep-reminder.svg`
const yearlyHero = `${memberAssetRoot}/yearly-member/member-yearly-hero-crown.svg`
const yearlyAssessment = `${memberAssetRoot}/common/member-ai-assessment.svg`
const yearlyFollowup = `${memberAssetRoot}/common/member-followup-plan.svg`
const yearlyReport = `${memberAssetRoot}/common/member-health-report-pdf.svg`
const yearlyTrend = `${memberAssetRoot}/common/member-health-trend.svg`
const yearlyCamera = `${memberAssetRoot}/common/member-health-followup-camera.svg`
const yearlyReminder = `${memberAssetRoot}/common/member-meal-reminder.svg`

type BenefitTile = {
  title: string
  description: string
  status: string
  icon: string
  tone: string
}

const loading = ref(true)
const error = ref('')
const summary = ref<MembershipSummary>()
const latestHealthScore = ref<number | null>(null)
const switching = ref(false)
const devMembershipEnabled = import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEVELOPMENT_LOGIN === 'true'

const isActive = computed(() => summary.value?.membershipStatus === 'ACTIVE' || summary.value?.active === true)
const benefits = computed(() => summary.value?.benefits || [])
const findBenefit = (...codes: string[]) => benefits.value.find((item) => codes.includes(item.benefitCode))
const numericRemaining = (item?: MembershipBenefit) => (item?.remaining == null ? 0 : Math.max(item.remaining, 0))
const benefitStatus = (...codes: string[]) => {
  const item = findBenefit(...codes)
  if (!item) return '暂未开通'
  if (item.remaining == null) return item.available ? '已解锁' : '暂未开通'
  return item.remaining > 0 ? `剩余 ${item.remaining} 次` : '本期已用完'
}
const benefitDescription = (item?: MembershipBenefit, fallback = '') => item?.description || fallback

const freeTiles = computed<BenefitTile[]>(() => [
  {
    title: 'AI 健康评估',
    description: benefitDescription(findBenefit('AI_HEALTH_ASSESSMENT'), '生成综合健康评估'),
    status: benefitStatus('AI_HEALTH_ASSESSMENT'),
    icon: freeAssessment,
    tone: 'mint',
  },
  {
    title: 'AI 初始随访 / 健康拍',
    description: '生成首次随访与面部检测',
    status: `${benefitStatus('AI_FOLLOWUP_INITIAL')} · ${benefitStatus('HEALTH_SHOT')}`,
    icon: freeFollowup,
    tone: 'blue',
  },
  {
    title: '吃饭语音提醒',
    description: benefitDescription(findBenefit('TTS_MEAL_REMINDER'), '生成个性化吃饭提醒'),
    status: benefitStatus('TTS_MEAL_REMINDER'),
    icon: freeMeal,
    tone: 'orange',
  },
  {
    title: '睡眠语音提醒',
    description: benefitDescription(findBenefit('TTS_SLEEP_REMINDER'), '睡前提醒与作息建议'),
    status: benefitStatus('TTS_SLEEP_REMINDER'),
    icon: freeSleep,
    tone: 'purple',
  },
])

const yearlyTiles = computed<BenefitTile[]>(() => [
  { title: 'AI 健康评估', description: '多维评估健康风险', status: '已解锁', icon: yearlyAssessment, tone: 'mint' },
  { title: 'AI 随访计划', description: '智能生成并持续调整', status: `${benefitStatus('AI_FOLLOWUP_INITIAL')} · ${benefitStatus('AI_FOLLOWUP_CONTINUE')}`, icon: yearlyFollowup, tone: 'blue' },
  { title: 'AI 健康报告', description: '整合报告一目了然', status: '已解锁', icon: yearlyReport, tone: 'purple' },
  { title: '健康趋势档案', description: '长期趋势智能分析', status: '会员专享', icon: yearlyTrend, tone: 'orange' },
  { title: '健康拍每日额度', description: '每日额度专属加赠', status: benefitStatus('HEALTH_SHOT_DAILY'), icon: yearlyCamera, tone: 'cyan' },
  { title: '吃饭 / 睡眠提醒', description: '个性化健康提醒', status: `${benefitStatus('TTS_MEAL_REMINDER')} · ${benefitStatus('TTS_SLEEP_REMINDER')}`, icon: yearlyReminder, tone: 'pink' },
])

const visibleTiles = computed(() => (isActive.value ? yearlyTiles.value : freeTiles.value))
const freeEffectiveCount = computed(() => benefits.value.filter((item) => item.available).length)
const freeAttentionCount = computed(() => Math.max(benefits.value.length - freeEffectiveCount.value, 0))
const freeRemainingCount = computed(() => {
  const quota = benefits.value.reduce((total, item) => total + numericRemaining(item), 0)
  return quota || freeEffectiveCount.value
})
const activeAvailableCount = computed(() => benefits.value.filter((item) => item.available).length)
const activeLockedCount = computed(() => Math.max(benefits.value.length - activeAvailableCount.value, 0))
const activeUsedCount = computed(() => benefits.value.reduce((total, item) => total + Math.max(item.used || 0, 0), 0))
const activeCoverage = computed(() => (benefits.value.length ? Math.round((activeAvailableCount.value / benefits.value.length) * 100) : 0))
const freeCoverage = computed(() => (benefits.value.length ? Math.round((freeEffectiveCount.value / benefits.value.length) * 100) : 0))

const assessmentScore = (assessment: Assessment) => {
  const scores = (assessment.results?.results || [])
    .filter((item) => item.status !== 'INSUFFICIENT_DATA' && typeof item.score === 'number')
    .map((item) => item.score as number)
    .filter((score) => Number.isFinite(score) && score >= 0 && score <= 100)
  return scores.length ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length) : null
}

const loadLatestHealthScore = async () => {
  latestHealthScore.value = null
  try {
    const assessments = await getMyAssessments()
    const latest = assessments
      .filter((item) => item.status === 'SUCCESS')
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt))[0]
    latestHealthScore.value = latest ? assessmentScore(latest) : null
  } catch {
    // 健康分是会员页的辅助信息，评估接口异常时不影响会员权益展示。
    latestHealthScore.value = null
  }
}

onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    summary.value = await getMembershipSummary()
    await loadLatestHealthScore()
    syncNavigationTitle()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '会员信息加载失败'
  } finally {
    loading.value = false
  }
})

const formatDate = (value?: string) => (value ? value.replace('T', ' ').slice(0, 10) : '—')
const syncNavigationTitle = () => uni.setNavigationBarTitle({ title: isActive.value ? '年度会员' : '健康会员' })
const subscribe = () => uni.navigateTo({ url: '/pages-customer/member/subscribe' })
const switchMembership = async (target: MembershipDevelopmentTarget) => {
  if (switching.value) return
  switching.value = true
  try {
    summary.value = await switchMembershipForDevelopment(target)
    syncNavigationTitle()
    uni.showToast({ title: target === 'FREE' ? '已恢复免费客户' : '已模拟年度会员', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '会员状态切换失败', icon: 'none' })
  } finally {
    switching.value = false
  }
}
</script>

<style scoped>
.member-page { min-height: 100vh; padding: 24rpx; background: linear-gradient(180deg, #f2fbf8 0%, #f8fbfa 100%); }
.hero { position: relative; min-height: 340rpx; padding: 38rpx 34rpx; border-radius: 34rpx; overflow: hidden; color: #0d5b4b; box-sizing: border-box; }
.hero-free { background: linear-gradient(135deg, #c5f2df 0%, #e8fbf2 100%); }
.hero-yearly { color: #fff; background: linear-gradient(135deg, #056a56 0%, #16a383 100%); }
.hero::after { content: ''; position: absolute; width: 420rpx; height: 420rpx; right: -160rpx; bottom: -240rpx; border: 2rpx solid rgba(255,255,255,.22); border-radius: 50%; box-shadow: 0 0 0 24rpx rgba(255,255,255,.08), 0 0 0 48rpx rgba(255,255,255,.05); }
.hero-copy { position: relative; z-index: 1; width: 65%; }
.eyebrow { font-size: 20rpx; font-weight: 700; letter-spacing: 4rpx; opacity: .58; }
.hero-title { margin-top: 24rpx; font-size: 42rpx; font-weight: 800; line-height: 1.2; }
.hero-subtitle { width: 460rpx; max-width: 100%; margin-top: 18rpx; font-size: 25rpx; line-height: 1.65; opacity: .84; }
.hero-badge { position: absolute; z-index: 2; right: 28rpx; top: 28rpx; padding: 12rpx 20rpx; border-radius: 28rpx; color: #0a6855; background: rgba(255,255,255,.78); font-size: 23rpx; font-weight: 700; }
.hero-yearly .hero-badge { color: #7b5317; background: #fff7d8; }
.hero-art { position: absolute; z-index: 1; right: 6rpx; bottom: -12rpx; width: 310rpx; height: 290rpx; }
.hero-meta { display: grid; gap: 8rpx; margin-top: 22rpx; font-size: 23rpx; opacity: .9; }
.dashboard-card { margin-top: 22rpx; padding: 30rpx 28rpx 26rpx; border-radius: 30rpx; background: #fff; box-shadow: 0 12rpx 32rpx rgba(24, 91, 75, .06); }
.section-heading { display: flex; align-items: flex-start; gap: 14rpx; color: #163a32; font-size: 32rpx; font-weight: 800; }
.heading-mark { display: flex; align-items: center; justify-content: center; width: 44rpx; height: 44rpx; margin-top: 2rpx; border-radius: 14rpx; color: #0d9a76; background: linear-gradient(145deg, #dcf8ed, #effcf7); font-size: 28rpx; }
.section-heading-copy { flex: 1; min-width: 0; }
.section-heading-note { margin-top: 6rpx; color: #8aa39b; font-size: 21rpx; font-weight: 400; line-height: 1.35; }
.dashboard-metrics { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16rpx; margin-top: 26rpx; }
.metric-card { min-height: 246rpx; padding: 22rpx 20rpx 18rpx; border: 1rpx solid #d9eee7; border-radius: 24rpx; box-sizing: border-box; background: linear-gradient(145deg, #f5fffb 0%, #fff 72%); }
.metric-score { border-color: #cceaf4; background: linear-gradient(145deg, #f4fcff 0%, #fff 72%); }
.metric-topline { display: flex; align-items: center; gap: 10rpx; }
.metric-icon { display: flex; align-items: center; justify-content: center; width: 46rpx; height: 46rpx; border-radius: 16rpx; color: #0b9d78; background: #dff7ed; font-size: 28rpx; font-weight: 700; }
.metric-score .metric-icon { color: #248fc0; background: #e0f4fb; }
.metric-kicker { color: #708a82; font-size: 21rpx; }
.metric-main { display: flex; align-items: baseline; min-height: 62rpx; margin-top: 20rpx; }
.metric-value { color: #0a956f; font-size: 52rpx; font-weight: 800; line-height: 1; }
.metric-score .metric-value { color: #1986b5; }
.metric-value-placeholder { font-size: 29rpx; color: #7caaa9; }
.metric-unit { margin-left: 6rpx; color: #58766d; font-size: 22rpx; }
.metric-label { margin-top: 6rpx; color: #264d43; font-size: 22rpx; font-weight: 700; }
.metric-progress { height: 10rpx; margin-top: 22rpx; overflow: hidden; border-radius: 10rpx; background: #dcefe8; }
.metric-progress-score { background: #deeff5; }
.metric-progress-fill { height: 100%; min-width: 0; border-radius: inherit; background: linear-gradient(90deg, #43c997, #0b9c78); }
.metric-progress-score .metric-progress-fill { background: linear-gradient(90deg, #53bde0, #218dc2); }
.metric-foot { margin-top: 12rpx; overflow: hidden; color: #78938b; font-size: 20rpx; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }
.dashboard-stats { display: flex; margin-top: 26rpx; padding-top: 22rpx; border-top: 1rpx solid #e4efeb; }
.stat-item { position: relative; flex: 1; text-align: center; }
.stat-item + .stat-item::before { content: ''; position: absolute; left: 0; top: 8rpx; width: 1rpx; height: 52rpx; background: #e1ece8; }
.stat-value { color: #123c32; font-size: 34rpx; font-weight: 800; }
.stat-value.warning { color: #d69316; }
.stat-label { margin-top: 6rpx; color: #78908a; font-size: 22rpx; }
.section-label { display: flex; align-items: center; gap: 12rpx; margin: 30rpx 8rpx 18rpx; color: #183c34; font-size: 32rpx; font-weight: 800; }
.section-line { width: 8rpx; height: 34rpx; border-radius: 6rpx; background: #23b88e; }
.benefit-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16rpx; }
.benefit-grid.yearly-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.benefit-tile { min-height: 218rpx; padding: 18rpx 16rpx 16rpx; border: 1rpx solid #d7eee6; border-radius: 26rpx; box-sizing: border-box; background: #fff; box-shadow: 0 8rpx 22rpx rgba(24, 91, 75, .04); }
.yearly-grid .benefit-tile { min-height: 214rpx; padding: 16rpx 10rpx; text-align: center; }
.benefit-icon { display: block; width: 92rpx; height: 92rpx; margin-bottom: 8rpx; }
.yearly-grid .benefit-icon { width: 86rpx; height: 86rpx; margin: 0 auto 4rpx; }
.benefit-title { color: #183d35; font-size: 26rpx; font-weight: 700; line-height: 1.35; }
.yearly-grid .benefit-title { font-size: 23rpx; }
.benefit-description { min-height: 38rpx; margin-top: 8rpx; color: #78908a; font-size: 21rpx; line-height: 1.35; }
.yearly-grid .benefit-description { font-size: 19rpx; }
.benefit-status { display: inline-block; margin-top: 10rpx; padding: 6rpx 14rpx; border: 1rpx solid currentColor; border-radius: 20rpx; color: #0d9f7b; font-size: 20rpx; line-height: 1.2; }
.tone-blue { border-color: #bfe9f2; background: linear-gradient(145deg, #fff, #f1fbfd); }
.tone-orange { border-color: #f7dfba; background: linear-gradient(145deg, #fff, #fff9ef); }
.tone-purple { border-color: #ddd2fa; background: linear-gradient(145deg, #fff, #faf7ff); }
.tone-cyan { border-color: #bcecf0; background: linear-gradient(145deg, #fff, #f1fdfe); }
.tone-pink { border-color: #ead8f7; background: linear-gradient(145deg, #fff, #fcf7ff); }
.unlock-card { margin-top: 22rpx; padding: 30rpx 28rpx; border: 1rpx solid #f0dda4; border-radius: 28rpx; background: linear-gradient(135deg, #fff9e9, #fffdf7); }
.unlock-title { color: #4b3c1e; font-size: 30rpx; font-weight: 800; }
.unlock-description { margin-top: 10rpx; color: #887656; font-size: 23rpx; line-height: 1.55; }
.primary-button { height: 88rpx; margin-top: 22rpx; border: 0; border-radius: 44rpx; color: #fff; background: linear-gradient(90deg, #5bcf7c, #08a9d4); font-size: 31rpx; font-weight: 700; line-height: 88rpx; }
.renew-area { margin-top: 22rpx; }
.dev-card { margin-top: 22rpx; padding: 24rpx; border: 1rpx solid #c3eadd; border-radius: 24rpx; background: #f4fffb; }
.dev-title { color: #176b58; font-size: 26rpx; font-weight: 700; }
.dev-description { margin-top: 8rpx; color: #77928a; font-size: 22rpx; }
.dev-actions { display: flex; gap: 14rpx; margin-top: 16rpx; }
.dev-button { flex: 1; height: 64rpx; margin: 0; padding: 0 8rpx; border: 0; border-radius: 16rpx; color: #0f8066; background: #dff6ee; font-size: 22rpx; line-height: 64rpx; }
.dev-button[disabled] { opacity: .55; }
.disclaimer { padding: 28rpx 8rpx 10rpx; color: #9aaba6; text-align: center; font-size: 21rpx; }
</style>

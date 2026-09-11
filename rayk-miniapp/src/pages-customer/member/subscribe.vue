<template>
  <view class="page subscribe-page">
    <PageState :loading="loading" :error="error" :empty="!yearlyPlan">
      <view class="open-hero">
        <view class="hero-copy">
          <view class="eyebrow">HEALTH MEMBERSHIP</view>
          <view class="hero-title">开通年度健康会员</view>
          <view class="price-line">¥{{ yearlyPrice }}<text>/年</text><view class="recommend">推荐方案</view></view>
          <view class="hero-description">从一次体检到长期健康行动，解锁持续评估、随访和健康趋势服务。</view>
        </view>
        <image class="hero-art" :src="openHero" mode="aspectFit" />
      </view>

      <view class="activity-card" aria-label="开通动态播报">
        <view class="activity-head">
          <view class="activity-title"><image class="membership-widget-icon activity-icon" :src="activityIcon" mode="aspectFit" />开通动态播报</view>
        </view>
        <view class="activity-window">
          <view class="activity-track">
            <view v-for="(item, index) in activityItems" :key="`${item.name}-${index}`" class="activity-item">
              <view class="activity-dot" />
              <view class="activity-copy">{{ item.name }} {{ item.message }}</view>
              <view class="activity-time">{{ item.time }}</view>
            </view>
          </view>
        </view>
      </view>

      <view class="comparison-card">
        <view class="card-heading"><image class="membership-widget-icon heading-icon" :src="comparisonIcon" mode="aspectFit" />会员权益对比</view>
        <view class="selected-plan">
          <view class="selected-header">
            <view class="selected-name"><image class="membership-widget-icon plan-icon" :src="annualBadgeIcon" mode="aspectFit" />年度健康会员 <text>推荐</text></view>
            <view class="selected-price">¥{{ yearlyPrice }}/年 <view class="selected-check">✓</view></view>
          </view>
          <view class="open-benefit-grid">
            <view v-for="benefit in openBenefits" :key="benefit.title" class="open-benefit">
              <image class="open-benefit-icon" :src="benefit.icon" mode="aspectFit" />
              <view class="open-benefit-title">{{ benefit.title }}</view>
              <view class="open-benefit-description">{{ benefit.description }}</view>
            </view>
          </view>
        </view>
        <view class="free-plan-row">
          <image class="membership-widget-icon free-plan-icon" :src="freeUserIcon" mode="aspectFit" />
          <view class="free-plan-copy"><view>免费用户 <text>（基础体验）</text></view><view class="free-plan-description">体验基础健康管理服务</view></view>
          <view class="arrow">›</view>
        </view>

        <view class="rights-table">
          <view class="rights-table-head">
            <view>权益</view>
            <view>免费用户</view>
            <view>年度健康会员</view>
          </view>
          <view v-for="row in rightsRows" :key="row.name" class="rights-table-row">
            <view class="rights-name">{{ row.name }}</view>
            <view>{{ row.freeValue }}</view>
            <view class="rights-member-value">{{ row.memberValue }}</view>
          </view>
        </view>
      </view>

      <button class="primary-button" :disabled="buying || !yearlyPlan" @click="buy">
        {{ buying ? '正在创建订单…' : `立即开通 ¥${yearlyPrice}/年` }}
      </button>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import PageState from '@/components/PageState.vue'
import { createMembershipOrder, getMembershipPlans, type MembershipPlan } from '@/api/membership'
const memberAssetRoot = '/pages-customer/static/member'
const openHero = `${memberAssetRoot}/open-member/member-open-hero-crown.svg`
const openAssessment = `${memberAssetRoot}/common/member-ai-assessment.svg`
const openFollowup = `${memberAssetRoot}/common/member-followup-plan.svg`
const openReport = `${memberAssetRoot}/common/member-health-report-pdf.svg`
const openTrend = `${memberAssetRoot}/common/member-health-trend.svg`
const openBroadcast = `${memberAssetRoot}/common/member-meal-reminder.svg`
const openSleep = `${memberAssetRoot}/common/member-sleep-reminder.svg`
const activityIcon = `${memberAssetRoot}/replacements/membership-broadcast.svg`
const comparisonIcon = `${memberAssetRoot}/replacements/membership-compare.svg`
const annualBadgeIcon = `${memberAssetRoot}/replacements/membership-annual-badge.svg`
const freeUserIcon = `${memberAssetRoot}/replacements/membership-free-user.svg`

const loading = ref(true)
const error = ref('')
const plans = ref<MembershipPlan[]>([])
const buying = ref(false)

const yearlyPlan = computed(() => plans.value.find((plan) => plan.planCode === 'AI_HEALTH_YEARLY') || plans.value.find((plan) => plan.planCode !== 'FREE_CUSTOMER'))
const yearlyPrice = computed(() => (yearlyPlan.value ? (yearlyPlan.value.priceCent / 100).toFixed(0) : '399'))

const openBenefits = [
  { title: 'AI 健康评估', description: '会员期内不限次数', icon: openAssessment },
  { title: 'AI 随访持续优化', description: '智能随访，动态调整', icon: openFollowup },
  { title: '健康拍每日提醒', description: '每天 1 次健康拍', icon: openBroadcast },
  { title: 'AI 健康报告', description: '会员期内不限次数', icon: openReport },
  { title: '趋势档案', description: '长期趋势，可视化', icon: openTrend },
  { title: '吃饭 / 睡眠提醒', description: '会员期内持续使用', icon: openSleep },
]

const rightsRows = computed(() => [
  { name: '健康档案', freeValue: '永久保存、随时查看', memberValue: '永久保存、随时查看' },
  { name: '体检报告管理', freeValue: '支持上传、保存、查看', memberValue: '支持上传、保存、查看' },
  { name: '历史健康数据', freeValue: '永久保存、基础查看', memberValue: '永久保存、完整查看' },
  { name: 'AI 健康评估', freeValue: '3 次', memberValue: '会员期内不限次数' },
  { name: 'AI 健康报告', freeValue: '3 次', memberValue: '会员期内不限次数' },
  { name: 'AI健康助手', freeValue: '3 次对话', memberValue: '会员期内不限次数' },
  { name: '健康树洞', freeValue: '免费体验 7 天', memberValue: '会员期内不限使用' },
  { name: 'AI 初始健康随访', freeValue: '首次 1 次', memberValue: '支持' },
  { name: 'AI 持续健康随访', freeValue: '不支持', memberValue: '会员期内持续使用' },
  { name: '健康拍', freeValue: '3 次', memberValue: '每天 1 次' },
  { name: '健康拍历史记录', freeValue: '查看近 3 天', memberValue: '长期保存、完整查看' },
  { name: '健康趋势分析', freeValue: '查看 3 天基础趋势', memberValue: '长期趋势分析' },
  { name: '吃饭语音提醒', freeValue: '3 次', memberValue: '会员期内持续使用' },
  { name: '睡眠语音提醒', freeValue: '3 次', memberValue: '会员期内持续使用' },
  { name: '价格', freeValue: '¥0', memberValue: `¥${yearlyPrice.value}/年` },
])

const activityItems = [
  { name: '王**', message: '刚刚开通年度健康会员', time: '刚刚' },
  { name: '李**', message: '已解锁会员专属服务', time: '1 分钟前' },
  { name: '周**', message: '已开通年度健康会员', time: '3 分钟前' },
  { name: '陈**', message: '已解锁 AI 健康评估', time: '5 分钟前' },
  { name: '赵**', message: '已开启会员健康随访', time: '8 分钟前' },
  { name: '孙**', message: '已解锁 AI 健康报告', time: '10 分钟前' },
  { name: '王**', message: '刚刚开通年度健康会员', time: '刚刚' },
  { name: '李**', message: '已解锁会员专属服务', time: '1 分钟前' },
  { name: '周**', message: '已开通年度健康会员', time: '3 分钟前' },
]

onShow(async () => {
  loading.value = true
  error.value = ''
  try {
    plans.value = await getMembershipPlans()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '会员方案加载失败'
  } finally {
    loading.value = false
  }
})

const buy = async () => {
  if (buying.value || !yearlyPlan.value) return
  buying.value = true
  try {
    const order = await createMembershipOrder(yearlyPlan.value.planCode)
    uni.navigateTo({ url: `/pages-customer/member/order-result?orderNo=${encodeURIComponent(order.orderNo)}` })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '创建订单失败', icon: 'none' })
  } finally {
    buying.value = false
  }
}
</script>

<style scoped>
.subscribe-page { min-height: 100vh; padding: 24rpx; background: linear-gradient(180deg, #f2fbf8 0%, #fbfcfb 100%); }
.open-hero { position: relative; min-height: 340rpx; padding: 38rpx 34rpx; border-radius: 34rpx; overflow: hidden; color: #fff; background: linear-gradient(135deg, #056952, #13a987 78%, #2cc59f); box-sizing: border-box; }
.open-hero::after { content: ''; position: absolute; right: -80rpx; bottom: -140rpx; width: 360rpx; height: 360rpx; border: 2rpx solid rgba(255,255,255,.22); border-radius: 50%; box-shadow: 0 0 0 24rpx rgba(255,255,255,.09), 0 0 0 52rpx rgba(255,255,255,.05); }
.hero-copy { position: relative; z-index: 1; width: 68%; }
.eyebrow { font-size: 20rpx; font-weight: 700; letter-spacing: 4rpx; opacity: .75; }
.hero-title { margin-top: 22rpx; font-size: 40rpx; font-weight: 800; line-height: 1.2; }
.price-line { display: flex; align-items: baseline; gap: 4rpx; margin-top: 18rpx; color: #fff6d2; font-size: 54rpx; font-weight: 800; }
.price-line text { color: #fff; font-size: 24rpx; font-weight: 500; }
.recommend { display: inline-block; margin-left: 12rpx; padding: 8rpx 16rpx; border-radius: 20rpx; color: #0f8d6f; background: #fff5d1; font-size: 21rpx; font-weight: 700; }
.hero-description { margin-top: 16rpx; color: rgba(255,255,255,.9); font-size: 24rpx; line-height: 1.55; }
.hero-art { position: absolute; z-index: 1; right: 2rpx; bottom: -16rpx; width: 320rpx; height: 295rpx; }
.activity-card { margin-top: 22rpx; padding: 22rpx 24rpx 18rpx; border: 1rpx solid #d5eee6; border-radius: 26rpx; background: linear-gradient(135deg, #effbf7, #fbfffd); box-shadow: 0 8rpx 22rpx rgba(24, 91, 75, .04); }
.activity-head { display: flex; align-items: center; min-height: 44rpx; }
.activity-title { display: flex; align-items: center; color: #0f8066; font-size: 28rpx; font-weight: 800; }
.membership-widget-icon { display: block; flex: none; object-fit: contain; }
.activity-icon { width: 40rpx; height: 40rpx; margin-right: 10rpx; }
.activity-window { height: 58rpx; margin-top: 10rpx; overflow: hidden; }
.activity-track { animation: member-open-ticker 8s linear infinite; }
.activity-item { display: flex; align-items: center; gap: 10rpx; height: 58rpx; color: #315f53; font-size: 23rpx; white-space: nowrap; }
.activity-dot { width: 10rpx; height: 10rpx; flex: none; border-radius: 50%; background: #27b890; box-shadow: 0 0 0 5rpx rgba(39, 184, 144, .12); }
.activity-copy { min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; }
.activity-time { flex: none; margin-left: 8rpx; color: #91aaa3; font-size: 19rpx; }
.comparison-card { margin-top: 22rpx; padding: 28rpx; border-radius: 30rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(24, 91, 75, .05); }
.card-heading { display: flex; align-items: center; gap: 12rpx; color: #153b34; font-size: 31rpx; font-weight: 800; }
.heading-icon { width: 42rpx; height: 42rpx; }
.selected-plan { margin-top: 22rpx; padding: 22rpx 18rpx 18rpx; border: 3rpx solid #13b88e; border-radius: 26rpx; background: linear-gradient(145deg, #f7fffc, #effbf7); }
.selected-header { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.selected-name { display: flex; align-items: center; color: #143b32; font-size: 28rpx; font-weight: 800; }
.selected-name text { margin-left: 8rpx; padding: 5rpx 10rpx; border: 1rpx solid #e7ad58; border-radius: 14rpx; color: #b87411; font-size: 19rpx; font-weight: 500; }
.plan-icon { width: 42rpx; height: 42rpx; margin-right: 10rpx; }
.selected-price { display: flex; align-items: center; color: #0c9273; font-size: 27rpx; font-weight: 800; white-space: nowrap; }
.selected-check { display: flex; align-items: center; justify-content: center; width: 36rpx; height: 36rpx; margin-left: 8rpx; border-radius: 50%; color: #fff; background: #13b88e; font-size: 22rpx; }
.open-benefit-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12rpx; margin-top: 18rpx; }
.open-benefit { min-height: 184rpx; padding: 12rpx 6rpx; border-radius: 20rpx; text-align: center; background: rgba(255,255,255,.84); }
.open-benefit-icon { width: 76rpx; height: 76rpx; }
.open-benefit-title { margin-top: 4rpx; color: #1b3d35; font-size: 22rpx; font-weight: 700; line-height: 1.3; }
.open-benefit-description { margin-top: 6rpx; color: #78908a; font-size: 19rpx; line-height: 1.3; }
.free-plan-row { display: flex; align-items: center; margin-top: 18rpx; padding: 22rpx 16rpx; border-radius: 24rpx; background: #fafafa; }
.free-plan-icon { width: 50rpx; height: 50rpx; margin-right: 14rpx; }
.free-plan-copy { flex: 1; color: #273c37; font-size: 28rpx; font-weight: 700; }
.free-plan-copy text { color: #7e8e89; font-size: 22rpx; font-weight: 400; }
.free-plan-description { margin-top: 6rpx; color: #83918d; font-size: 22rpx; font-weight: 400; }
.arrow { color: #899893; font-size: 40rpx; }
.rights-table { margin-top: 24rpx; overflow: hidden; border: 1rpx solid #dcebe5; border-radius: 20rpx; background: #fff; }
.rights-table-head, .rights-table-row { display: grid; grid-template-columns: 1.05fr 1fr 1fr; }
.rights-table-head { color: #174338; background: #eef9f5; font-size: 26rpx; font-weight: 800; }
.rights-table-head view, .rights-table-row view { display: flex; align-items: center; min-width: 0; padding: 20rpx 12rpx; border-right: 1rpx solid #e2eee9; border-bottom: 1rpx solid #e6efec; line-height: 1.5; word-break: break-all; }
.rights-table-head view:last-child, .rights-table-row view:last-child { border-right: 0; }
.rights-table-row { color: #5d706a; font-size: 24rpx; font-weight: 500; }
.rights-table-row:last-child view { border-bottom: 0; }
.rights-name { color: #23483e; font-weight: 700; }
.rights-member-value { color: #087e62; font-weight: 650; }
.primary-button { height: 88rpx; margin: 26rpx 0 0; border: 0; border-radius: 44rpx; color: #fff; background: linear-gradient(90deg, #5bcf7c, #08a9d4); font-size: 31rpx; font-weight: 700; line-height: 88rpx; }
.primary-button[disabled] { opacity: .65; }
@keyframes member-open-ticker { from { transform: translateY(0); } to { transform: translateY(-50%); } }
@media (prefers-reduced-motion: reduce) { .activity-track { animation: none; } }
@media (max-width: 360px) {
  .open-benefit-title { font-size: 20rpx; }
  .open-benefit-description { font-size: 17rpx; }
  .selected-name, .selected-price { font-size: 24rpx; }
  .rights-table-head { font-size: 24rpx; }
  .rights-table-row { font-size: 22rpx; }
  .rights-table-head view, .rights-table-row view { padding: 18rpx 9rpx; line-height: 1.45; }
}
</style>

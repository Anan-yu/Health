<template>
  <view class="page legendary-page">
    <view v-if="!goldBeanEnabled" class="disabled-card">
      <view class="disabled-title">金豆会员暂未开放</view>
      <view class="disabled-copy">该功能仅用于开发环境验收，正式版本不会启用。</view>
    </view>

    <PageState v-else :loading="loading" :error="error" :empty="!summary">
      <view v-if="summary" class="legendary-content">
        <view class="legendary-hero">
          <image class="hero-wave" :src="heroWave" mode="scaleToFill" aria-hidden="true" />
          <view class="hero-glow" aria-hidden="true" />
          <view class="hero-art" aria-hidden="true">
            <view class="hero-art-ring" />
            <image class="hero-emblem" :src="heroEmblem" mode="aspectFit" />
          </view>

          <view class="hero-copy">
            <view class="legendary-eyebrow">LEGENDARY CLUB</view>
            <view class="legendary-title">传奇人物俱乐部</view>
            <view class="legendary-slogan">— 以顶尖医疗科技 · 守护生命的更多可能</view>
            <view class="legendary-balance-label">数字银行余额</view>
            <view class="legendary-balance-row">
            <view class="legendary-balance">{{ formatGoldBeanBalance(summary.digitalBankBalance) }}</view>
              <view class="legendary-balance-mark">›</view>
            </view>
            <view class="legendary-unit">金豆 · ￥1.00 / 个</view>
          </view>

          <view class="hero-motto">更健康的今天<br />成就更传奇的未来</view>
        </view>

        <view class="market-entry-card" role="button" aria-label="进入金豆集市" @tap="openMarket">
          <view class="market-entry-icon" aria-hidden="true">
            <image :src="marketIcon" mode="aspectFit" />
          </view>
          <view class="market-entry-copy">
            <view class="market-entry-title">金豆集市</view>
            <view class="market-entry-note">使用金豆兑换全国优质医疗健康服务</view>
          </view>
          <image class="market-entry-arrow" :src="chevronIcon" mode="aspectFit" aria-hidden="true" />
        </view>

        <view class="service-heading">
          <view class="service-heading-main">
            <view class="service-title-row">
              <view class="service-heading-line" />
              <view class="service-heading-title">专属服务</view>
            </view>
            <view class="service-heading-subtitle">七大核心服务 · 全方位守护您的健康</view>
          </view>
          <view class="service-heading-note">专属的医疗<br />更长久的陪伴</view>
        </view>

        <view class="service-grid" aria-label="七项专属服务展示">
          <view
            v-for="service in serviceItems"
            :key="service.index"
            class="service-card"
            :class="{ 'service-card-wide': service.wide }"
          >
            <view class="service-icon" aria-hidden="true">
              <image :src="service.icon" mode="aspectFit" />
            </view>
            <view class="service-copy">
              <view class="service-name"><text class="service-index">{{ service.index }}.</text>{{ service.title }}</view>
              <view class="service-description">{{ service.description }}</view>
              <image v-if="service.future" class="future-badge" :src="futureBadge" mode="aspectFit" aria-hidden="true" />
            </view>
          </view>
        </view>

        <view class="vision-banner">
          <image class="vision-wave" :src="heroWave" mode="scaleToFill" aria-hidden="true" />
          <view class="vision-copy">
            <view class="vision-title">顶尖医疗 · 致敬生命</view>
            <view class="vision-note">让每一位传奇人物，活出更健康的未来</view>
          </view>
          <view class="vision-mark" aria-hidden="true">
            <image :src="goldBeanIcon" mode="aspectFit" />
          </view>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PageState from '@/components/PageState.vue'
import { goldBeanEnabled } from '@/constants/features'
import { formatGoldBeanBalance } from '@/utils/gold-bean-amount'
import {
  getGoldBeanLegendarySummary,
  type GoldBeanLegendarySummary,
} from '@/api/gold-bean'
import heroEmblem from '@/assets/ui/legendary-club/01_legendary_club_emblem.svg'
import marketIcon from '@/assets/ui/legendary-club/02_gold_bean_market.svg'
import chevronIcon from '@/assets/ui/legendary-club/10_chevron_right.svg'
import stethoscopeIcon from '@/assets/ui/legendary-club/03_stethoscope.svg'
import cancerScreeningIcon from '@/assets/ui/legendary-club/04_cancer_screening.svg'
import medicalTeamIcon from '@/assets/ui/legendary-club/05_medical_team_ai.svg'
import nutritionIcon from '@/assets/ui/legendary-club/06_nutrition_leaf.svg'
import lifestyleIcon from '@/assets/ui/legendary-club/07_lifestyle_running.svg'
import personalizedDrugIcon from '@/assets/ui/legendary-club/08_personalized_drug.svg'
import organTransplantIcon from '@/assets/ui/legendary-club/09_organ_transplant.svg'
import goldBeanIcon from '@/assets/ui/legendary-club/15_gold_bean.svg'
import heroWave from '@/assets/ui/legendary-club/17_hero_wave_pattern.svg'
import futureBadge from '@/assets/ui/legendary-club/18_future_service_badge.svg'

const serviceItems = [
  { index: 1, icon: stethoscopeIcon, title: '慢性病全身检查与治疗', description: '全面评估 · 系统干预' },
  { index: 2, icon: cancerScreeningIcon, title: '癌症全面筛查', description: '早筛预警 · 风险评估' },
  { index: 3, icon: medicalTeamIcon, title: '国家三甲医院医生团队 + AI医疗团队服务', description: '专家会诊 · AI辅助分析' },
  { index: 4, icon: nutritionIcon, title: '私人一对一营养素补充方案', description: '精准营养 · 个性定制' },
  { index: 5, icon: lifestyleIcon, title: '个性化生活饮食运动习惯指导', description: '长期陪伴 · 习惯优化' },
  { index: 6, icon: personalizedDrugIcon, title: '药物一对一生产治疗（2032年）', description: '前沿精准治疗', future: true },
  { index: 7, icon: organTransplantIcon, title: '器官移植治疗（2032年）', description: '高端医疗支持', future: true, wide: true },
]

const loading = ref(true)
const error = ref('')
const summary = ref<GoldBeanLegendarySummary>()

async function load() {
  if (!goldBeanEnabled) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    summary.value = await getGoldBeanLegendarySummary()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '传奇俱乐部加载失败'
  } finally {
    loading.value = false
  }
}

defineExpose({ refresh: load })

function openMarket() {
  uni.navigateTo({ url: '/pages-customer/gold-bean-market/index' })
}
</script>

<style scoped>
.legendary-page {
  min-height: 100vh;
  padding: 20rpx 24rpx calc(56rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: linear-gradient(180deg, #f1faf6 0%, #f8fbfa 55%, #edf8f4 100%);
}

.legendary-content { padding-bottom: 20rpx; }

.legendary-hero {
  position: relative;
  min-height: 390rpx;
  overflow: hidden;
  padding: 38rpx 30rpx 28rpx;
  border-radius: 32rpx;
  box-sizing: border-box;
  color: #f9fff9;
  background: linear-gradient(135deg, #0b5d4e 0%, #0d725d 52%, #0f8a6d 100%);
  box-shadow: 0 20rpx 44rpx rgba(8, 91, 70, 0.2);
}

.hero-wave,
.vision-wave {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 62%;
  opacity: 0.72;
  pointer-events: none;
}

.hero-glow {
  position: absolute;
  right: 48rpx;
  bottom: 30rpx;
  width: 270rpx;
  height: 270rpx;
  border: 1rpx solid rgba(236, 248, 224, 0.14);
  border-radius: 50%;
  box-shadow: 0 0 0 28rpx rgba(236, 248, 224, 0.04), 0 0 0 56rpx rgba(236, 248, 224, 0.025);
}

.hero-art {
  position: absolute;
  right: 48rpx;
  bottom: 30rpx;
  z-index: 1;
  width: 270rpx;
  height: 270rpx;
}

.hero-art-ring {
  position: absolute;
  top: 20rpx;
  right: 18rpx;
  bottom: 20rpx;
  left: 18rpx;
  border: 1rpx solid rgba(215, 180, 106, 0.38);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(221, 246, 226, 0.12) 0%, rgba(221, 246, 226, 0) 68%);
}

.hero-emblem {
  position: absolute;
  top: 25rpx;
  right: 25rpx;
  width: 220rpx;
  height: 220rpx;
}

.hero-copy {
  position: relative;
  z-index: 2;
  width: 64%;
}

.legendary-eyebrow {
  color: rgba(255, 255, 255, 0.68);
  font-size: 18rpx;
  font-weight: 750;
  letter-spacing: 4rpx;
}

.legendary-title {
  margin-top: 12rpx;
  color: #fff9e8;
  font-family: 'STSong', 'Songti SC', 'Noto Serif CJK SC', serif;
  font-size: 42rpx;
  font-weight: 800;
  line-height: 1.18;
  white-space: nowrap;
}

.legendary-slogan {
  margin-top: 12rpx;
  color: rgba(247, 255, 245, 0.86);
  font-size: 20rpx;
  line-height: 1.5;
  white-space: nowrap;
}

.legendary-balance-label {
  margin-top: 30rpx;
  color: rgba(244, 255, 247, 0.72);
  font-size: 21rpx;
}

.legendary-balance-row { display: flex; align-items: center; gap: 14rpx; }

.legendary-balance {
  margin-top: 2rpx;
  color: #fffdf1;
  font-size: 74rpx;
  font-weight: 820;
  line-height: 1;
}

.legendary-balance-mark {
  margin-top: 12rpx;
  color: #fff6d7;
  font-size: 56rpx;
  font-weight: 300;
  line-height: 1;
}

.legendary-unit {
  margin-top: 8rpx;
  color: rgba(244, 255, 247, 0.82);
  font-size: 21rpx;
}

.hero-motto {
  position: absolute;
  top: 34rpx;
  right: 28rpx;
  z-index: 2;
  color: rgba(255, 246, 216, 0.86);
  font-family: 'STSong', 'Songti SC', serif;
  font-size: 19rpx;
  line-height: 1.7;
  text-align: right;
}

.market-entry-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 146rpx;
  margin-top: 20rpx;
  padding: 22rpx 20rpx 22rpx 24rpx;
  border: 1rpx solid #dceee8;
  border-radius: 30rpx;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12rpx 28rpx rgba(29, 100, 81, 0.07);
}

.market-entry-card:active { opacity: 0.78; }

.market-entry-icon {
  display: flex;
  flex: 0 0 76rpx;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: #dff7f0;
}

.market-entry-icon image { width: 58rpx; height: 58rpx; }

.market-entry-copy { flex: 1; min-width: 0; }

.market-entry-title {
  color: #163d36;
  font-size: 31rpx;
  font-weight: 800;
  line-height: 1.25;
}

.market-entry-note {
  margin-top: 8rpx;
  color: #7b918b;
  font-size: 21rpx;
  line-height: 1.4;
}

.market-entry-arrow { flex: 0 0 34rpx; width: 34rpx; height: 34rpx; margin-left: 8rpx; }

.service-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20rpx;
  margin: 38rpx 6rpx 20rpx;
}

.service-heading-main { min-width: 0; }

.service-title-row { display: flex; align-items: center; gap: 14rpx; }

.service-heading-line {
  width: 8rpx;
  height: 38rpx;
  border-radius: 8rpx;
  background: linear-gradient(180deg, #0f8a6d, #34bd97);
}

.service-heading-title {
  color: #153b34;
  font-family: 'STSong', 'Songti SC', 'Noto Serif CJK SC', serif;
  font-size: 36rpx;
  font-weight: 800;
  line-height: 1.2;
}

.service-heading-subtitle {
  margin-top: 8rpx;
  color: #809891;
  font-size: 21rpx;
  line-height: 1.4;
}

.service-heading-note {
  flex: none;
  color: #809891;
  font-family: 'STSong', 'Songti SC', serif;
  font-size: 20rpx;
  line-height: 1.7;
  text-align: right;
}

.service-grid { display: flex; flex-wrap: wrap; gap: 18rpx; }

.service-card {
  display: flex;
  flex: 0 0 calc((100% - 18rpx) / 2);
  align-items: center;
  min-height: 182rpx;
  padding: 20rpx 16rpx;
  border: 1rpx solid #e0eee9;
  border-radius: 28rpx;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 10rpx 26rpx rgba(24, 91, 75, 0.05);
}

.service-card-wide { flex-basis: 100%; min-height: 154rpx; padding-right: 28rpx; }

.service-icon {
  display: flex;
  flex: 0 0 78rpx;
  align-items: center;
  justify-content: center;
  width: 78rpx;
  height: 78rpx;
  border-radius: 50%;
  background: #e1f8f0;
}

.service-icon image { width: 58rpx; height: 58rpx; }

.service-copy { flex: 1; min-width: 0; margin-left: 14rpx; }

.service-name {
  color: #1d3933;
  font-size: 25rpx;
  font-weight: 800;
  line-height: 1.42;
}

.service-index { margin-right: 4rpx; color: #173d35; }

.service-description {
  margin-top: 6rpx;
  color: #849992;
  font-size: 20rpx;
  line-height: 1.45;
}

.future-badge { width: 92rpx; height: 32rpx; margin-top: 9rpx; }

.vision-banner {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 148rpx;
  margin-top: 24rpx;
  overflow: hidden;
  padding: 24rpx 28rpx;
  border-radius: 30rpx;
  box-sizing: border-box;
  background: linear-gradient(135deg, #e4f3f1 0%, #f8fbf6 52%, #e4f0e7 100%);
}

.vision-banner::after {
  position: absolute;
  top: -70rpx;
  right: -48rpx;
  width: 230rpx;
  height: 230rpx;
  border: 1rpx solid rgba(15, 138, 109, 0.12);
  border-radius: 50%;
  content: '';
}

.vision-copy { position: relative; z-index: 1; }

.vision-title {
  color: #0b5d4e;
  font-family: 'STSong', 'Songti SC', 'Noto Serif CJK SC', serif;
  font-size: 31rpx;
  font-weight: 800;
}

.vision-note {
  margin-top: 9rpx;
  color: #70938a;
  font-size: 20rpx;
  line-height: 1.4;
}

.vision-mark {
  position: absolute;
  right: 56rpx;
  bottom: 20rpx;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 76rpx;
  border: 1rpx solid rgba(15, 138, 109, 0.16);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.52);
}

.vision-mark image { width: 48rpx; height: 48rpx; }

.disabled-card {
  margin-top: 30rpx;
  padding: 40rpx 28rpx;
  border: 1rpx solid #dcebe6;
  border-radius: 28rpx;
  text-align: center;
  background: #fff;
}

.disabled-title { color: #244a40; font-size: 34rpx; font-weight: 800; }
.disabled-copy { margin-top: 12rpx; color: #7f938c; font-size: 24rpx; line-height: 1.55; }

@media (max-width: 360px) {
  .legendary-title { font-size: 38rpx; }
  .legendary-slogan { font-size: 18rpx; }
  .hero-art { right: 28rpx; width: 245rpx; height: 245rpx; }
  .hero-glow { right: 28rpx; width: 245rpx; height: 245rpx; }
  .hero-emblem { top: 23rpx; right: 23rpx; width: 198rpx; height: 198rpx; }
  .service-card { padding-right: 12rpx; padding-left: 12rpx; }
  .service-icon { flex-basis: 68rpx; width: 68rpx; height: 68rpx; }
  .service-icon image { width: 51rpx; height: 51rpx; }
  .service-copy { margin-left: 10rpx; }
  .service-name { font-size: 23rpx; }
}
</style>

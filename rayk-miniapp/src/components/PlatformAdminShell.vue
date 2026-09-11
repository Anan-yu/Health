<template>
  <view class="platform-admin-shell">
    <view v-if="enabled" class="desktop-admin-sidebar">
      <view class="sidebar-brand">
        <view class="brand-mark">三</view>
        <view class="brand-copy">
          <view class="brand-name">三羊健康</view>
          <view class="brand-caption">平台运营管理台</view>
        </view>
      </view>

      <view class="sidebar-intro">
        <view class="sidebar-eyebrow">OPERATIONS DESK</view>
        <view class="sidebar-heading">把平台工作放在一处</view>
      </view>

      <view class="desktop-admin-nav" aria-label="平台管理导航">
        <view v-for="section in navSections" :key="section.title" class="nav-section">
          <view class="nav-section-title">{{ section.title }}</view>
          <button
            v-for="item in section.items"
            :key="item.route"
            type="button"
            class="nav-item"
            :class="{ active: isActive(item.route) }"
            :aria-current="isActive(item.route) ? 'page' : undefined"
            :aria-label="`${item.title}：${item.description}`"
            @click="openItem(item.route)"
          >
            <view class="nav-symbol" aria-hidden="true">{{ item.icon }}</view>
            <view class="nav-label">{{ item.title }}</view>
            <view v-if="isActive(item.route)" class="nav-active-mark" aria-hidden="true" />
          </button>
        </view>
      </view>

      <view class="sidebar-bottom">
        <view class="service-status">
          <view class="status-dot" aria-hidden="true" />
          <view>
            <view class="service-title">服务正常</view>
            <view class="service-copy">平台权限已校验</view>
          </view>
        </view>
        <view class="sidebar-user">
          <view class="user-avatar">{{ userInitial }}</view>
          <view class="user-copy">
            <view class="user-name">{{ userName }}</view>
            <view class="user-role">平台管理员</view>
          </view>
        </view>
      </view>
    </view>

    <view class="desktop-admin-main">
      <view v-if="enabled" class="desktop-admin-topbar">
        <view class="topbar-context">
          <view class="topbar-kicker">三羊健康 / 运营台</view>
          <view class="topbar-title">{{ activeTitle }}</view>
        </view>
        <view class="topbar-actions">
          <view class="topbar-user">{{ userName }}</view>
          <view class="topbar-role">管理员</view>
          <button type="button" class="logout-button" @click="signOut">退出登录</button>
        </view>
      </view>
      <view class="desktop-admin-content">
        <slot />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
/* global getCurrentPages */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { menusFor, type MenuItem } from '@/constants/menus'
import { useAuthStore } from '@/stores/auth'

withDefaults(defineProps<{ enabled?: boolean }>(), { enabled: true })

const auth = useAuthStore()
type AdminNavItem = MenuItem & { group: string }

const additionalItems: AdminNavItem[] = [
  {
    title: '运营总览',
    description: '查看平台核心指标与待办入口',
    icon: '总',
    route: '/pages/workbench/index',
    group: '总览',
  },
  {
    title: 'AI模型管理',
    description: '切换健康评估使用的模型与思考模式',
    icon: '模',
    route: '/pages-platform/model/index',
    permission: 'platform:tenant:list',
    group: '系统与服务',
  },
]

const navItems = computed<AdminNavItem[]>(() => {
  const platformItems = menusFor('PLATFORM_ADMIN').map((item) => ({
    ...item,
    group: item.title === '反馈中心' ? '系统与服务' : '业务管理',
  }))
  return [...additionalItems.slice(0, 1), ...platformItems, ...additionalItems.slice(1)].filter(
    (item) => !item.permission || auth.permissions.includes(item.permission),
  )
})

const navSections = computed(() => {
  const order = ['总览', '业务管理', '系统与服务']
  return order
    .map((title) => ({ title, items: navItems.value.filter((item) => item.group === title) }))
    .filter((section) => section.items.length > 0)
})

const currentRoute = ref('')
const readCurrentRoute = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return current?.route ? `/${current.route}` : ''
}

const isActive = (route: string) => {
  const current = currentRoute.value
  if (route === '/pages-tenant/dashboard/index') {
    return current.startsWith('/pages-tenant/dashboard')
  }
  return current === route
}

const activeTitle = computed(
  () => navItems.value.find((item) => isActive(item.route))?.title || '运营总览',
)
const userName = computed(() => auth.user?.displayName?.trim() || '平台管理员')
const userInitial = computed(() => userName.value.slice(0, 1) || '管')

const tabRoutes = new Set([
  '/pages/home/index',
  '/pages/workbench/index',
  '/pages/club/index',
  '/pages/mine/index',
  '/pages/message/index',
])
const openItem = (route: string) => {
  if (tabRoutes.has(route)) {
    uni.switchTab({ url: route })
    return
  }
  uni.navigateTo({ url: route })
}

const signOut = () => {
  uni.showModal({
    title: '退出管理台',
    content: '退出后需要重新登录才能继续管理平台数据。',
    confirmText: '退出登录',
    cancelText: '继续使用',
    success: async ({ confirm }) => {
      if (!confirm) return
      await auth.signOut()
      uni.reLaunch({ url: '/pages/login/index' })
    },
  })
}

const isDesktopH5 = () => {
  let desktop = false
  // #ifdef H5
  desktop = uni.getSystemInfoSync().windowWidth >= 1024
  // #endif
  return desktop
}

onMounted(() => {
  currentRoute.value = readCurrentRoute()
  if (isDesktopH5()) {
    uni.hideTabBar({ animation: false })
  }
})

onUnmounted(() => {
  if (isDesktopH5()) {
    uni.showTabBar({ animation: false })
  }
})
</script>

<style scoped>
.platform-admin-shell {
  --admin-ink: #203b34;
  --admin-muted: #71827b;
  --admin-line: #dcebe5;
  --admin-brand: #0f7a62;
  --admin-rail: #123f35;
  min-height: 100vh;
}

.desktop-admin-sidebar,
.desktop-admin-topbar {
  display: none;
}

.desktop-admin-main {
  min-height: inherit;
}

@media screen and (min-width: 1024px) {
  .platform-admin-shell {
    display: flex;
    min-height: 100vh;
    background: #f5faf8;
  }

  .desktop-admin-sidebar {
    position: sticky;
    top: 0;
    z-index: 30;
    display: flex;
    flex: 0 0 248px;
    flex-direction: column;
    width: 248px;
    height: 100vh;
    box-sizing: border-box;
    padding: 28px 16px 20px;
    overflow-y: auto;
    background:
      radial-gradient(circle at 100% 6%, rgba(82, 177, 147, 0.2), transparent 32%),
      linear-gradient(180deg, #164b3f 0%, var(--admin-rail) 100%);
    color: #dcefe8;
  }

  .sidebar-brand,
  .sidebar-user,
  .service-status,
  .topbar-actions {
    display: flex;
    align-items: center;
  }

  .sidebar-brand {
    padding: 0 12px;
  }

  .brand-mark {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    flex: 0 0 40px;
    border: 1px solid rgba(218, 246, 235, 0.3);
    border-radius: 13px;
    background: rgba(217, 247, 237, 0.14);
    color: #f2fff9;
    font-size: 20px;
    font-weight: 760;
  }

  .brand-copy {
    min-width: 0;
    margin-left: 11px;
  }

  .brand-name {
    color: #f5fffb;
    font-size: 16px;
    font-weight: 720;
    letter-spacing: 0.5px;
  }

  .brand-caption {
    margin-top: 3px;
    color: rgba(220, 239, 232, 0.65);
    font-size: 11px;
  }

  .sidebar-intro {
    margin: 48px 12px 20px;
  }

  .sidebar-eyebrow,
  .topbar-kicker,
  .nav-section-title {
    color: rgba(211, 237, 228, 0.58);
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 1.5px;
  }

  .sidebar-heading {
    max-width: 160px;
    margin-top: 9px;
    color: #f2fff9;
    font-size: 17px;
    line-height: 1.45;
    font-weight: 650;
  }

  .desktop-admin-nav {
    flex: 1;
  }

  .nav-section {
    margin-bottom: 22px;
  }

  .nav-section-title {
    margin: 0 12px 8px;
    color: rgba(211, 237, 228, 0.48);
    letter-spacing: 1px;
  }

  .nav-item {
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
    min-height: 44px;
    margin: 3px 0;
    padding: 0 12px;
    border: 0;
    border-radius: 12px;
    box-sizing: border-box;
    background: transparent;
    color: rgba(225, 244, 237, 0.76);
    text-align: left;
    cursor: pointer;
    transition: background-color 180ms ease, color 180ms ease, transform 180ms ease;
  }

  .nav-item::after {
    border: 0;
  }

  .nav-item:hover {
    background: rgba(220, 250, 239, 0.09);
    color: #fff;
  }

  .nav-item:focus-visible,
  .logout-button:focus-visible {
    outline: 3px solid rgba(152, 235, 205, 0.9);
    outline-offset: 2px;
  }

  .nav-item:active {
    transform: translateY(1px);
  }

  .nav-item.active {
    background: linear-gradient(90deg, rgba(143, 231, 200, 0.2), rgba(143, 231, 200, 0.07));
    color: #f5fffb;
  }

  .nav-symbol {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    flex: 0 0 26px;
    border: 1px solid rgba(190, 237, 219, 0.2);
    border-radius: 8px;
    background: rgba(232, 253, 244, 0.08);
    color: #aee8d3;
    font-size: 12px;
    font-weight: 700;
  }

  .nav-item.active .nav-symbol {
    border-color: rgba(190, 247, 222, 0.42);
    background: #b6eed8;
    color: #174d3f;
  }

  .nav-label {
    min-width: 0;
    margin-left: 11px;
    overflow: hidden;
    font-size: 14px;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .nav-active-mark {
    width: 4px;
    height: 20px;
    margin-left: auto;
    border-radius: 999px;
    background: #b6eed8;
  }

  .sidebar-bottom {
    padding: 14px 4px 0;
    border-top: 1px solid rgba(221, 246, 237, 0.12);
  }

  .service-status {
    padding: 10px 9px;
    border-radius: 12px;
    background: rgba(225, 250, 240, 0.08);
  }

  .status-dot {
    width: 8px;
    height: 8px;
    flex: 0 0 8px;
    margin: 0 10px 0 2px;
    border-radius: 50%;
    background: #73e0b6;
    box-shadow: 0 0 0 4px rgba(115, 224, 182, 0.12);
  }

  .service-title {
    color: #e7fff4;
    font-size: 12px;
    font-weight: 650;
  }

  .service-copy,
  .user-role {
    margin-top: 2px;
    color: rgba(220, 239, 232, 0.55);
    font-size: 10px;
  }

  .sidebar-user {
    margin-top: 16px;
    padding: 0 9px;
  }

  .user-avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    flex: 0 0 32px;
    border-radius: 50%;
    background: #d5f5e8;
    color: #1d644f;
    font-size: 14px;
    font-weight: 750;
  }

  .user-copy {
    min-width: 0;
    margin-left: 10px;
  }

  .user-name {
    overflow: hidden;
    color: #ecfff7;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .desktop-admin-main {
    flex: 1;
    min-width: 0;
    background:
      radial-gradient(circle at 94% 0%, rgba(177, 234, 215, 0.38), transparent 22%),
      #f5faf8;
  }

  .desktop-admin-topbar {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 74px;
    box-sizing: border-box;
    padding: 14px clamp(28px, 4vw, 56px);
    border-bottom: 1px solid rgba(211, 230, 222, 0.8);
    background: rgba(248, 252, 250, 0.86);
    backdrop-filter: blur(16px);
  }

  .topbar-kicker {
    color: #7d938a;
    letter-spacing: 1.2px;
  }

  .topbar-title {
    margin-top: 4px;
    color: var(--admin-ink);
    font-size: 20px;
    font-weight: 720;
  }

  .topbar-actions {
    gap: 12px;
  }

  .topbar-user {
    color: #36584e;
    font-size: 13px;
    font-weight: 650;
  }

  .topbar-role {
    padding: 5px 9px;
    border: 1px solid #cce7dc;
    border-radius: 999px;
    color: #0c745b;
    background: #e8f8f1;
    font-size: 11px;
  }

  .logout-button {
    min-height: 36px;
    margin: 0;
    padding: 0 13px;
    border: 1px solid #d4e5de;
    border-radius: 9px;
    color: #526d63;
    background: #fff;
    font-size: 12px;
    cursor: pointer;
  }

  .logout-button::after {
    border: 0;
  }

  .desktop-admin-content {
    width: 100%;
    max-width: 1480px;
    box-sizing: border-box;
    margin: 0 auto;
    padding: 32px clamp(28px, 4vw, 56px) 56px;
  }

  /* Normalize mobile rpx sizing into a comfortable desktop data density. */
  :deep(.page) {
    width: 100% !important;
    max-width: none !important;
    min-height: auto !important;
    margin: 0 !important;
    padding: 0 !important;
    font-size: 15px;
  }

  :deep(.page-heading) {
    margin-bottom: 24px !important;
  }

  :deep(.title),
  :deep(.hero-title) {
    font-size: 30px !important;
    line-height: 1.25 !important;
  }

  :deep(.subtitle),
  :deep(.hero-copy),
  :deep(.hero-note) {
    font-size: 14px !important;
    line-height: 1.6 !important;
  }

  :deep(.eyebrow),
  :deep(.hero-eyebrow) {
    font-size: 11px !important;
    letter-spacing: 1.6px !important;
  }

  :deep(.card) {
    margin-bottom: 16px !important;
    padding: 22px 24px 24px !important;
    border-radius: 16px !important;
    box-shadow: 0 8px 24px rgba(27, 83, 67, 0.06) !important;
  }

  :deep(.section-title),
  :deep(.card-title) {
    font-size: 18px !important;
  }

  :deep(.section-label),
  :deep(.muted),
  :deep(.card-subtitle),
  :deep(.field-helper),
  :deep(.field-label),
  :deep(.meta-label),
  :deep(.metric-note) {
    font-size: 13px !important;
  }

  :deep(input),
  :deep(textarea),
  :deep(.input),
  :deep(.textarea) {
    font-size: 14px !important;
  }

  :deep(button),
  :deep(.primary-button),
  :deep(.action-button) {
    min-height: 44px !important;
    font-size: 14px !important;
  }

  :deep(.metric-grid),
  :deep(.summary-grid),
  :deep(.menu-grid) {
    gap: 16px !important;
  }

  :deep(.menu-grid) {
    grid-template-columns: repeat(3, minmax(0, 1fr)) !important;
  }

  :deep(.menu-card) {
    min-height: 190px !important;
    padding: 22px !important;
    border-radius: 16px !important;
  }

  :deep(.menu-title) {
    margin-top: 16px !important;
    font-size: 16px !important;
  }

  :deep(.menu-copy) {
    font-size: 13px !important;
  }

  :deep(.support-center) {
    padding-top: 0 !important;
  }

  :deep(.support-hero),
  :deep(.membership-hero),
  :deep(.model-hero),
  :deep(.admin-hero),
  :deep(.gold-bean-hero),
  :deep(.hero) {
    border-radius: 18px !important;
  }

  :deep(.support-hero),
  :deep(.membership-hero),
  :deep(.model-hero),
  :deep(.admin-hero),
  :deep(.gold-bean-hero) {
    padding: 26px 28px !important;
  }

  :deep(.workbench-hero) {
    padding: 28px 32px !important;
    border-radius: 20px !important;
  }

  :deep(.workbench-page .hero-title) {
    font-size: 30px !important;
  }

  :deep(.workbench-page .hero-copy) {
    font-size: 14px !important;
  }

  :deep(.hero-stats) {
    margin-top: 24px !important;
    padding-top: 20px !important;
  }
}

@media screen and (min-width: 1024px) and (max-width: 1199px) {
  .desktop-admin-sidebar {
    flex-basis: 224px;
    width: 224px;
  }

  :deep(.menu-grid) {
    grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
  }
}

@media (prefers-reduced-motion: reduce) {
  .nav-item {
    transition: none;
  }
}
</style>

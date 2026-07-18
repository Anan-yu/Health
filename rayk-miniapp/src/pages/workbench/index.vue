<template>
  <view class="page"
    ><view class="card"
      ><view class="title">{{ auth.currentWorkbench || '工作台' }}</view
      ><view class="subtitle">功能入口随角色、权限和当前工作台变化。</view></view
    ><view
      v-for="item in visibleMenus"
      :key="item.route"
      class="card menu"
      @click="open(item.route)"
      ><view
        ><view class="menu-title">{{ item.title }}</view
        ><view class="muted">{{ item.description }}</view></view
      ><text>›</text></view
    ></view
  >
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { menusFor } from '@/constants/menus'
const auth = useAuthStore()
const visibleMenus = computed(() =>
  menusFor(auth.currentWorkbench).filter(
    (item) => !item.permission || auth.permissions.includes(item.permission),
  ),
)
const open = (url: string) => uni.navigateTo({ url })
</script>
<style scoped>
.menu {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.menu-title {
  font-size: 30rpx;
  font-weight: 650;
  margin-bottom: 8rpx;
}
</style>

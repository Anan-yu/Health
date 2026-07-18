<template>
  <text class="tag" :class="status.toLowerCase()">{{ status }}</text>
</template>
<script setup lang="ts">
defineProps<{ status: string }>()
</script>
<style scoped>
.tag {
  display: inline-block;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #edf2f7;
  color: #52606d;
  font-size: 22rpx;
}
.published,
.success,
.completed,
.approved {
  background: #e5f8ef;
  color: #16794b;
}
.failed,
.rejected,
.high {
  background: #ffebeb;
  color: #b42318;
}
.attention,
.reviewing,
.waiting_review {
  background: #fff3d6;
  color: #8a5700;
}
</style>

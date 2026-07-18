<template>
  <view class="page"
    ><view class="title">历史指标趋势</view
    ><view class="card"
      ><view class="section-title">空腹血糖（演示）</view
      ><view class="chart"
        ><view v-for="item in values" :key="item.date" class="column"
          ><view class="bar" :style="{ height: `${item.value * 18}rpx` }"></view
          ><text>{{ item.value }}</text
          ><text class="muted">{{ item.date }}</text></view
        ></view
      ><view class="subtitle">图表组件边界已预留，正式阶段接入 ECharts 按需加载。</view></view
    >
  </view>
</template>
<script setup lang="ts">
const values = [
  { date: '04月', value: 5.5 },
  { date: '05月', value: 5.8 },
  { date: '07月', value: 6.2 },
]
</script>
<style scoped>
.chart {
  height: 300rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
}
.column {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  gap: 8rpx;
}
.bar {
  width: 48rpx;
  background: #68b69f;
  border-radius: 12rpx 12rpx 0 0;
}
</style>

<template>
  <view class="stats">
    <!-- 周/月趋势图占位：lime-echart 集成后替换 -->
    <view class="card trend-card">
      <view class="card-title">摄入趋势</view>
      <view class="chart-placeholder">（折线图占位：lime-echart 集成后替换）</view>
    </view>

    <!-- 达成率卡片 -->
    <view class="card achievement-card">
      <view class="card-title">达成率</view>
      <!-- TODO: 渲染 statsApi.achievement(range) -->
    </view>

    <!-- 周/月切换 -->
    <view class="range-switch">
      <u-button size="mini" @click="switchRange(7)">近 7 天</u-button>
      <u-button size="mini" @click="switchRange(30)">近 30 天</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";

const range = ref(7);

function switchRange(n: number): void {
  range.value = n;
  // TODO: 重新拉 statsApi.weekly(daysAgo(n)) 或 monthly(daysAgo(n))
}
</script>

<style scoped>
.stats {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
.chart-placeholder {
  height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 24rpx;
}
.range-switch {
  display: flex;
  gap: 16rpx;
}
</style>

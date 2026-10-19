<template>
  <view>
    <view class="legend">
      <view class="lg"><view class="lg-dot carbo"></view>碳水</view>
      <view class="lg"><view class="lg-dot protein"></view>蛋白</view>
      <view class="lg"><view class="lg-dot fat"></view>脂肪</view>
    </view>
    <view class="bars" v-if="data.length">
      <view class="bar-col" v-for="(d, i) in data" :key="i">
        <view class="bar-track">
          <view class="seg carbo" :style="{ height: pct(d.carbo, d) + '%' }"></view>
          <view class="seg protein" :style="{ height: pct(d.protein, d) + '%' }"></view>
          <view class="seg fat" :style="{ height: pct(d.fat, d) + '%' }"></view>
        </view>
        <text class="bar-label">{{ d.label }}</text>
      </view>
    </view>
    <view v-else class="empty">暂无数据</view>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";

export interface PfcDay {
  label: string;
  carbo: number;
  protein: number;
  fat: number;
}

/** 每日 PFC 堆叠柱状（纯 view 实现），单位 g */
const props = withDefaults(defineProps<{ data?: PfcDay[] }>(), { data: () => [] });

const maxTotal = computed(() =>
  Math.max(...props.data.map((d) => d.carbo + d.protein + d.fat), 1)
);

function pct(v: number, d: PfcDay): number {
  return Math.round((v / maxTotal.value) * 100);
}
</script>

<style scoped>
.legend {
  display: flex;
  gap: 24rpx;
  margin-bottom: 12rpx;
}
.lg {
  display: flex;
  align-items: center;
  font-size: 20rpx;
  color: #4e5561;
}
.lg-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 4rpx;
  margin-right: 8rpx;
}
.carbo {
  background: #5b8ff9;
}
.protein {
  background: #ff7a45;
}
.fat {
  background: #f5c518;
}
.bars {
  display: flex;
  align-items: flex-end;
  height: 260rpx;
}
.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.bar-track {
  /* 列宽自适应：7 天时封顶 40rpx，30 天时自动变窄 */
  width: 60%;
  max-width: 40rpx;
  height: 210rpx;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  border-radius: 6rpx;
  overflow: hidden;
}
.seg {
  width: 100%;
}
.bar-label {
  margin-top: 8rpx;
  font-size: 18rpx;
  color: #8a919e;
}
.empty {
  padding: 60rpx 0;
  text-align: center;
  color: #8a919e;
  font-size: 22rpx;
}
</style>

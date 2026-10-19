<template>
  <view class="pfc-bars">
    <view class="pfc-row" v-for="row in rows" :key="row.name">
      <text class="pfc-name">{{ row.name }}</text>
      <view class="pfc-track">
        <view class="pfc-fill" :style="{ width: row.pct + '%', background: row.color }"></view>
      </view>
      <text class="pfc-val">{{ row.actual }} / {{ row.target }}g</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { clamp } from "@/utils/calc";

/** PFC 三行进度条：actual/target 单位 g */
const props = withDefaults(
  defineProps<{
    carboActual?: number;
    carboTarget?: number;
    proteinActual?: number;
    proteinTarget?: number;
    fatActual?: number;
    fatTarget?: number;
  }>(),
  { carboActual: 0, carboTarget: 0, proteinActual: 0, proteinTarget: 0, fatActual: 0, fatTarget: 0 }
);

function row(name: string, actual: number, target: number, color: string) {
  return {
    name,
    actual: Math.round(actual),
    target: Math.round(target),
    pct: target > 0 ? clamp((actual / target) * 100, 0, 100) : 0,
    color,
  };
}

const rows = computed(() => [
  row("碳水", props.carboActual, props.carboTarget, "#5b8ff9"),
  row("蛋白", props.proteinActual, props.proteinTarget, "#ff7a45"),
  row("脂肪", props.fatActual, props.fatTarget, "#f5c518"),
]);
</script>

<style scoped>
.pfc-row {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
}
.pfc-name {
  width: 60rpx;
  flex: none;
  font-size: 22rpx;
  color: #4e5561;
}
.pfc-track {
  flex: 1;
  height: 14rpx;
  border-radius: 8rpx;
  background: #eff1f5;
  overflow: hidden;
}
.pfc-fill {
  height: 100%;
  border-radius: 8rpx;
  transition: width 0.35s ease;
}
.pfc-val {
  width: 150rpx;
  flex: none;
  text-align: right;
  font-size: 20rpx;
  color: #8a919e;
}
</style>

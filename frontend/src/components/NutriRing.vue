<template>
  <view class="nutri-ring" :style="{ width: size + 'rpx', height: size + 'rpx' }">
    <view class="ring-bg" :style="{ borderWidth: thickness + 'rpx' }"></view>
    <view class="half-clip right">
      <view
        class="half half-r"
        :style="{ borderWidth: thickness + 'rpx', borderColor: color, transform: `rotate(${rightDeg}deg)` }"
      ></view>
    </view>
    <view class="half-clip left">
      <view
        class="half half-l"
        :style="{ borderWidth: thickness + 'rpx', borderColor: color, transform: `rotate(${leftDeg}deg)` }"
      ></view>
    </view>
    <view class="ring-center">
      <text class="ring-value" :style="{ color: textColor }">{{ centerText }}</text>
      <text class="ring-sub">{{ subText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { clamp } from "@/utils/calc";

/**
 * 环形进度（纯 view 双半圆实现，跨端零依赖）
 * percent: 0~1；进度从 12 点方向顺时针增长
 */
const props = withDefaults(
  defineProps<{
    percent: number;
    centerText?: string;
    subText?: string;
    size?: number; // rpx
    thickness?: number; // rpx
    color?: string;
    textColor?: string;
  }>(),
  {
    centerText: "",
    subText: "",
    size: 220,
    thickness: 20,
    color: "#07c160",
    textColor: "#1f2329",
  }
);

const p = computed(() => clamp(props.percent, 0, 1));
// 初始两半均 rotate(180deg)（空环）；右半负责 0~50%，左半负责 50~100%
const rightDeg = computed(() => 180 - Math.min(p.value, 0.5) * 360);
const leftDeg = computed(() => 180 - Math.max(p.value - 0.5, 0) * 360);
</script>

<style scoped>
.nutri-ring {
  position: relative;
}
.ring-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  border-style: solid;
  border-color: #edeff3;
  border-radius: 50%;
}
.half-clip {
  position: absolute;
  top: 0;
  width: 50%;
  height: 100%;
  overflow: hidden;
}
.half-clip.right {
  right: 0;
}
.half-clip.left {
  left: 0;
}
.half {
  position: absolute;
  top: 0;
  width: 200%;
  height: 100%;
  box-sizing: border-box;
  border-style: solid;
  border-radius: 50%;
  transition: transform 0.35s ease;
}
.half-r {
  left: -100%;
  transform-origin: 50% 50%;
}
.half-l {
  left: 0;
  transform-origin: 50% 50%;
}
.ring-center {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.ring-value {
  font-size: 40rpx;
  font-weight: 700;
}
.ring-sub {
  margin-top: 4rpx;
  font-size: 20rpx;
  color: #8a919e;
}
</style>

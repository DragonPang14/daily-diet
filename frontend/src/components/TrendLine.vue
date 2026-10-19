<template>
  <view class="trend-wrap">
    <view class="trend-line" id="trendBox">
      <template v-if="ready && pts.length">
        <!-- 目标虚线 -->
        <view v-if="target" class="target-line" :style="{ top: targetY + 'px' }"></view>
        <view v-if="target" class="target-label" :style="{ top: targetY - 16 + 'px' }">目标 {{ target }}</view>
        <!-- 折线段 -->
        <view
          v-for="(s, i) in segments"
          :key="'s' + i"
          class="seg"
          :style="{ left: s.x + 'px', top: s.y + 'px', width: s.len + 'px', transform: `rotate(${s.deg}deg)` }"
        ></view>
        <!-- 数据点 -->
        <view
          v-for="(pt, i) in pts"
          :key="'p' + i"
          class="dot"
          :class="{ last: i === pts.length - 1 }"
          :style="{ left: pt.x - 3 + 'px', top: pt.y - 3 + 'px' }"
        ></view>
        <!-- 数值标签（点少时展示） -->
        <text
          v-if="pts.length <= 7"
          v-for="(pt, i) in pts"
          :key="'t' + i"
          class="val-label"
          :style="{ left: pt.x - 22 + 'px', top: pt.y - 24 + 'px' }"
        >{{ Math.round(pt.v) }}</text>
      </template>
      <view v-if="ready && !pts.length" class="empty">暂无数据</view>
    </view>
    <view class="x-labels" v-if="labels.length">
      <text v-for="(lb, i) in labels" :key="i" class="x-label" :style="{ width: 100 / labels.length + '%' }">{{ lb }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, ref } from "vue";

/** 每日摄入折线（纯 view 实现）：点 + 旋转线段 + 目标虚线 */
const props = withDefaults(
  defineProps<{
    values: number[];
    labels?: string[];
    target?: number;
    heightPx?: number;
  }>(),
  { labels: () => [], target: 0, heightPx: 150 }
);

const inst = getCurrentInstance();
const boxWidth = ref(0);
const ready = ref(false);

const PAD_X = 14;
const PAD_TOP = 26;
const PAD_BOTTOM = 8;

onMounted(() => {
  // 延迟一帧确保布局完成
  setTimeout(measure, 60);
});

function measure(): void {
  uni
    .createSelectorQuery()
    .in(inst?.proxy)
    .select("#trendBox")
    .boundingClientRect((r) => {
      const rect = Array.isArray(r) ? r[0] : r;
      if (rect && rect.width) boxWidth.value = rect.width;
      ready.value = true;
    })
    .exec();
}

const maxV = computed(() => {
  const m = Math.max(...props.values, props.target || 0, 1);
  return m * 1.12;
});

const pts = computed(() => {
  if (!boxWidth.value || !props.values.length) return [];
  const n = props.values.length;
  const w = boxWidth.value - PAD_X * 2;
  const h = props.heightPx - PAD_TOP - PAD_BOTTOM;
  return props.values.map((v, i) => ({
    v,
    x: PAD_X + (n === 1 ? w / 2 : (w * i) / (n - 1)),
    y: PAD_TOP + h * (1 - v / maxV.value),
  }));
});

const segments = computed(() => {
  const out: { x: number; y: number; len: number; deg: number }[] = [];
  for (let i = 0; i < pts.value.length - 1; i++) {
    const a = pts.value[i];
    const b = pts.value[i + 1];
    const dx = b.x - a.x;
    const dy = b.y - a.y;
    out.push({
      x: a.x,
      y: a.y,
      len: Math.sqrt(dx * dx + dy * dy),
      deg: (Math.atan2(dy, dx) * 180) / Math.PI,
    });
  }
  return out;
});

const targetY = computed(() => {
  const h = props.heightPx - PAD_TOP - PAD_BOTTOM;
  return PAD_TOP + h * (1 - (props.target || 0) / maxV.value);
});
</script>

<style scoped>
.trend-line {
  position: relative;
  width: 100%;
  height: 300rpx;
}
.target-line {
  position: absolute;
  left: 0;
  width: 100%;
  border-top: 2rpx dashed #fa8c16;
}
.target-label {
  position: absolute;
  right: 4rpx;
  font-size: 18rpx;
  color: #fa8c16;
}
.seg {
  position: absolute;
  height: 4rpx;
  background: #07c160;
  border-radius: 2rpx;
  transform-origin: 0 50%;
}
.dot {
  position: absolute;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #07c160;
}
.dot.last {
  background: #fff;
  border: 4rpx solid #07c160;
  box-sizing: border-box;
}
.val-label {
  position: absolute;
  width: 60rpx;
  text-align: center;
  font-size: 16rpx;
  color: #4e5561;
}
.empty {
  padding: 60rpx 0;
  text-align: center;
  color: #8a919e;
  font-size: 22rpx;
}
.x-labels {
  display: flex;
  margin-top: 8rpx;
}
.x-label {
  text-align: center;
  font-size: 18rpx;
  color: #8a919e;
}
</style>

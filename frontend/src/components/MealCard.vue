<template>
  <view class="meal-card">
    <view class="meal-head">
      <text class="meal-tag" :class="tagClass">{{ mealName }}</text>
      <text class="meal-kcal">
        <text class="meal-kcal-num">{{ Math.round(totalKcal) }}</text> kcal
      </text>
      <text v-if="state === 'done'" class="meal-state done">✓ 已吃</text>
      <text v-else class="meal-state todo">待确认</text>
    </view>
    <view class="meal-items" v-if="items.length">
      <view class="meal-item" v-for="it in items" :key="it.id">
        <text class="it-name">{{ dietStore.foodName(it.foodId) }}</text>
        <text class="it-g">{{ Math.round(it.foodWeightG) }}g · {{ Math.round(it.energyKcal) }} kcal</text>
        <text v-if="editable && state !== 'done'" class="it-sub" @click.stop="emit('substitute', it)">换</text>
      </view>
    </view>
    <view class="meal-items empty" v-else>
      <text class="it-g">该餐暂无推荐食材</text>
    </view>
    <view class="meal-acts" v-if="editable && state !== 'done' && items.length">
      <view class="btn-eat" @click.stop="emit('eat')">✓ 本餐已吃</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { RecipeItem } from "@/api/recipe";
import { useDietStore } from "@/store/diet";
import { sum } from "@/utils/calc";

/** 餐次卡片：首页/食谱页复用 */
const props = withDefaults(
  defineProps<{
    mealType: number;
    items: RecipeItem[];
    /** done=已吃（或部分记录），todo=待确认 */
    state?: "done" | "todo";
    /** 是否展示操作（已吃/换） */
    editable?: boolean;
  }>(),
  { state: "todo", editable: false }
);

const emit = defineEmits<{
  (e: "eat"): void;
  (e: "substitute", item: RecipeItem): void;
}>();

const dietStore = useDietStore();

const MEAL_MAP: Record<number, { name: string; cls: string }> = {
  1: { name: "早餐", cls: "t-breakfast" },
  2: { name: "午餐", cls: "t-lunch" },
  3: { name: "晚餐", cls: "t-dinner" },
  4: { name: "加餐", cls: "t-snack" },
};

const mealName = computed(() => MEAL_MAP[props.mealType]?.name ?? "餐次");
const tagClass = computed(() => MEAL_MAP[props.mealType]?.cls ?? "t-breakfast");
const totalKcal = computed(() => sum(props.items, (it) => Number(it.energyKcal) || 0));
</script>

<style scoped>
.meal-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 22rpx 26rpx;
  box-shadow: 0 4rpx 20rpx rgba(30, 40, 60, 0.05);
}
.meal-head {
  display: flex;
  align-items: center;
}
.meal-tag {
  font-size: 22rpx;
  font-weight: 700;
  padding: 4rpx 16rpx;
  border-radius: 10rpx;
}
.t-breakfast {
  background: #fff4e5;
  color: #d48806;
}
.t-lunch {
  background: #e8f9f0;
  color: #06a856;
}
.t-dinner {
  background: #e8f0fe;
  color: #3b6fe0;
}
.t-snack {
  background: #f3e8ff;
  color: #7b3fe4;
}
.meal-kcal {
  margin-left: auto;
  font-size: 22rpx;
  color: #4e5561;
}
.meal-kcal-num {
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2329;
}
.meal-state {
  margin-left: 16rpx;
  font-size: 22rpx;
}
.meal-state.done {
  color: #06a856;
  font-weight: 600;
}
.meal-state.todo {
  color: #8a919e;
}
.meal-items {
  margin-top: 12rpx;
}
.meal-item {
  display: flex;
  align-items: center;
  padding: 8rpx 0;
  border-top: 1rpx dashed #f0f1f4;
}
.meal-item:first-child {
  border-top: none;
}
.it-name {
  flex: 1;
  font-size: 26rpx;
  color: #1f2329;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.it-g {
  font-size: 22rpx;
  color: #8a919e;
}
.it-sub {
  margin-left: 14rpx;
  flex: none;
  font-size: 20rpx;
  color: #06a856;
  border: 1rpx solid #9fe3c2;
  border-radius: 8rpx;
  padding: 2rpx 12rpx;
}
.meal-acts {
  margin-top: 16rpx;
  display: flex;
}
.btn-eat {
  flex: 1;
  background: #07c160;
  color: #fff;
  border-radius: 16rpx;
  text-align: center;
  font-size: 26rpx;
  font-weight: 600;
  padding: 14rpx 0;
}
</style>

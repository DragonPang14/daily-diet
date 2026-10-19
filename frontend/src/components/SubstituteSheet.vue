<template>
  <u-popup :show="show" mode="bottom" :round="24" @close="close">
    <view class="sheet">
      <view class="grab"></view>
      <view class="sheet-title">替换食材</view>

      <view class="origin" v-if="item">
        <view class="origin-info">
          <text class="origin-name">{{ dietStore.foodName(item.foodId) }}</text>
          <text class="origin-sub">{{ Math.round(item.foodWeightG) }}g · {{ Math.round(item.energyKcal) }} kcal</text>
        </view>
        <text class="origin-pfc">
          蛋 {{ fmt(item.proteinG) }} · 碳 {{ fmt(item.carboG) }} · 脂 {{ fmt(item.fatG) }}
        </text>
      </view>

      <view class="cand-tip">同类食材 · 热量 ±20% · 按 PFC 相似度排序</view>

      <view v-if="loading" class="cand-loading">匹配候选中…</view>
      <view v-else-if="!candidates.length" class="cand-loading">暂无可替换候选</view>

      <view
        v-else
        v-for="(c, idx) in candidates"
        :key="c.id"
        class="cand"
        :class="{ best: idx === 0 }"
        @click="onPick(c)"
      >
        <view class="cand-top">
          <text class="cand-name">{{ c.name }}</text>
          <text class="match" :class="idx === 0 ? '' : idx === 1 ? 'm2' : 'm3'">Top {{ idx + 1 }}</text>
          <text class="delta" :class="delta(c) >= 0 ? 'up' : 'down'">
            {{ delta(c) >= 0 ? "+" : "" }}{{ delta(c) }} kcal
          </text>
        </view>
        <view class="cand-pfc">
          <view class="bar carbo" :style="{ flexGrow: grow(c, 'carbo') }"></view>
          <view class="bar protein" :style="{ flexGrow: grow(c, 'protein') }"></view>
          <view class="bar fat" :style="{ flexGrow: grow(c, 'fat') }"></view>
          <text class="cand-pfc-txt">
            按 {{ Math.round(item?.foodWeightG || 0) }}g 估算 · 蛋 {{ gram(c, "protein") }} 脂 {{ gram(c, "fat") }}
          </text>
        </view>
      </view>

      <view class="sheet-foot">
        <text class="dislike" @click="onDislike">👎 不喜欢这个食材（3 次后不再推荐）</text>
        <text class="foot-note">替换后自动更新本餐热量</text>
      </view>
    </view>
  </u-popup>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { recipeApi, type RecipeItem } from "@/api/recipe";
import type { Food } from "@/api/food";
import { useDietStore } from "@/store/diet";

/** 食材替换底部弹层：首页/食谱页复用 */
const props = defineProps<{
  show: boolean;
  item: RecipeItem | null;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "substituted", item: RecipeItem): void;
  (e: "disliked"): void;
}>();

const dietStore = useDietStore();
const candidates = ref<Food[]>([]);
const loading = ref(false);

watch(
  () => props.show,
  async (v) => {
    if (v && props.item) {
      loading.value = true;
      candidates.value = [];
      try {
        candidates.value = await recipeApi.getSubstitutes(props.item.id);
      } finally {
        loading.value = false;
      }
    }
  }
);

function fmt(n: number): string {
  return (Math.round(Number(n) * 10) / 10).toString();
}

/** 同克数估算热量差：候选每100g kcal × 原克数/100 − 原 kcal */
function delta(c: Food): number {
  if (!props.item) return 0;
  const est = (Number(c.energyKcal) * Number(props.item.foodWeightG)) / 100;
  return Math.round(est - Number(props.item.energyKcal));
}

function gram(c: Food, kind: "protein" | "fat" | "carbo"): string {
  if (!props.item) return "0";
  const per100 = kind === "protein" ? c.proteinG : kind === "fat" ? c.fatG : c.carboG;
  return fmt((Number(per100) * Number(props.item.foodWeightG)) / 100);
}

function grow(c: Food, kind: "protein" | "fat" | "carbo"): number {
  const per100 = kind === "protein" ? c.proteinG : kind === "fat" ? c.fatG : c.carboG;
  return Math.max(Number(per100) || 0, 0.5);
}

async function onPick(c: Food): Promise<void> {
  if (!props.item) return;
  uni.showLoading({ title: "替换中…", mask: true });
  try {
    const newItem = await recipeApi.doSubstitute(props.item.id, c.id);
    emit("substituted", newItem);
    close();
  } finally {
    uni.hideLoading();
  }
}

function onDislike(): void {
  if (!props.item) return;
  uni.showModal({
    title: "标记不喜欢",
    content: `确定不再想吃「${dietStore.foodName(props.item.foodId)}」吗？累计 3 次将从推荐池排除`,
    confirmColor: "#f5222d",
    success: async (r) => {
      if (!r.confirm || !props.item) return;
      await recipeApi.dislike(props.item.id);
      uni.showToast({ title: "已标记", icon: "none" });
      emit("disliked");
      close();
    },
  });
}

function close(): void {
  emit("close");
}
</script>

<style scoped>
.sheet {
  padding: 16rpx 28rpx 48rpx;
}
.grab {
  width: 76rpx;
  height: 8rpx;
  border-radius: 4rpx;
  background: #e0e3e8;
  margin: 0 auto 20rpx;
}
.sheet-title {
  font-size: 32rpx;
  font-weight: 700;
}
.origin {
  display: flex;
  align-items: center;
  background: #f7f8fa;
  border-radius: 18rpx;
  padding: 18rpx 22rpx;
  margin-top: 18rpx;
}
.origin-info {
  display: flex;
  flex-direction: column;
}
.origin-name {
  font-size: 28rpx;
  font-weight: 600;
}
.origin-sub {
  font-size: 20rpx;
  color: #8a919e;
  margin-top: 4rpx;
}
.origin-pfc {
  margin-left: auto;
  font-size: 20rpx;
  color: #8a919e;
}
.cand-tip {
  font-size: 20rpx;
  color: #8a919e;
  margin: 22rpx 0 4rpx;
}
.cand-loading {
  text-align: center;
  color: #8a919e;
  font-size: 24rpx;
  padding: 40rpx 0;
}
.cand {
  border: 2rpx solid #ebedf1;
  border-radius: 20rpx;
  padding: 18rpx 22rpx;
  margin-top: 16rpx;
}
.cand.best {
  border-color: #07c160;
  background: #e8f9f0;
}
.cand-top {
  display: flex;
  align-items: center;
}
.cand-name {
  font-size: 28rpx;
  font-weight: 600;
  margin-right: 12rpx;
}
.match {
  font-size: 18rpx;
  font-weight: 700;
  color: #fff;
  background: #07c160;
  border-radius: 8rpx;
  padding: 2rpx 12rpx;
}
.match.m2 {
  background: #7bc8a4;
}
.match.m3 {
  background: #9aa5b5;
}
.delta {
  margin-left: auto;
  font-size: 22rpx;
  font-weight: 600;
}
.delta.up {
  color: #fa8c16;
}
.delta.down {
  color: #06a856;
}
.cand-pfc {
  display: flex;
  align-items: center;
  margin-top: 14rpx;
}
.bar {
  height: 10rpx;
  border-radius: 6rpx;
  flex-basis: 0;
  margin-right: 6rpx;
}
.bar.carbo {
  background: #5b8ff9;
}
.bar.protein {
  background: #ff7a45;
}
.bar.fat {
  background: #f5c518;
}
.cand-pfc-txt {
  flex: none;
  font-size: 18rpx;
  color: #8a919e;
  margin-left: 8rpx;
}
.sheet-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 26rpx;
}
.dislike {
  font-size: 22rpx;
  color: #4e5561;
}
.foot-note {
  font-size: 18rpx;
  color: #8a919e;
}
</style>

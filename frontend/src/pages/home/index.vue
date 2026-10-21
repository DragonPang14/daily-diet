<template>
  <view class="home">
    <!-- 问候 -->
    <view class="head">
      <view>
        <view class="hello">{{ greeting }}，{{ nickname }}</view>
        <view class="date">{{ dateText }} · {{ goalText }}</view>
      </view>
    </view>

    <!-- 摄入汇总 -->
    <view class="card">
      <view class="ring-wrap">
        <NutriRing
          :percent="ringPercent"
          :center-text="ringCenter"
          :sub-text="ringSub"
          :size="220"
          :thickness="20"
        />
        <view class="nums">
          <view class="num-row"><text>已摄入</text><text class="num-val">{{ Math.round(actualKcal) }} kcal</text></view>
          <view class="num-row"><text>目标</text><text class="num-val">{{ Math.round(targetKcal) }} kcal</text></view>
          <view class="num-row"><text>剩余</text><text class="num-val remain">{{ Math.round(remainKcal) }} kcal</text></view>
          <PfcBars
            :carbo-actual="summary?.totalCarboG || 0"
            :carbo-target="summary?.targetCarboG || 0"
            :protein-actual="summary?.totalProteinG || 0"
            :protein-target="summary?.targetProteinG || 0"
            :fat-actual="summary?.totalFatG || 0"
            :fat-target="summary?.targetFatG || 0"
          />
        </view>
      </view>
    </view>

    <!-- 近 7 天打卡 -->
    <view v-if="achievement" class="banner">
      🎉 近 7 天打卡 <text class="banner-num">{{ achievement.activeDays }}/{{ achievement.totalDays }}</text> 天，继续保持！
    </view>

    <!-- 今日食谱 -->
    <view class="sec-head">
      <text class="sec-title">今日食谱</text>
      <text class="sec-more" @click="goRecipe">全部食谱 ›</text>
    </view>

    <view v-if="loading" class="loading">加载中…</view>
    <template v-else-if="recipe">
      <MealCard
        v-for="m in meals"
        :key="m.mealType"
        :meal-type="m.mealType"
        :items="m.items"
        :state="m.done ? 'done' : 'todo'"
        editable
        @eat="() => onEatMeal(m)"
        @substitute="(it) => onSubstitute(it)"
      />
    </template>
    <view v-else class="loading">今日食谱生成失败，请稍后重试</view>

    <view class="cta-holder"></view>
    <view v-if="recipe" class="cta" @click="onBulk">⚡ 一键今日食谱入库</view>

    <SubstituteSheet
      :show="sheetShow"
      :item="sheetItem"
      @close="sheetShow = false"
      @substituted="onSubstituted"
      @disliked="onSubstituted"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import dayjs from "dayjs";
import NutriRing from "@/components/NutriRing.vue";
import PfcBars from "@/components/PfcBars.vue";
import MealCard from "@/components/MealCard.vue";
import SubstituteSheet from "@/components/SubstituteSheet.vue";
import { recipeApi, type RecipeItem } from "@/api/recipe";
import { recordApi } from "@/api/record";
import { statsApi } from "@/api/stats";
import { userApi } from "@/api/user";
import { useDietStore } from "@/store/diet";
import { useUserStore } from "@/store/user";
import { clamp } from "@/utils/calc";

const dietStore = useDietStore();
const userStore = useUserStore();

const loading = ref(true);
const sheetShow = ref(false);
const sheetItem = ref<RecipeItem | null>(null);
const achievement = ref<{ activeDays: number; totalDays: number } | null>(null);

const recipe = computed(() => dietStore.todayRecipe);
const summary = computed(() => dietStore.todaySummary);

const nickname = computed(() => userStore.userInfo?.nickname || "朋友");
const greeting = computed(() => {
  const h = dayjs().hour();
  if (h < 6) return "夜深了";
  if (h < 11) return "早上好";
  if (h < 14) return "中午好";
  if (h < 18) return "下午好";
  return "晚上好";
});
const WEEK = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
const dateText = computed(() => dayjs().format("M月D日 ") + WEEK[dayjs().day()]);
const GOAL_MAP: Record<number, string> = { 1: "增肌中", 2: "减脂中", 3: "维持中", 4: "自定义目标" };
const goalText = computed(() => GOAL_MAP[userStore.userInfo?.goalType || 0] || "健康饮食");

const actualKcal = computed(() => Number(summary.value?.totalKcal) || 0);
const targetKcal = computed(() => Number(summary.value?.targetKcal) || recipe.value?.targetKcal || 0);
const remainKcal = computed(() => Math.max(targetKcal.value - actualKcal.value, 0));
const ringPercent = computed(() => (targetKcal.value > 0 ? actualKcal.value / targetKcal.value : 0));
const ringCenter = computed(() => `${Math.round(clamp(ringPercent.value, 0, 1) * 100)}%`);
const ringSub = computed(() => "已摄入");

interface MealGroup {
  mealType: number;
  items: RecipeItem[];
  done: boolean;
}

const meals = computed<MealGroup[]>(() => {
  if (!recipe.value) return [];
  const recordedItemIds = new Set(
    (dietStore.todayRecords || []).map((r) => r.recipeItemId).filter((x) => x != null)
  );
  return [1, 2, 3, 4]
    .map((mt) => {
      const items = (recipe.value!.items || [])
        .filter((it) => it.mealType === mt)
        .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
      return {
        mealType: mt,
        items,
        done: items.length > 0 && items.every((it) => recordedItemIds.has(it.id)),
      };
    })
    .filter((m) => m.items.length > 0);
});

onShow(() => {
  loadAll();
});

/**
 * 加载首页数据：逐项容错，未登录时食谱/汇总接口会返回 401，
 * 但首页公共内容仍需正常展示，因此每个请求单独兜底。
 */
async function loadAll(): Promise<void> {
  loading.value = true;
  try {
    const [rcp, sum, ach, me] = await Promise.all([
      recipeApi.getToday().catch(() => null),
      recordApi.todaySummary().catch(() => null),
      statsApi.achievement(7).catch(() => null),
      userStore.userInfo ? Promise.resolve(userStore.userInfo) : userApi.getMe().catch(() => null),
    ]);
    if (rcp) dietStore.setRecipe(rcp);
    if (sum) dietStore.setSummary(sum);
    achievement.value = ach;
    if (me && !userStore.userInfo) userStore.setUser(me);
    const ids = [
      ...(rcp?.items || []).map((it) => it.foodId),
      ...(sum?.records || []).map((r) => r.foodId),
    ];
    if (ids.length) await dietStore.ensureFoodNames(ids);
  } finally {
    loading.value = false;
  }
}

async function refreshSummary(): Promise<void> {
  const sum = await recordApi.todaySummary();
  dietStore.setSummary(sum);
}

/** 单餐已吃：该餐未入库的食材逐条创建记录 */
async function onEatMeal(m: MealGroup): Promise<void> {
  const recordedItemIds = new Set(
    (dietStore.todayRecords || []).map((r) => r.recipeItemId).filter((x) => x != null)
  );
  const pending = m.items.filter((it) => !recordedItemIds.has(it.id));
  if (!pending.length) return;
  uni.showLoading({ title: "入库中…", mask: true });
  try {
    for (const it of pending) {
      await recordApi.create({
        mealType: it.mealType,
        foodId: it.foodId,
        foodWeightG: Number(it.foodWeightG),
        recipeItemId: it.id,
      });
    }
    await refreshSummary();
    uni.showToast({ title: "已记录", icon: "none" });
  } finally {
    uni.hideLoading();
  }
}

/** 一键入库：按今日食谱批量创建 */
async function onBulk(): Promise<void> {
  if (!recipe.value) return;
  uni.showModal({
    title: "一键入库",
    content: "将今日推荐食谱全部标记为已吃？",
    confirmColor: "#07c160",
    success: async (r) => {
      if (!r.confirm || !recipe.value) return;
      uni.showLoading({ title: "入库中…", mask: true });
      try {
        await recordApi.bulkToday(recipe.value.id);
        await refreshSummary();
        uni.showToast({ title: "已全部入库", icon: "none" });
      } finally {
        uni.hideLoading();
      }
    },
  });
}

function onSubstitute(item: RecipeItem): void {
  sheetItem.value = item;
  sheetShow.value = true;
}

/** 替换/标记不喜欢后刷新食谱 */
async function onSubstituted(): Promise<void> {
  const rcp = await recipeApi.getToday();
  dietStore.setRecipe(rcp);
  await dietStore.ensureFoodNames(rcp.items.map((it) => it.foodId));
}

function goRecipe(): void {
  uni.navigateTo({ url: "/pages/recipe/index" });
}
</script>

<style scoped>
.home {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  min-height: 100vh;
  box-sizing: border-box;
  background: #f5f6f8;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8rpx;
}
.hello {
  font-size: 36rpx;
  font-weight: 700;
}
.date {
  font-size: 22rpx;
  color: #8a919e;
  margin-top: 6rpx;
}
.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 26rpx;
  box-shadow: 0 4rpx 20rpx rgba(30, 40, 60, 0.05);
}
.ring-wrap {
  display: flex;
  align-items: center;
  gap: 28rpx;
}
.nums {
  flex: 1;
}
.num-row {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #4e5561;
  padding: 6rpx 0;
}
.num-val {
  font-weight: 700;
  color: #1f2329;
}
.num-val.remain {
  color: #06a856;
}
.banner {
  font-size: 22rpx;
  color: #4e5561;
  background: #fffbe8;
  border: 1rpx solid #feedb7;
  border-radius: 16rpx;
  padding: 16rpx 24rpx;
}
.banner-num {
  color: #d48806;
  font-weight: 700;
}
.sec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8rpx;
}
.sec-title {
  font-size: 30rpx;
  font-weight: 700;
}
.sec-more {
  font-size: 22rpx;
  color: #8a919e;
}
.loading {
  text-align: center;
  color: #8a919e;
  font-size: 24rpx;
  padding: 48rpx 0;
}
.cta-holder {
  height: 110rpx;
}
.cta {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 32rpx;
  background: #07c160;
  color: #fff;
  border-radius: 24rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 700;
  padding: 24rpx 0;
  box-shadow: 0 16rpx 32rpx rgba(7, 193, 96, 0.3);
  z-index: 10;
}
</style>

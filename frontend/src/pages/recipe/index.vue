<template>
  <view class="recipe-page">
    <!-- 目标卡 -->
    <view v-if="recipe" class="card target-card">
      <view class="t-row">
        <view class="t-cell">
          <text class="t-num">{{ Math.round(recipe.targetKcal) }}</text>
          <text class="t-label">目标热量 kcal</text>
        </view>
        <view class="t-cell">
          <text class="t-num carbo">{{ Math.round(recipe.targetCarboG) }}g</text>
          <text class="t-label">碳水</text>
        </view>
        <view class="t-cell">
          <text class="t-num protein">{{ Math.round(recipe.targetProteinG) }}g</text>
          <text class="t-label">蛋白质</text>
        </view>
        <view class="t-cell">
          <text class="t-num fat">{{ Math.round(recipe.targetFatG) }}g</text>
          <text class="t-label">脂肪</text>
        </view>
      </view>
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
import MealCard from "@/components/MealCard.vue";
import SubstituteSheet from "@/components/SubstituteSheet.vue";
import { recipeApi, type RecipeItem } from "@/api/recipe";
import { recordApi } from "@/api/record";
import { useDietStore } from "@/store/diet";

const dietStore = useDietStore();
const loading = ref(true);
const sheetShow = ref(false);
const sheetItem = ref<RecipeItem | null>(null);

const recipe = computed(() => dietStore.todayRecipe);

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

async function loadAll(): Promise<void> {
  loading.value = true;
  try {
    const [rcp, sum] = await Promise.all([recipeApi.getToday(), recordApi.todaySummary()]);
    dietStore.setRecipe(rcp);
    dietStore.setSummary(sum);
    await dietStore.ensureFoodNames(rcp.items.map((it) => it.foodId));
  } finally {
    loading.value = false;
  }
}

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
    dietStore.setSummary(await recordApi.todaySummary());
    uni.showToast({ title: "已记录", icon: "none" });
  } finally {
    uni.hideLoading();
  }
}

function onSubstitute(item: RecipeItem): void {
  sheetItem.value = item;
  sheetShow.value = true;
}

async function onSubstituted(): Promise<void> {
  const rcp = await recipeApi.getToday();
  dietStore.setRecipe(rcp);
  await dietStore.ensureFoodNames(rcp.items.map((it) => it.foodId));
}
</script>

<style scoped>
.recipe-page {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  min-height: 100vh;
  box-sizing: border-box;
  background: #f5f6f8;
}
.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 26rpx;
  box-shadow: 0 4rpx 20rpx rgba(30, 40, 60, 0.05);
}
.t-row {
  display: flex;
  text-align: center;
}
.t-cell {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}
.t-cell + .t-cell:before {
  content: "";
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 1rpx;
  background: #ebedf1;
}
.t-num {
  font-size: 32rpx;
  font-weight: 700;
}
.t-num.carbo {
  color: #5b8ff9;
}
.t-num.protein {
  color: #ff7a45;
}
.t-num.fat {
  color: #d9a416;
}
.t-label {
  font-size: 20rpx;
  color: #8a919e;
  margin-top: 6rpx;
}
.loading {
  text-align: center;
  color: #8a919e;
  font-size: 24rpx;
  padding: 48rpx 0;
}
</style>

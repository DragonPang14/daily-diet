import { defineStore } from "pinia";
import { ref } from "vue";
import type { DailyRecipe } from "@/api/recipe";
import type { DietRecord, RecordSummary } from "@/api/record";
import { foodApi } from "@/api/food";

export interface FoodBrief {
  name: string;
  category: string;
}

export const useDietStore = defineStore("diet", () => {
  const todayRecipe = ref<DailyRecipe | null>(null);
  const todayRecords = ref<DietRecord[]>([]);
  const todaySummary = ref<RecordSummary | null>(null);

  /** foodId → 名称/类别 缓存（RecipeItem/DietRecord 只有 foodId，无名称） */
  const foodNameCache = ref<Record<number, FoodBrief>>({});

  /** 记录页「手动添加」跨 tab 传参：pickMode=true 时食物库页进入挑选模式 */
  const pickMode = ref(false);

  function setPickMode(v: boolean): void {
    pickMode.value = v;
  }

  function setRecipe(recipe: DailyRecipe): void {
    todayRecipe.value = recipe;
  }

  function setRecords(records: DietRecord[]): void {
    todayRecords.value = records;
  }

  function setSummary(summary: RecordSummary): void {
    todaySummary.value = summary;
    todayRecords.value = summary.records || [];
  }

  /** 批量补齐 foodId 对应的食物名称（去重 + 跳过已缓存） */
  async function ensureFoodNames(foodIds: (number | null | undefined)[]): Promise<void> {
    const missing = [...new Set(foodIds.filter((id): id is number => typeof id === "number"))].filter(
      (id) => !(id in foodNameCache.value)
    );
    if (!missing.length) return;
    const results = await Promise.allSettled(missing.map((id) => foodApi.getById(id)));
    const patch: Record<number, FoodBrief> = {};
    results.forEach((r, i) => {
      const id = missing[i];
      patch[id] =
        r.status === "fulfilled"
          ? { name: r.value.name, category: r.value.category }
          : { name: "未知食物", category: "" };
    });
    foodNameCache.value = { ...foodNameCache.value, ...patch };
  }

  function foodName(id: number): string {
    return foodNameCache.value[id]?.name ?? "加载中…";
  }

  function clear(): void {
    todayRecipe.value = null;
    todayRecords.value = [];
    todaySummary.value = null;
  }

  return {
    todayRecipe,
    todayRecords,
    todaySummary,
    foodNameCache,
    pickMode,
    setPickMode,
    setRecipe,
    setRecords,
    setSummary,
    ensureFoodNames,
    foodName,
    clear,
  };
});

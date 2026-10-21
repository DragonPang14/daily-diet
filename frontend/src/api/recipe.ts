import { get, post } from "./http";
import type { Food } from "./food";

/** 食谱食材明细（对齐后端 RecipeItem 实体） */
export interface RecipeItem {
  id: number;
  recipeId: number;
  /** 餐次类型：1=早餐 2=午餐 3=晚餐 4=加餐 */
  mealType: number;
  foodId: number;
  /** 推荐克数，单位 g */
  foodWeightG: number;
  energyKcal: number;
  proteinG: number;
  fatG: number;
  carboG: number;
  orderIndex: number;
}

/** 每日食谱（对齐后端 RecipeResponse） */
export interface DailyRecipe {
  id: number;
  recipeDate: string;
  targetKcal: number;
  targetCarboG: number;
  targetProteinG: number;
  targetFatG: number;
  items: RecipeItem[];
}

/** 手动触发生成当日食谱 */
export function generate(): Promise<DailyRecipe> {
  return post<DailyRecipe>("/recipes/generate");
}

/** 获取今日推荐（若未生成则后端自动生成） */
export function getToday(): Promise<DailyRecipe> {
  return get<DailyRecipe>("/recipes/today");
}

/** 获取指定日期食谱 */
export function getByDate(date: string): Promise<DailyRecipe> {
  return get<DailyRecipe>(`/recipes/${date}`);
}

/** 获取某个食材的 Top N 替换候选 */
export function getSubstitutes(itemId: number): Promise<Food[]> {
  return get<{ candidates: Food[] }>(`/recipes/items/${itemId}/substitute`).then(
    (r) => r.candidates || []
  );
}

/** 执行替换：将 itemId 替换为 foodId，返回更新后的 item */
export function doSubstitute(itemId: number, foodId: number): Promise<RecipeItem> {
  return post<RecipeItem>(`/recipes/items/${itemId}/substitute?foodId=${foodId}`);
}

/** 标记某食材「不喜欢」（累计 3 次从推荐池排除） */
export function dislike(itemId: number): Promise<void> {
  return post<void>(`/recipes/items/${itemId}/dislike`);
}

export const recipeApi = {
  generate,
  getToday,
  getByDate,
  getSubstitutes,
  doSubstitute,
  dislike,
};

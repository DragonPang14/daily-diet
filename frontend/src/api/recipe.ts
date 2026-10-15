import { get, post } from "./http";

export interface RecipeItem {
  id: number;
  recipeId: number;
  foodId: number;
  foodName: string;
  mealType: number; // 1 早餐 2 午餐 3 晚餐 4 加餐
  grams: number;
  energyKcal: number;
  proteinG: number;
  fatG: number;
  carboG: number;
}

export interface DailyRecipe {
  id: number;
  userId: number;
  recipeDate: string;
  targetKcal: number;
  actualKcal: number;
  items: RecipeItem[];
}

/** 手动触发生成当日食谱 */
export function generate(): Promise<DailyRecipe> {
  return post<DailyRecipe>("/v1/recipes/generate");
}

/** 获取今日推荐（若未生成则后端自动生成） */
export function getToday(): Promise<DailyRecipe> {
  return get<DailyRecipe>("/v1/recipes/today");
}

/** 获取指定日期食谱 */
export function getByDate(date: string): Promise<DailyRecipe> {
  return get<DailyRecipe>(`/v1/recipes/${date}`);
}

/** 替换某个食材 → 返回新 item */
export function substitute(itemId: number): Promise<RecipeItem> {
  return post<RecipeItem>(`/v1/recipes/items/${itemId}/substitute`);
}

/** 标记某食材「不喜欢」 */
export function dislike(itemId: number): Promise<void> {
  return post<void>(`/v1/recipes/items/${itemId}/dislike`);
}

export const recipeApi = { generate, getToday, getByDate, substitute, dislike };


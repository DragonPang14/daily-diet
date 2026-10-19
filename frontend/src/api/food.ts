import { del, get, post, put } from "./http";

/** 食物（对齐后端 Food 实体，营养值为每 100g） */
export interface Food {
  id: number;
  name: string;
  /** 类别：谷物/蛋白/蔬菜/水果/乳品/坚果/调料 */
  category: string;
  brand?: string;
  /** 来源：1=标准库 2=用户自定义 */
  source: number;
  energyKcal: number;
  proteinG: number;
  fatG: number;
  carboG: number;
  /** 常用份量 g */
  commonWeight?: number;
  imageUrl?: string;
}

export interface FoodSearchParams {
  keyword?: string;
  category?: string;
  page?: number;
  size?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

/** 自定义食物请求体（对齐后端 CustomFoodRequest） */
export interface CustomFoodRequest {
  name: string;
  category: string;
  brand?: string;
  energyKcal: number;
  proteinG: number;
  fatG: number;
  carboG: number;
  commonWeight?: number;
  imageUrl?: string;
}

/** 搜索食物库（支持模糊 + 分类） */
export function search(params: FoodSearchParams): Promise<PageResult<Food>> {
  return get<PageResult<Food>>("/v1/foods", params as unknown as Record<string, unknown>);
}

/** 获取单个食物详情 */
export function getById(id: number): Promise<Food> {
  return get<Food>(`/v1/foods/${id}`);
}

/** 创建自定义食物 */
export function createCustom(data: CustomFoodRequest): Promise<Food> {
  return post<Food>("/v1/foods/custom", data as unknown as Record<string, unknown>);
}

/** 修改自定义食物 */
export function updateCustom(id: number, data: CustomFoodRequest): Promise<Food> {
  return put<Food>(`/v1/foods/custom/${id}`, data as unknown as Record<string, unknown>);
}

/** 删除自定义食物 */
export function deleteCustom(id: number): Promise<void> {
  return del<void>(`/v1/foods/custom/${id}`);
}

export const foodApi = { search, getById, createCustom, updateCustom, deleteCustom };

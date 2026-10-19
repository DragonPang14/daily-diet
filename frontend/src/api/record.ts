import { del, get, post, put } from "./http";

/** 饮食记录（对齐后端 DietRecord 实体） */
export interface DietRecord {
  id: number;
  userId: number;
  recordDate: string;
  /** 餐次类型：1=早餐 2=午餐 3=晚餐 4=加餐 */
  mealType: number;
  foodId: number;
  /** 实际食用克数，单位 g */
  foodWeightG: number;
  energyKcal: number;
  proteinG: number;
  fatG: number;
  carboG: number;
  /** 来源：1=一键入库 2=手动添加 3=替换生成 */
  source: number;
  recipeItemId: number | null;
}

/** 今日汇总（对齐后端 DailySummary） */
export interface RecordSummary {
  totalKcal: number;
  totalProteinG: number;
  totalFatG: number;
  totalCarboG: number;
  targetKcal: number;
  targetProteinG: number;
  targetFatG: number;
  targetCarboG: number;
  achievementRate: number;
  records: DietRecord[];
}

/** 新增/修改记录请求体（对齐后端 AddRecordRequest） */
export interface CreateRecordRequest {
  mealType: number;
  foodId: number;
  foodWeightG: number;
  recipeItemId?: number;
}

/** 一键入库（按今日食谱批量创建记录） */
export function bulkToday(recipeId: number): Promise<void> {
  return post<void>("/v1/records/today/bulk", { recipeId });
}

/** 单条手动添加 */
export function create(data: CreateRecordRequest): Promise<DietRecord> {
  return post<DietRecord>("/v1/records", data as unknown as Record<string, unknown>);
}

/** 修改记录（克数 / 食物） */
export function update(id: number, data: Partial<CreateRecordRequest>): Promise<DietRecord> {
  return put<DietRecord>(`/v1/records/${id}`, data as unknown as Record<string, unknown>);
}

/** 删除记录 */
export function remove(id: number): Promise<void> {
  return del<void>(`/v1/records/${id}`);
}

/** 今日汇总（实际 vs 目标） */
export function todaySummary(): Promise<RecordSummary> {
  return get<RecordSummary>("/v1/records/today/summary");
}

/** 某一天的明细 */
export function getByDate(date: string): Promise<DietRecord[]> {
  return get<DietRecord[]>(`/v1/records/${date}`);
}

export const recordApi = {
  bulkToday,
  create,
  update,
  remove,
  todaySummary,
  getByDate,
};

import { del, get, post, put } from "./http";

export interface DietRecord {
  id: number;
  userId: number;
  recordDate: string;
  foodId: number;
  foodName: string;
  grams: number;
  energyKcal: number;
  proteinG: number;
  fatG: number;
  carboG: number;
  mealType: number;
}

export interface RecordSummary {
  targetKcal: number;
  actualKcal: number;
  actualProtein: number;
  actualFat: number;
  actualCarbo: number;
  targetProtein: number;
  targetFat: number;
  targetCarbo: number;
}

export interface CreateRecordRequest {
  foodId: number;
  grams: number;
  mealType: number;
  recordDate?: string;
}

/** 一键入库（按今日食谱批量创建记录） */
export function bulkToday(): Promise<DietRecord[]> {
  return post<DietRecord[]>("/v1/records/today/bulk");
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


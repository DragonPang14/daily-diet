import { get } from "./http";

export interface DailyStats {
  date: string;
  actualKcal: number;
  targetKcal: number;
  actualProtein: number;
  actualFat: number;
  actualCarbo: number;
}

export interface WeeklyStats {
  records: DailyStats[];
  avgKcal: number;
  achievementRate: number;
}

export interface Achievement {
  range: number;
  achievementRate: number;
  perfectDays: number;
  totalDays: number;
}

/** 近 7 天趋势数据 */
export function weekly(startDate: string): Promise<WeeklyStats> {
  return get<WeeklyStats>("/v1/stats/weekly", { startDate });
}

/** 近 30 天趋势数据 */
export function monthly(startDate: string): Promise<WeeklyStats> {
  return get<WeeklyStats>("/v1/stats/monthly", { startDate });
}

/** 达成率 */
export function achievement(range = 7): Promise<Achievement> {
  return get<Achievement>("/v1/stats/achievement", { range });
}

export const statsApi = { weekly, monthly, achievement };


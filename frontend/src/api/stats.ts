import { get } from "./http";
import { daysAgo } from "@/utils/format";

/** 每日营养摄入统计（对齐后端 WeeklyStatsVO） */
export interface DailyStats {
  /** 日期 yyyy-MM-dd */
  date: string;
  /** 当日总热量 kcal */
  kcal: number;
  proteinG: number;
  fatG: number;
  carboG: number;
}

/** 打卡达成率（对齐后端 AchievementVO） */
export interface Achievement {
  /** 统计总天数 */
  totalDays: number;
  /** 有饮食记录的天数 */
  activeDays: number;
  /** 达成率 0~1 */
  achievementRate: number;
}

/** 近 N 天日聚合趋势；startDate 缺省为近 7 天 */
export function weekly(startDate?: string): Promise<DailyStats[]> {
  return get<DailyStats[]>("/v1/stats/weekly", startDate ? { startDate } : undefined);
}

/** 近 30 天日聚合趋势（复用 weekly 接口，startDate 传 29 天前） */
export function monthly(): Promise<DailyStats[]> {
  return weekly(daysAgo(29));
}

/** 达成率 */
export function achievement(range = 7): Promise<Achievement> {
  return get<Achievement>("/v1/stats/achievement", { range });
}

export const statsApi = { weekly, monthly, achievement };

import dayjs from "dayjs";

/** 格式化日期，默认 yyyy-MM-dd */
export function formatDate(date: dayjs.ConfigType, pattern = "YYYY-MM-DD"): string {
  return dayjs(date).format(pattern);
}

/** 当日零点日期字符串 */
export function today(): string {
  return dayjs().format("YYYY-MM-DD");
}

/** 取 N 天前的日期字符串 */
export function daysAgo(n: number): string {
  return dayjs().subtract(n, "day").format("YYYY-MM-DD");
}

/** 热量格式化：取整 + kcal 后缀 */
export function formatKcal(n: number): string {
  return `${Math.round(n)} kcal`;
}

/** 克数格式化：取整 + g 后缀 */
export function formatGram(n: number): string {
  return `${Math.round(n)} g`;
}

/** 百分比格式化 */
export function formatPct(n: number): string {
  return `${Math.round(n * 100)}%`;
}

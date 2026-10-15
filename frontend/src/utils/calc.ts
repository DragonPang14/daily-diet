/** 数组字段求和 */
export function sum<T>(arr: T[], pick: (item: T) => number): number {
  return arr.reduce((acc, item) => acc + pick(item), 0);
}

/** 限幅：n 落在 [min, max] */
export function clamp(n: number, min: number, max: number): number {
  if (n < min) return min;
  if (n > max) return max;
  return n;
}

/** PFC 百分比 → 克数（热量为 kcal，PFC 百分比为小数 0~1）
 * 蛋白质/碳水 4 kcal/g，脂肪 9 kcal/g
 */
export function pfcPctToGrams(
  kcal: number,
  pfc: { carbo: number; protein: number; fat: number }
): { carboG: number; proteinG: number; fatG: number } {
  return {
    carboG: (kcal * pfc.carbo) / 4,
    proteinG: (kcal * pfc.protein) / 4,
    fatG: (kcal * pfc.fat) / 9,
  };
}

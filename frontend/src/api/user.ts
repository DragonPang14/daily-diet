import { get, put } from "./http";

export interface UserInfo {
  id: number;
  openid: string;
  nickname: string;
  avatarUrl?: string;
  gender: number; // 1 男 2 女
  birthDate: string;
  height: number;
  weight: number;
  trainingLevel: number; // 1~4
  squat?: number;
  bench?: number;
  deadlift?: number;
  goalType: number; // 1 增肌 2 减脂 3 维持 4 自定义
  customTdee?: number;
  pfcCarboPct: number;
  pfcProteinPct: number;
  pfcFatPct: number;
  bmr?: number;
  tdee?: number;
  muscleFactor?: number;
}

export interface UserTarget {
  targetKcal: number;
  targetCarbo: number;
  targetProtein: number;
  targetFat: number;
}

export interface UpdateBaseInfoRequest {
  nickname?: string;
  gender?: number;
  birthDate?: string;
  height?: number;
  weight?: number;
}

export interface UpdateTrainingRequest {
  squat?: number;
  bench?: number;
  deadlift?: number;
  trainingLevel?: number;
}

export interface UpdateGoalRequest {
  goalType?: number;
  customTdee?: number;
  pfcCarboPct?: number;
  pfcProteinPct?: number;
  pfcFatPct?: number;
}

/** 获取当前用户完整信息 */
export function getMe(): Promise<UserInfo> {
  return get<UserInfo>("/v1/users/me");
}

/** 更新基础信息（触发 BMR/TDEE 重算） */
export function updateMe(data: UpdateBaseInfoRequest): Promise<UserInfo> {
  return put<UserInfo>("/v1/users/me", data as unknown as Record<string, unknown>);
}

/** 更新三大项 + 训练等级 */
export function updateTraining(data: UpdateTrainingRequest): Promise<UserInfo> {
  return put<UserInfo>("/v1/users/me/training", data as unknown as Record<string, unknown>);
}

/** 更新目标 + PFC 比例 */
export function updateGoal(data: UpdateGoalRequest): Promise<UserInfo> {
  return put<UserInfo>("/v1/users/me/goal", data as unknown as Record<string, unknown>);
}

/** 仅返回当日目标热量 + PFC（轻量接口） */
export function getTarget(): Promise<UserTarget> {
  return get<UserTarget>("/v1/users/me/target");
}

export const userApi = { getMe, updateMe, updateTraining, updateGoal, getTarget };


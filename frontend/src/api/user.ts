import { get, put } from "./http";

/** 当前用户完整信息（对齐后端 User 实体） */
export interface UserInfo {
  id: number;
  openid: string;
  nickname: string;
  avatarUrl?: string;
  /** 性别：0=女 1=男 */
  gender: number;
  /** 出生日期 yyyy-MM-dd */
  birthday: string;
  /** 身高 cm */
  height: number;
  /** 当前体重 kg */
  currentWeight: number;
  /** 目标体重 kg（可选） */
  targetWeight?: number;
  /** 深蹲 1RM kg */
  squat1rm?: number;
  /** 卧推 1RM kg */
  bench1rm?: number;
  /** 硬拉 1RM kg */
  deadlift1rm?: number;
  /** 训练等级：0=未填 1=久坐 2=轻度活动 3=规律力量训练 4=高强度 */
  trainingLevel: number;
  /** 目标类型：1=增肌 2=减脂 3=维持 4=自定义 */
  goalType: number;
  /** 自定义每日目标热量 kcal（仅 goalType=4） */
  customTdee?: number;
  pfcCarboPct: number;
  pfcProteinPct: number;
  pfcFatPct: number;
  bmr?: number;
  tdee?: number;
  muscleFactor?: number;
}

/** 当日目标（对齐后端 TargetResponse） */
export interface UserTarget {
  bmr: number;
  tdee: number;
  targetKcal: number;
  pfcCarboPct: number;
  pfcProteinPct: number;
  pfcFatPct: number;
  targetCarboG: number;
  targetProteinG: number;
  targetFatG: number;
}

/** 更新基础信息（对齐后端 UserUpdateRequest） */
export interface UpdateBaseInfoRequest {
  gender?: number;
  birthday?: string;
  height?: number;
  currentWeight?: number;
  targetWeight?: number;
}

/** 更新三大项 + 训练等级（对齐后端 TrainingUpdateRequest） */
export interface UpdateTrainingRequest {
  squat1rm?: number;
  bench1rm?: number;
  deadlift1rm?: number;
  trainingLevel?: number;
}

/** 更新目标 + PFC 比例（对齐后端 GoalUpdateRequest） */
export interface UpdateGoalRequest {
  goalType: number;
  customTdee?: number;
  pfcCarboPct?: number;
  pfcProteinPct?: number;
  pfcFatPct?: number;
}

/** 获取当前用户完整信息 */
export function getMe(): Promise<UserInfo> {
  return get<UserInfo>("/users/me");
}

/** 更新基础信息（触发 BMR/TDEE 重算） */
export function updateMe(data: UpdateBaseInfoRequest): Promise<UserInfo> {
  return put<UserInfo>("/users/me", data as unknown as Record<string, unknown>);
}

/** 更新三大项 + 训练等级 */
export function updateTraining(data: UpdateTrainingRequest): Promise<UserInfo> {
  return put<UserInfo>("/users/me/training", data as unknown as Record<string, unknown>);
}

/** 更新目标 + PFC 比例 */
export function updateGoal(data: UpdateGoalRequest): Promise<UserInfo> {
  return put<UserInfo>("/users/me/goal", data as unknown as Record<string, unknown>);
}

/** 仅返回当日目标热量 + PFC（轻量接口） */
export function getTarget(): Promise<UserTarget> {
  return get<UserTarget>("/users/me/target");
}

export const userApi = { getMe, updateMe, updateTraining, updateGoal, getTarget };

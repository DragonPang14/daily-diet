package com.dailydiet.algorithm;

import com.dailydiet.common.enums.GoalType;
import org.springframework.stereotype.Component;

/**
 * TDEE (每日总能量消耗) 计算器
 */
@Component
public class TdeeCalculator {

    // 活动系数
    private static final double[] ACTIVITY_FACTORS = {
            1.2,    // 0=久坐
            1.375,  // 1=轻度
            1.55,   // 2=力量训练
            1.725   // 3=高强度
    };

    // 目标偏移
    private static final int GOAL_OFFSET = 400;

    /**
     * 计算 TDEE
     *
     * @param correctedBmr   修正后 BMR
     * @param trainingLevel 训练等级 0-3
     */
    public double calculate(double correctedBmr, int trainingLevel) {
        int idx = Math.max(0, Math.min(3, trainingLevel));
        return correctedBmr * ACTIVITY_FACTORS[idx];
    }

    /**
     * 根据目标类型返回最终目标热量
     */
    public int applyGoal(double tdee, GoalType goalType, Integer customTdee) {
        if (goalType == GoalType.CUSTOM && customTdee != null) {
            return customTdee;
        }
        return switch (goalType) {
            case BULK     -> (int) Math.round(tdee + GOAL_OFFSET);
            case CUT      -> (int) Math.round(tdee - GOAL_OFFSET);
            case MAINTAIN -> (int) Math.round(tdee);
            case CUSTOM   -> customTdee != null ? customTdee : (int) Math.round(tdee);
        };
    }

    /**
     * 根据 goal_type 返回默认 PFC 比例 [碳水%, 蛋白%, 脂肪%]
     */
    public int[] defaultPfcRatio(GoalType goalType) {
        return switch (goalType) {
            case BULK     -> new int[]{45, 32, 23};
            case CUT      -> new int[]{35, 37, 28};
            case MAINTAIN -> new int[]{45, 28, 27};
            case CUSTOM   -> new int[]{50, 30, 20};
        };
    }
}

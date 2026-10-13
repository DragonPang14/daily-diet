package com.dailydiet.algorithm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 肌肉量修正系数估算器 — 基于三大项 1RM
 */
@Component
public class MuscleEstimator {

    // 男性阈值
    private static final double MALE_HIGH = 6.0;
    private static final double MALE_MID  = 4.5;
    // 女性阈值
    private static final double FEMALE_HIGH = 5.0;
    private static final double FEMALE_MID  = 3.5;

    /**
     * 计算肌肉修正系数
     *
     * @param gender       0=女 1=男
     * @param weightKg     体重 kg
     * @param squat1rm     深蹲 1RM (kg)，null 或 0 视为未填
     * @param bench1rm     卧推 1RM
     * @param deadlift1rm  硬拉 1RM
     * @return 修正系数 (1.000 / 1.040 / 1.080)
     */
    public double correct(int gender, double weightKg,
                          BigDecimal squat1rm, BigDecimal bench1rm, BigDecimal deadlift1rm) {
        if (squat1rm == null || bench1rm == null || deadlift1rm == null) return 1.0;
        if (squat1rm.doubleValue() <= 0 || bench1rm.doubleValue() <= 0 || deadlift1rm.doubleValue() <= 0) {
            return 1.0;
        }

        double total = squat1rm.doubleValue() + bench1rm.doubleValue() + deadlift1rm.doubleValue();
        double strengthIndex = total / weightKg;

        if (gender == 1) {
            if (strengthIndex >= MALE_HIGH) return 1.08;
            if (strengthIndex >= MALE_MID)  return 1.04;
            return 1.00;
        } else {
            if (strengthIndex >= FEMALE_HIGH) return 1.08;
            if (strengthIndex >= FEMALE_MID)  return 1.04;
            return 1.00;
        }
    }
}

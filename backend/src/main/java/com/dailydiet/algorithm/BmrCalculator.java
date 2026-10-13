package com.dailydiet.algorithm;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

/**
 * BMR (基础代谢) 计算器 — Mifflin-St Jeor 公式
 */
@Component
public class BmrCalculator {

    /**
     * 计算 BMR
     *
     * @param gender    0=女 1=男
     * @param weightKg  体重 kg
     * @param heightCm  身高 cm
     * @param birthday  生日
     */
    public double mifflinStJeor(int gender, double weightKg, double heightCm, LocalDate birthday) {
        int age = Period.between(birthday, LocalDate.now()).getYears();
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        return gender == 1 ? base - 5 : base + 161;
    }
}

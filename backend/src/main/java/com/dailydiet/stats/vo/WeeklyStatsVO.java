package com.dailydiet.stats.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 每日营养摄入统计 VO
 */
@Data
@AllArgsConstructor
public class WeeklyStatsVO {

    /** 日期，格式 yyyy-MM-dd */
    private String date;

    /** 当日总热量，单位 kcal */
    private double kcal;

    /** 当日蛋白质，单位 g */
    private double proteinG;

    /** 当日脂肪，单位 g */
    private double fatG;

    /** 当日碳水化合物，单位 g */
    private double carboG;
}

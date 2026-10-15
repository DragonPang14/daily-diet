package com.dailydiet.stats.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 打卡达成率 VO
 */
@Data
@AllArgsConstructor
public class AchievementVO {

    /** 统计总天数 */
    private int totalDays;

    /** 有饮食记录的天数 */
    private int activeDays;

    /** 达成率 = activeDays / totalDays，取值 0~1 */
    private double achievementRate;
}

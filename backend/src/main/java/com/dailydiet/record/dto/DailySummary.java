package com.dailydiet.record.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DailySummary {

    private BigDecimal totalKcal;
    private BigDecimal totalProteinG;
    private BigDecimal totalFatG;
    private BigDecimal totalCarboG;

    private Integer targetKcal;
    private BigDecimal targetProteinG;
    private BigDecimal targetFatG;
    private BigDecimal targetCarboG;

    private double achievementRate;
    private List<com.dailydiet.record.entity.DietRecord> records;
}

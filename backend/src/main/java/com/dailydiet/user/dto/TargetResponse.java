package com.dailydiet.user.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TargetResponse {

    private BigDecimal bmr;
    private BigDecimal tdee;
    private int targetKcal;
    private int pfcCarboPct;
    private int pfcProteinPct;
    private int pfcFatPct;

    private double targetCarboG;
    private double targetProteinG;
    private double targetFatG;
}

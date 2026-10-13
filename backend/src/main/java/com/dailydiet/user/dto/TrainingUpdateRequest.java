package com.dailydiet.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrainingUpdateRequest {

    private BigDecimal squat1rm;
    private BigDecimal bench1rm;
    private BigDecimal deadlift1rm;

    @Min(value = 0)
    @Max(value = 4)
    private Integer trainingLevel;
}

package com.dailydiet.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GoalUpdateRequest {

    @NotNull
    @Min(1) @Max(4)
    private Integer goalType;

    private Integer customTdee;

    @Min(0) @Max(100)
    private Integer pfcCarboPct;

    @Min(0) @Max(100)
    private Integer pfcProteinPct;

    @Min(0) @Max(100)
    private Integer pfcFatPct;
}

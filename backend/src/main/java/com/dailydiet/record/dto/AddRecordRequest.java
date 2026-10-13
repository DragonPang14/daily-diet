package com.dailydiet.record.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddRecordRequest {

    @NotNull
    @Min(1) @Max(4)
    private Integer mealType;

    @NotNull
    private Long foodId;

    @NotNull @DecimalMin("1")
    private BigDecimal foodWeightG;

    private Long recipeItemId;
}

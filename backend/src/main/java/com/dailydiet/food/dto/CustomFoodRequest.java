package com.dailydiet.food.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomFoodRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String category;

    private String brand;

    @NotNull @DecimalMin("0")
    private BigDecimal energyKcal;

    @NotNull @DecimalMin("0")
    private BigDecimal proteinG;

    @NotNull @DecimalMin("0")
    private BigDecimal fatG;

    @NotNull @DecimalMin("0")
    private BigDecimal carboG;

    private BigDecimal commonWeight;
    private String imageUrl;
}

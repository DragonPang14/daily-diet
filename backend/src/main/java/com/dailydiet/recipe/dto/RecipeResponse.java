package com.dailydiet.recipe.dto;

import com.dailydiet.recipe.entity.RecipeItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RecipeResponse {

    private Long id;
    private LocalDate recipeDate;
    private Integer targetKcal;
    private BigDecimal targetCarboG;
    private BigDecimal targetProteinG;
    private BigDecimal targetFatG;
    private List<RecipeItem> items;
}

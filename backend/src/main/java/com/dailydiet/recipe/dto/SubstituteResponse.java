package com.dailydiet.recipe.dto;

import com.dailydiet.food.entity.Food;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SubstituteResponse {

    private List<Food> candidates;
}

package com.dailydiet.food.dto;

import lombok.Data;

@Data
public class FoodSearchRequest {

    private String keyword;
    private String category;
    private int page = 1;
    private int size = 20;
}

package com.dailydiet.common.enums;

import lombok.Getter;

@Getter
public enum FoodCategory {

    GRAIN("谷物"),
    PROTEIN("蛋白"),
    VEGETABLE("蔬菜"),
    FRUIT("水果"),
    DAIRY("乳品"),
    NUT("坚果"),
    CONDIMENT("调料");

    private final String desc;

    FoodCategory(String desc) {
        this.desc = desc;
    }
}

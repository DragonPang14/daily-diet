package com.dailydiet.common.enums;

import lombok.Getter;

@Getter
public enum MealType {

    BREAKFAST(1, "早餐"),
    LUNCH(2, "午餐"),
    DINNER(3, "晚餐"),
    SNACK(4, "加餐");

    private final int code;
    private final String desc;

    MealType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MealType fromCode(int code) {
        for (MealType m : values()) {
            if (m.code == code) return m;
        }
        throw new IllegalArgumentException("未知餐次: " + code);
    }
}

package com.dailydiet.common.constant;

/**
 * 食物数据来源常量 —— foods.source 字段取值
 */
public final class FoodSource {

    private FoodSource() {
    }

    /** 中国食物成分表（标准库） */
    public static final int STANDARD = 1;

    /** 用户自定义 */
    public static final int CUSTOM = 2;
}

package com.dailydiet.common.enums;

import lombok.Getter;

@Getter
public enum GoalType {

    BULK(1, "增肌"),
    CUT(2, "减脂"),
    MAINTAIN(3, "维持"),
    CUSTOM(4, "自定义");

    private final int code;
    private final String desc;

    GoalType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static GoalType fromCode(int code) {
        for (GoalType g : values()) {
            if (g.code == code) return g;
        }
        throw new IllegalArgumentException("未知目标类型: " + code);
    }
}

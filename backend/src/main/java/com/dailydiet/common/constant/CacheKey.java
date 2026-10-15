package com.dailydiet.common.constant;

/**
 * Redis 缓存 key 前缀常量 —— 统一管理，避免跨应用 key 冲突
 */
public final class CacheKey {

    private CacheKey() {
    }

    /** 应用统一前缀 */
    public static final String APP_PREFIX = "daily-diet:";

    /** 食物详情缓存 key 前缀 */
    public static final String FOOD = APP_PREFIX + "food:";

    /** 食物缓存过期时间（分钟） */
    public static final long FOOD_TTL_MINUTES = 30;
}

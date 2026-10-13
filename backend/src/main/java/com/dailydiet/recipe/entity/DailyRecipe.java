package com.dailydiet.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日食谱（推荐方案）—— 由推荐引擎根据用户目标自动生成
 */
@Data
@TableName("daily_recipes")
public class DailyRecipe {

    /** 食谱 ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 食谱对应的日期 */
    private LocalDate recipeDate;

    // ===== 当日目标（生成时快照，便于追溯） =====

    /** 当日目标热量，单位 kcal */
    private Integer targetKcal;

    /** 当日目标碳水化合物，单位 g */
    private BigDecimal targetCarboG;

    /** 当日目标蛋白质，单位 g */
    private BigDecimal targetProteinG;

    /** 当日目标脂肪，单位 g */
    private BigDecimal targetFatG;

    /** 生成时的用户快照元数据（JSON），如生成时的 BMR/goal/PFC 比例等 */
    private String generationMeta;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

package com.dailydiet.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食谱食材明细 —— 一份 DailyRecipe 由多个 RecipeItem 组成
 */
@Data
@TableName("recipe_items")
public class RecipeItem {

    /** 明细 ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属食谱 ID，关联 daily_recipes.id */
    private Long recipeId;

    /** 餐次类型：1=早餐，2=午餐，3=晚餐，4=加餐 */
    private Integer mealType;

    /** 食材 ID，关联 foods.id */
    private Long foodId;

    /** 推荐克数，单位 g */
    private BigDecimal foodWeightG;

    // ===== 该食材在推荐克数下的营养贡献 =====

    /** 能量，单位 kcal */
    private BigDecimal energyKcal;

    /** 蛋白质，单位 g */
    private BigDecimal proteinG;

    /** 脂肪，单位 g */
    private BigDecimal fatG;

    /** 碳水化合物，单位 g */
    private BigDecimal carboG;

    /** 同餐次内排序序号，从 0 开始 */
    private Integer orderIndex;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

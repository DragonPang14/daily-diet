package com.dailydiet.record.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日实际饮食记录 —— 用户当天真正吃了什么
 */
@Data
@TableName("diet_records")
public class DietRecord {

    /** 记录 ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 记录日期（按天聚合） */
    private LocalDate recordDate;

    /** 餐次类型：1=早餐，2=午餐，3=晚餐，4=加餐 */
    private Integer mealType;

    /** 食材 ID，关联 foods.id */
    private Long foodId;

    /** 实际食用克数，单位 g */
    private BigDecimal foodWeightG;

    // ===== 该记录对应的营养成分（按实际克数折算） =====

    /** 能量，单位 kcal */
    private BigDecimal energyKcal;

    /** 蛋白质，单位 g */
    private BigDecimal proteinG;

    /** 脂肪，单位 g */
    private BigDecimal fatG;

    /** 碳水化合物，单位 g */
    private BigDecimal carboG;

    /** 记录来源：1=一键入库（从推荐食谱批量导入），2=手动添加，3=替换食材后自动生成 */
    private Integer source;

    /** 若该记录来自推荐食谱，关联 recipe_items.id */
    private Long recipeItemId;

    /** 逻辑删除：0=未删除，1=已删除（MyBatis-Plus 自动维护） */
    @TableLogic
    private Integer deleted;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

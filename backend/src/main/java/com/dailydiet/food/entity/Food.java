package com.dailydiet.food.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食物库（标准库 + 用户自定义）
 */
@Data
@TableName("foods")
public class Food {

    /** 食物 ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食物名称，如"鸡胸肉"、"桃李全麦面包" */
    private String name;

    /** 食物类别：谷物/蛋白/蔬菜/水果/乳品/坚果/调料 */
    private String category;

    /** 品牌（标准库为空，自定义食物可填） */
    private String brand;

    /** 数据来源：1=中国食物成分表（标准库），2=用户自定义 */
    private Integer source;

    // ===== 每 100g 营养成分 =====

    /** 能量，单位 kcal / 100g */
    private BigDecimal energyKcal;

    /** 蛋白质，单位 g / 100g */
    private BigDecimal proteinG;

    /** 脂肪，单位 g / 100g */
    private BigDecimal fatG;

    /** 碳水化合物，单位 g / 100g */
    private BigDecimal carboG;

    /** 常用份量，单位 g；如"1 个鸡蛋=50g"，减少记录时的输入 */
    private BigDecimal commonWeight;

    /** 食物图片 URL（自定义食物可上传） */
    private String imageUrl;

    /** PFC 向量冗余存储，格式 JSON："[碳水g,蛋白g,脂肪g]"，用于余弦相似度快速计算 */
    private String pfcVector;

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

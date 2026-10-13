package com.dailydiet.recipe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户标记"不喜欢"的食材 —— dislike_count 累计到 3 次则从推荐池排除
 */
@Data
@TableName("food_dislikes")
public class FoodDislike {

    /** 记录 ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 被标记不喜欢的食材 ID，关联 foods.id */
    private Long foodId;

    /** 累计被标记次数；>=3 次时推荐引擎排除该食材 */
    private Integer dislikeCount;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

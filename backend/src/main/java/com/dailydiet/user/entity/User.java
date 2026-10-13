package com.dailydiet.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("users")
public class User {

    /** 用户 ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信 openid，唯一标识一个小程序用户 */
    private String openid;

    /** 用户昵称（从微信授权获取或自行设置） */
    private String nickname;

    /** 用户头像 URL */
    private String avatarUrl;

    // ===== 基础信息 =====

    /** 性别：0=女，1=男 */
    private Integer gender;

    /** 出生日期 */
    private LocalDate birthday;

    /** 身高，单位 cm */
    private BigDecimal height;

    /** 当前体重，单位 kg */
    private BigDecimal currentWeight;

    /** 目标体重（可选），单位 kg */
    private BigDecimal targetWeight;

    // ===== 训练水平 =====

    /** 深蹲 1RM（一次性最大负重），单位 kg；null 或 0 视为未填 */
    private BigDecimal squat1rm;

    /** 卧推 1RM，单位 kg */
    private BigDecimal bench1rm;

    /** 硬拉 1RM，单位 kg */
    private BigDecimal deadlift1rm;

    /** 训练等级：0=未填/默认，1=久坐，2=轻度活动，3=规律力量训练，4=高强度 */
    private Integer trainingLevel;

    // ===== 目标设置 =====

    /** 目标类型：1=增肌，2=减脂，3=维持，4=自定义 */
    private Integer goalType;

    /** 自定义每日目标热量，单位 kcal；仅 goal_type=4 时有值 */
    private Integer customTdee;

    // ===== 计算结果（冗余字段，基础信息变更时重算） =====

    /** 基础代谢 BMR（Mifflin-St Jeor 公式结果） */
    private BigDecimal bmr;

    /** 每日总能量消耗 TDEE = BMR × 活动系数 × 肌肉修正系数 */
    private BigDecimal tdee;

    /** 肌肉量修正系数：1.00=未估算/新手，1.04=中级，1.08=高级 */
    private BigDecimal muscleFactor;

    // ===== PFC 偏好（碳水/蛋白/脂肪 占比之和=100） =====

    /** 碳水化合物占比 % */
    private Integer pfcCarboPct;

    /** 蛋白质占比 % */
    private Integer pfcProteinPct;

    /** 脂肪占比 % */
    private Integer pfcFatPct;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

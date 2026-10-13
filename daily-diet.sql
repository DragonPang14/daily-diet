-- =============================================================================
-- 每日饮食助手 (daily-diet) 数据库初始化脚本
-- 目标版本: MySQL 8.0.45 (Ubuntu 24.04)
-- 字符集:   utf8mb4 / utf8mb4_unicode_ci
-- 引擎:     InnoDB
-- 数据库名: diet
--
-- 执行方式（Ubuntu 24.04）:
--   方式 A: mysql -u root -p < /path/to/daily-diet.sql
--   方式 B: mysql -u root -p
--           > source /path/to/daily-diet.sql;
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. 基础会话设置
-- -----------------------------------------------------------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------------------------------
-- 1. 创建数据库（若已存在则跳过；需要重建请先手动 DROP DATABASE diet;）
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS diet
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE diet;

-- -----------------------------------------------------------------------------
-- 2. 用户表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    openid          VARCHAR(64)  NOT NULL COMMENT '微信 openid',
    nickname        VARCHAR(64)           DEFAULT NULL,
    avatar_url      VARCHAR(512)          DEFAULT NULL,

    -- Onboarding 基础信息
    gender          TINYINT      NOT NULL COMMENT '0=女 1=男',
    birthday        DATE         NOT NULL,
    height          DECIMAL(5,2) NOT NULL COMMENT 'cm',
    current_weight  DECIMAL(5,2) NOT NULL COMMENT 'kg',
    target_weight   DECIMAL(5,2)          DEFAULT NULL,

    -- 训练水平
    squat_1rm       DECIMAL(5,2)          DEFAULT NULL COMMENT 'kg',
    bench_1rm       DECIMAL(5,2)          DEFAULT NULL,
    deadlift_1rm    DECIMAL(5,2)          DEFAULT NULL,
    training_level  TINYINT                DEFAULT 0 COMMENT '0=未填 1=久坐 2=轻度 3=力量 4=高强度',

    -- 目标
    goal_type       TINYINT      NOT NULL COMMENT '1=增肌 2=减脂 3=维持 4=自定义',
    custom_tdee     INT                    DEFAULT NULL COMMENT '自定义目标热量 kcal',

    -- 计算结果（冗余；每次基础信息变更时重算）
    bmr             DECIMAL(7,2) NOT NULL DEFAULT 0,
    tdee            DECIMAL(7,2) NOT NULL DEFAULT 0,
    muscle_factor   DECIMAL(4,3) NOT NULL DEFAULT 1.000 COMMENT '肌肉修正系数',

    -- PFC 偏好（%），三项之和必须 = 100
    -- 应用层保证：Onboarding 时按 goal_type 初始化，用户可手动覆盖
    pfc_carbo_pct   INT          NOT NULL DEFAULT 50,
    pfc_protein_pct INT          NOT NULL DEFAULT 30,
    pfc_fat_pct     INT          NOT NULL DEFAULT 20,

    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_openid (openid),
    INDEX idx_goal (goal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------------------------
-- 3. 食物库（标准 + 自定义）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS foods;
CREATE TABLE foods (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(128) NOT NULL COMMENT '食物名称',
    category        VARCHAR(32)  NOT NULL COMMENT '谷物/蛋白/蔬菜/水果/乳品/坚果/调料',
    brand           VARCHAR(64)           DEFAULT NULL COMMENT '品牌（标准库为空）',
    source          TINYINT      NOT NULL COMMENT '1=中国食物成分表 2=用户自定义',

    -- 每 100g 营养
    energy_kcal     DECIMAL(7,2) NOT NULL COMMENT 'kcal',
    protein_g       DECIMAL(6,2) NOT NULL COMMENT 'g',
    fat_g           DECIMAL(6,2) NOT NULL,
    carbo_g         DECIMAL(6,2) NOT NULL,

    common_weight   DECIMAL(6,2)          DEFAULT NULL COMMENT '常用份量 g（如 1 个鸡蛋≈50g）',
    image_url       VARCHAR(512)          DEFAULT NULL,

    -- PFC 向量冗余（便于快速余弦相似度计算），格式：JSON 字符串 "[c,p,f]"
    pfc_vector      VARCHAR(64)           DEFAULT NULL,

    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_name (name),
    INDEX idx_category (category),
    INDEX idx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物库（标准 + 自定义）';

-- -----------------------------------------------------------------------------
-- 4. 用户自定义 / 收藏食物元数据
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS user_custom_foods;
CREATE TABLE user_custom_foods (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    food_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 foods.id',
    is_favorite     TINYINT      NOT NULL DEFAULT 0,
    is_common       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否常用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_food (user_id, food_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义/收藏食物';

-- -----------------------------------------------------------------------------
-- 5. 用户不喜欢的食材（累计 dislike_count >= 3 则从推荐池排除）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS food_dislikes;
CREATE TABLE food_dislikes (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    food_id         BIGINT UNSIGNED NOT NULL,
    dislike_count   INT          NOT NULL DEFAULT 1 COMMENT '累计被标记次数',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_food (user_id, food_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户不喜欢的食材';

-- -----------------------------------------------------------------------------
-- 6. 每日食谱（推荐方案）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS daily_recipes;
CREATE TABLE daily_recipes (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    recipe_date     DATE         NOT NULL COMMENT '食谱日期',

    target_kcal     INT          NOT NULL COMMENT '当日目标热量 kcal',
    target_carbo_g  DECIMAL(7,2) NOT NULL,
    target_protein_g DECIMAL(7,2) NOT NULL,
    target_fat_g    DECIMAL(7,2) NOT NULL,

    -- 生成时的用户快照（BMR、goal、PFC 等），便于追溯与复现
    generation_meta JSON                  DEFAULT NULL,

    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_date (user_id, recipe_date),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日食谱（推荐方案）';

-- -----------------------------------------------------------------------------
-- 7. 食谱食材明细
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS recipe_items;
CREATE TABLE recipe_items (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    recipe_id       BIGINT UNSIGNED NOT NULL,
    meal_type       TINYINT      NOT NULL COMMENT '1=早 2=午 3=晚 4=加餐',
    food_id         BIGINT UNSIGNED NOT NULL,
    food_weight_g   DECIMAL(6,2) NOT NULL COMMENT '推荐克数',

    -- 该食材贡献
    energy_kcal     DECIMAL(7,2) NOT NULL,
    protein_g       DECIMAL(6,2) NOT NULL,
    fat_g           DECIMAL(6,2) NOT NULL,
    carbo_g         DECIMAL(6,2) NOT NULL,

    order_index     INT          NOT NULL DEFAULT 0 COMMENT '同餐次内排序',

    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_recipe (recipe_id),
    INDEX idx_meal (meal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱食材明细';

-- -----------------------------------------------------------------------------
-- 8. 每日实际饮食记录
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS diet_records;
CREATE TABLE diet_records (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    record_date     DATE         NOT NULL,
    meal_type       TINYINT      NOT NULL COMMENT '1=早 2=午 3=晚 4=加餐',
    food_id         BIGINT UNSIGNED NOT NULL,
    food_weight_g   DECIMAL(6,2) NOT NULL,

    energy_kcal     DECIMAL(7,2) NOT NULL,
    protein_g       DECIMAL(6,2) NOT NULL,
    fat_g           DECIMAL(6,2) NOT NULL,
    carbo_g         DECIMAL(6,2) NOT NULL,

    source          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=一键入库 2=手动添加 3=替换',
    recipe_item_id  BIGINT UNSIGNED          DEFAULT NULL COMMENT '若从食谱来，关联 recipe_items.id',

    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_date (user_id, record_date),
    INDEX idx_food (food_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日实际饮食记录';

-- -----------------------------------------------------------------------------
-- 9. 外键约束（应用层保证数据一致性；FK 仅做兜底，不影响写入性能）
-- -----------------------------------------------------------------------------
ALTER TABLE user_custom_foods
    ADD CONSTRAINT fk_ucf_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    ADD CONSTRAINT fk_ucf_food FOREIGN KEY (food_id) REFERENCES foods (id) ON DELETE CASCADE;

ALTER TABLE food_dislikes
    ADD CONSTRAINT fk_fd_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    ADD CONSTRAINT fk_fd_food  FOREIGN KEY (food_id) REFERENCES foods (id) ON DELETE CASCADE;

ALTER TABLE daily_recipes
    ADD CONSTRAINT fk_dr_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE;

ALTER TABLE recipe_items
    ADD CONSTRAINT fk_ri_recipe FOREIGN KEY (recipe_id) REFERENCES daily_recipes (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ri_food   FOREIGN KEY (food_id)   REFERENCES foods (id)    ON DELETE CASCADE;

ALTER TABLE diet_records
    ADD CONSTRAINT fk_dr_user_fk   FOREIGN KEY (user_id)       REFERENCES users (id)        ON DELETE CASCADE,
    ADD CONSTRAINT fk_dr_food      FOREIGN KEY (food_id)       REFERENCES foods (id)        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_dr_recipe_item FOREIGN KEY (recipe_item_id) REFERENCES recipe_items (id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- 10. 常用查询加速索引
-- -----------------------------------------------------------------------------

-- 食物搜索前缀查询（先 category 再 name 模糊匹配）
-- 现有 idx_name / idx_category 已够用，此处按实际 QPS 观察再决定是否加联合索引

-- 每日汇总：按 user + date 拉取 diet_records
-- 已有 idx_user_date

-- 推荐引擎：按 category + source 拉取候选食物
-- 已有 idx_category + idx_source

SET FOREIGN_KEY_CHECKS = 1;

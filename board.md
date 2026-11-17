# 开发看板（board）

> 记录 daily-diet 当前已实现的内容。最后更新：2026-11-17
> 说明：本文件只记录**代码中已落地**的能力；规划中但尚未实现的内容见文末「未实现 / 待办」。

---

## 一、项目概况

| 项 | 内容 |
| --- | --- |
| 项目名 | 每日饮食助手（daily-diet） |
| 形态 | 微信小程序 / H5（uni-app 跨端），后端为单体 REST 服务 |
| 后端 | Java 17 + Spring Boot 3.3.5 |
| 前端 | uni-app + Vue 3.4 + TypeScript + Vite 5 + Pinia + uview-plus |
| 数据库 | MySQL 8（库名 `diet`）+ Redis 缓存 |
| 当前阶段 | v1.0 MVP 主链路已跑通，v1.1 部分能力已实现 |

---

## 二、后端已实现

### 2.1 模块清单

| 模块 | 包路径 | 实现内容 |
| --- | --- | --- |
| 鉴权 | `auth` | 微信 `code` 登录、开发态免登录（dev profile）、JWT 签发与校验、401 统一返回 |
| 用户 | `user` | 用户信息查询/更新、训练三大项与训练等级、目标与 PFC 设置、当日目标热量计算 |
| 食物库 | `food` | 分页搜索（名称模糊 + 分类过滤）、详情（Redis 缓存）、自定义食物增删改 |
| 食谱推荐 | `recipe` | 每日食谱生成、今日/指定日期查询、食材替换候选、执行替换、标记不喜欢 |
| 饮食记录 | `record` | 食谱一键入库、单条增删改、按日期查询、今日汇总（实际 vs 目标） |
| 统计 | `stats` | 近 7/30 天每日营养聚合、原始记录区间查询、打卡达成率 |
| 公共能力 | `common` / `config` | 统一响应 `ResponseResult`、全局异常处理、业务异常、逻辑删除、Redis/MyBatis-Plus/Security/HTTP 客户端配置 |

### 2.2 接口清单（均已实现）

**鉴权**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/wx-login` | 微信小程序登录，返回 JWT |
| POST | `/api/auth/dev-login` | 仅 `dev` profile 注册，测试用户直接签发 JWT |

**用户**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/users/me` | 当前用户完整信息 |
| PUT | `/api/users/me` | 更新基础信息并重算 BMR/TDEE |
| PUT | `/api/users/me/training` | 更新三大项 1RM + 训练等级 |
| PUT | `/api/users/me/goal` | 更新目标类型 / 自定义 TDEE / PFC（校验 PFC 合计 100） |
| GET | `/api/users/me/target` | 当日目标热量与 PFC 克数 |

**食物库**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/foods` | 分页搜索（`keyword` / `category` / `page` / `size`），免登录 |
| GET | `/api/foods/{id}` | 食物详情（Redis 缓存），免登录 |
| POST | `/api/foods/custom` | 新增自定义食物（自动生成 PFC 向量） |
| PUT | `/api/foods/custom/{id}` | 修改自定义食物（仅限 source=2） |
| DELETE | `/api/foods/custom/{id}` | 删除自定义食物（仅限 source=2） |

**食谱**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/recipes/generate` | 生成当日食谱（同日重复生成会重建） |
| GET | `/api/recipes/today` | 今日食谱（不存在则自动生成） |
| GET | `/api/recipes/{date}` | 指定日期食谱 |
| GET | `/api/recipes/items/{itemId}/substitute` | 返回 Top 3 替换候选 |
| POST | `/api/recipes/items/{itemId}/substitute?foodId=` | 执行替换并重算克数/营养 |
| POST | `/api/recipes/items/{itemId}/dislike` | 标记不喜欢（累计 3 次移出推荐池） |

**饮食记录**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/records/today/bulk` | 按食谱 id 一键入库 |
| POST | `/api/records` | 手动添加单条记录 |
| PUT | `/api/records/{id}` | 修改记录（克数/食物） |
| DELETE | `/api/records/{id}` | 逻辑删除记录 |
| GET | `/api/records/today/summary` | 今日汇总（总热量、PFC、目标、达成率、明细） |
| GET | `/api/records/{date}` | 指定日期明细 |

**统计**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/stats/weekly?startDate=` | 区间每日营养聚合，默认近 7 天 |
| GET | `/api/stats/monthly?startDate=` | 区间原始记录，默认近 30 天 |
| GET | `/api/stats/achievement?range=` | 打卡达成率，默认 7 天 |

### 2.3 核心算法（已实现）

| 算法 | 类 | 实现要点 |
| --- | --- | --- |
| BMR | `BmrCalculator` | Mifflin-St Jeor 公式 |
| 肌肉修正 | `MuscleEstimator` | 三大项总和 / 体重得强度指数；男 ≥6.0 / ≥4.5，女 ≥5.0 / ≥3.5，对应系数 1.08 / 1.04 / 1.00 |
| TDEE | `TdeeCalculator` | 活动系数 1.2 / 1.375 / 1.55 / 1.725；目标偏移 ±400 kcal；按目标给出默认 PFC 比例 |
| PFC 向量 | `PfcVector` | 碳水/蛋白/脂肪三维向量与余弦相似度 |
| 替换匹配 | `SubstitutionMatcher` | 同类别 + 热量 ±20% 预筛；得分 = 0.6×PFC 余弦 + 0.4×热量接近度 − 不喜欢惩罚，取 Top 3 |
| 食谱生成 | `RecipeService` | 餐次比例早 25% / 午 35% / 晚 30% / 加餐 10%；蛋白、谷物按缺口 60% 反算，乳品、水果按 30%，蔬菜兜底 200g；按类别随机选标准库食物；排除不喜欢 ≥3 次的食材 |

### 2.4 其他后端能力

- 统一响应体 `ResponseResult { code, msg, data }`，`code=0` 表示成功
- 全局异常处理，未登录返回 401、无权限返回 403（JSON 格式统一）
- MyBatis-Plus 逻辑删除（`deleted` 字段），查询自动过滤
- 食物详情 Redis 缓存，自定义食物变更时失效缓存
- Swagger / OpenAPI：`/swagger-ui.html`、`/v3/api-docs`
- Actuator：暴露 `health`、`info`、`metrics`
- 配置分离：`application.yml` + `dev` / `prod` profile，密钥经 `.env` 注入
- 越权保护：食谱项操作前校验归属当前用户

---

## 三、前端已实现

### 3.1 页面（7 个）

| 页面 | 路径 | 实现内容 |
| --- | --- | --- |
| 首页 | `pages/home/index` | 今日营养环形/进度展示、目标对比、食谱与记录入口 |
| 引导页 | `pages/onboarding/index` | 基础信息、训练水平、目标与 PFC 设置 |
| 食谱页 | `pages/recipe/index` | 今日食谱展示、替换食材底部弹层、标记不喜欢 |
| 食物库 | `pages/food/index` | 搜索、分类筛选、分页列表、自定义食物维护；支持「挑选模式」供记录页跨 tab 选食物 |
| 记录页 | `pages/record/index` | 今日记录、手动添加、修改/删除、一键入库、汇总 |
| 统计页 | `pages/stats/index` | 近 7/30 天趋势、达成率 |
| 我的 | `pages/mine/index` | 用户信息与目标设置入口 |

底部 tabBar 为：首页 / 食物 / 记录 / 统计 / 我的。

### 3.2 组件（6 个）

`MealCard`（餐次卡片）、`NutriRing`（营养环形图）、`PfcBars`（PFC 条形）、`PfcStackBars`（PFC 堆叠条）、`SubstituteSheet`（替换弹层）、`TrendLine`（趋势折线）。

### 3.3 请求与状态

- `api/http.ts`：统一封装 `uni.request`，自动注入 JWT、解包 `ResponseResult`、401 清除登录态、错误 toast
- API 模块：`auth` / `food` / `recipe` / `record` / `stats` / `user`，类型定义与后端 DTO 对齐
- Pinia：`store/user.ts`（登录态与用户信息）、`store/diet.ts`（今日食谱、记录、汇总、食物名称缓存、跨 tab 挑选模式）
- 启动策略：登录不阻塞首页，未登录也可浏览食物库

---

## 四、数据与脚本

| 文件 | 说明 |
| --- | --- |
| `sql/daily-diet.sql` | 建库建表：`users`、`foods`、`user_custom_foods`、`food_dislikes`、`daily_recipes`、`recipe_items`、`diet_records`，含索引与外键 |
| `sql/food_init.sql` | 由 `dependencies/food_composition_full.csv` 转换的标准食物数据，共 1677 条（`source=1`），映射到 7 大类 |
| `dependencies/food_composition_full.csv` | 原始数据源（中国食物成分表） |

`food_init.sql` 的清洗规则：`—` / `Tr` 按 0 处理；能量 kcal 缺失时按 `kJ / 4.184` 换算；类别映射到「谷物/蛋白/蔬菜/水果/乳品/坚果/调料」；脚本可重复执行（先删 `source=1` 再插入，不影响用户自定义食物）。

---

## 五、未实现 / 待办

| 项 | 状态 | 说明 |
| --- | --- | --- |
| 收藏 / 常用食物置顶 | 未实现 | `user_custom_foods` 表已建，但无对应接口与服务层逻辑 |
| 食谱历史收藏 | 未实现 | 仅支持按日期查询，无收藏能力 |
| AI 饮食建议（v1.2） | 未实现 | 无接口、无算法 |
| 运动消耗手动录入（v1.2） | 未实现 | 需求文档中标为 TBD |
| 周目标追踪（v1.2） | 未实现 | — |
| 后台管理 / Web 管理端（v2.0） | 未实现 | — |
| 后端单元/集成测试 | 缺失 | `backend/src/test` 不存在，测试数为 0 |
| 前端测试用例 | 缺失 | Vitest 已配置，但无 `*.test.ts` 文件 |
| 微信真实登录联调 | 待配置 | 依赖 `.env` 中 `WX_APPID` / `WX_SECRET` |

### 已知依赖隐患

1. `frontend/vitest.config.ts` 使用 `@vitejs/plugin-vue`，但 `package.json` 未显式声明该依赖（当前依赖传递提升，升级时有断裂风险）。
2. `vue-tsc` 为 `^1.0.24`（已停止维护），与较新 TS/Vue 组合可能出现类型检查误报。

---

## 六、文档索引

| 文档 | 路径 |
| --- | --- |
| 需求文档（详细版） | `docs/需求文档-详细版.md` |
| 开发设计文档 | `docs/开发设计文档.md` |
| 数据库脚本说明 | `docs/数据库脚本.md` |
| UI 原型 | `docs/ui-mockups.html` |

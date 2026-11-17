# 每日饮食助手（daily-diet）

面向健身人群的饮食管理应用：根据身体数据与训练水平计算 BMR/TDEE，按目标生成每日食谱，支持食材替换、饮食打卡与营养统计。前端为 uni-app（微信小程序 / H5），后端为 Spring Boot 单体服务。

## 功能已实现

### 用户与计算
- 微信小程序登录（JWT），开发环境提供免登录调试接口
- Onboarding：性别 / 生日 / 身高 / 体重、三大项 1RM 与训练等级、目标类型（增肌 / 减脂 / 维持 / 自定义）
- BMR（Mifflin-St Jeor）→ 肌肉量修正 → TDEE → 目标热量，全链路自动重算
- 按目标生成默认 PFC 比例，支持用户自定义（校验合计 100%）

### 食谱推荐
- 按餐次比例（早 25% / 午 35% / 晚 30% / 加餐 10%）生成每日食谱
- 按营养缺口反算推荐克数（蛋白/谷物 60%、乳品/水果 30%、蔬菜兜底 200g）
- 食材替换：同类别 + 热量 ±20% 预筛，PFC 余弦相似度 + 热量接近度排序，返回 Top 3
- 标记不喜欢，累计 3 次自动移出推荐池

### 饮食记录
- 食谱一键入库、手动添加、修改克数/食物、逻辑删除
- 今日汇总：总热量、PFC、目标对比、达成率与明细

### 食物库
- 标准食物库：中国食物成分表 1677 条，映射为 7 大类
- 名称模糊搜索 + 分类筛选 + 分页，未登录也可浏览
- 自定义食物增删改，自动生成 PFC 向量
- 食物详情 Redis 缓存

### 统计
- 近 7 天 / 30 天每日营养摄入聚合
- 打卡达成率统计

### 前端页面
首页、引导页、食谱、食物库、记录、统计、我的（共 7 页，底部 5 个 Tab），配套营养环、PFC 柱状、趋势折线、替换弹层等组件。

> 完整实现清单、接口列表与未实现项见 [board.md](./board.md)。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.5、Spring Security、MyBatis-Plus 3.5.7、MySQL 8、Redis、jjwt、SpringDoc、Hutool |
| 前端 | uni-app、Vue 3.4、TypeScript、Vite 5、Pinia、uview-plus、Sass |
| 测试 | Vitest（配置就绪，用例待补） |

## 目录结构

```
daily-diet/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/com/dailydiet/
│   │   ├── algorithm/       # BMR / TDEE / 肌肉修正 / PFC 向量 / 替换匹配
│   │   ├── auth/            # 登录、JWT
│   │   ├── food/            # 食物库
│   │   ├── recipe/          # 食谱推荐与替换
│   │   ├── record/          # 饮食记录
│   │   ├── stats/           # 统计
│   │   ├── user/            # 用户与目标
│   │   ├── common/          # 统一响应、异常、常量、枚举
│   │   └── config/          # Security / Redis / MyBatis-Plus 等配置
│   └── .env.example         # 环境变量模板
├── frontend/                # uni-app 前端
│   └── src/
│       ├── api/             # 请求封装与接口模块
│       ├── components/      # 通用组件
│       ├── pages/           # 7 个页面
│       ├── store/           # Pinia 状态
│       └── utils/           # 计算与格式化工具
├── sql/                     # 数据库脚本
│   ├── daily-diet.sql       # 建库建表
│   └── food_init.sql        # 标准食物数据（1677 条）
├── dependencies/            # 原始食物成分 CSV
├── docs/                    # 需求 / 设计 / 数据库 / UI 原型
├── board.md                 # 已实现内容看板
└── README.md
```

## 快速开始

### 1. 初始化数据库

```bash
# 建库建表
mysql -u root -p < sql/daily-diet.sql
# 导入标准食物库（1677 条）
mysql -u root -p diet < sql/food_init.sql
```

### 2. 配置后端环境变量

```bash
cd backend
cp .env.example .env
```

按需填写 `.env`：

| 变量 | 说明 |
| --- | --- |
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DB` / `MYSQL_USER` / `MYSQL_PASSWORD` | MySQL 连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 |
| `WX_APPID` / `WX_SECRET` | 微信小程序登录凭证（H5 调试可不填） |
| `JWT_SECRET` | JWT 密钥，至少 32 字符 |

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认 `http://localhost:8080`，默认 profile 为 `dev`。

- 接口文档：`http://localhost:8080/swagger-ui.html`
- 健康检查：`http://localhost:8080/actuator/health`
- 开发态登录（免微信凭证）：`POST /api/auth/dev-login`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev:h5          # H5
npm run dev:mp-weixin   # 微信小程序（产物用微信开发者工具打开）
```

H5 开发服务器端口 `5173`，`/api` 已代理到 `http://localhost:8080`。

## 常用脚本

| 命令 | 说明 |
| --- | --- |
| `mvn spring-boot:run` | 启动后端 |
| `mvn clean package` | 打包后端（产物 `target/daily-diet.jar`） |
| `npm run dev:h5` | 前端 H5 开发 |
| `npm run dev:mp-weixin` | 前端微信小程序开发 |
| `npm run build:h5` | 构建 H5 |
| `npm run type-check` | TypeScript 类型检查 |
| `npm run test` | 运行前端测试（Vitest） |

## 已知问题

- 后端与前端均缺少测试用例（后端无 `src/test`，前端无 `*.test.ts`）
- `@vitejs/plugin-vue` 未在 `package.json` 显式声明，依赖传递提升运行
- `vue-tsc` 为 1.x，建议升级到 2.x

## 文档

- [开发看板（已实现内容）](./board.md)
- [需求文档（详细版）](./docs/需求文档-详细版.md)
- [开发设计文档](./docs/开发设计文档.md)
- [数据库脚本说明](./docs/数据库脚本.md)
- [UI 原型](./docs/ui-mockups.html)

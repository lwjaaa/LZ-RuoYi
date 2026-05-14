# AGENTS.md

本文档是 Codex 在本仓库工作的协作指南。后续维护者可以直接修改本文件来调整 AI 协作规则。

## 信息来源优先级

- 以实际代码、配置文件、`pom.xml`、`package.json`、SQL 脚本和运行结果为准。
- 项目背景信息只允许参考 `README.md` 和 `docs/项目结构与技术栈.md`。
- 除非用户明确要求，否则不要把其他 Markdown 文档作为依据。
- 如果文档和实际代码冲突，按实际代码处理；必要时在回复中说明冲突点。

## 项目概览

LZ-RuoYi 是基于若依 RuoYi 深度定制的跨境电商 ERP 系统。仓库包含 Spring Boot 多模块后端、Vue 3 管理前端，以及用于采集淘宝、天猫、1688 等平台商品信息的 Chrome 插件。

当前有效的前端项目是 `RuoYi-Vue3/`。不要在旧的 `ruoyi-ui/` 目录中开发新功能。

## 仓库结构

```text
LZ-RuoYi/
├── pom.xml                         # Maven 父 POM
├── ruoyi-admin/                    # Spring Boot 启动模块和运行配置
├── ruoyi-common/                   # 公共工具、注解、常量、异常等
├── ruoyi-framework/                # 安全、Web、数据源、拦截器、框架配置
├── ruoyi-system/                   # 用户、角色、菜单、部门等系统管理
├── ruoyi-quartz/                   # 定时任务模块
├── ruoyi-generator/                # 代码生成模块
├── vh-erp/                         # 自定义 ERP 业务模块
├── RuoYi-Vue3/                     # 当前有效的 Vue 3 + Vite 管理前端
├── Purchase-x/                     # Chrome 插件，Vue 3 + Vite + CRX
├── sql/                            # 数据库初始化和迁移脚本
└── docs/项目结构与技术栈.md          # 允许参考的结构和技术栈文档
```

ERP 后端重点目录：

```text
vh-erp/src/main/java/com/ruoyi/erp/
├── config/       # ERP 配置，包括 ShopifyGraphQLClient 和同步线程池配置
├── constant/     # ERP 常量
├── controller/   # ERP REST 控制器
├── exception/    # ERP 业务异常
├── executor/     # 异步执行器相关逻辑
├── mapper/       # MyBatis-Plus Mapper
├── model/        # domain、dto、vo
├── service/      # Service 接口和实现
├── task/         # 定时任务
├── util/         # 工具包
└── utils/        # 工具包
```

前端重点目录：

```text
RuoYi-Vue3/src/
├── api/erp/          # ERP API 模块
├── components/erp/   # ERP 公共组件
├── views/erp/        # ERP 页面：商品、变体、标签、媒体、任务、运费等
├── router/           # Vue Router 配置
├── store/            # Pinia 状态管理
├── utils/            # 前端工具、request 封装
└── layout/           # 若依布局
```

## 技术栈

后端：

- Java 17
- Spring Boot 3.5.11
- RuoYi 3.9.2
- MyBatis Spring Boot 3.0.5
- MyBatis-Plus 3.5.9
- Druid 1.2.28
- PageHelper 2.1.1
- Spring Security、JWT 0.9.1
- SpringDoc 2.8.16
- FastJSON2 2.0.61
- Hutool 5.8.38
- Lombok 1.18.36
- Redisson 3.27.0
- Quartz
- `vh-erp` 中使用 Spring GraphQL/WebClient 对接 Shopify

前端 `RuoYi-Vue3/`：

- Vue 3.5.26
- Vite 6.4.1
- Element Plus 2.13.1
- Pinia 3.0.4
- Vue Router 4.6.4
- Axios 1.13.2
- ECharts 5.6.0
- TypeScript、JavaScript、SCSS
- 富文本相关依赖包括 Vue Quill、WangEditor、TinyMCE

Chrome 插件 `Purchase-x/`：

- Vue 3
- Vite 5
- TypeScript
- Pinia
- CRXJS Vite 插件
- Tailwind CSS

## 常用命令

后端，在仓库根目录执行：

```bash
mvn clean package -DskipTests
```

启动后端：

```bash
cd ruoyi-admin
mvn spring-boot:run
```

也可以直接运行打包产物：

```bash
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

前端：

```bash
cd RuoYi-Vue3
npm install
npm run dev
npm run build:prod
npm run build:stage
npm run type-check
npm run lint
```

Chrome 插件：

```bash
cd Purchase-x
npm install
npm run dev
npm run build
```

## 运行配置

- 后端默认激活 profile：`dev,erp-dev`，配置位置为 `ruoyi-admin/src/main/resources/application.yml`。
- 后端端口：`8080`。
- 开发数据库名：`lz-ruoyi`，配置位置为 `ruoyi-admin/src/main/resources/application-dev.yml`。
- Redis 默认地址：`127.0.0.1:6379`，数据库索引为 `0`。
- 文件上传和静态资源根路径默认是 `C:/velarthome`。
- 前端开发服务器端口：`80`。
- 前端开发 API 前缀是 `/dev-api`，Vite 会代理到 `http://localhost:8080`。
- `Purchase-x` 开发服务器端口：`5173`。
- SpringDoc 已在 `application-dev.yml` 中配置，但当前默认关闭，除非修改配置。

不要提交真实生产密钥。配置文件中的 token、app key、API key、数据库密码都应视为环境敏感信息。

## 数据库脚本

SQL 脚本位于 `sql/`。当前包含：

1. `20240629_init1.sql`
2. `20240629_init2.sql`
3. `20260324_erp.sql`
5. `20260427_form_suggestion.sql`
7. `20260430_shopify.sql`

如果修改数据库相关功能，需要同步维护 SQL 脚本，并确保后端实体、DTO/VO、Mapper XML、前端类型和表单字段保持一致。

## 后端开发规范

- 遵循若依现有分层：Controller -> Service 接口 -> Service 实现 -> Mapper -> XML/domain model。
- ERP 业务代码优先放在 `vh-erp`，除非变更明确属于框架、系统或公共模块。
- Controller 遵循若依习惯，优先使用 `BaseController`、`AjaxResult`、`TableDataInfo`、权限注解和操作日志注解。
- Service 接口命名为 `I*Service`，实现类放在 `service/impl`。
- 多步骤写操作和状态变更需要使用 `@Transactional`。
- 新增查询优先使用 MyBatis-Plus `LambdaQueryWrapper`，减少字符串字段名带来的风险。
- 复杂 SQL 放到 `src/main/resources/mapper/**` 下的 Mapper XML。
- 实体放在 `model/domain`，请求 DTO 放在 `model/dto`，响应 VO 放在 `model/vo`。
- Lombok 使用方式要和附近代码保持一致。
- 保持若依接口返回结构稳定：列表接口通常返回 `TableDataInfo`，增删改通常返回 `AjaxResult`。

## 前端开发规范

- 新的前端需求只在 `RuoYi-Vue3/` 中实现。
- 新组件或现代化改造优先使用 Vue 3 `<script setup>`，但要和周边代码风格一致。
- API 调用使用 `@/utils/request` 的现有封装。
- ERP API 模块放在 `RuoYi-Vue3/src/api/erp/`。
- ERP 页面放在 `RuoYi-Vue3/src/views/erp/`，ERP 公共组件放在 `RuoYi-Vue3/src/components/erp/`。
- 页面交互遵循 Element Plus 和若依后台现有模式：查询表单、工具栏、表格、分页、弹窗表单、导入导出等。
- 路由路径、权限标识、后端 Controller 映射和菜单 SQL 要保持一致。
- 不要交付只有占位内容或 TODO 的功能，功能应当端到端可用。

## Chrome 插件开发规范

- 插件相关工作在 `Purchase-x/` 中进行。
- 源码位于 `Purchase-x/src/`，包括 `background`、`content_scripts`、`dashboard`、`popup`、`api`、`types`、`utils` 等目录。
- 商品采集逻辑位于 `Purchase-x/src/content_scripts/extractors/`。
- 构建命令是 `npm run build`，会先执行 `vue-tsc --noEmit` 再执行 Vite 构建。

## Shopify 对接规范

- 修改 Shopify 集成时，目标 Shopify Admin API 版本必须是 `2026-04`。
- 当前 Shopify GraphQL 客户端是 `vh-erp/src/main/java/com/ruoyi/erp/config/ShopifyGraphQLClient.java`。
- 店铺和 API 版本信息由 `ShopifyStore` 表示，GraphQL URL 由 `shopName` 和 `apiVersion` 生成。
- 商品、变体、媒体、发布渠道、任务和店铺同步时，要保证数据库状态与 Shopify 返回结果一致。
- Shopify token 过期和 API 错误应沿用现有 ERP 异常处理模式。
- 注意 Shopify 限流、异步任务状态、重试逻辑和部分失败结果。不要让同步任务停留在无法判断的中间状态。
- 如果 Shopify API 行为不确定，需要优先查阅官方 Shopify Admin GraphQL API `2026-04` 文档后再实现。

## 关键 ERP 实体

- `Product`
- `ProductVariant`
- `ProductOption`
- `ProductOptionValue`
- `ProductVariantOption`
- `TagDict`
- `ProductTagRel`
- `Media`
- `ShopifyTask`
- `ShopifyStore`
- `FormSuggestion`

## 代码注释要求

- 后续交付的代码中，新增注释必须使用中文。
- 注释应解释业务意图、边界条件或复杂逻辑，不要写无意义的逐行翻译。
- 只有在代码不够自解释时才添加注释，避免堆砌注释。

## 交付质量要求

- 每次开发的功能必须完整，不要用 TODO 代替实现。
- 改动范围要聚焦在用户请求和受影响的相关文件上。
- 完成前尽量运行最相关的检查：
  - 后端变更：优先编译受影响 Maven 模块，必要时执行 `mvn clean package -DskipTests`。
  - 前端变更：优先执行 `npm run type-check` 和/或 `npm run build:prod`。
  - Chrome 插件变更：执行 `npm run build`。
- 如果因为服务、凭据、依赖或环境限制无法运行检查，需要在回复中说明。
- 工作区可能已有用户改动，不要回滚用户未明确要求回滚的内容。
- 交付的代码必须带有必要的注释和日志记录，避免出现无用的注释和日志, 每个方法必须要有文档注释描述方法的作用。
- 后端单个文件的代码行数不超过800行，超过需要考虑拆分。


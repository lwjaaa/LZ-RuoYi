# Trae 企业级工程规则（LZ-RuoYi / ERP）

Scope: Trae AI Coding / 检索 / 生成 / 评审 / 交付

## 0. 强制声明（最高优先级）

- 后端业务代码主目录：`vh-erp/**`（以下简称“后端 erp 目录”）。
- 前端业务代码主目录：`RuoYi-Vue3/`（以下简称“前端 erp 目录”）。
- `ruoyi-ui/**` 为历史弃用目录，禁止生成、修改、修复、重构。
- Trae 处理业务需求时，**优先在上述 erp 目录检索与改动**，有依赖可向其他模块检索

## 1. 代码检索策略（优先 erp 域）

### 1.1 目录优先级（从高到低）

1. `vh-erp/**`
2. `RuoYi-Vue3/src/api/erp/**`
3. `RuoYi-Vue3/src/views/erp/**`
4. `RuoYi-Vue3/src/components/erp/**`
5. `sql/*_erp.sql`
6. `docs/功能清单与需求文档.md`（功能清单与需求文档参考）
7. `docs/项目结构与技术栈.md`（项目结构与技术栈参考）

### 1.2 文件命名规范（检索命中规则）

- Java：
  - 控制器：`*Controller.java`
  - 服务接口：`I*Service.java`
  - 服务实现：`*ServiceImpl.java`
  - 持久层：`*Mapper.java` + `*Mapper.xml`
  - 实体/模型：`*Entity.java` / `*Model.java` / `*Dto.java` / `*Vo.java`
  - 定时任务：`*Task.java` / `*Job.java`
- Vue3：
  - 页面：`views/erp/**/index.vue` 或业务语义 `.vue`
  - API：`api/erp/*.js`（kebab-case 或语义命名）
  - 组件：`components/erp/*.vue`（PascalCase）

### 1.3 关键词映射表（检索路由）

| 业务关键词             | 后端优先路径                            | 前端优先路径                                                                            | 扩展关键词           |
| ---------------------- | --------------------------------------- | --------------------------------------------------------------------------------------- | -------------------- |
| 商品/产品(product/spu) | `vh-erp/**/product*`                    | `RuoYi-Vue3/src/{api,views}/erp/product/**`                                             | 选品、SPU、详情      |
| 变体(variant/sku)      | `vh-erp/**/productVariant*`             | `RuoYi-Vue3/src/{api,views}/erp/variant/**`                                             | SKU、规格、选项      |
| 标签(tagDict)          | `vh-erp/**/tagDict*`                    | `RuoYi-Vue3/src/{api,views}/erp/tag/**`, `RuoYi-Vue3/src/{api,views}/erp/ProductTag/**` | 菜单型标签、活动标签 |
| 媒体(image/media)      | `vh-erp/**/media*`                      | `RuoYi-Vue3/src/{api,views}/erp/media/**`                                               | 图片、视频、压缩     |
| 汇率                   | `vh-erp/**/exchange*`                   | `RuoYi-Vue3/src/{api,views}/erp/exchange*`                                              | 汇率                 |
| 定时任务               | `vh-erp/**/task/**` + `ruoyi-quartz/**` | `RuoYi-Vue3/src/{api,views}/erp/*task*`                                                 | cron、任务、调度     |

### 1.4 搜索排除规则（硬限制）

- 业务检索默认排除：
  - `ruoyi-ui/**`
  - `node_modules/**`, `target/**`, `dist/**`, `build/**`, `logs/**`
  - `.git/**`, `.idea/**`, `.vscode/**`, `*.log`, `*.min.js`

## 2. 开发规范（统一编码标准）

### 2.1 Java（后端）

- 统一采用《阿里巴巴 Java 开发手册》强制项。
- 必须：
  - 明确分层（Controller/Service/Mapper）
  - 参数校验（JSR-303）
  - 日志脱敏与分级
- 建议：
  - 查询优先 `LambdaQueryWrapper`
  - 写操作评估事务边界并使用 `@Transactional`

### 2.2 前端（RuoYi-Vue3）

- ESLint 基线：`Airbnb + Vue3 + 自定义 ERP 规则`
- 必须：
  - 使用 Composition API（推荐 `<script setup>`）
  - API 调用统一 `@/utils/request`
  - 组件命名语义化、避免超大组件
- ERP 自定义规则：
  - `views/erp` 页面单文件建议 <= 1000 行，超限需拆分
  - API 文件按业务聚合，不跨域混放

## 4. 目录级约定（强约束）

- 后端业务：`vh-erp/**`（唯一业务主目录）
- 前端业务：`RuoYi-Vue3/src/**`，其中业务页面/API 统一在 `.../erp/**`
- 禁改目录：`ruoyi-ui/**`（废弃）
- 说明性目录：`docs/项目结构与技术栈.md` 为项目结构与技术栈，`docs/功能清单与需求文档.md`为功能清单与需求文档，变更后需同步。

## 5. 交互与文档同步规范（每次改动都执行）

每次功能改动必须执行以下动作：

1. 提炼“改动点摘要”（背景、范围、接口、风险、回滚）。
2. 在 `docs/` 新建变更记录文件（建议命名：`YYYY-MM-DD-功能名-变更记录.md`）。
3. 同步更新：
   - `docs/项目结构与技术栈.md`（项目结构、技术栈）
   - `docs/功能清单与需求文档.md`（功能清单与需求文档）
4. 提交前核对“三文一致性”：代码实现 = docs/{{变更记录.md}} = 功能清单与需求文档。

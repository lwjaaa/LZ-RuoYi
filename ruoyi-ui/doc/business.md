# 🚀 Shopify ERP 商品管理模块开发指令 (Optimized for Ai)

## 1. 项目背景与技术栈

- **框架**: RuoYi-Vue (Vue 2.6.12 + Element UI 2.15.14) + Spring Boot + MyBatis Plus + java 17
- **场景**: 跨境电商 ERP，对接 Shopify API。
- **核心功能**: 基于树形标签（Tag）体系的商品选品、SPU 自动生成、多属性变体管理、批量推送 Shopify、分步维护商品。

## 2. 数据库模型假设 (TypeScript Interface / Java DTO)

基于/sql 目录下的 sql 文件对应的数据结构进行开发。

## 3. 前端页面功能详细设计 (`/vh-erp/product/index.vue`)

### 3.1 布局结构

- **布局**: 左右分栏布局 (`el-row` / `el-col`)。
- **左侧 (30%)**: 自定义级联标签选择器 (`TagTreeSelector`)。
- **右侧 (70%)**: 商品列表与操作区 (`ProductListPanel`)。

### 3.2 左侧：标签树组件 (`TagTreeSelector`)

- **数据源**: 递归加载 `erp_shopify_tag_dict` 级联数据。
- **节点渲染 (`slot`)**:
  - **显示格式**: `[中文 Label] (英文 Value)`。
  - **展开/折叠**: 仅当 `children.length > 0` 时显示箭头。
  - **悬停操作**: 鼠标 `hover` 时显示图标按钮组：
    - `编辑` (打开拖拽排序/属性编辑弹窗)。
    - `删除` (软删除校验)。
    - `置顶` (调整 `sortOrder` 至同级首位)。
  - **选中逻辑**:
    - 支持**多选** (Checkbox 模式或 Click Toggle)。
    - 点击节点切换选中状态 (`selectedTags` 数组)。
    - 选中多个标签时，右侧列表展示**交集**商品 (同时拥有所有选中 Tag)。
- **顶部工具栏**:
  - `清除选中`: 清空 `selectedTags`。
  - `新增标签`: 打开 `TagEditDialog`。
- **拖拽排序 (`TagEditDialog`)**:
  - 使用 `vuedraggable` 或 `Sortable.js`。
  - **约束逻辑**:
    - `type === 'MENU'` 的节点永远固定在同级最前方。
    - 非 MENU 节点拖拽或点击置顶到 MENU 节点之前时，自动修改排序为该 MENU 节点之后的第一个位置。
    - 允许跨层级拖拽（改变 `parentId`）。

### 3.3 右侧：商品列表与操作流程 (`ProductListPanel`)

- **顶部操作栏**:
  - `选品` (Create): 仅在选中至少一个 Tag ，且 Tag 中含有`type === 'MENU'`的 Tag 时可用。
  - `批量推送 Shopify`: 选中多个商品后触发。
  - `批量删除`: 逻辑删除。
- **列表列**: SPU, 标题, 关联标签, 状态, 创建时间, 操作列。
- **行操作**: `编辑`, `推送`, `详情`, `保存媒体资源`。

### 3.4 核心流程：选品向导 (`ProductCreationWizard`)

采用 `el-steps` 分两步走：

#### 第一步：选品基础信息 (`Step1: Selection`)

- **触发条件**: 点击【选品】按钮。
- **主商品信息**:
  - 字段: `SPU`, `来源URL`, `采购链接`, `采购商品选项`。
- **自动填充**:
  - `selectedTagIds`: 来自左侧选中项。
  - `SPU 生成逻辑`:
    - 检测选中的 Tag 中是否有 `type === 'MENU'` 的节点。
    - 若有，读取其 `spuPrefix` 和 `currentMaxSeq`。
    - 自动生成 `spuCode = prefix + '-' + padZero(currentMaxSeq + 1, 3)`。
    - **交互**: 允许用户手动修改生成的 SPU。
    - **校验**: 保存时，若手动输入的 SPU 序号 > `currentMaxSeq`，则需要后端在保存逻辑中更新该 Tag 的 `currentMaxSeq`。
  - `purchaseLink`, `sourceUrl`: 必填。
- **商品选项 (Options)**:
  - 参考 Shopify 多属性输入 (Name + Values)。
  - **联动**: 输入选项后，下方实时预览生成 `Variants` 表格 (笛卡尔积)，允许删除不需要的 `Variants` 行。
  - **默认**: 若无选项，默认生成 1 个变体 (Default Title)。
- **变体编辑 (内嵌表格)**:
  - 列: 选项组合, 采购链接 (默认继承主商品), 采购价。
  - 允许拖拽来调整 `Variants` 行的顺序，鼠标悬浮某一行时，末尾展示该行的删除按钮。
- **底部操作**: `下一步` , `保存并新增`(保存并继续下一个选品) , `保存并关闭` 。

#### 第二步：信息录入 (`Step2: Details`)

- **主商品信息**:
  - 字段: `spu`,`商品标题`,`商品类别ID`,`商品类型`,`描述`, `大小`, `材质`, `备注`, `Package 包含的包材`, `商品详情描述` (富文本编辑器),`商品图片列表`。
- **商品图片列表管理**:
  - **导入**: 按钮调用后端接口，从服务器指定目录扫描图片/视频，返回列表供选择。
  - **展示**: 网格展示已选媒体缩略图。
  - **拖拽绑定**: 支持将上方媒体图片直接 `drag & drop` 到下方变体行的“图片”列，实现 `variant.imagePaths` 绑定。
- **变体详细设置**:
  - 列: 选项, SKU (可编辑), 图片 (显示绑定的图), 尺寸 (L/W/H), 重量, 运费模板。
  - **自动计算**:
    - `材积重`: =(`包装宽度`_`包装高度`_`包装长度`)/8000。
    - `运费`： 在填写产品重量后，掉用后端接口，返回运费。
    - `是否实际发货`: 默认 `false`。
    - `商品成本价`: 读取采购价 + 运费。
    - `销售价格`: 公式 `(商品成本价 * 1.3) / 美元汇率` (自动折算 USD)。
  - 校验: 必填项检查。

### 3.5 批量推送与异常处理

- **动作**: 选中商品 -> 点击【批量推送】。
- **逻辑**:
  - 调用 Shopify GraphQL
  - **成功**: 更新本地状态为 `PUSHED`，记录 Shopify Product ID 、 shopify_variant_id 、shopify_url 和 shopify_image_id。
  - **失败**:
    - 捕获错误信息 (如: 标题过长, 价格格式错误)。
    - **通知**: 调用企业微信 Webhook，发送失败报告 (包含 SPU 列表和错误原因)。
    - **记录**: 本地数据库 `error_log` 字段记录详情。
    - **反馈**: 前端弹出摘要：“成功 X 个，失败 Y 个”，并提供“查看失败详情”链接。

## 4. 后端 API 接口定义 (Spring Boot Controller)

请生成以下 Controller 方法骨架：

1. `PUT /vh-erp/tag`: 更新标签 (处理树形结构、排序、类型约束)。
2. `GET  /vh-erp/product/list`: 分页查询商品 (支持 `tagIds` 交集筛选)。
3. `POST /vh-erp/product/save`: 保存商品 (事务控制：保存 SPU -> 更新 Tag 流水号 -> 保存 Variants -> 保存 Media)。
4. `GET  /vh-erp/media/scan`: 扫描服务器指定路径返回媒体列表。
5. `POST /vh-erp/product/push-batch`: 批量推送任务 (建议使用 `@Async`，返回 taskId)。底层逻辑是一个商品一个商品地推送，可考虑使用多线程优化性能。每个商品推送会触发一个异步任务，返回 taskId。
6. `GET  /vh-erp/product/push-result/{taskId}`: 查询推送结果。

## 5. 编码规范与注意事项

- **UI 风格**: 模仿“语雀”左侧目录样式，简洁、高亮清晰。
- **并发安全**: SPU 流水号生成必须使用数据库乐观锁 或 java 的 lock 锁 操作，防止重复。
- **数据一致性**: 商品保存时，若 SPU 序号更新了 Tag 的最大值，必须在同一事务中完成。
- **用户体验**: 变体图片的拖拽绑定需有视觉反馈 (Drop Zone 高亮)。
- **错误处理**: 推送失败必须有详细的错误日志落库，不能仅停留在前端 alert。

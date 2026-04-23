你好！我是你的技术产品经理兼系统架构师。这份需求文档描述了一个功能闭环的跨境电商选品工具。为了让 Trae 或类似的 AI 编程助手能够精准执行，我们需要将模糊的业务描述转化为**确定性的技术协议**。

以下是为你深度细化后的“开发就绪”技术规格文档。

---

# 技术规格文档：跨境电商选品助手 (Browser Extension)

## 1. 技术栈确认

- **框架**: Vue 3 (Composition API)
- **样式**: Tailwind CSS (原子化 CSS，便于快速构建 UI)
- **语言**: TypeScript (严格类型定义，减少数据传递错误)
- **构建工具**: Vite + `@crxjs/vite-plugin` (主流插件开发工具)
- **通信**: Chrome Extension Messaging API (Runtime / Tabs)
- **存储**: `chrome.storage.local` (核心数据持久化)

---

## 2. 数据模型设计 (Schema)

### 2.1 任务实体 (Task)

```typescript
interface FetchTask {
  id: string; // 唯一标识：商品标题或者URL
  title: string; // 商品标题
  url: string; // 来源URL
  platform: "taobao" | "tmall" | "1688";
  status: "pending" | "fetching" | "completed" | "failed";
  totalVariants: number; // 总变体数（获取详情后更新）
  currentVariants: number; // 已获取变体数
  progress: number; // 百分比：current/total
  startTime: number; // 时间戳
}
```

### 2.2 商品实体 (Product)

```typescript
interface Product {
  id: string;
  productName: string;
  sourceUrl: string;
  spu: string; // 自动生成的 SPU (Prefix + Seq)
  tagIds: number[]; // 选中的标签ID数组
  mediaUrlList: string[]; // 包含主图和规格主图
  optionList: ProductOption[];
  productVariantList: ProductVariant[];
}

interface ProductOption {
  chineseName: string;
  englishName: string; // 默认可与中文名一致，或拼音
  values: { chineseValue: string; englishName: string }[];
}

interface ProductVariant {
  purchasePrice: number;
  mediaUrl: string;
  isActiveAvailable: 0 | 1;
  optionValueList: {
    chineseName: string;
    englishName: string;
    chineseValue: string;
    englishValue: string;
  }[];
}
```

---

## 3. 目录与文件结构建议

```text
/src
  /api              # 封装 tagFetch 和 productSync 接口
  /background       # Service Worker: 维护全局任务队列、存储监听
  /content_scripts  # 注入脚本: 包含 extractors，负责抓取 DOM
  /popup            # Popup 页面: 任务发起与简单进度
  /dashboard        # 独立标签页: Vue 完整的管理后台
    /components     # 商品卡片、标签选择器、工具栏
    /store          # Pinia: 管理本地同步的商品状态
  /types            # 全局 TS 类型定义
  /utils            # 任务调度器、SPU 生成逻辑、Storage 封装
```

---

## 4. 核心交互逻辑细化

1.  **唯一性校验**: 触发获取前，先检索 `chrome.storage.local` 中的 `productList` 和 `taskQueue`。若 URL 或标题已存在，则 Popup 按钮置灰或提示。
2.  **SPU 生成逻辑**:
    - 当用户在 Dashboard 选择标签时，遍历所选标签。
    - 若标签 `tagType === 'MENU'` 且为叶子节点，提取 `spuPrefix`。
    - SPU = `spuPrefix` + `currentMaxSeq` (需补位填充，如 0001)。
3.  **任务进度预测**:
    - 公式：`剩余时间 = (当前耗时 / 已完成变体数) * 剩余变体数`。
    - 每秒计算一次，平滑展示。
4.  **同步/导出清除**: 同步成功后弹出 `Confirm` 弹窗。点击“是”，调用 `chrome.storage.local.remove(['productList'])` 并刷新 Store。

---

## 5. 拆解任务流 (Steps for Trae)

你可以按照以下顺序依次向 Trae 发送指令：

- **Step 1**: “初始化 Vite + Vue3 + Tailwind 的 Manifest V3 插件项目，配置好 Background、Popup、Content Script 和 Dashboard 的入口文件。”
- **Step 2**: “在 `utils/storage.ts` 中封装对 `chrome.storage.local` 的操作，定义 Product 和 Task 以及标签 的 TypeScript 接口。”
- **Step 3**: “编写 Background 任务调度器，支持消息监听。当收到 `START_FETCH` 消息时，维护一个 `taskQueue` 数组，并能处理热添加逻辑。”
- **Step 4**: “开发 Content Script 抓取逻辑。集成提供的 `extractors` JS 文件，确保能将页面 DOM 转化为我们定义的 `Product` 结构，并发送回 Background 保存。”
- **Step 5**: “实现 Popup 页面。支持单品/批量按钮，每秒从本地 Storage 获取 `taskQueue` 并渲染进度条，计算总进度百分比。”
- **Step 6**: “开发 Dashboard 独立标签页。使用 Tailwind 布局，实现右侧置顶工具栏、右侧插件名称和商品列表卡片。”
- **Step 7**: “在 Dashboard 中实现级联标签选择器组件。接入获取标签接口，支持多选、删除，并实现 SPU 自动生成逻辑。”
- **Step 8**: “完善 Dashboard 的商品编辑功能。实现图片预览、图片删除、规格选项删除及其联动变体删除的功能（需二次确认）。”
- **Step 9**: “接入商品同步接口（POST）和 JSON 导出功能。加入同步成功后的清空逻辑和全局消息通知。”

---

## 6. 边缘情况处理

| 场景             | 处理策略                                                                                  |
| :--------------- | :---------------------------------------------------------------------------------------- |
| **网络请求失败** | 标签获取接口失败时，使用本地旧缓存；若无缓存，显示“刷新”按钮重试。                        |
| **存储上限**     | `chrome.storage.local` 虽大但有限。若商品图片过多，存储前建议进行 Base64 检查或仅存 URL。 |
| **抓取中断**     | 若用户关闭商品详情页导致抓取中断，Background 应在 30 秒超时后将该任务标记为 `failed`。    |
| **标签联动**     | 删除一个规格选项时，必须遍历 `productVariantList`，将包含该规格值的变体一并删除。         |
| **SPU 冲突**     | 同步至 ERP 成功后，应提示用户本地 SPU 序列号可能需要自增（由后端返回最新 seq）。          |

---

### 给 Trae 的第一个 Prompt 建议：

> "请基于 Vue3, Tailwind CSS, TypeScript 和 Vite 帮我初始化一个 Chrome 插件项目。要求：1. 使用 Manifest V3；2. 包含 Popup、Background Service Worker、Content Script 和一个名为 Dashboard 的独立标签页；3. 目录结构清晰；4. 引入 chrome-types 方便类型提示。"

这份文档现在已经具备了开发所需的结构、数据模型和逻辑细节。你可以直接开始你的开发旅程了！

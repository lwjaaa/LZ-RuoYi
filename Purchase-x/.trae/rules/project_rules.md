# Trae 开发规则 - 跨境电商选品助手

## 技术栈

- **框架**: Vue 3 (Composition API)
- **样式**: Tailwind CSS
- **语言**: TypeScript (严格模式)
- **构建**: Vite + @crxjs/vite-plugin
- **清单**: Manifest V3

## 编码规范

- 所有组件使用 Composition API，禁止 Options API
- 严格 TypeScript 类型定义，禁止 any
- 数据模型遵循文档定义的 FetchTask/Product/ProductOption/ProductVariant 接口
- 组件命名 PascalCase，文件命名 kebab-case

## 架构要求

- /api 封装接口，/background 处理 Service Worker 任务队列
- /content_scripts 负责 DOM 提取，/popup 和 /dashboard 分离 UI
- 使用 chrome.storage.local 持久化，注意存储上限
- SPU 生成: spuPrefix + 序列号(补位如 0001)

## 强制要求

- **文档优先**: 每次执行任务必须严格参考并遵循 `docs/需求文档.md` 和 `docs/AI开发文档.md`
- **冲突处理**: 如文档内容冲突，以 `docs/需求文档.md` 为准
- **需求回写**: 明确修改需求时，必须将功能变更回写到 `docs/需求文档.md`
- **执行报告**: 每次任务完成后，在结果报告中明确标注对两份文档的参考情况

## 关键逻辑

- 唯一性校验: 获取前检查 URL/标题是否已存在
- 任务超时: 30 秒无响应标记 failed
- 同步成功后清空本地 productList 并刷新 Store
- 删除规格选项时联动删除相关变体

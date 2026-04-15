# LZ-RuoYi 项目 AI 助手编码规范

## 项目概述
- **项目名称**: LZ-RuoYi (基于若依 RuoYi-Vue3 二次开发)
- **版本**: 3.9.2
- **业务模块**: vh-erp (ERP商品管理系统，集成 Shopify GraphQL API)
- **前端目录**: `RuoYi-Vue3/` (Vue 3 + Element Plus + Vite)
- **后端目录**: 多模块 Maven 项目 (Spring Boot 3.x + MyBatis-Plus)
- **废弃目录**: `ruoyi-ui/` (已放弃使用，不要在此目录修改代码)

## 技术栈

### 后端技术栈
- **框架**: Spring Boot 3.5.11
- **Java 版本**: Java 17
- **ORM**: MyBatis-Plus 3.5.9 + MyBatis 3.0.5
- **数据库连接池**: Druid 1.2.28
- **分页插件**: PageHelper 2.1.1
- **安全框架**: Spring Security + JWT
- **API 文档**: SpringDoc OpenAPI 2.8.16
- **工具类**: Hutool 5.8.38, FastJSON2 2.0.61
- **GraphQL**: Spring Boot GraphQL Client (用于 Shopify API 集成)
- **其他**: Lombok 1.18.36, POI 4.1.2, Quartz 定时任务

### 前端技术栈
- **框架**: Vue 3.5.26 (Composition API)
- **构建工具**: Vite 6.4.1
- **UI 库**: Element Plus 2.13.1
- **状态管理**: Pinia 3.0.4
- **路由**: Vue Router 4.6.4
- **HTTP 客户端**: Axios 1.13.2
- **图表**: ECharts 5.6.0
- **富文本**: @vueup/vue-quill 1.2.0
- **拖拽**: vuedraggable 4.1.0, splitpanes 4.0.4

## 项目模块结构

### 后端模块
```
LZ-RuoYi/
├── ruoyi-admin/          # 主启动模块 (入口)
├── ruoyi-common/         # 通用工具模块
├── ruoyi-framework/      # 核心框架模块 (安全、拦截器、数据源等)
├── ruoyi-system/         # 系统管理模块 (用户、角色、菜单等)
├── ruoyi-quartz/         # 定时任务模块
├── ruoyi-generator/      # 代码生成模块
└── vh-erp/              # ERP 业务模块 (自定义开发)
    └── src/main/java/com/ruoyi/erp/
        ├── controller/    # 控制器层
        ├── service/       # 服务层
        ├── mapper/        # MyBatis-Plus Mapper
        ├── domain/        # 实体类
        └── graphql/       # GraphQL 客户端 (Shopify API)
```

### 前端目录
```
RuoYi-Vue3/
├── src/
│   ├── api/             # API 接口定义
│   │   ├── erp/         # ERP 业务接口
│   │   ├── system/      # 系统管理接口
│   │   └── monitor/     # 监控接口
│   ├── views/           # 页面组件
│   │   ├── erp/         # ERP 业务页面
│   │   │   ├── product/     # 商品管理
│   │   │   ├── variant/     # 商品变体
│   │   │   ├── tag/         # 标签管理
│   │   │   ├── ProductTag/  # 商品标签关联
│   │   │   ├── image/       # 图片管理
│   │   │   ├── media/       # 媒体管理
│   │   │   └── task/        # 任务管理
│   │   ├── system/      # 系统管理页面
│   │   └── monitor/     # 监控页面
│   ├── components/      # 公共组件
│   ├── store/           # Pinia 状态管理
│   ├── router/          # 路由配置
│   ├── utils/           # 工具函数
│   └── layout/          # 布局组件
└── vite/plugins/        # Vite 插件配置
```

## 后端开发规范

### 1. 代码分层架构
- **Controller 层**: 接收请求、参数校验、返回响应
  - 使用 `@RestController` 注解
  - 路径统一使用 `/erp/xxx` 前缀 (vh-erp 模块)
  - 使用若依统一的 `AjaxResult` 返回格式
  
- **Service 层**: 业务逻辑处理
  - 接口命名: `IXxxService`
  - 实现类命名: `XxxServiceImpl`
  - 使用 `@Service` 注解
  - 事务控制使用 `@Transactional`
  
- **Mapper 层**: 数据访问
  - 继承 `BaseMapper<T>` (MyBatis-Plus)
  - 复杂查询在 XML 中编写
  - XML 文件放在 `resources/mapper/` 目录

- **Domain 层**: 实体类
  - 使用 Lombok 简化代码 (`@Data`, `@Builder` 等)
  - 继承 `BaseEntity` (若依基础实体)
  - 使用 MyBatis-Plus 注解 (`@TableName`, `@TableId` 等)

### 2. MyBatis-Plus 使用规范
```java
// ✅ 推荐：使用 LambdaQueryWrapper
LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Product::getStatus, "active")
       .like(Product::getName, keyword)
       .orderByDesc(Product::getCreateTime);

// ❌ 避免：使用字符串字段名
QueryWrapper<Product> wrapper = new QueryWrapper<>();
wrapper.eq("status", "active"); // 容易拼写错误
```

### 3. 统一返回格式
```java
// 成功返回
return AjaxResult.success(data);
return AjaxResult.success("操作成功", data);

// 失败返回
return AjaxResult.error("操作失败");
return AjaxResult.error(500, "错误信息");
```

### 4. 异常处理
- 使用若依全局异常处理器
- 业务异常抛出 `ServiceException`
- 不要在 Controller 层捕获异常，让全局处理器统一处理

### 5. 日志规范
- 使用 Slf4j: `private static final log = LoggerFactory.getLogger(XxxClass.class);`
- 关键业务操作记录 INFO 级别日志
- 异常记录 ERROR 级别日志
- 调试信息使用 DEBUG 级别

### 6. Shopify GraphQL 集成
- GraphQL 客户端配置在 `vh-erp` 模块
- 使用 Spring GraphQL Client 发起请求
- GraphQL 查询语句放在独立的 `.graphql` 文件或常量类中
- 注意 Shopify API 的速率限制和分页处理

## 前端开发规范

### 1. 组件结构
- **必须**使用 `<script setup>` 语法
- **必须**定义 `name` 属性 (用于 keep-alive 缓存)
- **优先**使用 Composition API，避免 Options API
- 组件文件命名使用 PascalCase (如 `ProductListPanel.vue`)

```vue
<script setup name="ProductList">
import { ref, reactive, onMounted } from 'vue'
import { getProductList } from '@/api/erp/product'

// 响应式数据
const productList = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  productName: undefined
})

// 方法定义
function getList() {
  getProductList(queryParams).then(response => {
    productList.value = response.rows
  })
}

onMounted(() => {
  getList()
})
</script>

<template>
  <div class="app-container">
    <!-- 模板内容 -->
  </div>
</template>

<style scoped lang="scss">
// 样式
</style>
```

### 2. API 调用规范
- API 文件放在 `src/api/erp/` 目录
- 使用统一的 request 封装 (Axios)
- 异步操作使用 async/await 或 Promise.then()

```javascript
// api/erp/product.js
import request from '@/utils/request'

// 查询商品列表
export function getProductList(params) {
  return request({
    url: '/erp/product/list',
    method: 'get',
    params
  })
}

// 新增商品
export function addProduct(data) {
  return request({
    url: '/erp/product',
    method: 'post',
    data
  })
}
```

### 3. 表格与分页
- 使用 Element Plus 的 `el-table` 和 `el-pagination`
- 使用若依封装的 `Pagination` 组件
- 表格数据绑定 `v-loading` 显示加载状态

```vue
<template>
  <el-table v-loading="loading" :data="productList">
    <el-table-column label="商品名称" prop="productName" />
    <el-table-column label="价格" prop="price" />
    <el-table-column label="操作" align="center">
      <template #default="scope">
        <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
      </template>
    </el-table-column>
  </el-table>
  
  <pagination
    v-show="total > 0"
    :total="total"
    v-model:page="queryParams.pageNum"
    v-model:limit="queryParams.pageSize"
    @pagination="getList"
  />
</template>
```

### 4. 表单验证
- 使用 Element Plus 的表单验证规则
- 必填字段添加 `required` 规则
- 自定义验证器放在单独的验证函数中

```vue
<script setup>
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const form = reactive({
  productName: '',
  price: null
})

const rules = {
  productName: [
    { required: true, message: '商品名称不能为空', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '价格不能为空', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格必须大于0', trigger: 'blur' }
  ]
}

function handleSubmit() {
  formRef.value.validate(valid => {
    if (valid) {
      // 提交表单
    }
  })
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules">
    <el-form-item label="商品名称" prop="productName">
      <el-input v-model="form.productName" />
    </el-form-item>
    <el-form-item label="价格" prop="price">
      <el-input-number v-model="form.price" />
    </el-form-item>
  </el-form>
</template>
```

### 5. 状态管理 (Pinia)
- 全局状态使用 Pinia Store
- Store 文件放在 `src/store/modules/` 目录
- 避免在组件中直接修改 Store 状态，使用 actions

```javascript
// store/modules/erp.js
import { defineStore } from 'pinia'

export const useErpStore = defineStore('erp', {
  state: () => ({
    currentProduct: null,
    tagList: []
  }),
  actions: {
    setCurrentProduct(product) {
      this.currentProduct = product
    },
    updateTagList(tags) {
      this.tagList = tags
    }
  }
})
```

### 6. 路由与权限
- 路由配置在 `src/router/index.js`
- 动态路由从后端获取 (若依权限系统)
- 页面级权限使用 `v-hasPermi` 指令
- 按钮级权限也使用 `v-hasPermi` 指令

```vue
<template>
  <!-- 按钮权限控制 -->
  <el-button v-hasPermi="['erp:product:add']" @click="handleAdd">新增</el-button>
  <el-button v-hasPermi="['erp:product:edit']" @click="handleEdit">编辑</el-button>
  <el-button v-hasPermi="['erp:product:remove']" @click="handleDelete">删除</el-button>
</template>
```

### 7. 字典数据使用
- 使用若依封装的 `useDict` composable
- 字典数据缓存在本地，减少请求

```vue
<script setup>
import { useDict } from '@/utils/dict'

const { product_status, product_type } = useDict('product_status', 'product_type')
</script>

<template>
  <el-select v-model="form.status">
    <el-option
      v-for="dict in product_status"
      :key="dict.value"
      :label="dict.label"
      :value="dict.value"
    />
  </el-select>
</template>
```

### 8. 组件通信
- 父子组件: props / emits
- 兄弟组件: Pinia Store 或事件总线
- 跨层级组件: provide / inject 或 Pinia Store

## 数据库规范

### 1. 表命名
- 使用小写字母和下划线: `erp_product`, `erp_product_tag`
- 业务模块表添加前缀: `erp_`
- 避免使用 MySQL 保留字

### 2. 字段规范
- **必须**包含的基础字段:
  - `id`: bigint, 主键，自增
  - `create_by`: varchar(64), 创建者
  - `create_time`: datetime, 创建时间
  - `update_by`: varchar(64), 更新者
  - `update_time`: datetime, 更新时间
  - `remark`: varchar(500), 备注
  
- 软删除字段: `del_flag` char(1) default '0' ('0'存在 '2'删除)

### 3. 索引规范
- 主键自动创建索引
- 外键字段添加索引
- 频繁查询的字段添加索引
- 联合索引遵循最左前缀原则

### 4. SQL 文件管理
- 初始化脚本放在 `sql/` 目录
- 按日期命名: `20260324_erp.sql`
- 菜单数据单独文件: `20260324_erp_menu.sql`

## 代码生成器使用

### 1. 使用若依代码生成器
- 访问: 系统工具 -> 代码生成
- 导入数据库表
- 配置生成信息 (包路径、作者等)
- 生成代码并下载到项目对应目录

### 2. 生成后调整
- 检查生成的 Controller、Service、Mapper
- 补充业务逻辑
- 调整前端页面布局和交互
- 添加权限标识


## 常见注意事项

### 1. 前端开发
- ✅ **在 `RuoYi-Vue3/` 目录下开发**
- ❌ **不要在 `ruoyi-ui/` 目录下修改** (已废弃)
- 修改前端代码后，Vite 会自动热重载
- 生产环境打包: `npm run build:prod`

### 2. 后端开发
- 修改 Java 代码后需要重新编译: `mvn clean package`
- 启动命令: `mvn spring-boot:run` (在 ruoyi-admin 模块)
- 或使用 IDE 运行 `RuoYiApplication.java`

### 3. 前后端联调
- 前端开发环境代理配置在 `RuoYi-Vue3/vite.config.js`
- 后端默认端口: 8080
- 前端默认端口: 80 (开发环境可能是其他端口)
- 跨域问题在后端已通过 CORS 配置解决

### 4. Shopify API 集成
- API 密钥和凭证放在配置文件，不要硬编码
- 注意 Shopify GraphQL API 的版本号
- 处理 API 速率限制 (使用重试机制)
- 大量数据同步考虑使用后台任务

### 5. 性能优化
- 后端: 使用 MyBatis-Plus 分页，避免全表查询
- 前端: 大列表使用虚拟滚动，图片懒加载
- 缓存: 字典数据、配置信息使用缓存
- 数据库: 合理添加索引，避免 N+1 查询

### 6. 安全注意事项
- 敏感信息 (密码、密钥) 不要明文存储
- 使用若依的数据权限控制
- 防止 SQL 注入 (使用参数化查询)
- 防止 XSS 攻击 (若依已内置过滤器)
- API 接口添加权限验证

## 调试技巧

### 后端调试
```java
// 1. 日志输出
log.info("查询参数: {}", params);
log.error("异常信息: ", exception);

// 2. 断点调试
// 在 IDE 中设置断点，使用 Debug 模式启动

// 3. SQL 日志
// 在 application.yml 中开启 MyBatis SQL 日志
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 前端调试
```javascript
// 1. 控制台输出
console.log('数据:', data)
console.table(list) // 表格形式展示数组

// 2. Vue DevTools
// 安装 Vue DevTools 浏览器扩展查看组件状态

// 3. Network 面板
// 查看 API 请求和响应
```

## 快速开始

### 后端启动
```bash
# 1. 导入数据库脚本
mysql -u root -p < sql/20240629_init1.sql
mysql -u root -p < sql/20240629_init2.sql
mysql -u root -p < sql/20260324_erp.sql
mysql -u root -p < sql/20260324_erp_menu.sql

# 2. 修改配置文件
# ruoyi-admin/src/main/resources/application.yml
# 配置数据库连接、Redis 等

# 3. 编译项目
mvn clean package

# 4. 启动后端
cd ruoyi-admin
mvn spring-boot:run
```

### 前端启动
```bash
# 1. 进入前端目录
cd RuoYi-Vue3

# 2. 安装依赖
npm install
# 或
yarn install

# 3. 启动开发服务器
npm run dev

# 4. 访问 http://localhost:80 (或配置的端口)
```

## AI 助手使用建议

当你向 AI 助手提问时：

1. **明确模块**: 说明是前端还是后端，哪个模块 (如 vh-erp)
2. **提供上下文**: 相关的代码片段、错误信息
3. **说明需求**: 清晰描述要实现的功能或解决的问题
4. **指定技术**: 如果需要特定技术方案，明确说明

### 示例提问
```
✅ 好的提问:
"在 vh-erp 模块的商品管理中，如何实现商品批量导入功能？
后端使用 MyBatis-Plus，前端使用 Element Plus 的上传组件。"

❌ 不好的提问:
"怎么做批量导入？"
```

---

**最后更新**: 2026-04-14
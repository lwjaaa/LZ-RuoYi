# LZ-RuoYi 项目说明文档 (AI Coding 友好版)

## 📌 项目概述

本项目是基于若依（RuoYi）框架深度定制的开发项目。为了提供更高效的协作体验和精准的 AI 辅助编码支持，特编写此文档以明确项目的技术栈、目录结构及开发规范。

## ⚠️ 重要声明 (AI Coding 核心上下文)

1. **前端项目说明**：
   - 🚫 **`ruoyi-ui` 是已弃用的前端项目**，仅作为历史代码归档或参考，**请勿在此目录下进行任何功能开发或代码修改**。
   - ✅ **真正的前端项目是 `RuoYi-Vue3`**，所有前端相关的新需求、Bug 修复和代码生成请严格在 `RuoYi-Vue3` 目录下进行。
2. **后端项目说明**：
   - 后端依然沿用标准的若依 Spring Boot 多模块架构。

## 🛠️ 技术栈

### 后端 (Spring Boot)

| 技术            | 版本    | 说明           |
| --------------- | ------- | -------------- |
| Spring Boot     | 3.5.11  | 核心框架       |
| Java            | 17      | 开发语言       |
| MyBatis-Plus    | 3.5.9   | ORM 框架       |
| MyBatis         | 3.0.5   | 持久层框架     |
| Druid           | 1.2.28  | 数据库连接池   |
| PageHelper      | 2.1.1   | 分页插件       |
| Spring Security | -       | 安全框架       |
| JWT             | 0.9.1   | Token 认证     |
| SpringDoc       | 2.8.16  | API 文档       |
| FastJSON2       | 2.0.61  | JSON 解析      |
| Hutool          | 5.8.38  | 工具类库       |
| Lombok          | 1.18.36 | 代码简化       |
| Spring GraphQL  | -       | GraphQL 客户端 |
| POI             | 4.1.2   | Excel 处理     |
| Quartz          | -       | 定时任务       |

### 前端 (RuoYi-Vue3)

| 技术         | 版本   | 说明                   |
| ------------ | ------ | ---------------------- |
| Vue          | 3.5.26 | 渐进式 JavaScript 框架 |
| Vite         | 6.4.1  | 构建工具               |
| Element Plus | 2.13.1 | UI 组件库              |
| Pinia        | 3.0.4  | 状态管理               |
| Vue Router   | 4.6.4  | 路由管理               |
| Axios        | 1.13.2 | HTTP 客户端            |
| ECharts      | 5.6.0  | 图表库                 |
| Vue Quill    | 1.2.0  | 富文本编辑器           |
| VueDraggable | 4.1.0  | 拖拽排序               |
| Splitpanes   | 4.0.4  | 面板分割               |

## 📂 核心目录结构

```text
LZ-RuoYi/
├── ruoyi-admin/ # 主启动模块（入口）
│ └── src/main/
│ ├── java/ # Java 源码
│ └── resources/ # 配置文件
├── ruoyi-common/ # 通用工具模块
│ └── src/main/java/
│ ├── annotation/ # 自定义注解
│ ├── config/ # 通用配置
│ ├── constant/ # 常量定义
│ ├── core/ # 核心类
│ ├── enums/ # 枚举类
│ ├── exception/ # 异常处理
│ ├── filter/ # 过滤器
│ ├── utils/ # 工具类
│ └── xss/ # XSS 防护
├── ruoyi-framework/ # 核心框架模块
│ └── src/main/java/
│ ├── aspectj/ # AOP 切面
│ ├── config/ # 框架配置
│ ├── datasource/ # 数据源
│ ├── interceptor/ # 拦截器
│ ├── manager/ # 管理器
│ ├── security/ # 安全认证
│ └── web/ # Web 相关
├── ruoyi-system/ # 系统管理模块
│ └── src/main/
│ ├── java/ # 用户、角色、菜单等
│ └── resources/mapper/ # MyBatis XML
├── ruoyi-quartz/ # 定时任务模块
├── ruoyi-generator/ # 代码生成模块
├── vh-erp/ # ERP 业务模块（自定义开发）⭐
│ └── src/main/java/com/ruoyi/erp/
│ ├── config/ # 配置类
│ ├── constant/ # 常量
│ ├── controller/ # 控制器层
│ ├── mapper/ # MyBatis-Plus Mapper
│ ├── model/ # 实体类
│ ├── service/ # 服务层
│ ├── task/ # 定时任务
│ └── graphql/ # GraphQL 客户端（Shopify）
├── RuoYi-Vue3/ # 前端项目（Vue 3 + Vite）⭐
│ ├── src/
│ │ ├── api/ # API 接口
│ │ │ ├── erp/ # ERP 业务接口
│ │ │ ├── system/ # 系统管理接口
│ │ │ └── monitor/ # 监控接口
│ │ ├── views/ # 页面组件
│ │ │ ├── erp/ # ERP 业务页面
│ │ │ │ ├── product/ # 商品管理
│ │ │ │ ├── variant/ # 商品变体
│ │ │ │ ├── tag/ # 标签管理
│ │ │ │ ├── ProductTag/ # 商品标签关联
│ │ │ │ ├── image/ # 图片管理
│ │ │ │ ├── media/ # 媒体管理
│ │ │ │ └── task/ # 任务管理
│ │ │ ├── system/ # 系统管理页面
│ │ │ └── monitor/ # 监控页面
│ │ ├── components/ # 公共组件
│ │ ├── store/ # Pinia 状态管理
│ │ ├── router/ # 路由配置
│ │ ├── utils/ # 工具函数
│ │ └── layout/ # 布局组件
│ └── vite/plugins/ # Vite 插件配置
├── ruoyi-ui/ # ⚠️ 已废弃（不要在此目录修改）
├── sql/ # 数据库脚本
│ ├── 20240629_init1.sql # 初始化脚本 1
│ ├── 20240629_init2.sql # 初始化脚本 2
│ ├── 20260324_erp.sql # ERP 模块表结构
│ └── 20260324_erp_menu.sql # ERP 模块菜单数据
├── doc/ # 项目文档
└── pom.xml # Maven 父 POM
```

---

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 5.7+ / 8.0+
- **Redis**: 5.0+
- **Node.js**: 18+
- **npm/yarn**: 最新稳定版

### 数据库初始化

1. 创建数据库 `ry-vue`（或自定义名称）
2. 按顺序执行 SQL 脚本：

```text
bash
进入 sql 目录
cd sql
执行初始化脚本
mysql -u root -p ry-vue < 20240629_init1.sql mysql -u root -p ry-vue < 20240629_init2.sql
执行 ERP 模块脚本
mysql -u root -p ry-vue < 20260324_erp.sql mysql -u root -p ry-vue < 20260324_erp_menu.sql
```

### 后端启动

1. **修改配置文件**

编辑 `ruoyi-admin/src/main/resources/application-dev.yml`

2. **编译项目**

```text
bash
在项目根目录执行
mvn clean package -DskipTests
```

3. **启动应用**

```text
bash
方式一：使用 Maven
cd ruoyi-admin
mvn spring-boot:run
方式二：使用启动脚本（Windows）
.\ry.bat
方式三：使用启动脚本（Linux/Mac）
./ry.sh
方式四：直接运行 JAR
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

4. **访问后端**

- 应用地址: http://localhost:8080
- API 文档: http://localhost:8080/swagger-ui.html

### 前端启动

1. **安装依赖**

```text
bash
进入前端目录
cd RuoYi-Vue3
使用 npm
npm install
或使用 yarn
yarn install
```

2. **修改配置**

编辑 `.env.development`：

```text
env
开发环境配置
VUE_APP_BASE_API = 'http://localhost:8080'
```

3. **启动开发服务器**

```text
bash
开发模式
npm run dev
或
yarn dev
```

4. **访问前端**

浏览器打开: http://localhost:80

默认账号: `admin`  
默认密码: `admin123`

### 生产环境部署

1. **前端打包**

```text
bash
cd RuoYi-Vue3
npm run build:prod
```

打包后的文件在 `dist` 目录，可部署到 Nginx 或其他 Web 服务器。

2. **后端打包**

```
bash
mvn clean package -DskipTests
```

生成的 JAR 文件在 `ruoyi-admin/target/ruoyi-admin.jar`

3. **Nginx 配置示例**

```text
nginx
server {
    listen 80;
    server_name your-domain.com;
    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /prod-api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 📋 功能清单

### 系统管理（若依原生）

1. 👤 **用户管理**: 系统用户配置与管理
2. 🏢 **部门管理**: 组织机构配置，树形结构展示，支持数据权限
3. 💼 **岗位管理**: 用户职务配置
4. 📑 **菜单管理**: 菜单配置、操作权限、按钮权限标识
5. 🎭 **角色管理**: 角色权限分配、数据范围划分
6. 📚 **字典管理**: 固定数据维护
7. ⚙️ **参数管理**: 系统动态参数配置
8. 📢 **通知公告**: 系统消息发布
9. 📝 **操作日志**: 正常操作与异常信息记录
10. 🔐 **登录日志**: 登录记录与异常查询
11. 👥 **在线用户**: 活跃用户监控
12. ⏰ **定时任务**: 任务调度与执行结果日志
13. 🔧 **代码生成**: 前后端代码自动生成（Java/HTML/XML/SQL）
14. 📖 **系统接口**: API 接口文档自动生成
15. 📊 **服务监控**: CPU、内存、磁盘、堆栈监控
16. 💾 **缓存监控**: 缓存信息查询与统计
17. 🎨 **在线构建器**: 表单拖拽生成 HTML
18. 🔗 **连接池监控**: 数据库连接池状态与 SQL 分析

### ERP 业务模块（vh-erp）⭐

19. 🛍️ **商品管理**:
    - 商品创建向导（两步流程）
    - 商品信息录入（标题、类别、类型）
    - 商品详情多选项卡（DESCRIPTION/SIZE/MATERIAL/NOTE/PACKAGE_INCLUDE）
    - 富文本编辑器支持 HTML 编辑与预览
    - 商品来源 URL、采购链接管理

20. 🏷️ **标签管理**:
    - 多级标签分类
    - 标签级联选择器
    - 标签与商品关联

21. 📦 **变体管理**:
    - 动态选项配置（采购名称/英文名称）
    - 变体分录表格
    - SKU 自动生成与管理
    - 规格图拖拽上传
    - 变体拖拽排序

22. 💰 **价格与成本**:
    - 采购价录入
    - 包装尺寸（长/宽/高）
    - 实重与材积重自动计算
    - 运费自动计算
    - 成本价 = 采购价 + 运费
    - 汇率管理（自动同步）
    - 建议售价（30% 利润）
    - 实际利润与利润率计算

23. 🖼️ **媒体资源管理**:
    - 图片/视频上传与管理
    - 服务器图片导入
    - 图片拖拽排序
    - 图片预览与放大
    - 视频缩略图与播放
    - 媒体文件搜索

24. 🔄 **Shopify 集成**:
    - GraphQL API 对接
    - 商品同步到 Shopify
    - 变体同步
    - 媒体资源同步
    - 速率限制处理

25. ⚡ **定时任务**:
    - 汇率自动更新
    - 商品同步任务
    - 数据清理任务

---

## 🔧 开发指南

### 后端开发规范

#### 代码分层

```java
// Controller 层 - 接收请求
@RestController
@RequestMapping("/erp/product")
public class ErpProductController {
    @Autowired
    private IErpProductService productService;

    @GetMapping("/list")
    public TableDataInfo list(ErpProduct product) {
        startPage();
        List<ErpProduct> list = productService.selectList(product);
        return getDataTable(list);
    }
}

// Service 层 - 业务逻辑
@Service
public class ErpProductServiceImpl implements IErpProductService {
    @Autowired
    private ErpProductMapper productMapper;

    @Override
    @Transactional
    public int insert(ErpProduct product) {
        return productMapper.insert(product);
    }
}

// Mapper 层 - 数据访问
@Mapper
public interface ErpProductMapper extends BaseMapper<ErpProduct> {
    // 复杂查询在 XML 中编写
}
```

#### MyBatis-Plus 最佳实践

```java
// ✅ 推荐：使用 LambdaQueryWrapper
LambdaQueryWrapper<ErpProduct> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(ErpProduct::getStatus, "active")
       .like(ErpProduct::getProductName, keyword)
       .orderByDesc(ErpProduct::getCreateTime);

// ❌ 避免：字符串字段名
QueryWrapper<ErpProduct> wrapper = new QueryWrapper<>();
wrapper.eq("status", "active"); // 容易拼写错误
```

### 前端开发规范

#### 组件结构

```vue
<script setup name="ProductList">
import { ref, reactive, onMounted } from "vue";
import { getProductList } from "@/api/erp/product";

const productList = ref([]);
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  productName: undefined,
});

function getList() {
  getProductList(queryParams).then((response) => {
    productList.value = response.rows;
  });
}

onMounted(() => {
  getList();
});
</script>

<template>
  <div class="app-container">
    <!-- 内容 -->
  </div>
</template>

<style scoped lang="scss">
// 样式
</style>
```

#### API 调用

```javascript
// api/erp/product.js
import request from "@/utils/request";

export function getProductList(params) {
  return request({
    url: "/erp/product/list",
    method: "get",
    params,
  });
}

export function addProduct(data) {
  return request({
    url: "/erp/product",
    method: "post",
    data,
  });
}
```

### Shopify GraphQL 集成

```java
@Configuration
public class ShopifyGraphQlConfig {

    @Bean
    public GraphQlClient shopifyGraphQlClient(WebClient.Builder webClientBuilder) {
        WebClient webClient = webClientBuilder
            .baseUrl("https://{shop}.myshopify.com/admin/api/2024-01/graphql.json")
            .defaultHeader("X-Shopify-Access-Token", accessToken)
            .build();

        return GraphQlClient.create(webClient);
    }
}
```

---

## 📊 数据库设计

### 核心表结构

- **erp_product**: 商品主表
- **erp_product_variant**: 商品变体表
- **erp_tag**: 标签表
- **erp_product_tag**: 商品标签关联表
- **erp_media**: 媒体资源表

详细表结构请参考 `sql/20260324_erp.sql`

---

## 🔒 安全说明

- 使用 Spring Security + JWT 进行身份认证
- 密码加密存储（BCrypt）
- XSS 攻击防护
- SQL 注入防护（参数化查询）
- CSRF 防护
- 数据权限控制（基于角色和部门）

---

## 📝 常见问题

### Q1: 前端页面空白？

检查浏览器控制台是否有跨域错误，确保后端 CORS 配置正确。

### Q2: 登录失败？

1. 检查 Redis 是否正常运行
2. 检查数据库连接配置
3. 确认账号密码是否正确（默认 admin/admin123）

### Q3: Shopify API 调用失败？

1. 检查 Access Token 是否正确
2. 确认 Shop 域名格式
3. 查看 API 速率限制

### Q4: 图片无法显示？

检查文件上传路径配置和 Nginx 静态资源配置。

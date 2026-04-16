# RuoYi-Vue3 前端开发指南

> **LZ-RuoYi 系统前端工程** - 基于 Vue 3 + Vite + Element Plus 的现代化企业级 ERP 管理系统前端。

---

## 📋 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术栈](#2-技术栈)
- [3. 项目结构](#3-项目结构)
- [4. 快速开始](#4-快速开始)
- [5. 开发规范](#5-开发规范)
- [6. ERP 业务模块](#6-erp-业务模块)
- [7. 核心功能](#7-核心功能)
- [8. API 调用规范](#8-api-调用规范)
- [9. 组件开发](#9-组件开发)
- [10. 常见问题](#10-常见问题)
- [11. AI Coding 指南](#11-ai-coding-指南)

---

## 1. 项目概述

### 1.1 项目信息

- **项目名称**: LZ-RuoYi Frontend (RuoYi-Vue3)
- **版本**: 3.9.2
- **框架**: Vue 3.5.26 + Vite 6.4.1
- **UI 库**: Element Plus 2.13.1
- **状态管理**: Pinia 3.0.4
- **路由**: Vue Router 4.6.4
- **构建工具**: Vite 6.4.1

### 1.2 项目特点

✅ **现代化技术栈**: Vue 3 Composition API + Vite  
✅ **企业级 UI**: Element Plus 组件库  
✅ **响应式设计**: 适配多种屏幕尺寸  
✅ **权限控制**: 基于角色的动态路由和按钮权限  
✅ **模块化设计**: 清晰的目录结构和组件划分  
✅ **ERP 业务集成**: 完整的商品管理、Shopify 集成  

### 1.3 重要声明

```
⚠️ 这是唯一的前端开发目录！

✅ RuoYi-Vue3/     # 当前项目，所有前端开发在此进行
❌ ruoyi-ui/       # 已废弃的 Vue 2 项目，禁止修改
```

---

## 2. 技术栈

### 2.1 核心依赖

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.26 | 渐进式 JavaScript 框架 |
| Vite | 6.4.1 | 下一代前端构建工具 |
| Element Plus | 2.13.1 | Vue 3 UI 组件库 |
| Pinia | 3.0.4 | Vue 3 状态管理 |
| Vue Router | 4.6.4 | 官方路由管理器 |
| Axios | 1.13.2 | HTTP 客户端 |

### 2.2 UI 增强

| 技术 | 版本 | 用途 |
|------|------|------|
| @element-plus/icons-vue | 2.3.2 | Element Plus 图标库 |
| @vueup/vue-quill | 1.2.0 | 富文本编辑器 |
| vue-cropper | 1.1.1 | 图片裁剪 |
| splitpanes | 4.0.4 | 面板分割 |
| vuedraggable | 4.1.0 | 拖拽排序 |

### 2.3 工具库

| 技术 | 版本 | 用途 |
|------|------|------|
| @vueuse/core | 14.1.0 | Vue 组合式 API 工具集 |
| echarts | 5.6.0 | 数据可视化图表 |
| clipboard | 2.0.11 | 剪贴板操作 |
| js-cookie | 3.0.5 | Cookie 管理 |
| jsencrypt | 3.3.2 | RSA 加密 |
| nprogress | 0.2.0 | 进度条 |
| file-saver | 2.0.5 | 文件保存 |
| fuse.js | 7.1.0 | 模糊搜索 |

### 2.4 开发依赖

| 技术 | 版本 | 用途 |
|------|------|------|
| @vitejs/plugin-vue | 5.2.4 | Vue 3 Vite 插件 |
| sass-embedded | 1.97.2 | CSS 预处理器 |
| unplugin-auto-import | 0.18.6 | 自动导入 API |
| vite-plugin-compression | 0.5.1 | Gzip 压缩 |
| vite-plugin-svg-icons | 2.0.1 | SVG 图标 |

---

## 3. 项目结构

```
RuoYi-Vue3/
├── src/
│   ├── api/                    # API 接口定义
│   │   ├── erp/                # ERP 业务接口 ⭐
│   │   │   ├── product.js           # 商品管理
│   │   │   ├── variant.js           # 变体管理
│   │   │   ├── tag.js               # 标签管理
│   │   │   ├── ProductTag.js        # 商品标签关联
│   │   │   ├── media.js             # 媒体管理
│   │   │   ├── image.js             # 图片管理
│   │   │   ├── exchange-rate.js     # 汇率管理
│   │   │   ├── task.js              # 任务管理
│   │   │   └── productWizard.js     # 商品向导
│   │   ├── system/             # 系统管理接口
│   │   ├── monitor/            # 监控接口
│   │   ├── tool/               # 工具接口
│   │   ├── login.js            # 登录接口
│   │   └── menu.js             # 菜单接口
│   │
│   ├── assets/                 # 静态资源
│   │   ├── images/             # 图片
│   │   ├── icons/              # 图标
│   │   ├── logo/               # Logo
│   │   └── styles/             # 全局样式
│   │
│   ├── components/             # 公共组件
│   │   ├── Breadcrumb/         # 面包屑导航
│   │   ├── DictTag/            # 字典标签
│   │   ├── Editor/             # 富文本编辑器
│   │   ├── FileUpload/         # 文件上传
│   │   ├── ImageUpload/        # 图片上传
│   │   ├── Pagination/         # 分页组件
│   │   ├── RightToolbar/       # 右侧工具栏
│   │   ├── SvgIcon/            # SVG 图标
│   │   ├── erp/                # ERP 业务组件 ⭐
│   │   │   ├── ProductCreationWizard.vue  # 商品创建向导
│   │   │   ├── VariantTable.vue           # 变体表格
│   │   │   ├── MediaManager.vue           # 媒体管理器
│   │   │   └── ...
│   │   └── ...
│   │
│   ├── directive/              # 自定义指令
│   │   ├── permission/         # 权限指令
│   │   │   ├── hasPermi.js          # 按钮权限
│   │   │   └── hasRole.js           # 角色权限
│   │   └── index.js
│   │
│   ├── layout/                 # 布局组件
│   │   ├── components/         # 布局子组件
│   │   │   ├── Sidebar/             # 侧边栏
│   │   │   ├── Navbar/              # 顶部导航
│   │   │   ├── TagsView/            # 标签页
│   │   │   └── AppMain/             # 主内容区
│   │   └── index.vue           # 布局入口
│   │
│   ├── plugins/                # 插件
│   │   ├── auth.js             # 认证插件
│   │   ├── cache.js            # 缓存插件
│   │   ├── download.js         # 下载插件
│   │   ├── modal.js            # 弹窗插件
│   │   └── tab.js              # 标签页插件
│   │
│   ├── router/                 # 路由配置
│   │   └── index.js            # 路由定义
│   │
│   ├── store/                  # Pinia 状态管理
│   │   ├── modules/            # 状态模块
│   │   │   ├── app.js               # 应用配置
│   │   │   ├── user.js              # 用户信息
│   │   │   ├── permission.js        # 权限路由
│   │   │   ├── tagsView.js          # 标签页
│   │   │   └── settings.js          # 系统设置
│   │   └── index.js            # Store 入口
│   │
│   ├── utils/                  # 工具函数
│   │   ├── auth.js             # 认证工具
│   │   ├── dict.js             # 字典工具
│   │   ├── request.js          # Axios 封装 ⭐
│   │   ├── validate.js         # 验证工具
│   │   ├── ruoyi.js            # 若依工具
│   │   └── ...
│   │
│   ├── views/                  # 页面组件
│   │   ├── erp/                # ERP 业务页面 ⭐
│   │   │   ├── product/             # 商品管理
│   │   │   │   ├── index.vue            # 商品列表
│   │   │   │   ├── create.vue           # 商品创建
│   │   │   │   └── detail.vue           # 商品详情
│   │   │   ├── variant/             # 变体管理
│   │   │   ├── tag/                 # 标签管理
│   │   │   ├── ProductTag/          # 商品标签关联
│   │   │   ├── media/               # 媒体管理
│   │   │   ├── image/               # 图片管理
│   │   │   └── task/                # 任务管理
│   │   ├── system/             # 系统管理页面
│   │   ├── monitor/            # 监控页面
│   │   ├── tool/               # 工具页面
│   │   ├── index.vue           # 首页
│   │   ├── login.vue           # 登录页
│   │   └── register.vue        # 注册页
│   │
│   ├── App.vue                 # 根组件
│   ├── main.js                 # 入口文件
│   ├── permission.js           # 权限控制
│   └── settings.js             # 系统配置
│
├── vite/                       # Vite 配置
│   └── plugins/                # Vite 插件
│       ├── auto-import.js           # 自动导入
│       ├── compression.js           # 压缩插件
│       ├── index.js                 # 插件集合
│       ├── setup-extend.js          # setup 扩展
│       └── svg-icon.js              # SVG 图标
│
├── public/                     # 公共资源
│   └── favicon.ico             # 网站图标
│
├── .env.development            # 开发环境变量
├── .env.production             # 生产环境变量
├── .env.staging                # 测试环境变量
├── vite.config.js              # Vite 配置文件
├── package.json                # 依赖配置
└── yarn.lock                   # 依赖锁定
```

---

## 4. 快速开始

### 4.1 环境要求

- **Node.js**: 18+ (推荐 20 LTS)
- **npm/yarn**: 最新稳定版
- **浏览器**: Chrome 90+, Edge 90+, Firefox 88+

### 4.2 安装依赖

```bash
# 进入项目目录
cd RuoYi-Vue3

# 使用 npm
npm install

# 或使用 yarn（推荐）
yarn install
```

### 4.3 配置环境变量

编辑 `.env.development`：

```env
# 页面标题
VITE_APP_TITLE = LZ-RuoYi 管理系统

# 开发环境配置
VITE_APP_ENV = 'development'

# 后端 API 地址（通过 Vite 代理）
VITE_APP_BASE_API = '/dev-api'
```

**Vite 代理配置** (`vite.config.js`)：

```javascript
server: {
  port: 80,
  host: true,
  open: true,
  proxy: {
    '/dev-api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true,
      rewrite: (p) => p.replace(/^\/dev-api/, '')
    }
  }
}
```

### 4.4 启动开发服务器

```bash
# 开发模式
npm run dev
# 或
yarn dev
```

访问: http://localhost:80

默认账号: `admin`  
默认密码: `admin123`

### 4.5 生产环境打包

```bash
# 生产环境
npm run build:prod

# 测试环境
npm run build:stage
```

打包后的文件在 `dist/` 目录，可部署到 Nginx 或其他 Web 服务器。

### 4.6 Nginx 部署配置

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /usr/share/nginx/html;
    index index.html;

    # 前端路由 History 模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /prod-api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
}
```

---

## 5. 开发规范

### 5.1 Vue 3 组件规范

**强制规则：** 必须使用 `<script setup>` 语法。

```vue
<!-- ✅ 正确：Vue 3 Composition API -->
<script setup name="ProductList">
import { ref, reactive, onMounted } from 'vue'
import { getProductList } from '@/api/erp/product'

// 响应式数据
const productList = ref([])
const loading = ref(false)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  productName: undefined
})

// 方法定义
function getList() {
  loading.value = true
  getProductList(queryParams).then(response => {
    productList.value = response.rows
    loading.value = false
  })
}

// 生命周期
onMounted(() => {
  getList()
})
</script>

<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="productList">
      <!-- 表格内容 -->
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}
</style>
```

```vue
<!-- ❌ 错误：Vue 2 Options API（禁止使用） -->
<script>
export default {
  data() {
    return {
      productList: []
    }
  },
  methods: {
    getList() {
      // ...
    }
  }
}
</script>
```

### 5.2 命名规范

#### 文件命名

```
✅ 组件文件：PascalCase.vue
ProductList.vue
ProductCreationWizard.vue
VariantTable.vue

✅ API 文件：camelCase.js
product.js
exchange-rate.js

✅ 样式文件：kebab-case.scss
product-list.scss
variant-table.scss
```

#### 变量/函数命名

```javascript
// ✅ 正确：小驼峰
const productList = ref([])
const queryParams = reactive({})
function getProductList() { }
function handleEdit(row) { }

// ❌ 错误：其他命名方式
const product_list = ref([])  // 蛇形命名
const ProductList = ref([])   // 大驼峰（用于组件名）
```

#### CSS 类名

```scss
// ✅ 正确：短横线分隔
.product-list-container
.product-item-card
.variant-table-row

// ❌ 错误：其他命名方式
.productListContainer  // 小驼峰
.product_list_container // 蛇形命名
```

### 5.3 代码组织规范

```vue
<script setup name="ComponentName">
// 1. 导入语句
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductList, deleteProduct } from '@/api/erp/product'

// 2. 组件选项（如需要）
defineOptions({
  name: 'ComponentName'
})

// 3. Props 定义
const props = defineProps({
  productId: {
    type: Number,
    required: true
  }
})

// 4. Emits 定义
const emit = defineEmits(['update', 'delete'])

// 5. 响应式数据
const loading = ref(false)
const dataList = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

// 6. 计算属性
const total = computed(() => dataList.value.length)

// 7. 方法定义
function getList() {
  loading.value = true
  getProductList(queryParams).then(response => {
    dataList.value = response.rows
    loading.value = false
  })
}

function handleDelete(id) {
  ElMessageBox.confirm('确认删除？', '提示', {
    type: 'warning'
  }).then(() => {
    deleteProduct(id).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  })
}

// 8. 生命周期钩子
onMounted(() => {
  getList()
})
</script>

<template>
  <!-- 模板内容 -->
</template>

<style scoped lang="scss">
// 样式
</style>
```

### 5.4 权限控制规范

**按钮权限：** 使用 `v-hasPermi` 指令

```vue
<template>
  <!-- ✅ 正确：添加权限控制 -->
  <el-button v-hasPermi="['erp:product:add']" @click="handleAdd">
    新增
  </el-button>
  
  <el-button v-hasPermi="['erp:product:edit']" @click="handleEdit">
    编辑
  </el-button>
  
  <el-button v-hasPermi="['erp:product:remove']" @click="handleDelete">
    删除
  </el-button>
  
  <!-- ❌ 错误：没有权限控制 -->
  <el-button @click="handleAdd">新增</el-button>
</template>
```

**权限标识规范：**
```
模块:功能:操作
erp:product:list      # 查询
erp:product:add       # 新增
erp:product:edit      # 修改
erp:product:remove    # 删除
erp:product:export    # 导出
erp:product:import    # 导入
```

---

## 6. ERP 业务模块

### 6.1 模块概览

RuoYi-Vue3 包含完整的 ERP 业务模块，位于 `src/views/erp/` 目录：

| 模块 | 路径 | 说明 |
|------|------|------|
| **商品管理** | `/views/erp/product/` | 商品列表、创建、详情 |
| **变体管理** | `/views/erp/variant/` | 商品变体配置 |
| **标签管理** | `/views/erp/tag/` | 多级标签分类 |
| **商品标签** | `/views/erp/ProductTag/` | 商品与标签关联 |
| **媒体管理** | `/views/erp/media/` | 图片/视频管理 |
| **图片管理** | `/views/erp/image/` | 图片上传、裁剪 |
| **任务管理** | `/views/erp/task/` | Shopify 同步任务 |

### 6.2 商品管理模块

**核心组件：** `ProductCreationWizard.vue`

**功能特点：**
- 两步式商品创建向导
- 第一步：选品基础信息（标签、SPU、URL、选项配置）
- 第二步：信息录入（商品详情、媒体、价格成本）
- 动态变体生成
- 拖拽排序
- 实时利润计算

**使用示例：**

```vue
<template>
  <div class="app-container">
    <!-- 商品创建向导 -->
    <ProductCreationWizard
      ref="wizardRef"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup name="ProductCreate">
import { ref } from 'vue'
import ProductCreationWizard from '@/components/erp/ProductCreationWizard.vue'

const wizardRef = ref(null)

function handleSubmit({ action, hasChanged }) {
  if (action === 'close' && hasChanged) {
    // 处理保存逻辑
  }
}
</script>
```

### 6.3 媒体管理模块

**核心功能：**
- 图片/视频上传
- 服务器图片导入
- 拖拽排序
- 图片预览
- 视频播放

**API 调用：**

```javascript
import { uploadMedia, getServerImages } from '@/api/erp/media'

// 上传媒体
uploadMedia(file).then(response => {
  console.log('上传成功', response.data)
})

// 从服务器加载图片
getServerImages({ keyword: 'products' }).then(response => {
  console.log('图片列表', response.data)
})
```

---

## 7. 核心功能

### 7.1 路由系统

**路由配置：** `src/router/index.js`

**动态路由：** 从后端获取，根据用户权限生成

```javascript
// 路由守卫（permission.js）
router.beforeEach((to, from, next) => {
  NProgress.start()
  
  if (getToken()) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      // 获取用户信息和权限路由
      const roles = useUserStore().roles
      if (roles.length === 0) {
        useUserStore().getInfo().then(() => {
          const accessRoutes = usePermissionStore().generateRoutes(roles)
          accessRoutes.forEach(route => router.addRoute(route))
          next({ ...to, replace: true })
        })
      } else {
        next()
      }
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`)
    }
  }
})
```

### 7.2 状态管理（Pinia）

**Store 模块：**

```javascript
// store/modules/user.js
import { defineStore } from 'pinia'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'

const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    name: '',
    avatar: '',
    roles: [],
    permissions: []
  }),
  
  actions: {
    // 登录
    login(userInfo) {
      const { username, password, code, uuid } = userInfo
      return new Promise((resolve, reject) => {
        login({ username, password, code, uuid }).then(res => {
          setToken(res.token)
          this.token = res.token
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },
    
    // 获取用户信息
    getInfo() {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user
          const avatar = user.avatar || require('@/assets/images/profile.jpg')
          
          if (res.roles && res.roles.length > 0) {
            this.roles = res.roles
            this.permissions = res.permissions
          }
          
          this.name = user.userName
          this.avatar = avatar
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },
    
    // 退出登录
    logOut() {
      return new Promise((resolve, reject) => {
        logout(this.token).then(() => {
          this.token = ''
          this.roles = []
          this.permissions = []
          removeToken()
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
})

export default useUserStore
```

**使用 Store：**

```vue
<script setup>
import { storeToRefs } from 'pinia'
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const { name, avatar, roles } = storeToRefs(userStore)

// 调用 action
function handleLogin() {
  userStore.login({
    username: 'admin',
    password: 'admin123',
    code: '1234',
    uuid: 'xxx'
  })
}
</script>
```

### 7.3 字典系统

**使用字典：**

```vue
<script setup>
import { useDict } from '@/utils/dict'

// 使用字典
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

### 7.4 请求封装

**Axios 封装：** `src/utils/request.js`

**特性：**
- 自动携带 Token
- 统一错误处理
- 防重复提交
- 请求/响应拦截
- 下载文件支持

**使用示例：**

```javascript
import request from '@/utils/request'

// GET 请求
export function getProductList(params) {
  return request({
    url: '/erp/product/list',
    method: 'get',
    params
  })
}

// POST 请求
export function addProduct(data) {
  return request({
    url: '/erp/product',
    method: 'post',
    data
  })
}

// 下载文件
import { download } from '@/utils/request'

download('/erp/product/export', queryParams, 'product.xlsx')
```

---

## 8. API 调用规范

### 8.1 API 文件组织

```
src/api/erp/
├── product.js              # 商品管理 API
├── variant.js              # 变体管理 API
├── tag.js                  # 标签管理 API
├── ProductTag.js           # 商品标签关联 API
├── media.js                # 媒体管理 API
├── image.js                # 图片管理 API
├── exchange-rate.js        # 汇率管理 API
├── task.js                 # 任务管理 API
└── productWizard.js        # 商品向导 API
```

### 8.2 API 定义规范

```javascript
// api/erp/product.js
import request from '@/utils/request'

/**
 * 查询商品列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getProductList(params) {
  return request({
    url: '/erp/product/list',
    method: 'get',
    params
  })
}

/**
 * 查询商品详情
 * @param {Number} productId - 商品ID
 * @returns {Promise}
 */
export function getProduct(productId) {
  return request({
    url: `/erp/product/${productId}`,
    method: 'get'
  })
}

/**
 * 新增商品
 * @param {Object} data - 商品数据
 * @returns {Promise}
 */
export function addProduct(data) {
  return request({
    url: '/erp/product',
    method: 'post',
    data
  })
}

/**
 * 修改商品
 * @param {Object} data - 商品数据
 * @returns {Promise}
 */
export function updateProduct(data) {
  return request({
    url: '/erp/product',
    method: 'put',
    data
  })
}

/**
 * 删除商品
 * @param {Array} productIds - 商品ID数组
 * @returns {Promise}
 */
export function deleteProduct(productIds) {
  return request({
    url: `/erp/product/${productIds}`,
    method: 'delete'
  })
}

/**
 * 导出商品
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function exportProduct(params) {
  return request({
    url: '/erp/product/export',
    method: 'post',
    params,
    responseType: 'blob'
  })
}
```

### 8.3 API 调用最佳实践

```vue
<script setup name="ProductList">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getProductList, 
  deleteProduct, 
  exportProduct 
} from '@/api/erp/product'

// 响应式数据
const loading = ref(false)
const productList = ref([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  productName: undefined,
  status: undefined
})

// 查询列表
function getList() {
  loading.value = true
  getProductList(queryParams).then(response => {
    productList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

// 删除商品
function handleDelete(row) {
  ElMessageBox.confirm(`确认删除商品"${row.productTitle}"吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteProduct([row.productId]).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  }).catch(() => {})
}

// 导出商品
function handleExport() {
  download('正在导出数据，请稍候...', () => {
    exportProduct(queryParams).then(response => {
      // request.js 的 download 方法会自动处理
    })
  })
}

// 初始化
getList()
</script>
```

---

## 9. 组件开发

### 9.1 公共组件开发

**组件位置：** `src/components/erp/`

**组件模板：**

```vue
<!-- components/erp/CustomComponent.vue -->
<script setup name="CustomComponent">
import { ref, computed, watch } from 'vue'

// Props 定义
const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

// Emits 定义
const emit = defineEmits(['update:modelValue', 'change'])

// 内部状态
const inputValue = ref(props.modelValue)

// 计算属性
const displayValue = computed(() => {
  return inputValue.value || '-'
})

// 监听器
watch(() => props.modelValue, (newVal) => {
  inputValue.value = newVal
})

// 方法
function handleChange(value) {
  emit('update:modelValue', value)
  emit('change', value)
}

// 暴露方法给父组件
defineExpose({
  focus: () => {
    // 聚焦逻辑
  }
})
</script>

<template>
  <div class="custom-component">
    <el-input
      :model-value="inputValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @update:model-value="handleChange"
    />
  </div>
</template>

<style scoped lang="scss">
.custom-component {
  width: 100%;
}
</style>
```

### 9.2 组件使用

```vue
<script setup name="ParentComponent">
import { ref } from 'vue'
import CustomComponent from '@/components/erp/CustomComponent.vue'

const value = ref('')
const componentRef = ref(null)

function handleChange(newValue) {
  console.log('值改变了', newValue)
}

function handleFocus() {
  componentRef.value?.focus()
}
</script>

<template>
  <CustomComponent
    ref="componentRef"
    v-model="value"
    placeholder="请输入内容"
    @change="handleChange"
  />
</template>
```

### 9.3 ERP 业务组件

#### ProductCreationWizard（商品创建向导）

**位置：** `src/components/erp/ProductCreationWizard.vue`

**功能：**
- 两步式商品创建流程
- 动态选项配置
- 变体自动生成
- 媒体文件管理
- 价格成本计算

**使用示例：**

```vue
<template>
  <ProductCreationWizard
    ref="wizardRef"
    :initial-data="productData"
    @submit="handleSubmit"
  />
</template>

<script setup>
import { ref } from 'vue'
import ProductCreationWizard from '@/components/erp/ProductCreationWizard.vue'

const wizardRef = ref(null)
const productData = ref(null)

function handleSubmit({ action, hasChanged }) {
  if (action === 'close') {
    // 保存并关闭
  } else if (action === 'next') {
    // 下一步
  }
}
</script>
```

---

## 10. 常见问题

### 10.1 开发环境问题

#### Q1: 依赖安装失败？

**现象：**
```
npm ERR! ERESOLVE unable to resolve dependency tree
```

**解决方案：**
```bash
# 清除缓存
npm cache clean --force

# 删除 node_modules 和锁文件
rm -rf node_modules package-lock.json

# 重新安装
npm install

# 或使用 yarn（推荐）
yarn install
```

#### Q2: 端口被占用？

**现象：**
```
Port 80 is already in use
```

**解决方案：**
```javascript
// vite.config.js
server: {
  port: 8081,  // 修改端口
  host: true,
  open: true
}
```

#### Q3: 后端接口跨域？

**现象：**
```
Access to XMLHttpRequest at 'http://localhost:8080' from origin 'http://localhost:80' has been blocked by CORS policy
```

**解决方案：**

检查 Vite 代理配置：

```javascript
// vite.config.js
server: {
  proxy: {
    '/dev-api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (p) => p.replace(/^\/dev-api/, '')
    }
  }
}
```

确保前端请求使用代理：

```javascript
// .env.development
VITE_APP_BASE_API = '/dev-api'
```

### 10.2 运行时问题

#### Q4: 页面空白？

**可能原因：**
1. 路由配置错误
2. 组件导入失败
3. JavaScript 错误

**排查步骤：**
1. 打开浏览器控制台查看错误
2. 检查 Network 面板是否有失败的请求
3. 检查路由配置是否正确

#### Q5: 登录后跳转失败？

**原因：** 权限路由未正确生成

**解决方案：**
```javascript
// permission.js
router.beforeEach((to, from, next) => {
  if (getToken()) {
    const roles = useUserStore().roles
    if (roles.length === 0) {
      // 获取用户信息并生成路由
      useUserStore().getInfo().then(() => {
        const accessRoutes = usePermissionStore().generateRoutes(roles)
        accessRoutes.forEach(route => router.addRoute(route))
        next({ ...to, replace: true })
      })
    } else {
      next()
    }
  }
})
```

#### Q6: 按钮权限不生效？

**原因：** 权限标识错误或指令未注册

**解决方案：**

1. 检查权限标识是否正确：
```vue
<!-- 确认后端返回的权限包含此标识 -->
<el-button v-hasPermi="['erp:product:add']">新增</el-button>
```

2. 检查指令是否注册：
```javascript
// directive/index.js
import hasPermi from './permission/hasPermi'

export default function directive(app) {
  app.directive('hasPermi', hasPermi)
}
```

### 10.3 构建问题

#### Q7: 打包后白屏？

**原因：** 路由 History 模式未配置 Nginx

**解决方案：**

Nginx 配置：
```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```

或使用 Hash 模式：
```javascript
// router/index.js
const router = createRouter({
  history: createWebHashHistory(),  // 使用 Hash 模式
  routes
})
```

#### Q8: 打包体积过大？

**优化方案：**

1. 路由懒加载：
```javascript
const ProductList = () => import('@/views/erp/product/index.vue')
```

2. 启用 Gzip 压缩：
```javascript
// vite.config.js
import compression from 'vite-plugin-compression'

plugins: [
  compression({
    verbose: true,
    disable: false,
    threshold: 10240,
    algorithm: 'gzip',
    ext: '.gz'
  })
]
```

3. 分包策略：
```javascript
// vite.config.js
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        'element-plus': ['element-plus'],
        'echarts': ['echarts'],
        'vendor': ['vue', 'vue-router', 'pinia']
      }
    }
  }
}
```

---

## 11. AI Coding 指南

### 11.1 给通义灵码/Trae 的提示

在为本项目生成前端代码时，请遵循以下规则：

#### 组件生成

```
请为 RuoYi-Vue3 生成 Vue 3 组件，要求：

1. 文件位置：src/views/erp/{module}/ 或 src/components/erp/
2. 必须使用 <script setup> 语法
3. 必须定义组件 name 属性
4. 使用 Element Plus 组件
5. 响应式数据使用 ref/reactive
6. 方法使用普通函数定义（不用箭头函数）
7. 添加适当的注释
8. 样式使用 scoped SCSS
9. 按钮添加 v-hasPermi 权限控制
10. 表格使用 el-table + Pagination

参考现有代码风格：
- src/views/erp/product/index.vue
- src/components/erp/ProductCreationWizard.vue
```

#### API 生成

```
请为 RuoYi-Vue3 生成 API 接口定义，要求：

1. 文件位置：src/api/erp/{module}.js
2. 导入 request 工具：import request from '@/utils/request'
3. 使用 JSDoc 注释
4. GET 请求使用 params，POST/PUT 使用 data
5. 导出命名函数
6. 遵循 RESTful 规范

参考现有代码风格：
- src/api/erp/product.js
- src/api/erp/media.js
```

#### 页面生成

```
请为 RuoYi-Vue3 生成 ERP 业务页面，要求：

1. 文件位置：src/views/erp/{module}/index.vue
2. 包含搜索表单、数据表格、分页组件
3. 使用 RightToolbar 组件
4. 表格列支持显示/隐藏
5. 操作按钮添加权限控制
6. 删除操作二次确认
7. 加载状态使用 v-loading
8. 响应式布局使用 el-row/el-col

参考现有代码风格：
- src/views/erp/product/index.vue
- src/views/erp/tag/index.vue
```

### 11.2 代码模板

#### 列表页面模板

```vue
<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['erp:module:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['erp:module:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['erp:module:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:module:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:module:remove']">删除</el-button>
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
  </div>
</template>

<script setup name="ModuleList">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getList, delModule } from '@/api/erp/module'

const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const dataList = ref([])
const queryRef = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: undefined
})

/** 查询列表 */
function getList() {
  loading.value = true
  getList(queryParams).then(response => {
    dataList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  queryRef.value.resetFields()
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  // TODO: 实现新增逻辑
}

/** 修改按钮操作 */
function handleUpdate(row) {
  // TODO: 实现修改逻辑
}

/** 删除按钮操作 */
function handleDelete(row) {
  const moduleIds = row.id || ids.value
  ElMessageBox.confirm('是否确认删除？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function () {
    return delModule(moduleIds)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

getList()
</script>
```

### 11.3 最佳实践检查清单

生成代码后，AI 应自检：

#### 基础检查
- [ ] 是否在正确的目录生成文件？
- [ ] 是否使用 `<script setup>` 语法？
- [ ] 是否定义了组件 `name` 属性？
- [ ] 是否使用 Element Plus 组件？

#### 权限检查
- [ ] 按钮是否添加 `v-hasPermi` 权限？
- [ ] 权限标识是否符合规范（模块:功能:操作）？

#### 性能检查
- [ ] 路由是否懒加载？
- [ ] 图片是否使用 `el-image` 懒加载？
- [ ] 搜索框是否使用防抖？
- [ ] 大列表是否考虑虚拟滚动？

#### 代码质量检查
- [ ] 变量命名是否符合规范（小驼峰）？
- [ ] 是否有适当的注释？
- [ ] 是否处理了加载状态（v-loading）？
- [ ] 是否处理了错误情况？
- [ ] 是否使用了统一的 API 调用方式？

#### 用户体验检查
- [ ] 删除操作是否有二次确认？
- [ ] 是否有成功/失败提示？
- [ ] 表单是否有验证规则？
- [ ] 是否有空状态提示？

---

## 12. 总结

### 12.1 项目优势

✅ **现代化技术栈**: Vue 3 + Vite + Element Plus  
✅ **完整的 ERP 功能**: 商品、变体、标签、媒体管理  
✅ **权限控制系统**: 动态路由 + 按钮权限  
✅ **良好的代码规范**: 统一的编码风格和最佳实践  
✅ **丰富的组件库**: 可复用的业务组件  
✅ **优秀的性能**: 路由懒加载、Gzip 压缩、代码分割  

### 12.2 技术亮点

- **Vue 3 Composition API**: 更灵活的代码组织
- **Pinia 状态管理**: 类型安全、模块化
- **Vite 构建工具**: 极速的开发体验
- **Element Plus**: 现代化的 UI 组件
- **若依框架**: 成熟的权限、字典、日志体系

### 12.3 快速开始

```bash
# 1. 安装依赖
cd RuoYi-Vue3
yarn install

# 2. 启动开发服务器
yarn dev

# 3. 访问系统
# http://localhost:80
# 账号: admin
# 密码: admin123

# 4. 生产环境打包
yarn build:prod
```

---

**文档版本:** v1.0  
**最后更新:** 2026-04-14  
**维护团队:** LZ-RuoYi Team  
**适用工具:** 通义灵码、Trae、Cursor 等 AI 编程助手

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LZ-RuoYi is a customized ERP system for cross-border e-commerce, built on the RuoYi framework. It integrates with Shopify for product synchronization and includes a Chrome extension for product sourcing from Chinese e-commerce platforms (Taobao, Tmall, 1688).

## Architecture

This is a monorepo containing three main components:

### Backend (Spring Boot Multi-Module Maven Project)
- **ruoyi-admin**: Main entry point, contains application configuration and startup
- **ruoyi-common**: Shared utilities, annotations, constants, exception handling
- **ruoyi-framework**: Core framework config (security, datasource, interceptors)
- **ruoyi-system**: System management (users, roles, menus, departments)
- **ruoyi-quartz**: Scheduled task management
- **ruoyi-generator**: Code generator for CRUD operations
- **vh-erp**: Custom ERP business module (products, variants, tags, media, Shopify sync)

### Frontend (Vue 3 + Vite)
- **RuoYi-Vue3/**: Main frontend application using Element Plus, Pinia, Vue Router

### Chrome Extension
- **Purchase-x/**: Chrome extension for extracting product info from Taobao/Tmall/1688

## Key Commands

### Backend
```bash
# Build entire project (from root)
mvn clean package -DskipTests

# Run backend (from ruoyi-admin)
mvn spring-boot:run

# Or run JAR directly
java -jar ruoyi-admin/target/ruoyi-admin.jar

# Windows startup script
.\ry.bat

# Linux/Mac startup script
./ry.sh
```

### Frontend
```bash
cd RuoYi-Vue3

# Install dependencies
npm install

# Development server (runs on port 80)
npm run dev

# Production build
npm run build:prod

# Staging build
npm run build:stage

# Type check
npm run type-check

# Lint
npm run lint
```

### Chrome Extension
```bash
cd Purchase-x
npm install
npm run dev    # Development
npm run build  # Production build
```

## Environment Requirements

- Java 17+
- Maven 3.6+
- MySQL 5.7+ / 8.0+
- Redis 5.0+
- Node.js 18+

## Database Setup

Execute SQL scripts in order:
1. `sql/20240629_init1.sql` - Base system tables
2. `sql/20240629_init2.sql` - System data
3. `sql/20260324_erp.sql` - ERP module tables
5. `sql/20260427_form_suggestion.sql` - Form suggestion tables

Database name: `lz-ruoyi` (configurable in `application-dev.yml`)

## Backend Configuration

Key configuration files in `ruoyi-admin/src/main/resources/`:
- `application.yml` - Main config, activates `dev` profile
- `application-dev.yml` - Database, Redis, file upload settings
- `application-prod.yml` - Production settings

ERP-specific config in `vh-erp/src/main/resources/`:
- `application-erp-dev.yml` - Shopify, exchange rate API, FPX API settings

## Frontend Configuration

- `RuoYi-Vue3/.env.development` - Dev API base (`/dev-api`)
- `RuoYi-Vue3/.env.production` - Production API base
- `RuoYi-Vue3/vite.config.js` - Vite config with proxy to backend (localhost:8080)

## Backend Code Patterns

### Controller Layer
```java
@RestController
@RequestMapping("/erp/product")
public class ProductController extends BaseController {
    @Autowired
    private IProductService productService;

    @GetMapping("/list")
    public TableDataInfo list(Product product) {
        startPage();
        List<Product> list = productService.selectList(product);
        return getDataTable(list);
    }
}
```

### Service Layer
- Interfaces named `I*Service` (e.g., `IProductService`)
- Implementations in `service/impl/`
- Use `@Transactional` for write operations

### Mapper Layer
- Use MyBatis-Plus `BaseMapper<T>` for basic CRUD
- Complex queries in XML files under `resources/mapper/`
- Prefer `LambdaQueryWrapper` over string-based column names

### Entity Classes
- Domain entities in `model/domain/`
- DTOs in `model/dto/`
- VOs in `model/vo/`
- Use Lombok annotations (`@Data`, `@Builder`, etc.)

## Frontend Code Patterns

### API Definition
```javascript
// api/erp/product.ts
import request from "@/utils/request";

export function getProductList(params) {
  return request({
    url: "/erp/product/list",
    method: "get",
    params,
  });
}
```

### Vue Component Structure
```vue
<script setup name="ProductList">
import { ref, reactive, onMounted } from "vue";
import { getProductList } from "@/api/erp/product";

const productList = ref([]);
const queryParams = reactive({ pageNum: 1, pageSize: 10 });

function getList() {
  getProductList(queryParams).then(res => {
    productList.value = res.rows;
  });
}

onMounted(() => getList());
</script>

<template>
  <!-- Template content -->
</template>

<style scoped lang="scss">
</style>
```

## Key Domain Entities

- **Product**: Main product entity
- **ProductVariant**: Product variants (SKUs)
- **ProductOption/ProductOptionValue**: Variant options
- **TagDict**: Product tags
- **ProductTagRel**: Product-tag relationships
- **Media**: Product images/videos
- **ShopifyTask**: Shopify sync tasks

## External Integrations

- **Shopify GraphQL API**: Product/variant sync (`vh-erp` config)
- **Exchange Rate API**: Currency conversion (`exchange-rate.api-key`)
- **FPX API**: Logistics/shipping integration

## Important Notes

1. **API Proxy**: Frontend dev server proxies `/dev-api/*` to `http://localhost:8080/*`
2. **Default Credentials**: admin / admin123
3. **File Upload Path**: Configured via `ruoyi.profile` in `application-dev.yml` (default: `C:/velarthome`)
4. **Active Profiles**: Backend uses `dev,erp-dev` profiles by default
5. **Port Configuration**:
   - Backend: 8080
   - Frontend dev server: 80
6. 确保每次开发的功能都是完整的，不要留一个todo。
7. 对接shopify的时候，请确保对接的shopify的2026-04版本的接口。

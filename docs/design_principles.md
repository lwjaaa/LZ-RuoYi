# LZ-RuoYi 设计原则与编码规范 (AI Coding 专用)

> **本文档专为 AI 辅助编程设计**（通义灵码、Trae 等），明确系统的设计原则、架构约束、安全规范、性能要求和可扩展性约定。**AI 在生成代码时必须严格遵守这些规则。**

---

## 📋 目录

- [1. 核心设计原则](#1-核心设计原则)
- [2. 架构约束](#2-架构约束)
- [3. 安全规范](#3-安全规范)
- [4. 性能优化约定](#4-性能优化约定)
- [5. 可扩展性设计](#5-可扩展性设计)
- [6. 代码质量规范](#6-代码质量规范)
- [7. AI 生成代码检查清单](#7-ai-生成代码检查清单)

---

## 1. 核心设计原则

### 1.1 SOLID 原则在本项目的实践

#### ✅ 单一职责原则 (SRP)

**规则：** 每个类、方法只负责一项职责。

```java
// ✅ 正确：职责分离
@Service
public class ProductService {
    public Product createProduct(Product product) { ... }
    public void pushToShopify(Long productId) { ... }
}

@Service
public class MediaService {
    public Media uploadMedia(MultipartFile file) { ... }
}

// ❌ 错误：职责混乱
@Service
public class ProductService {
    public Product createProduct(Product product) { ... }
    public Media uploadImage(MultipartFile file) { ... }  // 不属于商品服务
    public String sendEmail(String to, String content) { ... }  // 不属于商品服务
}
```

**AI 生成代码时：**
- Controller 只负责接收请求和返回响应
- Service 只负责业务逻辑
- Mapper 只负责数据访问
- 不要在一个类中混合多种职责

#### ✅ 开闭原则 (OCP)

**规则：** 对扩展开放，对修改关闭。

```java
// ✅ 正确：通过接口扩展
public interface IShopifyClient {
    ProductResponse createProduct(Product product);
}

@Component
public class ShopifyGraphQlClient implements IShopifyClient {
    @Override
    public ProductResponse createProduct(Product product) {
        // GraphQL 实现
    }
}

// 未来可以轻松替换为 REST 客户端
@Component
public class ShopifyRestClient implements IShopifyClient {
    @Override
    public ProductResponse createProduct(Product product) {
        // REST 实现
    }
}
```

#### ✅ 依赖倒置原则 (DIP)

**规则：** 高层模块不依赖低层模块，两者都依赖抽象。

```java
// ✅ 正确：依赖接口
@Service
public class ProductWizardServiceImpl implements IProductWizardService {
    @Resource
    private IProductService productService;      // 依赖接口
    @Resource
    private IMediaService mediaService;          // 依赖接口
    @Resource
    private IShopifyTaskService taskService;     // 依赖接口
}

// ❌ 错误：依赖具体实现
@Service
public class ProductWizardServiceImpl {
    @Autowired
    private ProductServiceImpl productService;   // 依赖具体类
}
```

### 1.2 DRY 原则 (Don't Repeat Yourself)

**规则：** 避免代码重复，提取公共逻辑。

```java
// ✅ 正确：提取公共方法
@Service
public class ProductServiceImpl {
    
    private ProductVo convertToVo(Product product) {
        ProductVo vo = new ProductVo();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
    
    public List<ProductVo> selectProductList(Product product) {
        List<Product> list = productMapper.selectList(wrapper);
        return list.stream().map(this::convertToVo).collect(Collectors.toList());
    }
}
```

### 1.3 KISS 原则 (Keep It Simple, Stupid)

**规则：** 保持简单，避免过度设计。

```java
// ✅ 正确：简洁明了
public BigDecimal calculateCost(BigDecimal purchasePrice, BigDecimal freight) {
    return purchasePrice.add(freight);
}
```

### 1.4 YAGNI 原则 (You Ain't Gonna Need It)

**规则：** 不要实现当前不需要的功能。

```java
// ✅ 正确：只实现当前需求
@Service
public class ExchangeRateService {
    public BigDecimal getRate(String from, String to) {
        // 查询数据库或缓存
    }
}
```

---

## 2. 架构约束

### 2.1 模块依赖约束

**强制规则：**

| 模块 | 可以依赖 | 禁止依赖 |
|------|---------|---------|
| ruoyi-admin | 所有业务模块 | - |
| vh-erp | ruoyi-common, ruoyi-framework | ruoyi-system, ruoyi-quartz |
| ruoyi-system | ruoyi-common, ruoyi-framework | vh-erp |
| ruoyi-common | 无（最底层） | 任何业务模块 |
| ruoyi-framework | ruoyi-common | 任何业务模块 |

**AI 生成代码时必须遵守：**
- ❌ **禁止** vh-erp 依赖 ruoyi-system
- ❌ **禁止** 循环依赖
- ✅ **必须** 通过接口通信，不直接依赖实现类

### 2.2 分层架构约束

**强制规则：** 严格遵循 Controller → Service → Mapper 调用链。

```java
// ✅ 正确的调用链
@RestController
@RequestMapping("/erp/product")
public class ProductController {
    @Resource
    private IProductService productService;
    
    @GetMapping("/list")
    public TableDataInfo list(ProductQuery query) {
        startPage();
        List<ProductVo> list = productService.selectProductList(query);
        return getDataTable(list);
    }
}

@Service
public class ProductServiceImpl implements IProductService {
    @Resource
    private ProductMapper productMapper;
    
    @Override
    public List<ProductVo> selectProductList(ProductQuery query) {
        LambdaQueryWrapper<Product> wrapper = buildWrapper(query);
        return productMapper.selectVoList(wrapper);
    }
}

// ❌ 错误的调用（跨层调用）
@RestController
public class ProductController {
    @Resource
    private ProductMapper productMapper;  // 禁止！Controller 不能直接调用 Mapper
}
```

**调用规则：**
- ✅ Controller → Service
- ✅ Service → Mapper
- ✅ Service → Service（同模块内）
- ❌ Controller → Mapper
- ❌ Mapper → Service
- ❌ Controller → Controller

### 2.3 前端目录约束

**⚠️ 重要声明：**

```
✅ RuoYi-Vue3/     # 唯一的前端开发目录
❌ ruoyi-ui/       # 已废弃，禁止修改
```

**AI 生成前端代码时：**
- ✅ **必须** 在 `RuoYi-Vue3/src/` 目录下生成
- ✅ **必须** 使用 Vue 3 Composition API (`<script setup>`)
- ✅ **必须** 使用 Element Plus 组件
- ❌ **禁止** 在 `ruoyi-ui/` 目录下生成任何代码
- ❌ **禁止** 使用 Vue 2 Options API

### 2.4 命名规范约束

#### 后端命名规范

```java
// 包名：全小写，多级用点分隔
package com.ruoyi.erp.controller;
package com.ruoyi.erp.service.impl;

// 类名：大驼峰（PascalCase）
public class ProductController { }
public class ProductServiceImpl implements IProductService { }

// 接口名：I + 大驼峰
public interface IProductService { }

// 方法名：小驼峰（camelCase）
public List<ProductVo> selectProductList(ProductQuery query) { }

// 变量名：小驼峰
private Long productId;
private String productName;

// 常量：全大写，下划线分隔
public static final String SHOPIFY_API_VERSION = "2024-01";

// 数据库表名：小写，下划线分隔
erp_product
erp_product_variant

// 字段名：小写，下划线分隔
product_id
product_name
```

#### 前端命名规范

```vue
<!-- 组件文件名：大驼峰.vue -->
ProductList.vue
ProductCreationWizard.vue

<!-- 组件 name 属性：必须定义 -->
<script setup name="ProductList">

<!-- 变量/函数：小驼峰 -->
const productList = ref([])
function getProductList() { }

<!-- CSS 类名：短横线分隔 -->
.product-list-container
.product-item-card
```

### 2.5 事务管理约束

**规则：** Service 层方法需要事务控制时使用 `@Transactional`。

```java
// ✅ 正确：在 Service 层添加事务
@Service
public class ProductWizardServiceImpl implements IProductWizardService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveProductWithWizard(Product product, Integer step) {
        // 1. 保存商品主表
        productMapper.insert(product);
        
        // 2. 保存变体
        variantMapper.batchInsert(product.getVariants());
        
        // 3. 保存媒体
        mediaMapper.batchInsert(product.getMediaList());
        
        return product.getProductId();
    }
}

// ❌ 错误：在 Controller 层添加事务
@RestController
public class ProductController {
    @Transactional  // 禁止！
    @PostMapping("/save")
    public AjaxResult save(@RequestBody Product product) { ... }
}
```

**事务注意事项：**
- ✅ **必须** 指定 `rollbackFor = Exception.class`
- ✅ 事务方法必须是 `public`
- ❌ 不要在事务中执行耗时操作（如 HTTP 请求）

---

## 3. 安全规范

### 3.1 权限控制

**强制规则：** 所有接口必须添加权限注解。

```java
// ✅ 正确：添加权限控制
@RestController
@RequestMapping("/erp/product")
public class ProductController {
    
    @PreAuthorize("@ss.hasPermi('erp:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductQuery query) { ... }
    
    @PreAuthorize("@ss.hasPermi('erp:product:add')")
    @Log(title = "商品管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Product product) { ... }
    
    @PreAuthorize("@ss.hasPermi('erp:product:remove')")
    @Log(title = "商品管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) { ... }
}
```

**前端按钮权限：**
```vue
<template>
  <!-- ✅ 正确：添加按钮权限 -->
  <el-button v-hasPermi="['erp:product:add']" @click="handleAdd">
    新增
  </el-button>
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
```

### 3.2 SQL 注入防护

**强制规则：** 使用参数化查询，禁止拼接 SQL。

```java
// ✅ 正确：使用 MyBatis-Plus LambdaQueryWrapper
LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Product::getStatus, status)
       .like(Product::getProductName, keyword);
List<Product> list = productMapper.selectList(wrapper);

// ❌ 错误：SQL 拼接（严重安全漏洞！）
String sql = "SELECT * FROM erp_product WHERE product_name = '" + keyword + "'";
```

### 3.3 XSS 防护

**规则：** 富文本内容需要特殊处理。

```java
// ✅ 正确：富文本内容使用 Jsoup 清理
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public String cleanHtml(String html) {
    return Jsoup.clean(html, Safelist.relaxed());
}

product.setBodyHtml(cleanHtml(product.getBodyHtml()));
```

### 3.4 敏感信息保护

**强制规则：** 敏感配置从环境变量读取，禁止硬编码。

```yaml
# ✅ 正确：从环境变量读取
shopify:
  access-token: ${SHOPIFY_ACCESS_TOKEN}
  shop-domain: ${SHOPIFY_SHOP_DOMAIN}

exchange:
  api:
    access-key: ${FIXER_API_KEY}
```

```java
// ❌ 错误：硬编码敏感信息（严重安全问题！）
@Configuration
public class ShopifyConfig {
    private static final String ACCESS_TOKEN = "shpat_xxxxxxxxxxxx";  // 禁止！
}
```

### 3.5 密码加密

**规则：** 用户密码使用 BCrypt 加密存储。

```java
// ✅ 正确：BCrypt 加密
import org.springframework.security.crypto.bcrypt.BCrypt;

String rawPassword = "admin123";
String encryptedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

// 验证密码
boolean matches = BCrypt.checkpw(rawPassword, encryptedPassword);
```

### 3.6 文件上传安全

**规则：** 限制文件类型、大小，防止恶意文件上传。

```java
// ✅ 正确：文件上传校验
@PostMapping("/upload")
public AjaxResult upload(@RequestParam("file") MultipartFile file) {
    // 1. 检查文件是否为空
    if (file.isEmpty()) {
        return error("文件不能为空");
    }
    
    // 2. 检查文件大小（限制 10MB）
    if (file.getSize() > 10 * 1024 * 1024) {
        return error("文件大小不能超过 10MB");
    }
    
    // 3. 检查文件类型
    String contentType = file.getContentType();
    if (!isAllowedType(contentType)) {
        return error("不支持的文件类型");
    }
    
    // 4. 生成随机文件名
    String extension = getFileExtension(file.getOriginalFilename());
    String fileName = UUID.randomUUID().toString() + "." + extension;
    
    // 5. 保存到安全目录
    String filePath = "/nas/erp-media/" + fileName;
    file.transferTo(new File(filePath));
    
    return success(filePath);
}
```

---

## 4. 性能优化约定

### 4.1 数据库查询优化

#### 规则 1：避免 N+1 查询问题

```java
// ✅ 正确：批量查询
@Service
public class ProductServiceImpl {
    
    public List<ProductVo> selectProductList(ProductQuery query) {
        // 1. 查询商品列表
        List<Product> products = productMapper.selectList(wrapper);
        
        // 2. 批量查询变体（避免 N+1）
        List<Long> productIds = products.stream()
            .map(Product::getProductId)
            .collect(Collectors.toList());
        
        Map<Long, List<ProductVariant>> variantMap = variantMapper
            .selectByProductIds(productIds)
            .stream()
            .collect(Collectors.groupingBy(ProductVariant::getProductId));
        
        // 3. 组装数据
        return products.stream().map(product -> {
            ProductVo vo = convertToVo(product);
            vo.setVariants(variantMap.getOrDefault(product.getProductId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());
    }
}

// ❌ 错误：N+1 查询
public List<ProductVo> selectProductList(ProductQuery query) {
    List<Product> products = productMapper.selectList(wrapper);
    return products.stream().map(product -> {
        ProductVo vo = convertToVo(product);
        // 每次循环都查询数据库！
        List<ProductVariant> variants = variantMapper.selectByProductId(product.getProductId());
        vo.setVariants(variants);
        return vo;
    }).collect(Collectors.toList());
}
```

#### 规则 2：分页查询优化

```java
// ✅ 正确：使用 PageHelper 分页
@GetMapping("/list")
public TableDataInfo list(ProductQuery query) {
    startPage();  // PageHelper 自动拦截
    List<ProductVo> list = productService.selectProductList(query);
    return getDataTable(list);
}
```

### 4.2 缓存使用规范

**规则：** 频繁读取、少量变化的数据使用 Redis 缓存。

```java
// ✅ 正确：使用 Spring Cache 注解
@Service
public class ExchangeRateServiceImpl implements IExchangeRateService {
    
    @Cacheable(value = "exchange_rate", key = "#currencyPair", unless = "#result == null")
    public BigDecimal getExchangeRate(String currencyPair) {
        // 只有缓存未命中时才查询数据库
        return exchangeRateMapper.selectLatestRate(currencyPair);
    }
    
    @CacheEvict(value = "exchange_rate", key = "#currencyPair")
    public void updateExchangeRate(String currencyPair, BigDecimal rate) {
        exchangeRateMapper.updateRate(currencyPair, rate);
    }
}
```

**缓存使用场景：**
- ✅ 汇率数据（24 小时更新一次）
- ✅ 字典数据（很少变化）
- ✅ 用户权限信息（会话期间不变）
- ❌ 实时库存（频繁变化）
- ❌ 订单状态（实时性要求高）

### 4.3 异步处理规范

**规则：** 耗时操作使用异步任务，避免阻塞主线程。

```java
// ✅ 正确：异步批量推送
@Service
public class ProductServiceImpl {
    
    @Async("taskExecutor")
    @Transactional
    public Long pushBatchAsync(List<Long> productIds) {
        Long taskId = createTask();
        try {
            for (Long productId : productIds) {
                pushToShopify(productId);
                updateProgress(taskId);
            }
            markTaskSuccess(taskId);
        } catch (Exception e) {
            markTaskFailed(taskId, e.getMessage());
            log.error("批量推送失败", e);
        }
        return taskId;
    }
}
```

**异步使用场景：**
- ✅ 批量推送到 Shopify
- ✅ 发送邮件通知
- ✅ 生成报表
- ✅ 数据同步
- ❌ 实时查询（需要同步返回结果）

### 4.4 前端性能优化

#### 规则 1：路由懒加载

```javascript
// ✅ 正确：路由懒加载
const routes = [
  {
    path: '/erp/product',
    component: () => import('@/views/erp/product/index.vue'),
    name: 'Product'
  }
]
```

#### 规则 2：图片懒加载

```vue
<template>
  <!-- ✅ 正确：使用 el-image 懒加载 -->
  <el-image :src="imageUrl" lazy fit="cover" />
</template>
```

#### 规则 3：防抖和节流

```javascript
// ✅ 正确：搜索框使用防抖
import { debounce } from 'lodash-es'

const handleSearch = debounce((keyword) => {
  queryParams.productName = keyword
  queryParams.pageNum = 1
  getList()
}, 500)
```

### 4.5 日志规范

**规则：** 合理使用日志级别，避免性能浪费。

```java
@Slf4j
@Service
public class ProductServiceImpl {
    
    public List<ProductVo> selectProductList(ProductQuery query) {
        // ✅ DEBUG：调试信息
        log.debug("查询参数: {}", query);
        
        List<ProductVo> list = productMapper.selectVoList(wrapper);
        
        // ✅ INFO：关键业务操作
        log.info("查询商品列表，共 {} 条", list.size());
        
        return list;
    }
    
    public Long saveProduct(Product product) {
        try {
            productMapper.insert(product);
            log.info("商品创建成功，productId: {}", product.getProductId());
            return product.getProductId();
        } catch (Exception e) {
            // ✅ ERROR：异常信息
            log.error("商品创建失败, product: {}", product, e);
            throw new ServiceException("商品创建失败");
        }
    }
}
```

**日志级别使用指南：**
- **ERROR**: 系统错误、异常（必须记录堆栈）
- **WARN**: 警告信息、潜在问题
- **INFO**: 关键业务操作、状态变更
- **DEBUG**: 调试信息（生产环境关闭）
- **TRACE**: 详细追踪（极少使用）

---

## 5. 可扩展性设计

### 5.1 接口设计规范

**规则：** 面向接口编程，便于扩展和测试。

```java
// ✅ 正确：定义接口
public interface IShopifyClient {
    ShopifyResponse createProduct(Product product);
    ShopifyResponse updateProduct(Long shopifyId, Product product);
    ShopifyResponse deleteProduct(Long shopifyId);
}

// GraphQL 实现
@Component
public class ShopifyGraphQlClient implements IShopifyClient {
    @Override
    public ShopifyResponse createProduct(Product product) {
        // GraphQL 实现
    }
}

// 使用时依赖接口
@Service
public class ProductServiceImpl {
    @Resource
    private IShopifyClient shopifyClient;  // 依赖接口，不关心实现
}
```

### 5.2 策略模式应用

**场景：** 不同商品类型有不同的价格计算策略。

```java
// 策略接口
public interface PriceCalculationStrategy {
    BigDecimal calculate(Product product);
    boolean supports(ProductType type);
}

// 普通商品策略
@Component
public class NormalPriceStrategy implements PriceCalculationStrategy {
    @Override
    public BigDecimal calculate(Product product) {
        return product.getCostPrice().multiply(new BigDecimal("1.3"));
    }
    
    @Override
    public boolean supports(ProductType type) {
        return type == ProductType.NORMAL;
    }
}

// 策略工厂
@Component
public class PriceStrategyFactory {
    @Resource
    private List<PriceCalculationStrategy> strategies;
    
    public PriceCalculationStrategy getStrategy(ProductType type) {
        return strategies.stream()
            .filter(s -> s.supports(type))
            .findFirst()
            .orElseThrow(() -> new ServiceException("不支持的商品类型"));
    }
}
```

### 5.3 事件驱动扩展

**场景：** 商品创建后触发多个后续操作。

```java
// 定义事件
public class ProductCreatedEvent extends ApplicationEvent {
    private final Product product;
    
    public ProductCreatedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }
    
    public Product getProduct() {
        return product;
    }
}

// 发布事件
@Service
public class ProductServiceImpl {
    @Resource
    private ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public Long createProduct(Product product) {
        productMapper.insert(product);
        eventPublisher.publishEvent(new ProductCreatedEvent(this, product));
        return product.getProductId();
    }
}

// 监听事件 - 同步到 Shopify
@Component
public class ShopifySyncListener {
    @EventListener
    @Async
    public void handleProductCreated(ProductCreatedEvent event) {
        // 同步到 Shopify
    }
}

// 监听事件 - 发送通知
@Component
public class NotificationListener {
    @EventListener
    @Async
    public void handleProductCreated(ProductCreatedEvent event) {
        // 发送邮件通知
    }
}
```

### 5.4 配置外部化

**规则：** 所有可配置项都放到配置文件，禁止硬编码。

```yaml
# application.yml
shopify:
  api-version: "2024-01"
  rate-limit:
    max-retries: 3
    retry-delay: 1000
    max-delay: 10000

product:
  wizard:
    step-timeout: 300
    max-options: 3
    max-values-per-option: 10
  
  price:
    default-profit-rate: 0.3
    min-profit-rate: 0.1
    max-profit-rate: 0.5
```

```java
// 读取配置
@Component
@ConfigurationProperties(prefix = "shopify.rate-limit")
@Data
public class ShopifyRateLimitProperties {
    private int maxRetries = 3;
    private long retryDelay = 1000;
    private long maxDelay = 10000;
}
```

---

## 6. 代码质量规范

### 6.1 注释规范

**规则：** 公共方法必须有 Javadoc，复杂逻辑必须有注释。

```java
/**
 * 商品服务实现类
 *
 * @author lwj
 * @date 2026-03-26
 */
@Service
public class ProductServiceImpl implements IProductService {
    
    /**
     * 查询商品列表
     *
     * @param query 查询条件
     * @return 商品视图对象列表
     */
    @Override
    public List<ProductVo> selectProductList(ProductQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<Product> wrapper = buildWrapper(query);
        
        // 执行查询
        List<Product> products = productMapper.selectList(wrapper);
        
        // 转换为 VO
        return products.stream()
            .map(this::convertToVo)
            .collect(Collectors.toList());
    }
}
```

### 6.2 异常处理规范

**规则：** 业务异常抛出 ServiceException，系统异常记录日志后抛出。

```java
@Service
public class ProductServiceImpl {
    
    public Long createProduct(Product product) {
        // 参数校验
        if (product == null) {
            throw new ServiceException("商品信息不能为空");
        }
        
        if (StringUtils.isEmpty(product.getSpu())) {
            throw new ServiceException("SPU 不能为空");
        }
        
        try {
            productMapper.insert(product);
            return product.getProductId();
        } catch (DuplicateKeyException e) {
            log.warn("商品 SPU 已存在: {}", product.getSpu());
            throw new ServiceException("商品 SPU 已存在");
        } catch (Exception e) {
            log.error("商品创建失败, product: {}", product, e);
            throw new ServiceException("商品创建失败，请联系管理员");
        }
    }
}
```

### 6.3 返回值规范

**规则：** 统一使用若依的返回格式。

```java
// ✅ 正确：使用 AjaxResult
@PostMapping
public AjaxResult add(@RequestBody Product product) {
    Long id = productService.createProduct(product);
    return AjaxResult.success(id);
}

@GetMapping("/list")
public TableDataInfo list(ProductQuery query) {
    startPage();
    List<ProductVo> list = productService.selectProductList(query);
    return getDataTable(list);
}
```

### 6.4 魔法值规范

**规则：** 禁止使用魔法值，必须定义为常量。

```java
// ✅ 正确：使用常量
public class ProductConstants {
    /** 商品状态：草稿 */
    public static final String STATUS_DRAFT = "0";
    
    /** 商品状态：已发布 */
    public static final String STATUS_PUBLISHED = "1";
    
    /** 默认利润率 */
    public static final BigDecimal DEFAULT_PROFIT_RATE = new BigDecimal("0.3");
}

@Service
public class ProductServiceImpl {
    public void checkStatus(Product product) {
        if (ProductConstants.STATUS_PUBLISHED.equals(product.getStatus())) {
            // 业务逻辑
        }
    }
}

// ❌ 错误：魔法值
if ("1".equals(product.getStatus())) {  // 这是什么意思？
    // ...
}
```

### 6.5 空指针防护

**规则：** 对所有可能为 null 的对象进行检查。

```java
// ✅ 正确：空指针防护
public ProductVo convertToVo(Product product) {
    if (product == null) {
        return null;
    }
    
    ProductVo vo = new ProductVo();
    BeanUtils.copyProperties(product, vo);
    
    // 安全处理集合
    if (product.getVariants() != null) {
        vo.setVariantCount(product.getVariants().size());
    } else {
        vo.setVariantCount(0);
    }
    
    return vo;
}
```

---

## 7. AI 生成代码检查清单

### 7.1 后端代码检查

在生成后端代码后，AI 必须自检以下项目：

#### ✅ 基础检查
- [ ] 是否在正确的模块中生成（vh-erp）？
- [ ] 是否遵循分层架构（Controller → Service → Mapper）？
- [ ] 是否使用 `@Resource` 而非 `@Autowired`？
- [ ] 是否添加了必要的注解（`@RestController`, `@Service`, `@Mapper`）？

#### ✅ 安全检查
- [ ] 是否添加了 `@PreAuthorize` 权限注解？
- [ ] 是否添加了 `@Log` 操作日志注解？
- [ ] 是否使用参数化查询（无 SQL 拼接）？
- [ ] 是否避免了硬编码敏感信息？

#### ✅ 性能检查
- [ ] 是否避免了 N+1 查询？
- [ ] 是否使用了分页查询？
- [ ] 是否需要添加缓存注解？
- [ ] 耗时操作是否异步处理？

#### ✅ 代码质量检查
- [ ] 是否添加了 Javadoc 注释？
- [ ] 是否避免了魔法值？
- [ ] 是否有空指针防护？
- [ ] 异常处理是否正确？

### 7.2 前端代码检查

在生成前端代码后，AI 必须自检以下项目：

#### ✅ 基础检查
- [ ] 是否在 `RuoYi-Vue3/src/` 目录下生成？
- [ ] 是否使用 `<script setup>` 语法？
- [ ] 是否定义了组件 `name` 属性？
- [ ] 是否使用 Element Plus 组件？

#### ✅ 安全检查
- [ ] 是否添加了 `v-hasPermi` 按钮权限？
- [ ] 是否使用封装的 `request` 工具？
- [ ] 富文本是否使用 DOMPurify 清理？

#### ✅ 性能检查
- [ ] 路由是否懒加载？
- [ ] 图片是否使用懒加载？
- [ ] 搜索框是否使用防抖？
- [ ] 大列表是否考虑虚拟滚动？

#### ✅ 代码质量检查
- [ ] 变量命名是否符合规范？
- [ ] 是否有适当的注释？
- [ ] 是否处理了加载状态？
- [ ] 是否处理了错误情况？

### 7.3 通用检查

- [ ] 代码是否符合 SOLID 原则？
- [ ] 是否遵循 DRY 原则（无重复代码）？
- [ ] 是否遵循 KISS 原则（保持简单）？
- [ ] 是否遵循 YAGNI 原则（不实现不需要的功能）？
- [ ] 是否有单元测试覆盖（如需要）？
- [ ] 是否更新了相关文档？

---

## 8. 常见错误示例

### 8.1 后端常见错误

```java
// ❌ 错误 1：Controller 直接调用 Mapper
@RestController
public class ProductController {
    @Resource
    private ProductMapper productMapper;  // 错误！
}

// ✅ 正确
@RestController
public class ProductController {
    @Resource
    private IProductService productService;
}
```

```java
// ❌ 错误 2：缺少权限控制
@GetMapping("/list")
public TableDataInfo list(ProductQuery query) {
    // 任何人都可以访问！
}

// ✅ 正确
@PreAuthorize("@ss.hasPermi('erp:product:list')")
@GetMapping("/list")
public TableDataInfo list(ProductQuery query) {
    // 有权限才能访问
}
```

```java
// ❌ 错误 3：SQL 拼接
String sql = "SELECT * FROM product WHERE name = '" + keyword + "'";

// ✅ 正确
LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
wrapper.like(Product::getProductName, keyword);
```

### 8.2 前端常见错误

```vue
<!-- ❌ 错误 1：使用 Vue 2 Options API -->
<script>
export default {
  data() {
    return {
      productList: []
    }
  }
}
</script>

<!-- ✅ 正确：使用 Vue 3 Composition API -->
<script setup name="ProductList">
import { ref } from 'vue'
const productList = ref([])
</script>
```

```vue
<!-- ❌ 错误 2：没有按钮权限 -->
<el-button @click="handleAdd">新增</el-button>

<!-- ✅ 正确：添加按钮权限 -->
<el-button v-hasPermi="['erp:product:add']" @click="handleAdd">新增</el-button>
```

```vue
<!-- ❌ 错误 3：在 ruoyi-ui 目录下生成代码 -->
<!-- 绝对禁止！ -->

<!-- ✅ 正确：在 RuoYi-Vue3 目录下生成 -->
```

---

## 9. 总结

### 9.1 核心原则速记

```
📌 架构约束
- 严格分层：Controller → Service → Mapper
- 模块隔离：vh-erp 独立，不依赖其他业务模块
- 前端目录：只在 RuoYi-Vue3 开发

🔒 安全第一
- 所有接口加权限注解
- 参数化查询防 SQL 注入
- 敏感配置环境变量
- 文件上传严格校验

⚡ 性能优化
- 避免 N+1 查询
- 合理使用缓存
- 耗时操作异步化
- 前端懒加载

🎯 代码质量
- 面向接口编程
- 避免魔法值
- 完整注释
- 统一异常处理
```

### 9.2 AI 使用建议

**给通义灵码/Trae 的提示：**

```
在为本项目生成代码时，请：

1. 首先确认是前端还是后端代码
2. 前端必须在 RuoYi-Vue3 目录，使用 Vue 3 + Element Plus
3. 后端必须在 vh-erp 模块，遵循分层架构
4. 所有接口必须添加权限控制
5. 使用 MyBatis-Plus LambdaQueryWrapper
6. 避免 N+1 查询，使用批量查询
7. 敏感配置从环境变量读取
8. 添加完整的 Javadoc 注释
9. 统一使用 AjaxResult 返回格式
10. 参考本文档中的最佳实践示例
```

---

**文档版本：** v1.0  
**最后更新：** 2026-04-14  
**维护团队：** LZ-RuoYi Team  
**适用工具：** 通义灵码、Trae、Cursor 等 AI 编程助手

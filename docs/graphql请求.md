在 Java (特别是若依框架基于 Spring Boot) 环境下，优雅地对接 GraphQL 核心在于：**类型安全、配置分离、错误处理标准化**。

不要手动拼接 JSON 字符串！推荐使用 **Spring for GraphQL** (Spring Boot 2.7+/3.x 原生支持) 或 **Apollo Android/Java Client** (如果是复杂场景)。鉴于若依通常使用较新的 Spring Boot 版本，**Spring for GraphQL** 是最佳选择，因为它与 `RestTemplate`/`WebClient` 风格一致，且支持自动映射 DTO。

以下是基于 **Spring for GraphQL** 的优雅对接方案：

### 1. 引入依赖 (pom.xml)

在您的若依模块（如 `ruoyi-shopify`）的 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- Spring Boot GraphQL Client -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-graphql</artifactId>
    </dependency>
    
    <!-- WebClient (响应式HTTP客户端，Spring GraphQL底层依赖) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Lombok (简化DTO代码) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 2. 配置 Shopify GraphQL 客户端 (application.yml)

将 endpoint 和 Token 配置化，避免硬编码。

```yaml
shopify:
  api:
    # 格式: https://{shop}.myshopify.com/admin/api/{version}/graphql.json
    base-url: https://your-shop-name.myshopify.com/admin/api/2024-10/graphql.json 
    access-token: ${SHOPIFY_ACCESS_TOKEN} # 建议从环境变量读取
    timeout: 5000 # 超时时间 ms
```

### 3. 定义 DTO (Data Transfer Objects)

**这是“优雅”的关键**。不要使用 `Map<String, Object>` 接收数据，而是定义强类型的 Java 类。Spring 会自动将 GraphQL 的 JSON 响应映射到这些类。

#### A. 请求参数 DTO (Input)
对应您之前问的 `variables` 结构。

```java
package com.ruoyi.shopify.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductVariantCreateInput {
    private String productId; // GID格式
    private List<VariantInput> variants;

    @Data
    public static class VariantInput {
        private BigDecimal price;
        private BigDecimal compareAtPrice;
        private List<OptionValueInput> optionValues;
    }

    @Data
    public static class OptionValueInput {
        private String name;
        private String optionId; // GID格式
    }
}
```

#### B. 响应结果 DTO (Response)
对应 GraphQL `query` 字符串中 `{ ... }` 里请求的字段。

```java
package com.ruoyi.shopify.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ProductVariantBulkCreateResponse {
    // 对应 GraphQL 返回中的 productVariantsBulkCreate 对象
    private ProductVariantsBulkPayload productVariantsBulkCreate;

    @Data
    public static class ProductVariantsBulkPayload {
        private List<ProductVariant> productVariants;
        private List<UserError> userErrors;
    }

    @Data
    public static class ProductVariant {
        private String id;
        private String title;
        private List<SelectedOption> selectedOptions;
    }

    @Data
    public static class SelectedOption {
        private String name;
        private String value;
    }

    @Data
    public static class UserError {
        private List<String> field;
        private String message;
    }
}
```

### 4. 封装 GraphQL 执行器 (Service Layer)

创建一个专门的 Service 来管理 GraphQL 请求。利用 `GraphQlClient` (Spring 6+ 新标准) 或 `WebClient`。

```java
package com.ruoyi.shopify.service.impl;

import com.ruoyi.shopify.dto.request.ProductVariantCreateInput;
import com.ruoyi.shopify.dto.response.ProductVariantBulkCreateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.WebGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ShopifyGraphQlService {

    private final GraphQlClient graphQlClient;

    // 注入配置
    public ShopifyGraphQlService(@Value("${shopify.api.base-url}") String baseUrl, 
                                 @Value("${shopify.api.access-token}") String token) {
        
        // 初始化 WebClient 并设置 Header
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Shopify-Access-Token", token)
                .defaultHeader("Content-Type", "application/json")
                .build();

        // 创建 GraphQlClient
        this.graphQlClient = WebGraphQlClient.builder(webClient).build();
    }

    /**
     * 执行批量创建变体
     */
    public ProductVariantBulkCreateResponse createVariants(ProductVariantCreateInput input) {
        
        // 定义 GraphQL 语句 (建议提取到 resources/graphql/*.gql 文件中管理)
        String mutationDocument = """
            mutation ProductVariantsCreate($productId: ID!, $variants: [ProductVariantsBulkInput!]!) {
              productVariantsBulkCreate(productId: $productId, variants: $variants) {
                productVariants {
                  id
                  title
                  selectedOptions { name value }
                }
                userErrors {
                  field
                  message
                }
              }
            }
            """;

        // 执行请求并映射结果
        return graphQlClient.document(mutationDocument)
                .variable("productId", input.getProductId())
                .variable("variants", input.getVariants())
                .retrieve("productVariantsBulkCreate") // 指定要 retrieve 的根节点
                .toEntity(ProductVariantBulkCreateResponse.ProductVariantsBulkPayload.class)
                .getBody()
                .wrapRoot(); // 辅助方法，需自行封装或直接处理
    }
}
```
*注：上述 `toEntity` 用法是简化示意，实际 Spring GraphQL 可能需要稍微调整映射逻辑，或者直接使用 `toObject` 获取整个响应树。更稳健的方式是直接获取 `Response` 对象然后手动检查 errors。*

**更稳健的通用执行方法：**

```java
public <T> T executeMutation(String document, Map<String, Object> variables, Class<T> responseType, String dataPath) {
    var response = graphQlClient.document(document)
            .variables(variables)
            .execute()
            .toSpec(); // 获取规范响应

    // 1. 检查网络级错误
    if (response.hasErrors()) {
        throw new RuntimeException("GraphQL Execution Failed: " + response.getErrors());
    }

    // 2. 提取数据路径 (如 productVariantsBulkCreate)
    var data = response.getData(dataPath);
    if (data == null) {
        throw new RuntimeException("No data returned for path: " + dataPath);
    }

    // 3. 检查业务级错误 (userErrors) - 这一步非常关键，Shopify常在此返回错误
    // 需要手动解析 data 中的 userErrors 字段，如果有则抛出自定义异常
    checkUserErrors(data); 

    // 4. 转换为 DTO
    return convertToDto(data, responseType);
}

private void checkUserErrors(Object data) {
    // 这里需要用 Jackson 或 Gson 解析 data 中的 userErrors 列表
    // 如果 list 不为空，抛出 ShopifyApiException
}
```

### 5. 业务调用 (Controller/Manager)

在您的 `ErpShopifyProductServiceImpl` 中调用：

```java
@Service
public class ErpShopifyProductServiceImpl implements IErpShopifyProductService {

    @Autowired
    private ShopifyGraphQlService shopifyGraphQlService;

    @Override
    @Transactional
    public int syncVariantsToShopify(ErpShopifyProduct product) {
        // 1. 准备数据
        ProductVariantCreateInput input = new ProductVariantCreateInput();
        input.setProductId("gid://shopify/Product/" + product.getShopifyProductId());
        
        // 转换本地 Variant 列表为 Input DTO
        List<VariantInput> variantInputs = product.getVariants().stream()
            .map(v -> {
                VariantInput vi = new VariantInput();
                vi.setPrice(v.getPrice());
                // ... 设置其他字段
                // 关键点：optionId 需要从缓存或数据库获取对应的 Shopify GID
                vi.setOptionValues(buildOptionValues(v)); 
                return vi;
            }).collect(Collectors.toList());
        
        input.setVariants(variantInputs);

        try {
            // 2. 调用 GraphQL
            ProductVariantBulkCreateResponse response = shopifyGraphQlService.createVariants(input);

            // 3. 处理成功结果：回写 Shopify Variant ID 到本地数据库
            if (response.getProductVariantsBulkCreate().getUserErrors().isEmpty()) {
                updateLocalVariantIds(response.getProductVariantsBulkCreate().getProductVariants());
                return 1;
            } else {
                // 4. 处理业务错误
                String errorMsg = response.getProductVariantsBulkCreate().getUserErrors().stream()
                    .map(e -> e.getMessage())
                    .collect(Collectors.joining("; "));
                throw new CustomException("Shopify同步失败: " + errorMsg);
            }
        } catch (Exception e) {
            // 记录日志，更新同步状态为失败
            log.error("同步变体失败", e);
            throw e;
        }
    }
}
```

### 6. 进阶技巧：如何更优雅？

#### A. 使用 `.gql` 文件管理查询
不要在 Java 代码里写长长的字符串。在 `src/main/resources/graphql/` 目录下创建 `create-variants.graphql` 文件：

```graphql
mutation ProductVariantsCreate($productId: ID!, $variants: [ProductVariantsBulkInput!]!) {
  productVariantsBulkCreate(productId: $productId, variants: $variants) {
    productVariants {
      id
      title
      selectedOptions { name value }
    }
    userErrors {
      field
      message
    }
  }
}
```

然后在 Java 中引用：
```java
// Spring 会自动加载 classpath:graphql/*.graphql 文件
graphQlClient.documentName("create-variants") // 文件名
    .variable("productId", ...)
    ...
```
**好处**：语法高亮、易于维护、方便复制给前端调试。

#### B. 统一异常处理
创建一个 `ShopifyApiException`，包含 `userErrors` 列表。在全局异常处理器 (`GlobalExceptionHandler`) 中捕获它，返回友好的提示信息给前端，而不是堆栈跟踪。

#### C. 限流处理 (Leaky Bucket)
Shopify API 有漏桶限流。优雅的客户端应该能处理 `429 Too Many Requests`。
Spring WebClient 可以配置 `Retry` 策略：

```java
WebClient webClient = WebClient.builder()
    .filter((request, next) -> next.exchange(request)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)) // 重试3次，指数退避
            .filter(t -> t instanceof WebClientResponseException && 
                         ((WebClientResponseException) t).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)))
    .build();
```

### 总结
优雅对接的核心在于：
1.  **强类型 DTO**：拒绝 `Map`，使用 Java Bean 映射。
2.  **配置与代码分离**：URL 和 Token 走配置，GQL 语句走文件。
3.  **结构化错误处理**：区分网络错误和 Shopify 业务错误 (`userErrors`)。
4.  **利用 Spring 生态**：使用 `spring-boot-starter-graphql` 而非手写 HTTP 请求。

这样设计后，您的代码将清晰、易测试，且符合若依框架的整体架构风格。
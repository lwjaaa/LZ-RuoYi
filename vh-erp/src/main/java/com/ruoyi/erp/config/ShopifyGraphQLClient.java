package com.ruoyi.erp.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.exception.ShopifyApiException;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.service.IShopifyStoreService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shopify GraphQL API 客户端
 * <p>
 * 支持多店铺管理，通过数据库配置获取 Token 信息
 */
@Slf4j
@Component
public class ShopifyGraphQLClient {

    private static final String FIELD_QUERY = "query";
    private static final String FIELD_VARIABLES = "variables";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_ERRORS = "errors";

    private static final String PATH_USER_ERRORS = "userErrors";
    private static final String PATH_MESSAGE = "message";
    private static final String PATH_ID = "id";
    private static final String PATH_PRODUCT = "product";
    private static final String PATH_MEDIA = "media";
    private static final String PATH_PRODUCT_VARIANTS = "productVariants";
    private static final String PATH_STAGED_TARGETS = "stagedTargets";
    private static final String PATH_PARAMETERS = "parameters";
    private static final String PATH_NAME = "name";
    private static final String PATH_VALUE = "value";
    private static final String PATH_URL = "url";
    private static final String PATH_RESOURCE_URL = "resourceUrl";

    private static final String MUTATION_STAGED_UPLOADS_CREATE = "stagedUploadsCreate";
    private static final String MUTATION_MEDIA_CREATE = "mediaCreate";
    private static final String MUTATION_PRODUCT_CREATE = "productCreate";
    private static final String MUTATION_PRODUCT_UPDATE = "productUpdate";
    private static final String MUTATION_PRODUCT_VARIANTS_BULK_CREATE = "productVariantsBulkCreate";

    private static final String RESOURCE_TYPE_IMAGE = "IMAGE";
    private static final String HTTP_METHOD_POST = "POST";

    private static final String MULTIPART_BOUNDARY_PREFIX = "----ShopifyBoundary";
    private static final String FILE_FIELD_NAME = "file";
    private static final String FILE_UPLOAD_NAME = "upload";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private static final int RETRY_MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_SECONDS = 2;

    @Resource
    private IShopifyStoreService shopifyStoreService;

    /**
     * 每个店铺独立的 WebClient 缓存 (避免每次创建)
     */
    private final ConcurrentMap<Long, WebClient> webClientCache = new ConcurrentHashMap<>();

    /**
     * 根据 storeId 获取 WebClient
     */
    private WebClient getWebClient(Long storeId) {
        return webClientCache.computeIfAbsent(storeId, id -> {
            ShopifyStore store = shopifyStoreService.selectByStoreId(id);
            if (store == null) {
                throw new ShopifyApiException("店铺不存在: storeId=" + id);
            }
            return createWebClient(store);
        });
    }

    /**
     * 根据 shopName 获取 WebClient
     */
    private WebClient getWebClientByShopName(String shopName) {
        ShopifyStore store = shopifyStoreService.selectByShopName(shopName);
        if (store == null) {
            throw new ShopifyApiException("店铺不存在: shopName=" + shopName);
        }
        return webClientCache.computeIfAbsent(store.getStoreId(), id -> createWebClient(store));
    }

    /**
     * 创建 WebClient 实例
     */
    private WebClient createWebClient(ShopifyStore store) {
        return WebClient.builder()
                .baseUrl(store.getFullGraphQLUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-Shopify-Access-Token", store.getAccessToken())
                .build();
    }

    /**
     * 获取店铺配置 (用于 Token 刷新)
     */
    private ShopifyStore getStore(Long storeId) {
        ShopifyStore store = shopifyStoreService.selectByStoreId(storeId);
        if (store == null) {
            throw new ShopifyApiException("店铺不存在: storeId=" + storeId);
        }
        return store;
    }

    /**
     * 清除 WebClient 缓存 (Token 刷新后调用)
     */
    private void invalidateCache(Long storeId) {
        webClientCache.remove(storeId);
        log.info("已清除 WebClient 缓存: storeId={}", storeId);
    }

    /**
     * 执行 GraphQL 查询 (指定店铺)
     *
     * @param storeId 店铺 ID
     * @param query   GraphQL 查询语句
     * @param variables 变量参数
     * @return 响应数据节点
     */
    public JsonNode execute(Long storeId, String query, Object variables) {
        Map<String, Object> requestBody = buildRequestBody(query, variables);

        try {
            JsonNode response = getWebClient(storeId).post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, Duration.ofSeconds(RETRY_BACKOFF_SECONDS))
                        .onRetryExhaustedThrow((spec, e) -> new ShopifyApiException("GraphQL 请求失败: " + e.failure().getMessage(), e.failure())))
                    .block();

            return processResponse(response);
        } catch (WebClientResponseException e) {
            return handleError(storeId, e, requestBody);
        }
    }

    /**
     * 执行 GraphQL 查询 (指定 shopName)
     */
    public JsonNode execute(String shopName, String query, Object variables) {
        ShopifyStore store = shopifyStoreService.selectByShopName(shopName);
        if (store == null) {
            throw new ShopifyApiException("店铺不存在: shopName=" + shopName);
        }
        return execute(store.getStoreId(), query, variables);
    }

    /**
     * 处理响应，检查错误
     */
    private JsonNode processResponse(JsonNode response) {
        if (response == null) {
            throw new ShopifyApiException("GraphQL 返回空响应");
        }

        if (response.has(FIELD_ERRORS)) {
            throw new ShopifyApiException("GraphQL 错误: " + response.get(FIELD_ERRORS).toString());
        }

        return response.get(FIELD_DATA);
    }

    /**
     * 处理 HTTP 错误，包括 401 Token 过期
     */
    private JsonNode handleError(Long storeId, WebClientResponseException e, Map<String, Object> requestBody) {
        if (e.getStatusCode().value() == 401) {
            log.warn("Shopify API 返回 401 (Unauthorized)，尝试刷新 Token: storeId={}", storeId);

            // 刷新 Token
            boolean refreshed = shopifyStoreService.refreshToken(storeId);
            if (refreshed) {
                // 清除缓存，重试请求
                invalidateCache(storeId);
                ShopifyStore store = shopifyStoreService.selectByStoreId(storeId);

                JsonNode response = WebClient.builder()
                        .baseUrl(store.getFullGraphQLUrl())
                        .defaultHeader("Content-Type", "application/json")
                        .defaultHeader("X-Shopify-Access-Token", store.getAccessToken())
                        .build()
                        .post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, Duration.ofSeconds(RETRY_BACKOFF_SECONDS))
                            .onRetryExhaustedThrow((spec, ex) -> new ShopifyApiException("GraphQL 请求失败: " + ex.failure().getMessage(), ex.failure())))
                        .block();

                return processResponse(response);
            }
        }

        throw new ShopifyApiException("Shopify API HTTP 错误: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
    }

    /**
     * 检查 userErrors
     */
    public void checkUserErrors(JsonNode data, String mutationName) {
        JsonNode userErrors = data.path(mutationName).path(PATH_USER_ERRORS);
        if (userErrors.isArray() && !userErrors.isEmpty()) {
            List<String> errorList = new ArrayList<>();
            for (JsonNode err : userErrors) {
                errorList.add(err.path(PATH_MESSAGE).asText());
            }
            throw new ShopifyApiException(String.join("; ", errorList));
        }
    }

    /**
     * 上传媒体文件并返回 Shopify 媒体信息 (指定店铺)
     */
    public StagedUploadResult stagedUploadMedia(Long storeId, String filename, String mimeType, InputStream inputStream, long fileSize) {
        String mutation = ShopifyGraphQLQueries.STAGED_UPLOADS_CREATE.getQuery();

        StagedUploadInput input = StagedUploadInput.builder()
                .resource(RESOURCE_TYPE_IMAGE)
                .filename(filename)
                .mimeType(mimeType)
                .httpMethod(HTTP_METHOD_POST)
                .fileSize(String.valueOf(fileSize))
                .build();

        JsonNode data = execute(storeId, mutation, Map.of("input", List.of(input)));
        checkUserErrors(data, MUTATION_STAGED_UPLOADS_CREATE);

        JsonNode target = data.path(MUTATION_STAGED_UPLOADS_CREATE).path(PATH_STAGED_TARGETS).get(0);
        String uploadUrl = target.path(PATH_URL).asText();
        String resourceUrl = target.path(PATH_RESOURCE_URL).asText();

        List<Parameter> parameters = parseParameters(target.path(PATH_PARAMETERS));

        try {
            byte[] fileBytes = inputStream.readAllBytes();
            uploadToStagedTarget(storeId, uploadUrl, parameters, fileBytes, mimeType);
            return new StagedUploadResult(uploadUrl, resourceUrl);
        } catch (IOException e) {
            throw new ShopifyApiException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到 staged target URL (指定店铺)
     */
    private void uploadToStagedTarget(Long storeId, String uploadUrl, List<Parameter> parameters, byte[] fileData, String mimeType) {
        ShopifyStore store = getStore(storeId);
        String boundary = MULTIPART_BOUNDARY_PREFIX + System.currentTimeMillis();
        byte[] body = buildMultipartBody(parameters, fileData, boundary);

        WebClient.create(uploadUrl)
                .put()
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Content-Length", String.valueOf(body.length))
                .header("X-Shopify-Access-Token", store.getAccessToken())
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private Map<String, Object> buildRequestBody(String query, Object variables) {
        return Map.of(FIELD_QUERY, query, FIELD_VARIABLES, variables != null ? variables : Map.of());
    }

    private List<Parameter> parseParameters(JsonNode parametersNode) {
        List<Parameter> parameters = new ArrayList<>();
        for (JsonNode param : parametersNode) {
            parameters.add(new Parameter(
                    param.path(PATH_NAME).asText(),
                    param.path(PATH_VALUE).asText()
            ));
        }
        return parameters;
    }

    private byte[] buildMultipartBody(List<Parameter> parameters, byte[] fileData, String boundary) {
        StringBuilder sb = new StringBuilder();
        for (Parameter param : parameters) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"").append(param.getName()).append("\"\r\n\r\n");
            sb.append(param.getValue()).append("\r\n");
        }
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(FILE_FIELD_NAME).append("\"; filename=\"")
                .append(FILE_UPLOAD_NAME).append("\"\r\n");
        sb.append("Content-Type: ").append(CONTENT_TYPE_OCTET_STREAM).append("\r\n\r\n");

        byte[] header = sb.toString().getBytes();
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes();

        byte[] result = new byte[header.length + fileData.length + footer.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(fileData, 0, result, header.length, fileData.length);
        System.arraycopy(footer, 0, result, header.length + fileData.length, footer.length);
        return result;
    }

    /**
     * 注册媒体到 Shopify (指定店铺)
     */
    public String createMediaImage(Long storeId, String imageUrl, String alt) {
        String mutation = ShopifyGraphQLQueries.MEDIA_CREATE.getQuery();

        MediaInput input = MediaInput.builder()
                .mediaUrl(imageUrl)
                .alt(alt != null ? alt : "")
                .build();

        JsonNode data = execute(storeId, mutation, Map.of("input", input));
        checkUserErrors(data, MUTATION_MEDIA_CREATE);

        return data.path(MUTATION_MEDIA_CREATE).path(PATH_MEDIA).path(PATH_ID).asText();
    }

    /**
     * 创建 Shopify 商品 (指定店铺)
     */
    public String createProduct(Long storeId, ProductInput input) {
        String mutation = ShopifyGraphQLQueries.PRODUCT_CREATE.getQuery();

        JsonNode data = execute(storeId, mutation, Map.of("input", input));
        checkUserErrors(data, MUTATION_PRODUCT_CREATE);

        String productId = data.path(MUTATION_PRODUCT_CREATE).path(PATH_PRODUCT).path(PATH_ID).asText();
        if (StringUtils.isEmpty(productId)) {
            throw new ShopifyApiException("商品创建成功但未返回 ID");
        }
        return productId;
    }

    /**
     * 更新 Shopify 商品 (指定店铺)
     */
    public String updateProduct(Long storeId, String shopifyProductId, ProductInput input) {
        String mutation = ShopifyGraphQLQueries.PRODUCT_UPDATE.getQuery();

        ProductInput inputWithId = ProductInput.builderFrom(input)
                .id(shopifyProductId)
                .build();

        JsonNode data = execute(storeId, mutation, Map.of("input", inputWithId));
        checkUserErrors(data, MUTATION_PRODUCT_UPDATE);

        return data.path(MUTATION_PRODUCT_UPDATE).path(PATH_PRODUCT).path(PATH_ID).asText();
    }

    /**
     * 批量创建变体 (指定店铺)
     */
    public List<String> createVariantsBulk(Long storeId, String shopifyProductId, List<VariantInput> variants) {
        if (variants.isEmpty()) {
            return List.of();
        }

        String mutation = ShopifyGraphQLQueries.PRODUCT_VARIANTS_BULK_CREATE.getQuery();

        JsonNode data = execute(storeId, mutation, Map.of(
                "productId", shopifyProductId,
                "variants", variants
        ));
        checkUserErrors(data, MUTATION_PRODUCT_VARIANTS_BULK_CREATE);

        JsonNode variantsNode = data.path(MUTATION_PRODUCT_VARIANTS_BULK_CREATE).path(PATH_PRODUCT_VARIANTS);
        List<String> variantIds = new ArrayList<>();
        for (JsonNode v : variantsNode) {
            variantIds.add(v.path(PATH_ID).asText());
        }
        return variantIds;
    }

    // ==================== 内部类定义 ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StagedUploadInput {
        private String resource;
        private String filename;
        private String mimeType;
        private String httpMethod;
        private String fileSize;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MediaInput {
        private String mediaUrl;
        private String alt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductInput {
        private String id;
        private String title;
        private String bodyHtml;
        private String descriptionHtml;
        private List<ProductImageInput> images;
        private List<ProductOptionInput> options;
        private String productType;
        private String vendor;
        private String category;
        private SeoInput seo;
        private List<String> tags;

        public static ProductInputBuilder builderFrom(ProductInput other) {
            return builder()
                    .id(other.id)
                    .title(other.title)
                    .bodyHtml(other.bodyHtml)
                    .descriptionHtml(other.descriptionHtml)
                    .images(other.images)
                    .options(other.options)
                    .productType(other.productType)
                    .vendor(other.vendor)
                    .category(other.category)
                    .seo(other.seo)
                    .tags(other.tags);
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductImageInput {
        private String src;
        private String altText;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SeoInput {
        private String title;
        private String description;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductOptionInput {
        private String name;
        private List<String> values;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InventoryItemInput {
        private String sku;
        private Boolean tracked;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VariantInput {
        private String sku;
        private BigDecimal price;
        private String mediaId;
        private BigDecimal compareAtPrice;
        private Integer inventoryQuantity;
        private String inventoryPolicy;
        private List<SelectedOptionInput> selectedOptions;
        private String imageId;
        private InventoryItemInput inventoryItem;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SelectedOptionInput {
        private String name;
        private String value;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Parameter {
        private String name;
        private String value;
    }

    public enum ShopifyGraphQLQueries {
        STAGED_UPLOADS_CREATE("""
            mutation stagedUploadsCreate($input: [StagedUploadInput!]!) {
              stagedUploadsCreate(input: $input) {
                userErrors { field message }
                stagedTargets {
                  url
                  resourceUrl
                  parameters { name value }
                }
              }
            }
            """),

        MEDIA_CREATE("""
            mutation mediaCreate($input: MediaInput!) {
              mediaCreate(input: $input) {
                userErrors { field message }
                media {
                  ... on MediaImage {
                    id
                  }
                }
              }
            }
            """),

        PRODUCT_CREATE("""
            mutation productCreate($input: ProductInput!) {
              productCreate(input: $input) {
                userErrors { field message }
                product {
                  id
                  title
                }
              }
            }
            """),

        PRODUCT_UPDATE("""
            mutation productUpdate($input: ProductInput!) {
              productUpdate(input: $input) {
                userErrors { field message }
                product {
                  id
                }
              }
            }
            """),

        PRODUCT_VARIANTS_BULK_CREATE("""
            mutation productVariantsBulkCreate($productId: ID!, $variants: [ProductVariantsBulkInput!]!) {
              productVariantsBulkCreate(productId: $productId, variants: $variants) {
                userErrors { field message }
                productVariants {
                  id
                  title
                }
              }
            }
            """);

        private final String query;

        ShopifyGraphQLQueries(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }

    public record StagedUploadResult(String url, String resourceUrl) {}
}
package com.ruoyi.erp.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.exception.ShopifyApiException;
import com.ruoyi.erp.exception.ShopifyTokenExpiredException;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.service.IShopifyStoreService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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
    private static final String MUTATION_PUBLICATION_UPDATE = "publicationUpdate";

    private static final String RESOURCE_TYPE_IMAGE = "IMAGE";
    private static final String RESOURCE_TYPE_VIDEO = "VIDEO";
    private static final String HTTP_METHOD_POST = "POST";

    private static final String MULTIPART_BOUNDARY_PREFIX = "----ShopifyBoundary";
    private static final String FILE_FIELD_NAME = "file";
    private static final String FILE_UPLOAD_NAME = "upload";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private static final int RETRY_MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_SECONDS = 2;

    @Resource
    private IShopifyStoreService shopifyStoreService;

    @Resource
    private RedisCache redisCache;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 每个店铺独立的 WebClient 缓存 (避免每次创建)
     */
    private final ConcurrentMap<Long, WebClient> webClientCache = new ConcurrentHashMap<>();

    /**
     * Token 刷新锁的 Key 前缀
     */
    private static final String TOKEN_REFRESH_LOCK_KEY = "shopify:token_refresh_lock:";

    /**
     * Token 刷新锁的过期时间（秒）
     */
    private static final int TOKEN_REFRESH_LOCK_EXPIRE = 10;

    /**
     * 重试次数的 Redis Key 前缀
     */
    private static final String RETRY_COUNT_KEY = "shopify:retry_count:";

    /**
     * 最大重试次数（Token 刷新后）
     */
    private static final int MAX_RETRY_AFTER_REFRESH = 1;

    /**
     * 重试计数过期时间（秒）
     */
    private static final int RETRY_COUNT_EXPIRE = 60;

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
     * 清除指定店铺的 WebClient 缓存。
     */
    public void invalidateStoreCache(Long storeId) {
        invalidateCache(storeId);
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
        log.info("执行 GraphQL 查询: storeId={}, query={}, requestBody={}", storeId, query, requestBody);
        // 第一次尝试
        try {
            JsonNode jsonNode = doExecute(storeId, requestBody);
            log.info("GraphQL 响应: storeId={}, response={}", storeId, jsonNode);
            return jsonNode;
        } catch (ShopifyTokenExpiredException e) {
            // Token 过期，刷新后重试一次
            log.warn("检测到 Token 过期，刷新后重试: storeId={}", storeId);
            refreshTokenIfNeeded(storeId);
            
            // 第二次尝试（最后一次）
            JsonNode jsonNode = doExecute(storeId, requestBody);
            log.info("GraphQL 响应: storeId={}, response={}", storeId, jsonNode);
            return jsonNode;
        }
    }

    /**
     * 实际执行 GraphQL 请求
     */
    private JsonNode doExecute(Long storeId, Map<String, Object> requestBody) {
        // 对于网络异常，最多重试2次
        int maxRetries = 2;
        int retryCount = 0;
        
        while (true) {
            try {
                JsonNode response = getWebClient(storeId).post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(status -> status.value() == HttpStatus.UNAUTHORIZED.value(), clientResponse -> {
                            // 401 时抛出特殊异常，触发 token 刷新
                            return Mono.error(new ShopifyTokenExpiredException("Token 已过期"));
                        })
                        .bodyToMono(JsonNode.class)
                        .block();

                return processResponse(response);
            } catch (Exception e) {
                Throwable cause = e.getCause();
                
                // 如果是 WebClientResponseException，直接抛出
                if (cause instanceof WebClientResponseException webException) {
                    log.error("GraphQL 请求失败，状态码: {}, 响应: {}",
                        webException.getStatusCode(), webException.getResponseBodyAsString());
                    throw new ShopifyApiException(
                        "Shopify API HTTP 错误: " + webException.getStatusCode() + " " + webException.getResponseBodyAsString(),
                        webException
                    );
                }
                
                // 如果是 Token 过期异常，直接抛出
                if (e instanceof ShopifyTokenExpiredException) {
                    throw e;
                }
                
                // 检查是否是网络连接异常（可重试）
                boolean isNetworkError = isRetryableNetworkError(e);
                
                if (isNetworkError && retryCount < maxRetries) {
                    retryCount++;
                    long backoffTime = (long) Math.pow(2, retryCount) * 1000; // 指数退避：2s, 4s
                    log.warn("检测到网络异常，第{}次重试（{}ms后）: storeId={}, 错误: {}", 
                        retryCount, backoffTime, storeId, e.getMessage());
                    
                    try {
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ShopifyApiException("重试被中断", ie);
                    }
                    continue; // 继续重试
                }
                
                // 其他异常或超过最大重试次数，直接抛出
                throw new ShopifyApiException("GraphQL 请求异常: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 判断是否为可重试的网络异常
     */
    private boolean isRetryableNetworkError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        
        // 常见的可重试网络异常
        return message.contains("Connection reset") ||
               message.contains("Connection refused") ||
               message.contains("Read timed out") ||
               message.contains("Connect timed out") ||
               message.contains("Broken pipe") ||
               e instanceof java.net.SocketException ||
               e instanceof java.net.SocketTimeoutException ||
               e instanceof java.io.IOException;
    }

    /**
     * 刷新 Token（带分布式锁）
     */
    private void refreshTokenIfNeeded(Long storeId) {
        String lockKey = TOKEN_REFRESH_LOCK_KEY + storeId;
        RLock lock = redissonClient.getLock(lockKey);
        
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TOKEN_REFRESH_LOCK_EXPIRE, TimeUnit.SECONDS);
            
            if (locked) {
                // 获取锁，执行刷新
                boolean refreshed = shopifyStoreService.refreshToken(storeId);
                if (refreshed) {
                    invalidateCache(storeId);
                    log.info("Token 刷新成功: storeId={}", storeId);
                } else {
                    log.error("Token 刷新失败: storeId={}", storeId);
                    throw new ShopifyApiException("Token 刷新失败");
                }
            } else {
                // 未获取到锁，等待其他线程刷新完成
                log.info("等待其他线程刷新 Token: storeId={}", storeId);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShopifyApiException("Token 刷新过程中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
        return stagedUploadMedia(storeId, filename, mimeType, RESOURCE_TYPE_IMAGE, inputStream, fileSize);
    }

    /**
     * 上传媒体文件并返回 Shopify staged upload 信息。
     */
    public StagedUploadResult stagedUploadMedia(Long storeId, String filename, String mimeType, String resource, InputStream inputStream, long fileSize) {
        String uploadResource = StringUtils.isEmpty(resource) ? RESOURCE_TYPE_IMAGE : resource;
        log.info("获取媒体文件上传地址: storeId={}, filename={}, mimeType={}, resource={}, fileSize={}",
                storeId, filename, mimeType, uploadResource, fileSize);
        String mutation = ShopifyGraphQLQueries.STAGED_UPLOADS_CREATE.getQuery();

        StagedUploadInput input = StagedUploadInput.builder()
                .resource(uploadResource)
                .filename(filename)
                .mimeType(mimeType)
                .httpMethod(HTTP_METHOD_POST)
                .fileSize(String.valueOf(fileSize))
                .build();

        JsonNode data = execute(storeId, mutation, Map.of("input", List.of(input)));
        checkUserErrors(data, MUTATION_STAGED_UPLOADS_CREATE);
        log.info("获取媒体文件上传地址成功: storeId={}, data={}", storeId, data);

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
        log.info("上传文件到 staged target URL: storeId={}, uploadUrl={}, parameters={}", storeId, uploadUrl, parameters);
        ShopifyStore store = getStore(storeId);
        String boundary = MULTIPART_BOUNDARY_PREFIX + System.currentTimeMillis();
        byte[] body = buildMultipartBody(parameters, fileData, boundary);

        WebClient.create(uploadUrl)
                .post()
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Content-Length", String.valueOf(body.length))
                .header("X-Shopify-Access-Token", store.getAccessToken())
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
        log.info("文件上传成功");
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
     * @deprecated Shopify API 2024-07+ 已移除 mediaCreate mutation
     * 现在应该在创建/更新产品时直接使用 images 字段，src 使用 staged upload 的 resourceUrl
     */
    @Deprecated
    public String createMediaImage(Long storeId, String imageUrl, String alt) {
        log.warn("createMediaImage 已废弃，请在创建/更新产品时直接使用 images 字段");
        // 直接返回 imageUrl，实际上应该在使用此方法的地方改为使用 ProductInput.images
        return imageUrl;
    }

    /**
     * 创建 Shopify 商品 (指定店铺)
     */
    public ProductCreateResult createProduct(Long storeId, ProductInput productInput, List<CreateMediaInput> mediaInputs) {
        String mutation = ShopifyGraphQLQueries.PRODUCT_CREATE.getQuery();

        Map<String, Object> variables = new HashMap<>();
        variables.put("product", productInput);
        if (mediaInputs != null && !mediaInputs.isEmpty()) {
            variables.put("media", mediaInputs);
        }

        JsonNode data = execute(storeId, mutation, variables);
        checkUserErrors(data, MUTATION_PRODUCT_CREATE);

        JsonNode productNode = data.path(MUTATION_PRODUCT_CREATE).path(PATH_PRODUCT);
        String productId = productNode.path(PATH_ID).asText();
        if (StringUtils.isEmpty(productId)) {
            throw new ShopifyApiException("商品创建成功但未返回 ID");
        }

        // 解析 media ID 列表
        List<String> mediaIds = new ArrayList<>();
        JsonNode mediaEdges = productNode.path("media").path("edges");
        if (mediaEdges.isArray()) {
            for (JsonNode edge : mediaEdges) {
                String mediaId = edge.path("node").path(PATH_ID).asText();
                if (StringUtils.isNotEmpty(mediaId)) {
                    mediaIds.add(mediaId);
                }
            }
        }
        if (mediaInputs == null || mediaInputs.isEmpty()) {
            mediaIds = List.of();
        } else if (mediaIds.size() > mediaInputs.size()) {
            mediaIds = new ArrayList<>(mediaIds.subList(mediaIds.size() - mediaInputs.size(), mediaIds.size()));
        }

        return new ProductCreateResult(productId, mediaIds);
    }

    /**
     * 更新 Shopify 商品 (指定店铺)
     */
    public ProductCreateResult updateProduct(Long storeId, String shopifyProductId, ProductInput input, List<CreateMediaInput> mediaInputs) {
        String mutation = ShopifyGraphQLQueries.PRODUCT_UPDATE.getQuery();

        // Shopify API 不允许在更新时包含 productOptions 字段
        ProductInput inputWithId = ProductInput.builderFrom(input)
                .id(shopifyProductId)
                .productOptions(null)  // 清空 productOptions
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("product", inputWithId);
        if (mediaInputs != null && !mediaInputs.isEmpty()) {
            variables.put("media", mediaInputs);
        }

        JsonNode data = execute(storeId, mutation, variables);
        checkUserErrors(data, MUTATION_PRODUCT_UPDATE);

        JsonNode productNode = data.path(MUTATION_PRODUCT_UPDATE).path(PATH_PRODUCT);
        String productId = productNode.path(PATH_ID).asText();
        if (StringUtils.isEmpty(productId)) {
            throw new ShopifyApiException("Product updated but Shopify did not return product ID");
        }

        List<String> mediaIds = new ArrayList<>();
        JsonNode mediaEdges = productNode.path("media").path("edges");
        if (mediaEdges.isArray()) {
            for (JsonNode edge : mediaEdges) {
                String mediaId = edge.path("node").path(PATH_ID).asText();
                if (StringUtils.isNotEmpty(mediaId)) {
                    mediaIds.add(mediaId);
                }
            }
        }
        if (mediaInputs == null || mediaInputs.isEmpty()) {
            mediaIds = List.of();
        } else if (mediaIds.size() > mediaInputs.size()) {
            mediaIds = new ArrayList<>(mediaIds.subList(mediaIds.size() - mediaInputs.size(), mediaIds.size()));
        }

        return new ProductCreateResult(productId, mediaIds);
    }

    /**
     * 批量创建变体 (指定店铺)
     */
    public List<String> createVariantsBulk(Long storeId, String shopifyProductId, List<VariantInput> variants) {
        return createVariantsBulk(storeId, shopifyProductId, variants, null);
    }

    /**
     * 批量创建变体 (指定店铺)
     */
    public List<String> createVariantsBulk(Long storeId, String shopifyProductId, List<VariantInput> variants, String strategy) {
        if (variants.isEmpty()) {
            return List.of();
        }

        String mutation = ShopifyGraphQLQueries.PRODUCT_VARIANTS_BULK_CREATE.getQuery();

        Map<String, Object> variables = new HashMap<>();
        variables.put("productId", shopifyProductId);
        variables.put("variants", variants);
        if (StringUtils.isNotEmpty(strategy)) {
            variables.put("strategy", strategy);
        }

        JsonNode data = execute(storeId, mutation, variables);
        log.info("批量创建变体成功: storeId={}, shopifyProductId={}, data={}", storeId, shopifyProductId, data);
        checkUserErrors(data, MUTATION_PRODUCT_VARIANTS_BULK_CREATE);

        JsonNode variantsNode = data.path(MUTATION_PRODUCT_VARIANTS_BULK_CREATE).path(PATH_PRODUCT_VARIANTS);
        List<String> variantIds = new ArrayList<>();
        for (JsonNode v : variantsNode) {
            variantIds.add(v.path(PATH_ID).asText());
        }
        return variantIds;
    }

    /**
     * 测试店铺 GraphQL 连接。
     */
    public void testConnection(Long storeId) {
        execute(storeId, ShopifyGraphQLQueries.SHOP_INFO.getQuery(), Map.of());
    }

    /**
     * 查询可用的库存仓库 Location。
     */
    public List<LocationInfo> getLocations(Long storeId) {
        String query = ShopifyGraphQLQueries.LOCATIONS.getQuery();
        JsonNode data = execute(storeId, query, Map.of("first", 100));

        List<LocationInfo> locations = new ArrayList<>();
        JsonNode edges = data.path("locations").path("edges");
        if (edges.isArray()) {
            for (JsonNode edge : edges) {
                JsonNode node = edge.path("node");
                locations.add(LocationInfo.builder()
                        .id(node.path("id").asText())
                        .name(node.path("name").asText())
                        .build());
            }
        }
        return locations;
    }

    /**
     * 查询可发布的 Publication。
     */
    public List<PublicationInfo> getPublications(Long storeId) {
        String query = ShopifyGraphQLQueries.PUBLICATIONS.getQuery();
        JsonNode data = execute(storeId, query, Map.of("first", 100));

        List<PublicationInfo> publications = new ArrayList<>();
        JsonNode edges = data.path("publications").path("edges");
        if (edges.isArray()) {
            for (JsonNode edge : edges) {
                JsonNode node = edge.path("node");
                publications.add(PublicationInfo.builder()
                        .id(node.path("id").asText())
                        .name(node.path("name").asText())
                        .build());
            }
        }
        return publications;
    }

    /**
     * 设置 Publication 是否自动发布新商品。
     */
    public void updatePublicationAutoPublish(Long storeId, String publicationId, boolean autoPublish) {
        if (StringUtils.isEmpty(publicationId)) {
            throw new ShopifyApiException("Publication ID 不能为空");
        }
        JsonNode data = execute(storeId, ShopifyGraphQLQueries.PUBLICATION_UPDATE.getQuery(), Map.of(
                "id", publicationId,
                "input", PublicationUpdateInput.builder().autoPublish(autoPublish).build()
        ));
        checkUserErrors(data, MUTATION_PUBLICATION_UPDATE);
    }

    /**
     * 查询可用的销售渠道
     */
    public List<ChannelInfo> getChannels(Long storeId) {
        String query = ShopifyGraphQLQueries.CHANNELS.getQuery();
        JsonNode data = execute(storeId, query, Map.of("first", 50));

        List<ChannelInfo> channels = new ArrayList<>();
        JsonNode edges = data.path("channels").path("edges");
        if (edges.isArray()) {
            for (JsonNode edge : edges) {
                JsonNode node = edge.path("node");
                channels.add(ChannelInfo.builder()
                        .id(node.path("id").asText())
                        .name(node.path("name").asText())
                        .isPublished(node.path("isPublished").asBoolean())
                        .build());
            }
        }
        return channels;
    }

    /**
     * 设置产品在多个渠道的发布状态
     */
    public List<PublicationResult> setProductPublications(Long storeId, String shopifyProductId, List<PublicationInput> publications) {
        String mutation = ShopifyGraphQLQueries.RESOURCE_PUBLICATION_SET.getQuery();

        Map<String, Object> variables = Map.of(
                "resourceId", shopifyProductId,
                "publications", publications
        );

        JsonNode data = execute(storeId, mutation, variables);
        checkUserErrors(data, "resourcePublicationSet");

        List<PublicationResult> results = new ArrayList<>();
        JsonNode pubs = data.path("resourcePublicationSet").path("resourcePublications");
        if (pubs.isArray()) {
            for (JsonNode pub : pubs) {
                results.add(PublicationResult.builder()
                        .channelId(pub.path("publication").path("channel").path("id").asText())
                        .channelName(pub.path("publication").path("channel").path("name").asText())
                        .isPublished(pub.path("isPublished").asBoolean())
                        .publishDate(pub.path("publishDate").asText(null))
                        .build());
            }
        }
        return results;
    }

    /**
     * 使用 Publication ID 发布商品。
     */
    public List<PublicationResult> publishProduct(Long storeId, String shopifyProductId, List<PublicationInput> publications) {
        String mutation = ShopifyGraphQLQueries.PUBLISHABLE_PUBLISH.getQuery();

        Map<String, Object> variables = Map.of(
                "id", shopifyProductId,
                "input", publications
        );

        JsonNode data = execute(storeId, mutation, variables);
        checkUserErrors(data, "publishablePublish");

        return publications.stream()
                .map(input -> PublicationResult.builder()
                        .publicationId(input.getPublicationId())
                        .isPublished(true)
                        .build())
                .toList();
    }

    // ==================== 内部类定义 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StagedUploadInput {
        private String resource;
        private String filename;
        private String mimeType;
        private String httpMethod;
        private String fileSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MediaInput {
        private String mediaUrl;
        private String alt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductInput {
        private String id;
        private String title;
        private String bodyHtml;
        private String descriptionHtml;
        private List<ProductOptionInput> productOptions;
        private String productType;
        private String vendor;
        private String category;
        private SeoInput seo;
        private String status;
        private List<String> tags;
        private List<ProductMetafield> metafields;

        public static ProductInputBuilder builderFrom(ProductInput other) {
            return builder()
                    .id(other.id)
                    .title(other.title)
                    .bodyHtml(other.bodyHtml)
                    .descriptionHtml(other.descriptionHtml)
                    .productOptions(other.productOptions)
                    .productType(other.productType)
                    .vendor(other.vendor)
                    .category(other.category)
                    .seo(other.seo)
                    .metafields(other.metafields)
                    .tags(other.tags);
        }
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductMetafield {
        /**
         * The unique ID of the metafield. Using namespace and key is preferred for creating and updating.
         */
        private String id;
        private String key;
        private String namespace;
        private String type;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateMediaInput {
        private String originalSource;
        private String alt;
        private String mediaContentType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductImageInput {
        private String originalSource;
        private String alt;
        private String mediaContentType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SeoInput {
        private String title;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductOptionInput {
        private String name;
        private Integer position;
        private List<OptionValueInput> values;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptionValueInput {
        private String name;
        private String optionId;
        private String optionName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VariantInput {
        private BigDecimal price;
        private String mediaId;
        private BigDecimal compareAtPrice;
        private String inventoryPolicy;
        private List<OptionValueInput> optionValues;
        private Boolean taxable;
        private InventoryItemInput inventoryItem;
        private List<InventoryQuantity> inventoryQuantities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InventoryItemInput {
        private String sku;
        private Boolean tracked;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InventoryQuantity {
        private String locationId;
        private Integer availableQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SelectedOptionInput {
        private String name;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        private String name;
        private String value;
    }

    public enum ShopifyGraphQLQueries {
        SHOP_INFO("""
            query shopInfo {
              shop {
                id
                name
                myshopifyDomain
              }
            }
            """),

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
            mutation productCreate($product: ProductCreateInput!, $media: [CreateMediaInput!]) {
              productCreate(product: $product, media: $media) {
                userErrors { field message }
                product {
                  id
                  title
                  media(first: 250) {
                    edges {
                      node {
                        id
                        alt
                        ... on MediaImage {
                          image {
                            originalSrc
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """),

        PRODUCT_UPDATE("""
            mutation productUpdate($product: ProductUpdateInput!, $media: [CreateMediaInput!]) {
              productUpdate(product: $product, media: $media) {
                userErrors { field message }
                product {
                  id
                  media(first: 250) {
                    edges {
                      node {
                        id
                      }
                    }
                  }
                }
              }
            }
            """),

        PRODUCT_VARIANTS_BULK_CREATE("""
            mutation productVariantsBulkCreate($productId: ID!, $variants: [ProductVariantsBulkInput!]!, $strategy: ProductVariantsBulkCreateStrategy) {
              productVariantsBulkCreate(productId: $productId, variants: $variants, strategy: $strategy) {
                userErrors { field message }
                productVariants {
                  id
                  title
                }
              }
            }
            """),

        LOCATIONS("""
            query locations($first: Int!) {
              locations(first: $first) {
                edges {
                  node {
                    id
                    name
                  }
                }
              }
            }
            """),

        PUBLICATIONS("""
            query publications($first: Int!) {
              publications(first: $first) {
                edges {
                  node {
                    id
                    name
                  }
                }
              }
            }
            """),

        CHANNELS("""
            query channels($first: Int!) {
              channels(first: $first) {
                edges {
                  node {
                    id
                    name
                    isPublished
                  }
                }
              }
            }
            """),

        PUBLISHABLE_PUBLISH("""
            mutation publishablePublish($id: ID!, $input: [PublicationInput!]!) {
              publishablePublish(id: $id, input: $input) {
                userErrors { field message }
                publishable {
                  ... on Product {
                    id
                  }
                }
              }
            }
            """),

        RESOURCE_PUBLICATION_SET("""
            mutation resourcePublicationSet($resourceId: ID!, $publications: [PublicationInput!]!) {
              resourcePublicationSet(resourceId: $resourceId, publications: $publications) {
                userErrors { field message }
                resourcePublications {
                  isPublished
                  publishDate
                  publication {
                    channel {
                      id
                      name
                    }
                  }
                }
              }
            }
            """),

        PUBLICATION_UPDATE("""
            mutation publicationUpdate($id: ID!, $input: PublicationUpdateInput!) {
              publicationUpdate(id: $id, input: $input) {
                userErrors { field message }
                publication {
                  id
                  autoPublish
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

    /**
     * 商品创建结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductCreateResult {
        private String productId;
        private List<String> mediaIds;
    }

    /**
     * 销售渠道信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelInfo {
        private String id;
        private String name;
        private Boolean isPublished;
    }

    /**
     * 库存仓库信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        private String id;
        private String name;
    }

    /**
     * 发布渠道信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationInfo {
        private String id;
        private String name;
    }

    /**
     * 发布渠道输入
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublicationInput {
        private String publicationId;
        private String channelId;
        private Boolean isPublished;
    }

    /**
     * Publication 自动发布设置输入。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublicationUpdateInput {
        private Boolean autoPublish;
    }

    /**
     * 发布结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationResult {
        private String publicationId;
        private String channelId;
        private String channelName;
        private Boolean isPublished;
        private String publishDate;
    }
}

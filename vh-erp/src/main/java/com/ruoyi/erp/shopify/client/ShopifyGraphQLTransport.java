package com.ruoyi.erp.shopify.client;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.model.vo.shopifyStore.ShopifyApiCallResponseVo;
import com.ruoyi.erp.service.IShopifyStoreService;
import com.ruoyi.erp.shopify.constant.ShopifySyncConstants;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.exception.ShopifyTokenExpiredException;
import com.ruoyi.erp.shopify.exception.ShopifyUserError;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Shopify GraphQL 底层传输层，集中处理 HTTP、Token 刷新、限流退避和 userErrors。
 */
@Slf4j
@Component
public class ShopifyGraphQLTransport {
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_VARIABLES = "variables";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_ERRORS = "errors";
    private static final String PATH_USER_ERRORS = "userErrors";
    private static final String PATH_MESSAGE = "message";
    private static final int THROTTLE_MIN_AVAILABLE = 100;

    @Resource
    private IShopifyStoreService shopifyStoreService;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 每个店铺独立的 WebClient 缓存，避免每次创建。
     */
    private final ConcurrentMap<Long, WebClient> webClientCache = new ConcurrentHashMap<>();

    /**
     * Token 刷新锁的过期时间（秒）。
     */
    private static final int TOKEN_REFRESH_LOCK_EXPIRE = 10;

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
    ShopifyStore getStore(Long storeId) {
        ShopifyStore store = shopifyStoreService.selectByStoreId(storeId);
        if (store == null) {
            throw new ShopifyApiException("店铺不存在: storeId=" + storeId);
        }
        return store;
    }

    /**
     * 清除 WebClient 缓存（Token 刷新后调用）。
     */
    private void invalidateCache(Long storeId) {
        webClientCache.remove(storeId);
        log.info("已清理 WebClient 缓存: storeId={}", storeId);
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

    private Map<String, Object> buildRequestBody(String query, Object variables) {
        return Map.of(FIELD_QUERY, query, FIELD_VARIABLES, variables != null ? variables : Map.of());
    }

    /**
     * 调用当前店铺 Shopify Admin API，保留 HTTP 状态和响应体给调试窗口展示。
     */
    public ShopifyApiCallResponseVo callAdminApi(Long storeId, String method, String url, Object body) {
        long startTime = System.currentTimeMillis();
        ShopifyApiCallResponseVo response = doCallAdminApi(storeId, method, url, body);
        if (response != null && response.getStatusCode() != null && response.getStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
            refreshTokenIfNeeded(storeId);
            response = doCallAdminApi(storeId, method, url, body);
        }
        if (response != null) {
            response.setDurationMs(System.currentTimeMillis() - startTime);
        }
        return response;
    }

    private ShopifyApiCallResponseVo doCallAdminApi(Long storeId, String method, String url, Object body) {
        try {
            WebClient.RequestBodySpec requestSpec = getWebClient(storeId)
                    .method(HttpMethod.valueOf(method))
                    .uri(URI.create(url))
                    .accept(MediaType.APPLICATION_JSON);
            WebClient.RequestHeadersSpec<?> headersSpec = requestSpec;
            if (body != null && !HttpMethod.GET.name().equals(method)) {
                headersSpec = requestSpec.contentType(MediaType.APPLICATION_JSON).bodyValue(body);
            }
            return headersSpec.exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(rawBody -> {
                        ShopifyApiCallResponseVo response = new ShopifyApiCallResponseVo();
                        response.setStatusCode(clientResponse.statusCode().value());
                        response.setContentType(clientResponse.headers().contentType().map(MediaType::toString).orElse(""));
                        response.setRawBody(rawBody);
                        response.setBody(parseApiCallBody(response.getContentType(), rawBody));
                        return response;
                    }))
                    .block();
        } catch (Exception e) {
            throw new ShopifyApiException("Shopify API 调用异常: " + e.getMessage(), e);
        }
    }

    private Object parseApiCallBody(String contentType, String rawBody) {
        if (StringUtils.isEmpty(rawBody)) {
            return "";
        }
        if (StringUtils.isNotEmpty(contentType) && contentType.toLowerCase().contains("json")) {
            try {
                return JSON.parse(rawBody);
            } catch (Exception ignored) {
                return rawBody;
            }
        }
        return rawBody;
    }

    /**
     * 实际执行 GraphQL 请求
     */
    private JsonNode doExecute(Long storeId, Map<String, Object> requestBody) {
        // 对于网络异常，最多重试 2 次。
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
                // 如果是 WebClientResponseException，直接抛出。
                if (cause instanceof WebClientResponseException webException) {
                    log.error("GraphQL 请求失败，状态码: {}, 响应: {}",
                        webException.getStatusCode(), webException.getResponseBodyAsString());
                    throw new ShopifyApiException(
                        "Shopify API HTTP 错误: " + webException.getStatusCode() + " " + webException.getResponseBodyAsString(),
                        webException
                    );
                }
                // 如果是 Token 过期异常，直接抛出。
                if (e instanceof ShopifyTokenExpiredException) {
                    throw e;
                }
                // 检查是否是网络连接异常（可重试）。
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
     * 判断是否为可重试的网络异常。
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
     * 刷新 Token（带分布式锁）。
     */
    private void refreshTokenIfNeeded(Long storeId) {
        String lockKey = ShopifySyncConstants.TOKEN_REFRESH_LOCK_KEY_PREFIX + storeId;
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
     * 处理响应，检查错误。
     */
    private JsonNode processResponse(JsonNode response) {
        if (response == null) {
            throw new ShopifyApiException("GraphQL 返回空响应");
        }
        if (response.has(FIELD_ERRORS)) {
            throw new ShopifyApiException("GraphQL 错误: " + response.get(FIELD_ERRORS).toString());
        }
        applyThrottleBackoff(response);
        return response.get(FIELD_DATA);
    }

    private void applyThrottleBackoff(JsonNode response) {
        JsonNode throttleStatus = response.path("extensions").path("cost").path("throttleStatus");
        if (throttleStatus.isMissingNode()) {
            return;
        }
        int currentlyAvailable = throttleStatus.path("currentlyAvailable").asInt(THROTTLE_MIN_AVAILABLE);
        double restoreRate = throttleStatus.path("restoreRate").asDouble(50.0D);
        if (currentlyAvailable >= THROTTLE_MIN_AVAILABLE || restoreRate <= 0) {
            return;
        }
        // Shopify GraphQL 返回 cost/throttleStatus 时，在额度较低处主动短退避，减少连续分页触发限流。
        long sleepMillis = Math.min(5_000L, Math.max(500L,
                (long) Math.ceil((THROTTLE_MIN_AVAILABLE - currentlyAvailable) / restoreRate * 1000)));
        try {
            log.info("Shopify GraphQL cost 剩余额度较低，退避 {}ms", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShopifyApiException("Shopify GraphQL 限流退避被中断", e);
        }
    }

    /**
     * 检查 userErrors
     */
    public void checkUserErrors(JsonNode data, String mutationName) {
        JsonNode userErrors = data.path(mutationName).path(PATH_USER_ERRORS);
        if (userErrors.isArray() && !userErrors.isEmpty()) {
            List<String> errorList = new ArrayList<>();
            List<ShopifyUserError> structuredErrors = new ArrayList<>();
            for (JsonNode err : userErrors) {
                String message = err.path(PATH_MESSAGE).asText();
                String field = resolveUserErrorField(err.path("field"));
                String code = err.path("code").asText(null);
                errorList.add(StringUtils.isEmpty(field) ? message : field + ": " + message);
                structuredErrors.add(new ShopifyUserError(field, message, code, resolveInputIndex(field)));
            }
            throw new ShopifyApiException(String.join("; ", errorList), structuredErrors);
        }
    }

    private String resolveUserErrorField(JsonNode fieldNode) {
        if (fieldNode == null || fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        if (fieldNode.isArray()) {
            List<String> path = new ArrayList<>();
            for (JsonNode item : fieldNode) {
                path.add(item.asText());
            }
            return String.join(".", path);
        }
        return fieldNode.asText(null);
    }

    private Integer resolveInputIndex(String field) {
        if (StringUtils.isEmpty(field)) {
            return null;
        }
        String[] parts = field.split("\\.");
        for (String part : parts) {
            try {
                return Integer.parseInt(part);
            } catch (NumberFormatException ignored) {
                // Shopify 字段路径中的第一个数字就是批量输入列表下标。
            }
        }
        return null;
    }

}

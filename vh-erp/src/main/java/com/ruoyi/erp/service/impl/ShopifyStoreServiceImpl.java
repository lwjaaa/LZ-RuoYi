package com.ruoyi.erp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.exception.ShopifyApiException;
import com.ruoyi.erp.mapper.ShopifyStoreMapper;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.service.IShopifyStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Shopify 店铺配置Service实现
 */
@Slf4j
@Service
public class ShopifyStoreServiceImpl extends ServiceImpl<ShopifyStoreMapper, ShopifyStore> implements IShopifyStoreService {

    private static final String STATUS_CONNECTED = "CONNECTED";
    private static final String STATUS_DISCONNECTED = "DISCONNECTED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_TOKEN_EXPIRING_SOON = "TOKEN_EXPIRING_SOON";

    private static final long RETRY_MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_SECONDS = 2;

    @Override
    public ShopifyStore selectDefaultStore() {
        return baseMapper.selectDefaultStore();
    }

    @Override
    public List<ShopifyStore> selectActiveStores() {
        return baseMapper.selectActiveStores();
    }

    @Override
    public ShopifyStore selectByShopName(String shopName) {
        return baseMapper.selectByShopName(shopName);
    }

    @Override
    public ShopifyStore selectByStoreId(Long storeId) {
        return baseMapper.selectById(storeId);
    }

    @Override
    public boolean refreshToken(Long storeId) {
        ShopifyStore store = baseMapper.selectById(storeId);
        if (store == null) {
            log.error("店铺不存在: storeId={}", storeId);
            return false;
        }

        // Private App 模式：token 是永久的，不需要刷新
        if ("PRIVATE_APP".equals(store.getAuthMode())) {
            log.debug("Private App 模式，token 不需要刷新: storeId={}", storeId);
            return true;
        }

        // OAuth 模式：调用 Shopify OAuth API 刷新 token
        if ("OAUTH".equals(store.getAuthMode())) {
            return refreshOAuthToken(store);
        }

        log.warn("未知的认证模式: authMode={}, storeId={}", store.getAuthMode(), storeId);
        return false;
    }

    /**
     * OAuth 模式刷新 token
     */
    private boolean refreshOAuthToken(ShopifyStore store) {
        if (StringUtils.isEmpty(store.getShopName())
                || StringUtils.isEmpty(store.getApiKey())
                || StringUtils.isEmpty(store.getApiSecret())
                || StringUtils.isEmpty(store.getRefreshToken())) {
            log.error("OAuth 刷新 token 失败：缺少必要配置, storeId={}", store.getStoreId());
            updateStatus(store.getStoreId(), STATUS_DISCONNECTED);
            return false;
        }

        String tokenUrl = String.format("https://%s.myshopify.com/admin/oauth/access_token", store.getShopName());

        try {
            Map<String, String> requestBody = Map.of(
                    "client_id", store.getApiKey(),
                    "client_secret", store.getApiSecret(),
                    "refresh_token", store.getRefreshToken()
            );

            String response = WebClient.builder()
                    .build()
                    .post()
                    .uri(URI.create(tokenUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, Duration.ofSeconds(RETRY_BACKOFF_SECONDS))
                            .onRetryExhaustedThrow((spec, ex) -> new ShopifyApiException("OAuth token 刷新请求失败: " + ex.failure().getMessage(), ex.failure())))
                    .block();

            if (StringUtils.isEmpty(response)) {
                log.error("OAuth token 刷新失败：响应为空, storeId={}", store.getStoreId());
                updateStatus(store.getStoreId(), STATUS_EXPIRED);
                return false;
            }

            JSONObject jsonResponse = JSON.parseObject(response);
            String newAccessToken = jsonResponse.getString("access_token");
            Integer expiresIn = jsonResponse.getInteger("expires_in");

            if (StringUtils.isEmpty(newAccessToken)) {
                log.error("OAuth token 刷新失败：响应中未包含 access_token, storeId={}", store.getStoreId());
                updateStatus(store.getStoreId(), STATUS_EXPIRED);
                return false;
            }

            // 更新 token 信息
            updateTokenInfo(store.getStoreId(), newAccessToken, expiresIn != null ? Long.valueOf(expiresIn) : null);
            updateStatus(store.getStoreId(), STATUS_CONNECTED);

            log.info("OAuth token 刷新成功: storeId={}, expiresIn={}s", store.getStoreId(), expiresIn);
            return true;

        } catch (Exception e) {
            log.error("OAuth token 刷新异常: storeId={}", store.getStoreId(), e);
            updateStatus(store.getStoreId(), STATUS_EXPIRED);
            return false;
        }
    }

    @Override
    public void updateTokenInfo(Long storeId, String accessToken, Long expiresInSeconds) {
        ShopifyStore store = new ShopifyStore();
        store.setStoreId(storeId);
        store.setAccessToken(accessToken);
        if (expiresInSeconds != null) {
            store.setTokenExpiresAt(new Date(System.currentTimeMillis() + expiresInSeconds * 1000));
        }
        store.setUpdateTime(DateUtils.getNowDate());
        baseMapper.updateById(store);
        log.info("更新店铺 token 成功: storeId={}, expiresIn={}s", storeId, expiresInSeconds);
    }

    @Override
    public void updateStatus(Long storeId, String status) {
        ShopifyStore store = new ShopifyStore();
        store.setStoreId(storeId);
        store.setStatus(status);
        store.setUpdateTime(DateUtils.getNowDate());
        baseMapper.updateById(store);
        log.info("更新店铺状态: storeId={}, status={}", storeId, status);
    }

    @Override
    public boolean isTokenExpiringSoon(Long storeId) {
        ShopifyStore store = baseMapper.selectById(storeId);
        if (store == null) {
            return false;
        }
        return store.isTokenExpiringSoon();
    }

    @Override
    public boolean isConnected(Long storeId) {
        ShopifyStore store = baseMapper.selectById(storeId);
        if (store == null) {
            return false;
        }
        return STATUS_CONNECTED.equals(store.getStatus());
    }
}
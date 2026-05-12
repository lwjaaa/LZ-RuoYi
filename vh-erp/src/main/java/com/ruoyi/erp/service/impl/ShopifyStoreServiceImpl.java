package com.ruoyi.erp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.config.ShopifyGraphQLClient;
import com.ruoyi.erp.constant.StoreConstants;
import com.ruoyi.erp.exception.ShopifyApiException;
import com.ruoyi.erp.mapper.ShopifyStoreMapper;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyStoreQuery;
import com.ruoyi.erp.model.vo.shopifyStore.ShopifyResourceOptionVo;
import com.ruoyi.erp.model.vo.shopifyStore.ShopifyStoreVo;
import com.ruoyi.erp.service.IShopifyStoreService;
import com.ruoyi.erp.util.ProductListMetrics;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final String DEFAULT_API_VERSION = "2026-04";

    @Lazy
    @Resource
    private ShopifyGraphQLClient shopifyGraphQLClient;

    @Override
    public List<ShopifyStore> selectShopifyStoreList(ShopifyStoreQuery query) {
        ShopifyStore store = ShopifyStoreQuery.queryToObj(query);
        LambdaQueryWrapper<ShopifyStore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopifyStore::getDelFlag, StoreConstants.DEL_FLAG_NORMAL)
                .like(store != null && StringUtils.isNotEmpty(store.getStoreName()), ShopifyStore::getStoreName, store != null ? store.getStoreName() : null)
                .like(store != null && StringUtils.isNotEmpty(store.getShopName()), ShopifyStore::getShopName, store != null ? store.getShopName() : null)
                .eq(store != null && StringUtils.isNotEmpty(store.getIsActive()), ShopifyStore::getIsActive, store != null ? store.getIsActive() : null)
                .eq(store != null && StringUtils.isNotEmpty(store.getIsDefault()), ShopifyStore::getIsDefault, store != null ? store.getIsDefault() : null)
                .eq(store != null && StringUtils.isNotEmpty(store.getAuthMode()), ShopifyStore::getAuthMode, store != null ? store.getAuthMode() : null)
                .eq(store != null && StringUtils.isNotEmpty(store.getStatus()), ShopifyStore::getStatus, store != null ? store.getStatus() : null)
                .orderByDesc(ShopifyStore::getIsDefault)
                .orderByAsc(ShopifyStore::getStoreId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertShopifyStore(ShopifyStore store) {
        normalizeAndValidate(store, false);
        store.setCreateTime(DateUtils.getNowDate());
        store.setDelFlag(StoreConstants.DEL_FLAG_NORMAL);
        if (StoreConstants.YES.equals(store.getIsDefault())) {
            resetDefaultStore(null);
        }
        int rows = baseMapper.insert(store);
        syncAutoPublishPublications(store);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateShopifyStore(ShopifyStore store) {
        if (store == null || store.getStoreId() == null) {
            throw new ServiceException("店铺ID不能为空");
        }
        ShopifyStore oldStore = baseMapper.selectById(store.getStoreId());
        if (oldStore == null || StoreConstants.DEL_FLAG_DELETED.equals(oldStore.getDelFlag())) {
            throw new ServiceException("店铺不存在");
        }
        keepOldSensitiveValue(store, oldStore);
        normalizeAndValidate(store, true);
        store.setUpdateTime(DateUtils.getNowDate());
        if (StoreConstants.YES.equals(store.getIsDefault())) {
            resetDefaultStore(store.getStoreId());
        }
        int rows = baseMapper.updateById(store);
        shopifyGraphQLClient.invalidateStoreCache(store.getStoreId());
        syncAutoPublishPublications(store);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopifyStoreByStoreIds(Long[] storeIds) {
        if (storeIds == null || storeIds.length == 0) {
            return 0;
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<ShopifyStore>()
                .set(ShopifyStore::getDelFlag, StoreConstants.DEL_FLAG_DELETED)
                .set(ShopifyStore::getUpdateTime, DateUtils.getNowDate())
                .in(ShopifyStore::getStoreId, List.of(storeIds)));
    }

    @Override
    public List<ShopifyStoreVo> convertVoList(List<ShopifyStore> storeList) {
        if (storeList == null || storeList.isEmpty()) {
            return List.of();
        }
        return storeList.stream().map(store -> {
            ShopifyStoreVo vo = ShopifyStoreVo.objToVo(store);
            vo.setApiSecret(null);
            vo.setAccessToken(null);
            return vo;
        }).collect(Collectors.toList());
    }

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

        // OAuth 模式：调用 Shopify OAuth API 刷新 token
        if (StoreConstants.AUTH_MODE_OAUTH.equals(store.getAuthMode())) {
            return refreshOAuthToken(store);
        }

        // Private App 模式：token 是永久的，不需要刷新
        if (StoreConstants.AUTH_MODE_PRIVATE.equals(store.getAuthMode())) {
            log.debug("Private App 模式，token 不需要刷新: storeId={}", storeId);
            return true;
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
                || StringUtils.isEmpty(store.getApiVersion())) {
            log.error("OAuth 刷新 token 失败：缺少必要配置, storeId={}", store.getStoreId());
            updateStatus(store.getStoreId(), STATUS_DISCONNECTED);
            return false;
        }

        String tokenUrl = String.format("https://%s.myshopify.com/admin/oauth/access_token", store.getShopName());

        try {
            Map<String, String> requestBody = Map.of(
                    "client_id", store.getApiKey(),
                    "client_secret", store.getApiSecret(),
                    "grant_type", "client_credentials"
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
        store.setStatus(STATUS_CONNECTED);
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

    @Override
    public boolean testConnection(Long storeId) {
        shopifyGraphQLClient.testConnection(storeId);
        updateStatus(storeId, STATUS_CONNECTED);
        return true;
    }

    @Override
    public List<ShopifyResourceOptionVo> fetchLocations(Long storeId) {
        return shopifyGraphQLClient.getLocations(storeId).stream()
                .map(location -> new ShopifyResourceOptionVo(location.getId(), location.getName()))
                .toList();
    }

    @Override
    public List<ShopifyResourceOptionVo> fetchPublications(Long storeId) {
        return shopifyGraphQLClient.getPublications(storeId).stream()
                .map(publication -> new ShopifyResourceOptionVo(publication.getId(), publication.getName()))
                .toList();
    }

    private void normalizeAndValidate(ShopifyStore store, boolean edit) {
        if (store == null) {
            throw new ServiceException("店铺信息不能为空");
        }
        if (StringUtils.isEmpty(store.getStoreName())) {
            throw new ServiceException("店铺名称不能为空");
        }
        if (StringUtils.isEmpty(store.getShopName())) {
            throw new ServiceException("Shop Name不能为空");
        }

        store.setShopName(normalizeShopName(store.getShopName()));
        if (StringUtils.isEmpty(store.getApiVersion())) {
            store.setApiVersion(DEFAULT_API_VERSION);
        }
        if (StringUtils.isEmpty(store.getAuthMode())) {
            store.setAuthMode(StoreConstants.AUTH_MODE_PRIVATE);
        }
        if (StringUtils.isEmpty(store.getIsActive())) {
            store.setIsActive(StoreConstants.YES);
        }
        if (StringUtils.isEmpty(store.getIsDefault())) {
            store.setIsDefault(StoreConstants.NO);
        }
        if (StringUtils.isEmpty(store.getInventoryTracked())) {
            store.setInventoryTracked(StoreConstants.NO);
        }
        if (store.getDefaultInventoryQuantity() == null) {
            store.setDefaultInventoryQuantity(100);
        }
        if (store.getDefaultInventoryQuantity() < 0) {
            throw new ServiceException("默认库存数量不能小于0");
        }
        if (StringUtils.isEmpty(store.getInventoryPolicy())) {
            store.setInventoryPolicy(StoreConstants.INVENTORY_POLICY_DENY);
        }
        if (!StoreConstants.INVENTORY_POLICY_DENY.equals(store.getInventoryPolicy())
                && !StoreConstants.INVENTORY_POLICY_CONTINUE.equals(store.getInventoryPolicy())) {
            throw new ServiceException("缺货销售策略不正确");
        }
        if (StoreConstants.YES.equals(store.getInventoryTracked())
                && StringUtils.isEmpty(store.getInventoryLocationId())) {
            throw new ServiceException("启用库存跟踪时必须配置库存仓库 Location ID");
        }
        if (StringUtils.isEmpty(store.getStatus())) {
            store.setStatus(StringUtils.isNotEmpty(store.getAccessToken()) ? STATUS_CONNECTED : STATUS_DISCONNECTED);
        }
        store.setDefaultProductStatus(normalizeProductStatus(store.getDefaultProductStatus()));
        store.setRequiredProductFields(ProductListMetrics.normalizeRequiredFields(store.getRequiredProductFields()));
        store.setPublishPublicationIds(normalizeCsv(store.getPublishPublicationIds()));
        store.setAvailablePublicationIds(normalizeCsv(store.getAvailablePublicationIds()));

        ensureUniqueShopName(store, edit);
    }

    private String normalizeProductStatus(String status) {
        if (StringUtils.isEmpty(status)) {
            return StoreConstants.PRODUCT_STATUS_DRAFT;
        }
        String normalized = status.trim().toUpperCase();
        if (!StoreConstants.PRODUCT_STATUS_DRAFT.equals(normalized)
                && !StoreConstants.PRODUCT_STATUS_ACTIVE.equals(normalized)) {
            throw new ServiceException("默认商品状态只允许 DRAFT 或 ACTIVE");
        }
        return normalized;
    }

    private void syncAutoPublishPublications(ShopifyStore store) {
        Set<String> selectedIds = parseCsvToSet(store.getPublishPublicationIds());
        Set<String> availableIds = parseCsvToSet(store.getAvailablePublicationIds());
        Set<String> syncIds = new LinkedHashSet<>(availableIds.isEmpty() ? selectedIds : availableIds);
        syncIds.addAll(selectedIds);
        if (syncIds.isEmpty()) {
            return;
        }
        Map<String, String> selectedNames = buildSelectedPublicationNameMap(store);
        for (String publicationId : syncIds) {
            boolean autoPublish = selectedIds.contains(publicationId);
            try {
                shopifyGraphQLClient.updatePublicationAutoPublish(store.getStoreId(), publicationId, autoPublish);
            } catch (ShopifyApiException e) {
                String publicationName = selectedNames.getOrDefault(publicationId, publicationId);
                throw new ServiceException("同步自动发布渠道失败：" + publicationName + "，" + e.getMessage());
            } catch (Exception e) {
                String publicationName = selectedNames.getOrDefault(publicationId, publicationId);
                throw new ServiceException("同步自动发布渠道失败：" + publicationName + "，" + e.getMessage());
            }
        }
    }

    private Map<String, String> buildSelectedPublicationNameMap(ShopifyStore store) {
        List<String> ids = splitCsv(store.getPublishPublicationIds());
        List<String> names = splitCsv(store.getPublishPublicationNames());
        return ids.stream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    int index = ids.indexOf(id);
                    return index >= 0 && index < names.size() ? names.get(index) : id;
                },
                (left, right) -> left
        ));
    }

    private Set<String> parseCsvToSet(String value) {
        return new LinkedHashSet<>(splitCsv(value));
    }

    private List<String> splitCsv(String value) {
        if (StringUtils.isEmpty(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .toList();
    }

    private String normalizeCsv(String value) {
        return String.join(",", splitCsv(value));
    }

    private String normalizeShopName(String shopName) {
        String normalized = shopName.trim();
        normalized = normalized.replace("https://", "").replace("http://", "");
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(".myshopify.com")) {
            normalized = normalized.substring(0, normalized.length() - ".myshopify.com".length());
        }
        return normalized;
    }

    private void ensureUniqueShopName(ShopifyStore store, boolean edit) {
        LambdaQueryWrapper<ShopifyStore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopifyStore::getShopName, store.getShopName())
                .eq(ShopifyStore::getDelFlag, StoreConstants.DEL_FLAG_NORMAL);
        if (edit) {
            wrapper.ne(ShopifyStore::getStoreId, store.getStoreId());
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("Shop Name已存在");
        }
    }

    private void keepOldSensitiveValue(ShopifyStore store, ShopifyStore oldStore) {
        if (StringUtils.isEmpty(store.getApiSecret())) {
            store.setApiSecret(oldStore.getApiSecret());
        }
        if (StringUtils.isEmpty(store.getAccessToken())) {
            store.setAccessToken(oldStore.getAccessToken());
        }
        if (StringUtils.isEmpty(store.getRefreshToken())) {
            store.setRefreshToken(oldStore.getRefreshToken());
        }
    }

    private void resetDefaultStore(Long excludeStoreId) {
        LambdaUpdateWrapper<ShopifyStore> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(ShopifyStore::getIsDefault, StoreConstants.NO)
                .set(ShopifyStore::getUpdateTime, DateUtils.getNowDate())
                .eq(ShopifyStore::getDelFlag, StoreConstants.DEL_FLAG_NORMAL);
        if (excludeStoreId != null) {
            wrapper.ne(ShopifyStore::getStoreId, excludeStoreId);
        }
        baseMapper.update(null, wrapper);
    }
}

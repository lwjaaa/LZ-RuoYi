package com.ruoyi.erp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.shopify.client.ShopifyGraphQLClient;
import com.ruoyi.erp.shopify.constant.ShopifySyncConstants;
import com.ruoyi.erp.shopify.enums.*;
import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.mapper.*;
import com.ruoyi.erp.model.domain.*;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedMedia;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedVariant;
import com.ruoyi.erp.service.*;
import com.ruoyi.erp.shopify.model.BulkOperationInfo;
import com.ruoyi.erp.shopify.model.ProductPage;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Shopify 商品反向导入服务。
 * 负责按店铺维度导入远端商品，并维护增量游标。
 */
@Slf4j
@Service
public class ShopifyProductImportServiceImpl implements IShopifyProductImportService {

    private static final String CURSOR_MODE_PRODUCT_IMPORT = ShopifySyncCursorMode.PRODUCT_IMPORT.getCode();
    private static final String CURSOR_STATUS_RUNNING = ShopifySyncCursorStatus.RUNNING.getCode();
    private static final String CURSOR_STATUS_SUCCESS = ShopifySyncCursorStatus.SUCCESS.getCode();
    private static final String CURSOR_STATUS_FAILED = ShopifySyncCursorStatus.FAILED.getCode();
    private static final String CURSOR_STATUS_PART_SUCCESS = ShopifySyncCursorStatus.PART_SUCCESS.getCode();
    private static final int PAGE_SIZE = ShopifySyncConstants.PRODUCT_IMPORT_PAGE_SIZE;
    private static final long SAFETY_WINDOW_MINUTES = ShopifySyncConstants.PRODUCT_IMPORT_SAFETY_WINDOW_MINUTES;

    @Resource
    private ShopifyGraphQLClient shopifyGraphQLClient;
    @Resource
    private IShopifyStoreService shopifyStoreService;
    @Resource
    private IShopifyTaskService shopifyTaskService;
    @Resource
    private IShopifyTaskDetailService shopifyTaskDetailService;
    @Resource
    private ShopifySyncCursorMapper syncCursorMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private ProductVariantMapper productVariantMapper;
    @Resource
    private MediaMapper mediaMapper;
    @Resource
    private IProductQualityService productQualityService;
    @Resource(name = "shopifySyncThreadPool")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public Long startFullImport(Long storeId) {
        return createTaskAndSubmit(storeId, ShopifyTaskType.PRODUCT_IMPORT_FULL.getCode());
    }

    @Override
    public Long startIncrementalImport(Long storeId) {
        return createTaskAndSubmit(storeId, ShopifyTaskType.PRODUCT_IMPORT_INCREMENTAL.getCode());
    }

    @Override
    public void syncIncrementalAllActiveStores() {
        List<ShopifyStore> stores = shopifyStoreService.selectActiveStores();
        if (stores == null || stores.isEmpty()) {
            log.info("Shopify 商品增量导入跳过：没有启用店铺");
            return;
        }
        log.info("开始为启用店铺创建 Shopify 商品增量导入任务，店铺数量={}", stores.size());
        for (ShopifyStore store : stores) {
            if (store == null || store.getStoreId() == null) {
                continue;
            }
            try {
                startIncrementalImport(store.getStoreId());
            } catch (Exception e) {
                log.warn("Shopify 商品增量导入任务创建失败，storeId={}", store.getStoreId(), e);
            }
        }
    }

    @Override
    public ShopifySyncCursor getCursor(Long storeId) {
        return getOrCreateCursor(storeId);
    }

    private Long createTaskAndSubmit(Long storeId, String taskType) {
        ShopifyStore store = resolveActiveStore(storeId);
        ensureNoRunningTask(store.getStoreId(), taskType);

        // 先落任务摘要，再异步执行；前端可通过 taskId 进入诊断页追踪结果。
        ShopifyTask task = new ShopifyTask();
        task.setStoreId(store.getStoreId());
        task.setShopName(store.getShopName());
        task.setTaskType(taskType);
        task.setTaskName(buildTaskName(taskType));
        task.setTaskStatus(ShopifyTaskStatus.PENDING.getCode());
        task.setProgress(0);
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setPartialCount(0);
        task.setFailedCount(0);
        task.setCreateTime(new Date());
        shopifyTaskService.insertShopifyTask(task);

        taskExecutor.execute(() -> executeImportTask(task.getTaskId(), taskType));
        log.info("Shopify 商品导入任务已创建，taskId={}, storeId={}, shopName={}, taskType={}",
                task.getTaskId(), store.getStoreId(), store.getShopName(), taskType);
        return task.getTaskId();
    }

    private ShopifyStore resolveActiveStore(Long storeId) {
        ShopifyStore store = storeId == null ? shopifyStoreService.selectDefaultStore() : shopifyStoreService.selectByStoreId(storeId);
        if (store == null) {
            throw new ServiceException("未找到可用 Shopify 店铺");
        }
        if ("0".equals(store.getIsActive())) {
            throw new ServiceException("Shopify 店铺已禁用");
        }
        return store;
    }

    private void ensureNoRunningTask(Long storeId, String taskType) {
        long running = shopifyTaskService.count(new LambdaQueryWrapper<ShopifyTask>()
                .eq(ShopifyTask::getStoreId, storeId)
                .eq(ShopifyTask::getTaskType, taskType)
                .in(ShopifyTask::getTaskStatus,
                        ShopifyTaskStatus.PENDING.getCode(),
                        ShopifyTaskStatus.RUNNING.getCode()));
        if (running > 0) {
            throw new ServiceException("当前店铺已有同类型 Shopify 商品导入任务正在执行");
        }
    }

    private String buildTaskName(String taskType) {
        String label = ShopifyTaskType.PRODUCT_IMPORT_FULL.getCode().equals(taskType) ? "全量导入" : "增量导入";
        return "Shopify 商品" + label + " " + new Date();
    }

    public void executeImportTask(Long taskId, String taskType) {
        long startTime = System.currentTimeMillis();
        ShopifyTask task = shopifyTaskService.selectShopifyTaskByTaskId(taskId);
        if (task == null) {
            log.error("Shopify 商品导入任务不存在，taskId={}", taskId);
            return;
        }
        ShopifyStore store = shopifyStoreService.selectByStoreId(task.getStoreId());
        if (store == null) {
            failTask(task, startTime, "Shopify 店铺不存在");
            return;
        }

        ShopifySyncCursor cursor = getOrCreateCursor(store.getStoreId());
        markTaskRunning(task);
        markCursorRunning(cursor, taskId);

        log.info("开始执行 Shopify 商品导入任务，taskId={}, storeId={}, shopName={}, taskType={}, cursorUpdatedAt={}",
                taskId, store.getStoreId(), store.getShopName(), taskType, cursor.getLastSuccessUpdatedAt());
        try {
            ImportSummary summary = ShopifyTaskType.PRODUCT_IMPORT_FULL.getCode().equals(taskType)
                    ? runFullImport(task, store, cursor)
                    : runIncrementalImport(task, store, cursor);
            finishTask(task, cursor, summary, startTime);
        } catch (Exception e) {
            log.error("Shopify 商品导入任务失败，taskId={}", taskId, e);
            failTask(task, startTime, e.getMessage());
            failCursor(cursor, taskId, e.getMessage());
        }
    }

    private ImportSummary runFullImport(ShopifyTask task, ShopifyStore store, ShopifySyncCursor cursor) {
        // 大店铺首次导入走 Bulk Operation，避免普通分页长时间占用 GraphQL cost。
        log.info("开始 Shopify 商品全量导入 Bulk Operation，taskId={}, storeId={}", task.getTaskId(), store.getStoreId());
        BulkOperationInfo operation = shopifyGraphQLClient.startProductBulkImport(store.getStoreId());
        cursor.setLastBulkOperationId(operation.getId());
        cursor.setUpdateTime(new Date());
        syncCursorMapper.updateById(cursor);
        log.info("Shopify 商品全量导入 Bulk Operation 已创建，taskId={}, operationId={}", task.getTaskId(), operation.getId());

        BulkOperationInfo completed = shopifyGraphQLClient.waitForBulkOperation(
                store.getStoreId(),
                operation.getId(),
                Duration.ofHours(2)
        );
        if (!"COMPLETED".equalsIgnoreCase(completed.getStatus())) {
            throw new ServiceException("Shopify Bulk Operation 未完成：" + completed.getStatus());
        }
        if (StringUtils.isEmpty(completed.getUrl())) {
            throw new ServiceException("Shopify Bulk Operation 未返回下载地址");
        }
        List<ShopifyImportedProduct> products = shopifyGraphQLClient.downloadBulkJsonlProducts(completed.getUrl());
        log.info("Shopify 商品全量导入 Bulk Operation 下载完成，taskId={}, operationId={}, productCount={}",
                task.getTaskId(), completed.getId(), products.size());
        return processProducts(task, store, products);
    }

    private ImportSummary runIncrementalImport(ShopifyTask task, ShopifyStore store, ShopifySyncCursor cursor) {
        Date since = calculateQuerySince(cursor);
        String pageCursor = null;
        ImportSummary total = new ImportSummary();
        int pageNo = 0;
        log.info("开始 Shopify 商品增量导入，taskId={}, storeId={}, since={}", task.getTaskId(), store.getStoreId(), since);
        do {
            pageNo++;
            // 查询分页
            ProductPage page = shopifyGraphQLClient.queryUpdatedProducts(store.getStoreId(), since, pageCursor, PAGE_SIZE);
            int pageSize = page.getProducts() == null ? 0 : page.getProducts().size();
            log.info("Shopify 商品增量导入分页返回，taskId={}, pageNo={}, pageSize={}, hasNextPage={}",
                    task.getTaskId(), pageNo, pageSize, page.isHasNextPage());
            // 处理分页
            ImportSummary pageSummary = processProducts(task, store, page.getProducts());
            total.merge(pageSummary);
            pageCursor = page.getEndCursor();
            if (!page.isHasNextPage()) {
                break;
            }
        } while (StringUtils.isNotEmpty(pageCursor));
        return total;
    }

    /**
     * 计算从 Shopify 的哪个更新时间开始同步。
     *
     * @param cursor
     * @return java.util.Date
     * @author lwj
     **/
    Date calculateQuerySince(ShopifySyncCursor cursor) {
        if (cursor == null || cursor.getLastSuccessUpdatedAt() == null) {
            return null;
        }
        // 游标回退 5 分钟，覆盖 Shopify 与本地时钟误差、分页边界和网络重试造成的临界漏单。
        Instant since = cursor.getLastSuccessUpdatedAt().toInstant().minus(SAFETY_WINDOW_MINUTES, ChronoUnit.MINUTES);
        return Date.from(since);
    }

    private ImportSummary processProducts(ShopifyTask task, ShopifyStore store, List<ShopifyImportedProduct> products) {
        ImportSummary summary = new ImportSummary();
        if (products == null || products.isEmpty()) {
            log.info("Shopify 商品导入批次为空，taskId={}, storeId={}", task.getTaskId(), store.getStoreId());
            task.setTotalCount(0);
            task.setProgress(100);
            shopifyTaskService.updateShopifyTask(task);
            return summary;
        }

        task.setTotalCount((task.getTotalCount() == null ? 0 : task.getTotalCount()) + products.size());
        shopifyTaskService.updateShopifyTask(task);

        ImportContext context = new ImportContext(task.getTaskId(), store.getStoreId(), store.getShopName(), task.getTaskType());
        int processed = 0;
        int baseSuccess = task.getSuccessCount() == null ? 0 : task.getSuccessCount();
        int baseFailed = task.getFailedCount() == null ? 0 : task.getFailedCount();
        for (ShopifyImportedProduct product : products) {
            processed++;
            try {
                ProductImportItemResult result = upsertProduct(product, context);
                if (result.isSuccess()) {
                    summary.success++;
                    if (result.isConflict()) {
                        log.warn("Shopify 商品导入检测到 ERP 优先冲突，taskId={}, storeId={}, productId={}, shopifyProductId={}",
                                task.getTaskId(), store.getStoreId(), result.getProductId(), product == null ? null : product.getId());
                    }
                } else {
                    summary.failed++;
                }
                summary.maxUpdatedAt = maxDate(summary.maxUpdatedAt, product.getUpdatedAt());
            } catch (Exception e) {
                summary.failed++;
                summary.errorSummary.append("商品 ")
                        .append(product == null ? "unknown" : product.getId())
                        .append(" 导入失败：")
                        .append(e.getMessage())
                        .append("\n");
                recordProductFailure(context, product, e.getMessage());
                log.warn("Shopify 商品导入单品失败，taskId={}, storeId={}, shopifyProductId={}, message={}",
                        task.getTaskId(), store.getStoreId(), product == null ? null : product.getId(), e.getMessage(), e);
            }
            task.setProgress(Math.min(99, (int) (processed * 100.0 / products.size())));
            task.setSuccessCount(baseSuccess + summary.success);
            task.setFailedCount(baseFailed + summary.failed);
            shopifyTaskService.updateShopifyTask(task);
        }
        return summary;
    }

    @Transactional(rollbackFor = Exception.class)
    ProductImportItemResult upsertProduct(ShopifyImportedProduct remote, ImportContext context) {
        if (remote == null || StringUtils.isEmpty(remote.getId())) {
            throw new ServiceException("Shopify 商品 ID 为空");
        }
        // 单商品一个事务，保证商品、变体、媒体要么一起成功，要么回滚后写失败明细。
        Date now = new Date();
        Product product = selectProduct(context.storeId(), remote.getId());
        boolean created = false;
        boolean conflict = shouldKeepLocalCoreFields(product);

        // 新增商品
        if (product == null) {
            product = buildNewProduct(remote, context, now);
            productMapper.insert(product);
            created = true;
            log.info("Shopify 商品导入创建本地商品，taskId={}, storeId={}, productId={}, shopifyProductId={}",
                    context.taskId(), context.storeId(), product.getProductId(), remote.getId());
        } else {
            // 更新商品
            Product update = buildProductUpdate(product, remote, context, conflict, now);
            productMapper.updateById(update);
            log.info("Shopify 商品导入更新本地商品，taskId={}, storeId={}, productId={}, shopifyProductId={}, conflict={}",
                    context.taskId(), context.storeId(), product.getProductId(), remote.getId(), conflict);
        }

        Long productId = product.getProductId();
        if (productId == null) {
            throw new ServiceException("本地商品 ID 生成失败");
        }
        // 保存媒体
        Map<String, Long> mediaIdMap = upsertMedia(remote, context, productId, now);
        if (!mediaIdMap.isEmpty() && !conflict) {
            Product mainMediaUpdate = new Product();
            mainMediaUpdate.setProductId(productId);
            mainMediaUpdate.setMainMediaId(mediaIdMap.values().iterator().next());
            productMapper.updateById(mainMediaUpdate);
        }
        upsertVariants(remote, context, productId, mediaIdMap, conflict, now);

        if (productQualityService != null) {
            productQualityService.refreshProductMissingFields(productId);
        }
        if (conflict) {
            recordProductConflict(context, productId, remote);
        } else {
            recordProductSuccess(context, productId, remote, created);
        }
        return new ProductImportItemResult(true, conflict, productId);
    }

    private Product buildNewProduct(ShopifyImportedProduct remote, ImportContext context, Date now) {
        Product product = new Product();
        fillCoreProductFields(product, remote, context);
        product.setLastShopifyImportTime(now);
        product.setCreateTime(now);
        product.setUpdateTime(now);
        product.setLastSyncTime(now);
        product.setSyncStatus(ProductConstants.SYNC_STATUS_SUCCESS);
        product.setSyncMessage("Shopify 商品导入成功");
        product.setDelFlag("0");
        return product;
    }

    private Product buildProductUpdate(Product existing, ShopifyImportedProduct remote, ImportContext context, boolean conflict, Date now) {
        Product update = new Product();
        update.setProductId(existing.getProductId());
        update.setStoreId(context.storeId());
        if (StringUtils.isEmpty(existing.getShopifyProductId()) || Objects.equals(existing.getShopifyProductId(), remote.getId())) {
            update.setShopifyProductId(remote.getId());
        }
        update.setShopifyUpdatedAt(remote.getUpdatedAt());
        update.setLastSyncTime(now);
        update.setSyncStatus(ProductConstants.SYNC_STATUS_SUCCESS);
        update.setSyncMessage(conflict ? "Shopify 商品导入存在 ERP 优先冲突，核心字段未覆盖" : "Shopify 商品导入成功");
        if (!conflict) {
            fillCoreProductFields(update, remote, context);
            update.setLastShopifyImportTime(now);
            update.setUpdateTime(now);
        }
        return update;
    }

    private void fillCoreProductFields(Product product, ShopifyImportedProduct remote, ImportContext context) {
        product.setStoreId(context.storeId());
        product.setShopifyProductId(remote.getId());
        product.setShopifyUpdatedAt(remote.getUpdatedAt());
        product.setProductTitle(remote.getTitle());
        product.setSpu(resolveSpu(remote, context.storeId()));
        product.setProductType(remote.getProductType());
        product.setStatus(ShopifyProductStatus.fromName(remote.getStatus()).getCode());
        product.setBodyHtml(remote.getDescriptionHtml());
    }

    private Map<String, Long> upsertMedia(ShopifyImportedProduct remote, ImportContext context, Long productId, Date now) {
        Map<String, Long> mediaIdMap = new LinkedHashMap<>();
        List<ShopifyImportedMedia> mediaList = remote.getMedia() == null ? List.of() : remote.getMedia();
        int createdCount = 0;
        int updatedCount = 0;
        for (ShopifyImportedMedia remoteMedia : mediaList) {
            if (remoteMedia == null || StringUtils.isEmpty(remoteMedia.getId())) {
                continue;
            }
            Media existing = selectMedia(context.storeId(), remoteMedia.getId());
            boolean created = false;
            Media media = new Media();
            if (existing == null) {
                media = new Media();
                media.setProductId(productId);
                media.setStoreId(context.storeId());
                media.setCreateTime(now);
                created = true;
            } else {
                media.setMediaId(existing.getMediaId());
                media.setProductId(productId);
                media.setStoreId(context.storeId());
                media.setUpdateTime(now);
            }
            media.setShopifyMediaId(remoteMedia.getId());
            media.setShopifyMediaUrl(resolveMediaUrl(remoteMedia));
            media.setAlt(remoteMedia.getAlt());
            media.setMediaContentType(remoteMedia.getMediaContentType());
            media.setPosition(remoteMedia.getPosition());
            media.setLastShopifyImportTime(now);
            media.setDelFlag("0");

            if (created) {
                mediaMapper.insert(media);
                createdCount++;
            } else {
                mediaMapper.updateById(media);
                updatedCount++;
            }

            Long mediaId = media.getMediaId();
            if (mediaId != null) {
                mediaIdMap.put(remoteMedia.getId(), mediaId);
            }
        }
        if (!mediaList.isEmpty()) {
            log.debug("Shopify 商品媒体导入完成，taskId={}, productId={}, created={}, updated={}",
                    context.taskId(), productId, createdCount, updatedCount);
        }
        return mediaIdMap;
    }

    private void upsertVariants(ShopifyImportedProduct remote, ImportContext context, Long productId,
                                Map<String, Long> mediaIdMap, boolean conflict, Date now) {
        List<ShopifyImportedVariant> variants = remote.getVariants() == null ? List.of() : remote.getVariants();
        int createdCount = 0;
        int updatedCount = 0;
        for (ShopifyImportedVariant remoteVariant : variants) {
            if (remoteVariant == null || StringUtils.isEmpty(remoteVariant.getId())) {
                continue;
            }
            ProductVariant existing = selectVariant(context.storeId(), productId, remoteVariant.getId(), remoteVariant.getSku());
            boolean created = false;
            ProductVariant variant = new ProductVariant();
            if (existing == null) {
                variant = new ProductVariant();
                variant.setProductId(productId);
                variant.setStoreId(context.storeId());
                variant.setCreateTime(now);
                created = true;
            } else {
                variant.setVariantId(existing.getVariantId());
                variant.setProductId(productId);
                variant.setStoreId(context.storeId());
                variant.setUpdateTime(now);
            }

            if (created || !conflict) {
                fillVariantFields(variant, remoteVariant, mediaIdMap, now);
                if (created) {
                    productVariantMapper.insert(variant);
                    createdCount++;
                } else {
                    productVariantMapper.updateById(variant);
                    updatedCount++;
                }
            }
        }
        if (!variants.isEmpty()) {
            log.debug("Shopify 商品变体导入完成，taskId={}, productId={}, created={}, updated={}, conflict={}",
                    context.taskId(), productId, createdCount, updatedCount, conflict);
        }
    }

    private void fillVariantFields(ProductVariant variant, ShopifyImportedVariant remoteVariant, Map<String, Long> mediaIdMap, Date now) {
        variant.setShopifyVariantId(remoteVariant.getId());
        variant.setShopifyInventoryItemId(remoteVariant.getInventoryItemId());
        variant.setSku(remoteVariant.getSku());
        variant.setPrice(parseMoneyToCents(remoteVariant.getPrice()));
        variant.setCompareAtPrice(parseMoneyToCents(remoteVariant.getCompareAtPrice()));
        variant.setPosition(remoteVariant.getPosition());
        variant.setOptionValues(buildOptionValuesJson(remoteVariant.getSelectedOptions()));
        variant.setMediaId(resolveVariantMediaId(remoteVariant, mediaIdMap));
        variant.setLastShopifyImportTime(now);
        variant.setIsActiveAvailable(ProductConstants.IS_ACTIVE_AVAILABLE_YES);
        variant.setDelFlag("0");
    }

    private ShopifySyncCursor getOrCreateCursor(Long storeId) {
        ShopifySyncCursor cursor = syncCursorMapper.selectOne(new LambdaQueryWrapper<ShopifySyncCursor>()
                .eq(ShopifySyncCursor::getStoreId, storeId)
                .eq(ShopifySyncCursor::getSyncMode, CURSOR_MODE_PRODUCT_IMPORT)
                .last("limit 1"));
        if (cursor != null) {
            return cursor;
        }
        cursor = new ShopifySyncCursor();
        cursor.setStoreId(storeId);
        cursor.setSyncMode(CURSOR_MODE_PRODUCT_IMPORT);
        cursor.setStatus(CURSOR_STATUS_SUCCESS);
        cursor.setCreateTime(new Date());
        cursor.setUpdateTime(new Date());
        syncCursorMapper.insert(cursor);
        return cursor;
    }

    private Product selectProduct(Long storeId, String shopifyProductId) {
        return productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getStoreId, storeId)
                .eq(Product::getShopifyProductId, shopifyProductId)
                .last("limit 1"));
    }

    private Media selectMedia(Long storeId, String shopifyMediaId) {
        return mediaMapper.selectOne(new LambdaQueryWrapper<Media>()
                .eq(Media::getStoreId, storeId)
                .eq(Media::getShopifyMediaId, shopifyMediaId)
                .last("limit 1"));
    }

    private ProductVariant selectVariant(Long storeId, Long productId, String shopifyVariantId, String sku) {
        ProductVariant byShopifyId = null;
        if (StringUtils.isNotEmpty(shopifyVariantId)) {
            byShopifyId = productVariantMapper.selectOne(new LambdaQueryWrapper<ProductVariant>()
                    .eq(ProductVariant::getStoreId, storeId)
                    .eq(ProductVariant::getShopifyVariantId, shopifyVariantId)
                    .last("limit 1"));
        }
        if (byShopifyId != null || StringUtils.isEmpty(sku)) {
            return byShopifyId;
        }
        return productVariantMapper.selectOne(new LambdaQueryWrapper<ProductVariant>()
                .eq(ProductVariant::getStoreId, storeId)
                .eq(ProductVariant::getProductId, productId)
                .eq(ProductVariant::getSku, sku)
                .last("limit 1"));
    }

    boolean shouldKeepLocalCoreFields(Product product) {
        if (product == null || product.getUpdateTime() == null || product.getLastShopifyImportTime() == null) {
            return false;
        }
        // ERP 后台编辑晚于上次 Shopify 导入时，本地核心字段优先，避免覆盖运营刚修改的资料。
        return product.getUpdateTime().after(product.getLastShopifyImportTime());
    }

    private String resolveSpu(ShopifyImportedProduct remote, Long storeId) {
        if (StringUtils.isNotEmpty(remote.getSpu())) {
            return remote.getSpu();
        }
        // Shopify 没有 custom.SPU 时生成临时 SPU，保证本地商品资料不出现空 SPU。
        String numericId = extractNumericId(remote.getId());
        return "SHOP" + storeId + "-P" + (StringUtils.isEmpty(numericId) ? remote.getId().hashCode() : numericId);
    }

    private String buildShopifyAdminUrl(String shopName, String productId) {
        String numericId = extractNumericId(productId);
        if (StringUtils.isEmpty(shopName) || StringUtils.isEmpty(numericId)) {
            return null;
        }
        return "https://" + shopName + ".myshopify.com/admin/products/" + numericId;
    }

    private String extractNumericId(String gid) {
        if (StringUtils.isEmpty(gid)) {
            return null;
        }
        int slash = gid.lastIndexOf('/');
        return slash >= 0 ? gid.substring(slash + 1) : gid;
    }

    private Integer parseMoneyToCents(String money) {
        if (StringUtils.isEmpty(money)) {
            return null;
        }
        return new BigDecimal(money).movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private String buildOptionValuesJson(Map<String, String> selectedOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return null;
        }
        List<Map<String, String>> values = new ArrayList<>();
        selectedOptions.forEach((name, value) -> values.add(Map.of("name", name, "value", value)));
        return JSON.toJSONString(values);
    }

    private Long resolveVariantMediaId(ShopifyImportedVariant remoteVariant, Map<String, Long> mediaIdMap) {
        if (remoteVariant.getMediaIds() == null || remoteVariant.getMediaIds().isEmpty()) {
            return null;
        }
        for (String shopifyMediaId : remoteVariant.getMediaIds()) {
            Long mediaId = mediaIdMap.get(shopifyMediaId);
            if (mediaId != null) {
                return mediaId;
            }
        }
        return null;
    }

    private String resolveMediaUrl(ShopifyImportedMedia media) {
        return StringUtils.isNotEmpty(media.getOriginalUrl()) ? media.getOriginalUrl() : media.getPreviewUrl();
    }

    private Date maxDate(Date first, Date second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.after(second) ? first : second;
    }

    private void markTaskRunning(ShopifyTask task) {
        task.setTaskStatus(ShopifyTaskStatus.RUNNING.getCode());
        task.setStartTime(new Date());
        task.setProgress(0);
        task.setSuccessCount(0);
        task.setPartialCount(0);
        task.setFailedCount(0);
        shopifyTaskService.updateShopifyTask(task);
    }

    private void markCursorRunning(ShopifySyncCursor cursor, Long taskId) {
        cursor.setStatus(CURSOR_STATUS_RUNNING);
        cursor.setLastTaskId(taskId);
        cursor.setUpdateTime(new Date());
        syncCursorMapper.updateById(cursor);
        log.info("Shopify 商品导入游标标记为运行中，taskId={}, storeId={}, cursorId={}",
                taskId, cursor.getStoreId(), cursor.getCursorId());
    }

    private void finishTask(ShopifyTask task, ShopifySyncCursor cursor, ImportSummary summary, long startTime) {
        task.setTaskStatus(resolveTaskStatus(summary.success, summary.failed));
        task.setProgress(100);
        task.setSuccessCount(summary.success);
        task.setPartialCount(0);
        task.setFailedCount(summary.failed);
        task.setEndTime(new Date());
        task.setExecutionTime(System.currentTimeMillis() - startTime);
        task.setErrorMessage(summary.errorSummary.toString());
        shopifyTaskService.updateShopifyTask(task);

        cursor.setStatus(summary.failed == 0 ? CURSOR_STATUS_SUCCESS : CURSOR_STATUS_PART_SUCCESS);
        cursor.setLastTaskId(task.getTaskId());
        cursor.setLastErrorSummary(summary.errorSummary.toString());
        cursor.setUpdateTime(new Date());
        if (summary.failed == 0) {
            // 只有整批关键写入都成功，才推进 updatedAt 游标；部分失败保留旧游标，方便下次重扫。
            cursor.setLastSuccessUpdatedAt(summary.maxUpdatedAt);
            cursor.setLastSuccessSyncTime(new Date());
        }
        syncCursorMapper.updateById(cursor);
        log.info("Shopify 商品导入任务完成，taskId={}, status={}, success={}, failed={}, cursorAdvanced={}, maxUpdatedAt={}, costMs={}",
                task.getTaskId(), task.getTaskStatus(), summary.success, summary.failed, summary.failed == 0,
                summary.maxUpdatedAt, task.getExecutionTime());
    }

    private String resolveTaskStatus(int success, int failed) {
        if (failed == 0) {
            return ShopifyTaskStatus.SUCCESS.getCode();
        }
        if (success == 0) {
            return ShopifyTaskStatus.FAILED.getCode();
        }
        return ShopifyTaskStatus.PART_SUCCESS.getCode();
    }

    private void failTask(ShopifyTask task, long startTime, String message) {
        task.setTaskStatus(ShopifyTaskStatus.FAILED.getCode());
        task.setErrorMessage(message);
        task.setProgress(100);
        task.setEndTime(new Date());
        task.setExecutionTime(System.currentTimeMillis() - startTime);
        shopifyTaskService.updateShopifyTask(task);
        log.warn("Shopify 商品导入任务已标记失败，taskId={}, message={}", task.getTaskId(), message);
    }

    private void failCursor(ShopifySyncCursor cursor, Long taskId, String message) {
        cursor.setStatus(CURSOR_STATUS_FAILED);
        cursor.setLastTaskId(taskId);
        cursor.setLastErrorSummary(message);
        cursor.setUpdateTime(new Date());
        syncCursorMapper.updateById(cursor);
        log.warn("Shopify 商品导入游标已标记失败，taskId={}, storeId={}, cursorId={}, message={}",
                taskId, cursor.getStoreId(), cursor.getCursorId(), message);
    }

    private void recordProductSuccess(ImportContext context, Long productId, ShopifyImportedProduct remote, boolean created) {
        recordProductDetail(context, productId, remote, created ? ShopifyTaskStep.PRODUCT_IMPORT_CREATE.getCode() : ShopifyTaskStep.PRODUCT_IMPORT_UPDATE.getCode(),
                ShopifyTaskDetailStatus.SUCCESS.getCode(), null, null);
    }

    private void recordProductConflict(ImportContext context, Long productId, ShopifyImportedProduct remote) {
        recordProductDetail(context, productId, remote, ShopifyTaskStep.PRODUCT_IMPORT_CONFLICT.getCode(),
                ShopifyTaskDetailStatus.SKIPPED.getCode(), ShopifyTaskErrorCode.ERP_PRIORITY_CONFLICT.getCode(),
                "ERP 本地更新时间晚于上次 Shopify 导入，核心字段未覆盖");
    }

    private void recordProductFailure(ImportContext context, ShopifyImportedProduct remote, String message) {
        recordProductDetail(context, null, remote, ShopifyTaskStep.PRODUCT_IMPORT.getCode(),
                ShopifyTaskDetailStatus.FAILED.getCode(), ShopifyTaskErrorCode.PRODUCT_IMPORT_FAILED.getCode(), message);
    }

    private void recordProductDetail(ImportContext context, Long productId, ShopifyImportedProduct remote, String step,
                                     String status, String errorCode, String errorMessage) {
        if (shopifyTaskDetailService == null || context.taskId() == null) {
            return;
        }
        ShopifyTaskDetail detail = new ShopifyTaskDetail();
        detail.setTaskId(context.taskId());
        detail.setStoreId(context.storeId());
        detail.setShopName(context.shopName());
        detail.setProductId(productId);
        detail.setItemType(ShopifyTaskDetailItemType.PRODUCT.getCode());
        detail.setItemId(productId);
        detail.setItemName(remote == null ? null : remote.getTitle());
        detail.setShopifyId(remote == null ? null : remote.getId());
        detail.setStep(step);
        detail.setStatus(status);
        detail.setErrorCode(errorCode);
        detail.setErrorMessage(errorMessage);
        shopifyTaskDetailService.insertShopifyTaskDetail(detail);
    }

    public record ImportContext(Long taskId, Long storeId, String shopName, String taskType) {
    }

    @Data
    public static class ProductImportItemResult {
        private final boolean success;
        private final boolean conflict;
        private final Long productId;
    }

    private static class ImportSummary {
        private int success;
        private int failed;
        private Date maxUpdatedAt;
        private final StringBuilder errorSummary = new StringBuilder();

        private void merge(ImportSummary other) {
            if (other == null) {
                return;
            }
            this.success += other.success;
            this.failed += other.failed;
            if (this.maxUpdatedAt == null || (other.maxUpdatedAt != null && other.maxUpdatedAt.after(this.maxUpdatedAt))) {
                this.maxUpdatedAt = other.maxUpdatedAt;
            }
            this.errorSummary.append(other.errorSummary);
        }
    }
}

package com.ruoyi.erp.executor;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.shopify.client.ShopifyGraphQLClient;
import com.ruoyi.erp.shopify.enums.*;
import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.constant.StoreConstants;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.exception.ShopifyUserError;
import com.ruoyi.erp.shopify.model.*;
import com.ruoyi.erp.shopify.support.ShopifyTaskStatusResolver;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ShopifyStoreMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductOption;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ProductVariantOption;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import com.ruoyi.erp.model.vo.media.PreparedMediaUpload;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.service.IMediaTranscodeService;
import com.ruoyi.erp.service.IProductTagRelService;
import com.ruoyi.erp.service.IProductVariantService;
import com.ruoyi.erp.service.IShopifyTaskDetailService;
import com.ruoyi.erp.service.IShopifyTaskService;
import com.ruoyi.erp.service.ITagDictService;
import com.ruoyi.erp.utils.PriceUtil;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shopify 批量同步执行器。
 */
@Slf4j
@Component
public class ShopifySyncExecutor {

    private static final String VARIANT_CREATE_STRATEGY_REMOVE_STANDALONE = "REMOVE_STANDALONE_VARIANT";

    @Resource
    private ShopifyGraphQLClient shopifyGraphQLClient;
    @Resource
    private IProductVariantService productVariantService;
    @Resource
    private IMediaService mediaService;
    @Resource
    private IMediaTranscodeService mediaTranscodeService;
    @Resource
    private IShopifyTaskService shopifyTaskService;
    @Resource
    private IShopifyTaskDetailService shopifyTaskDetailService;
    @Resource
    private ITagDictService tagDictService;
    @Resource
    private IProductTagRelService productTagRelService;
    @Resource
    private ShopifyStoreMapper shopifyStoreMapper;
    @Resource
    private ProductMapper productMapper;

    /**
     * 异步执行批量推送。
     */
    @Async("threadPoolTaskExecutor")
    public void execute(Long taskId, List<Long> productIds) {
        long startTime = System.currentTimeMillis();
        ShopifyTask task = shopifyTaskService.selectShopifyTaskByTaskId(taskId);
        if (task == null) {
            log.error("任务不存在，taskId={}", taskId);
            return;
        }

        Long storeId = resolveStoreId(task);
        if (storeId == null) {
            failTaskForMissingStore(startTime, task);
            return;
        }

        ShopifyStore store = shopifyStoreMapper.selectById(storeId);
        String shopName = store != null ? store.getShopName() : "unknown";
        int total = productIds == null ? 0 : productIds.size();
        int success = 0;
        int partial = 0;
        int failed = 0;
        StringBuilder errorSummary = new StringBuilder();

        task.setTaskStatus(ShopifyTaskStatus.RUNNING.getCode());
        task.setProgress(0);
        task.setStartTime(new Date());
        task.setSuccessCount(0);
        task.setPartialCount(0);
        task.setFailedCount(0);
        shopifyTaskService.updateShopifyTask(task);

        log.info("开始同步商品到 Shopify 店铺: {}, storeId={}, 商品数量={}", shopName, storeId, total);

        for (int i = 0; i < total; i++) {
            Long productId = productIds.get(i);
            int index = i + 1;
            markProductRunning(productId);

            try {
                ProductSyncResult syncResult = syncProduct(taskId, storeId, productId, index, total, shopName);
                Product update = new Product();
                update.setProductId(productId);
                update.setSyncStatus(ShopifyTaskStatusResolver.resolveProductSyncStatus(
                        syncResult.isProductCreatedOrUpdated(), syncResult.hasChildFailure()));
                update.setSyncMessage(syncResult.getSummary());
                update.setLastSyncTime(DateUtils.getNowDate());
                productMapper.updateById(update);

                if (syncResult.hasChildFailure()) {
                    partial++;
                    errorSummary.append(String.format("[商品%d] %s%n", index, syncResult.getSummary()));
                } else {
                    success++;
                }
            } catch (Exception e) {
                log.error("推送商品失败，productId={}", productId, e);
                Product update = new Product();
                update.setProductId(productId);
                update.setSyncStatus(ProductConstants.SYNC_STATUS_FAILED);
                update.setSyncMessage(e.getMessage());
                update.setLastSyncTime(DateUtils.getNowDate());
                productMapper.updateById(update);

                failed++;
                errorSummary.append(String.format("[商品%d] %s%n", index, e.getMessage()));
                recordDetail(taskId, storeId, shopName, productId, ShopifyTaskDetailItemType.PRODUCT.getCode(),
                        productId, null, null, ShopifyTaskStep.PRODUCT_SYNC.getCode(), ShopifyTaskDetailStatus.FAILED.getCode(),
                        ShopifyTaskErrorCode.PRODUCT_SYNC_FAILED.getCode(), null, null, e.getMessage());
            }

            int progress = total == 0 ? 100 : (int) ((i + 1) * 100.0 / total);
            task.setProgress(progress);
            task.setSuccessCount(success);
            task.setPartialCount(partial);
            task.setFailedCount(failed);
            shopifyTaskService.updateShopifyTask(task);
        }

        if (store != null) {
            store.setLastSyncTime(new Date());
            store.setSyncCount((store.getSyncCount() == null ? 0 : store.getSyncCount()) + 1);
            shopifyStoreMapper.updateById(store);
        }

        task.setTaskStatus(ShopifyTaskStatusResolver.resolveTaskStatus(total, success, partial, failed));
        task.setProgress(100);
        task.setSuccessCount(success);
        task.setPartialCount(partial);
        task.setFailedCount(failed);
        task.setEndTime(new Date());
        task.setExecutionTime(System.currentTimeMillis() - startTime);
        task.setErrorMessage(errorSummary.toString());
        shopifyTaskService.updateShopifyTask(task);

        log.info("商品同步完成: shop={}, 成功={}, 部分成功={}, 失败={}, 耗时={}ms",
                shopName, success, partial, failed, System.currentTimeMillis() - startTime);
    }

    private void failTaskForMissingStore(long startTime, ShopifyTask task) {
        String message = "无法确定店铺ID，请检查任务或商品的店铺配置";
        log.error("{} taskId={}", message, task.getTaskId());
        task.setTaskStatus(ShopifyTaskStatus.FAILED.getCode());
        task.setErrorMessage(message);
        task.setEndTime(new Date());
        task.setExecutionTime(System.currentTimeMillis() - startTime);
        shopifyTaskService.updateShopifyTask(task);
    }

    private void markProductRunning(Long productId) {
        Product running = new Product();
        running.setProductId(productId);
        running.setSyncStatus(ProductConstants.SYNC_STATUS_RUNNING);
        running.setSyncMessage("Shopify 同步任务执行中");
        productMapper.updateById(running);
    }

    /**
     * 解析店铺 ID
     */
    private Long resolveStoreId(ShopifyTask task) {
        if (task.getStoreId() != null) {
            return task.getStoreId();
        }
        if (task.getShopName() != null) {
            ShopifyStore store = shopifyStoreMapper.selectByShopName(task.getShopName());
            if (store != null) {
                return store.getStoreId();
            }
        }
        ShopifyStore defaultStore = shopifyStoreMapper.selectDefaultStore();
        return defaultStore == null ? null : defaultStore.getStoreId();
    }

    public ProductSyncResult syncProduct(Long storeId, Long productId, int index, int total) {
        return syncProduct(null, storeId, productId, index, total, null);
    }

    /**
     * 同步单个商品到 Shopify。
     */
    public ProductSyncResult syncProduct(Long taskId, Long storeId, Long productId, int index, int total, String shopName) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }
        ShopifyStore store = shopifyStoreMapper.selectById(storeId);
        String currentShopName = StringUtils.isEmpty(shopName) && store != null ? store.getShopName() : shopName;

        ProductSyncResult result = new ProductSyncResult();
        result.append(String.format("[商品《%s》] 第 %d/%d 个%n", product.getProductTitle(), index, total));

        MediaUploadResult mediaUploadResult = uploadMedia(taskId, storeId, currentShopName, product, result);
        List<Media> uploadMediaList = mediaUploadResult.getMediaList();
        List<String> tagNames = productTagRelService.selectTagCodeListByProductId(productId);

        ProductInput productInput = buildProductInput(product, uploadMediaList, tagNames, resolveDefaultProductStatus(store));
        List<Media> createMediaList = filterCreateMediaList(uploadMediaList);
        List<CreateMediaInput> mediaInputs = buildMediaInputs(createMediaList, product);

        String shopifyProductId;
        List<String> shopifyMediaIds;
        boolean isCreateShopifyProduct = StringUtils.isEmpty(product.getShopifyProductId());
        try {
            if (!isCreateShopifyProduct) {
                ProductCreateResult updateResult = shopifyGraphQLClient.updateProduct(
                        storeId, product.getShopifyProductId(), productInput, mediaInputs);
                shopifyProductId = updateResult.getProductId();
                shopifyMediaIds = updateResult.getMediaIds();
                result.append("商品更新成功 -> PID: ").append(shopifyProductId).append("\n");
            } else {
                ProductCreateResult createResult = shopifyGraphQLClient.createProduct(storeId, productInput, mediaInputs);
                shopifyProductId = createResult.getProductId();
                shopifyMediaIds = createResult.getMediaIds();
                result.append("商品创建成功 -> PID: ").append(shopifyProductId).append("\n");
            }
            result.setProductCreatedOrUpdated(true);
        } catch (ShopifyApiException e) {
            recordProductApiFailure(taskId, storeId, currentShopName, product, e);
            throw e;
        } catch (Exception e) {
            recordDetail(taskId, storeId, currentShopName, productId, ShopifyTaskDetailItemType.PRODUCT.getCode(),
                    productId, product.getProductTitle(), null, ShopifyTaskStep.PRODUCT_UPSERT.getCode(), ShopifyTaskDetailStatus.FAILED.getCode(),
                    ShopifyTaskErrorCode.PRODUCT_UPSERT_FAILED.getCode(), null, null, e.getMessage());
            throw e;
        }

        boolean mediaRegisterFailed = saveMediaIdsToDatabase(taskId, storeId, currentShopName, product, createMediaList, shopifyMediaIds, result);

        Product updateProduct = new Product();
        updateProduct.setProductId(productId);
        updateProduct.setShopifyProductId(shopifyProductId);
        productMapper.updateById(updateProduct);

        List<ProductVariant> variants = productVariantService.selectListByProductId(productId);
        boolean variantFailed = false;
        if (!variants.isEmpty()) {
            String variantCreateStrategy = isCreateShopifyProduct ? VARIANT_CREATE_STRATEGY_REMOVE_STANDALONE : null;
            variantFailed = syncVariants(taskId, storeId, currentShopName, productId, shopifyProductId, variants, result, variantCreateStrategy);
        }

        boolean childFailed = mediaUploadResult.hasFailure() || mediaRegisterFailed || variantFailed;
        result.setChildFailure(childFailed);
        recordDetail(taskId, storeId, currentShopName, productId, ShopifyTaskDetailItemType.PRODUCT.getCode(),
                productId, product.getProductTitle(), shopifyProductId, ShopifyTaskStep.PRODUCT_SYNC.getCode(),
                childFailed ? ShopifyTaskDetailStatus.PART_SUCCESS.getCode() : ShopifyTaskDetailStatus.SUCCESS.getCode(),
                null, null, null, childFailed ? "商品已创建/更新，但部分媒体或变体同步失败" : null);
        return result;
    }

    /**
     * 上传媒体文件。单个媒体失败只记录明细，不中断商品创建。
     */
    private MediaUploadResult uploadMedia(Long taskId, Long storeId, String shopName, Product product, ProductSyncResult result) {
        MediaUploadResult uploadResult = new MediaUploadResult();
        List<Media> mediaList = mediaService.listByProductId(product.getProductId());
        if (mediaList == null || mediaList.isEmpty()) {
            return uploadResult;
        }

        for (Media media : mediaList) {
            if (media == null) {
                continue;
            }
            String filename = media.getFilename();
            try {
                if (StringUtils.isNotEmpty(media.getShopifyMediaId())) {
                    result.append("媒体 ").append(filename).append(" 已存在 Shopify Media ID，跳过上传\n");
                    recordMediaDetail(taskId, storeId, shopName, product.getProductId(), media,
                            ShopifyTaskStep.MEDIA_UPLOAD.getCode(), ShopifyTaskDetailStatus.SKIPPED.getCode(),
                            media.getShopifyMediaId(), null, null, null, null);
                    continue;
                }

                if (StringUtils.isNotEmpty(media.getShopifyMediaUrl())) {
                    uploadResult.add(media);
                    result.append("媒体 ").append(filename).append(" 已有 Shopify URL，进入注册\n");
                    recordMediaDetail(taskId, storeId, shopName, product.getProductId(), media,
                            ShopifyTaskStep.MEDIA_UPLOAD.getCode(), ShopifyTaskDetailStatus.SUCCESS.getCode(),
                            media.getShopifyMediaId(), null, null, null, null);
                    continue;
                }

                String localPath = media.getNasMediaUrl();
                if (StringUtils.isEmpty(localPath)) {
                    uploadResult.markFailure();
                    recordMediaFailure(taskId, storeId, shopName, product.getProductId(), media,
                            ShopifyTaskStep.MEDIA_UPLOAD.getCode(), ShopifyTaskErrorCode.MEDIA_PATH_EMPTY.getCode(), "媒体缺少本地 NAS 路径");
                    result.append("媒体 ").append(filename).append(" 上传失败：缺少本地 NAS 路径\n");
                    continue;
                }

                PreparedMediaUpload preparedMedia = mediaTranscodeService.prepareForShopifyUpload(media);
                File file = preparedMedia.getFile();
                if (file == null || !file.exists()) {
                    uploadResult.markFailure();
                    String message = "媒体文件不存在: " + localPath;
                    recordMediaFailure(taskId, storeId, shopName, product.getProductId(), media,
                            ShopifyTaskStep.MEDIA_UPLOAD.getCode(), ShopifyTaskErrorCode.MEDIA_FILE_MISSING.getCode(), message);
                    result.append("媒体 ").append(filename).append(" 上传失败：").append(message).append("\n");
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    StagedUploadResult stagedUpload = shopifyGraphQLClient.stagedUploadMedia(
                            storeId,
                            preparedMedia.getFilename(),
                            preparedMedia.getMimeType(),
                            preparedMedia.getMediaContentType(),
                            fis,
                            file.length()
                    );

                    Media updateMedia = new Media();
                    updateMedia.setMediaId(media.getMediaId());
                    updateMedia.setShopifyMediaUrl(stagedUpload.resourceUrl());
                    int updated = mediaService.updateMedia(updateMedia);
                    if (updated <= 0) {
                        uploadResult.markFailure();
                        recordMediaFailure(taskId, storeId, shopName, product.getProductId(), media,
                                ShopifyTaskStep.MEDIA_UPLOAD.getCode(), ShopifyTaskErrorCode.MEDIA_URL_SAVE_FAILED.getCode(), "保存 Shopify 媒体上传 URL 失败");
                        result.append("媒体 ").append(filename).append(" 保存 Shopify 上传 URL 失败\n");
                        continue;
                    }
                    media.setShopifyMediaUrl(stagedUpload.resourceUrl());
                    media.setMediaContentType(preparedMedia.getMediaContentType());
                    uploadResult.add(media);

                    result.append("媒体 ").append(filename).append(" 上传成功\n");
                    recordMediaDetail(taskId, storeId, shopName, product.getProductId(), media,
                            ShopifyTaskStep.MEDIA_UPLOAD.getCode(), ShopifyTaskDetailStatus.SUCCESS.getCode(),
                            media.getShopifyMediaId(), null, null, null, null);
                }
            } catch (Exception e) {
                log.error("媒体《{}》上传失败", filename, e);
                uploadResult.markFailure();
                recordMediaFailure(taskId, storeId, shopName, product.getProductId(), media,
                        ShopifyTaskStep.MEDIA_UPLOAD.getCode(), ShopifyTaskErrorCode.MEDIA_UPLOAD_FAILED.getCode(), e.getMessage());
                result.append("媒体 ").append(filename).append(" 上传失败：").append(e.getMessage()).append("\n");
            }
        }
        return uploadResult;
    }

    /**
     * 同步变体。批量失败时按 Shopify userErrors 的 inputIndex 回写到本地变体明细。
     */
    private boolean syncVariants(Long storeId, String shopifyProductId, List<ProductVariant> variants,
                                 StringBuilder result, String variantCreateStrategy) {
        ProductSyncResult syncResult = new ProductSyncResult(result);
        return syncVariants(null, storeId, null, null, shopifyProductId, variants, syncResult, variantCreateStrategy);
    }

    private boolean syncVariants(Long taskId, Long storeId, String shopName, Long productId, String shopifyProductId,
                                 List<ProductVariant> variants, ProductSyncResult result, String variantCreateStrategy) {
        List<VariantInput> variantInputs = new ArrayList<>();
        List<ProductVariant> createVariants = new ArrayList<>();
        ShopifyStore store = shopifyStoreMapper.selectById(storeId);
        if (store == null) {
            throw new RuntimeException("店铺不存在: " + storeId);
        }

        for (ProductVariant variant : variants) {
            if (StringUtils.isNotEmpty(variant.getShopifyVariantId())) {
                result.append("变体 ").append(resolveVariantName(variant)).append(" 已存在 Shopify ID，跳过创建\n");
                recordVariantDetail(taskId, storeId, shopName, productId, variant,
                        ShopifyTaskStep.VARIANT_CREATE.getCode(), ShopifyTaskDetailStatus.SKIPPED.getCode(),
                        variant.getShopifyVariantId(), null, null, null, null);
                continue;
            }

            BigDecimal price = PriceUtil.fenToYuan(variant.getPrice());
            BigDecimal compareAtPrice = PriceUtil.fenToYuan(variant.getCompareAtPrice());
            String shopifyMediaId = resolveVariantMediaId(variant);

            VariantInput input = VariantInput.builder()
                    .price(price)
                    .mediaId(shopifyMediaId)
                    .compareAtPrice(compareAtPrice)
                    .inventoryPolicy(resolveInventoryPolicy(store))
                    .optionValues(parseOptionValuesAsList(variant.getOptionValues()))
                    .inventoryItem(buildInventoryItem(store, variant))
                    .inventoryQuantities(buildInventoryQuantities(store))
                    .taxable(false)
                    .build();

            variantInputs.add(input);
            createVariants.add(variant);
        }

        if (createVariants.isEmpty()) {
            return false;
        }

        try {
            List<String> variantIds = shopifyGraphQLClient.createVariantsBulk(storeId, shopifyProductId, variantInputs, variantCreateStrategy);
            return saveVariantIdsToDatabase(taskId, storeId, shopName, productId, createVariants, variantIds, result);
        } catch (ShopifyApiException e) {
            result.append("变体批量创建失败: ").append(e.getMessage()).append("\n");
            recordVariantApiFailures(taskId, storeId, shopName, productId, createVariants, e);
            return true;
        } catch (Exception e) {
            result.append("变体批量创建失败: ").append(e.getMessage()).append("\n");
            recordVariantFailures(taskId, storeId, shopName, productId, createVariants,
                    ShopifyTaskErrorCode.VARIANT_CREATE_FAILED.getCode(), null, null, e.getMessage());
            return true;
        }
    }

    private String resolveVariantMediaId(ProductVariant variant) {
        Long mediaId = variant.getMediaId();
        if (mediaId == null) {
            return null;
        }
        Media media = mediaService.getOne(new LambdaQueryWrapper<>(Media.class)
                .select(Media::getShopifyMediaId, Media::getShopifyMediaUrl)
                .eq(Media::getMediaId, mediaId));
        return media == null ? null : media.getShopifyMediaId();
    }

    /**
     * 保存 Shopify 变体 ID。返回 true 表示有部分变体保存失败。
     */
    private boolean saveVariantIdsToDatabase(Long taskId, Long storeId, String shopName, Long productId,
                                             List<ProductVariant> variants, List<String> shopifyVariantIds,
                                             ProductSyncResult result) {
        if (shopifyVariantIds == null || shopifyVariantIds.size() != variants.size()) {
            String message = "Shopify 返回的变体 ID 数量与本地待同步变体数量不一致";
            recordVariantFailures(taskId, storeId, shopName, productId, variants,
                    ShopifyTaskErrorCode.VARIANT_ID_COUNT_MISMATCH.getCode(), null, null, message);
            result.append(message).append("\n");
            return true;
        }

        boolean hasFailure = false;
        for (int i = 0; i < variants.size(); i++) {
            ProductVariant variant = variants.get(i);
            String shopifyVariantId = shopifyVariantIds.get(i);
            if (StringUtils.isEmpty(shopifyVariantId)) {
                hasFailure = true;
                recordVariantDetail(taskId, storeId, shopName, productId, variant,
                        ShopifyTaskStep.VARIANT_SAVE_ID.getCode(), ShopifyTaskDetailStatus.FAILED.getCode(),
                        null, ShopifyTaskErrorCode.VARIANT_ID_EMPTY.getCode(), null, null, "Shopify 返回了空的变体 ID");
                continue;
            }

            ProductVariant updateVariant = new ProductVariant();
            updateVariant.setVariantId(variant.getVariantId());
            updateVariant.setShopifyVariantId(shopifyVariantId);
            int updated = productVariantService.updateProductVariant(updateVariant);
            if (updated <= 0) {
                hasFailure = true;
                recordVariantDetail(taskId, storeId, shopName, productId, variant,
                        ShopifyTaskStep.VARIANT_SAVE_ID.getCode(), ShopifyTaskDetailStatus.FAILED.getCode(),
                        shopifyVariantId, ShopifyTaskErrorCode.VARIANT_ID_SAVE_FAILED.getCode(), null, null,
                        "保存 Shopify 变体 ID 失败，variantId=" + variant.getVariantId());
                continue;
            }

            result.append("变体 ").append(resolveVariantName(variant)).append(" 创建成功 -> VID: ")
                    .append(shopifyVariantId).append("\n");
            recordVariantDetail(taskId, storeId, shopName, productId, variant,
                    ShopifyTaskStep.VARIANT_CREATE.getCode(), ShopifyTaskDetailStatus.SUCCESS.getCode(),
                    shopifyVariantId, null, null, i, null);
        }
        return hasFailure;
    }

    /**
     * 保存 Shopify 媒体 ID。返回 true 表示有部分媒体注册或保存失败。
     */
    private boolean saveMediaIdsToDatabase(Long taskId, Long storeId, String shopName, Product product,
                                           List<Media> createMediaList, List<String> shopifyMediaIds,
                                           ProductSyncResult result) {
        if (createMediaList == null || createMediaList.isEmpty()) {
            return false;
        }

        boolean hasFailure = false;
        List<String> mediaIds = shopifyMediaIds == null ? List.of() : shopifyMediaIds;
        for (int i = 0; i < createMediaList.size(); i++) {
            Media media = createMediaList.get(i);
            String shopifyMediaId = i < mediaIds.size() ? mediaIds.get(i) : null;
            if (StringUtils.isEmpty(shopifyMediaId)) {
                hasFailure = true;
                recordMediaFailure(taskId, storeId, shopName, product.getProductId(), media,
                        ShopifyTaskStep.MEDIA_REGISTER.getCode(), ShopifyTaskErrorCode.MEDIA_ID_MISSING.getCode(), "Shopify 未返回对应的媒体 ID");
                result.append("媒体 ").append(media.getFilename()).append(" 注册失败：Shopify 未返回媒体 ID\n");
                continue;
            }

            Media updateMedia = new Media();
            updateMedia.setMediaId(media.getMediaId());
            updateMedia.setShopifyMediaId(shopifyMediaId);
            int updated = mediaService.updateMedia(updateMedia);
            if (updated <= 0) {
                hasFailure = true;
                recordMediaFailure(taskId, storeId, shopName, product.getProductId(), media,
                        ShopifyTaskStep.MEDIA_REGISTER.getCode(), ShopifyTaskErrorCode.MEDIA_ID_SAVE_FAILED.getCode(), "保存 Shopify 媒体 ID 失败");
                result.append("媒体 ").append(media.getFilename()).append(" 保存 Shopify ID 失败\n");
                continue;
            }

            recordMediaDetail(taskId, storeId, shopName, product.getProductId(), media,
                    ShopifyTaskStep.MEDIA_REGISTER.getCode(), ShopifyTaskDetailStatus.SUCCESS.getCode(),
                    shopifyMediaId, null, null, i, null);
            result.append("已保存媒体 ").append(media.getFilename()).append(" 的 Shopify ID\n");
        }
        return hasFailure;
    }

    private void recordProductApiFailure(Long taskId, Long storeId, String shopName, Product product, ShopifyApiException e) {
        if (e.getUserErrors().isEmpty()) {
            recordDetail(taskId, storeId, shopName, product.getProductId(), ShopifyTaskDetailItemType.PRODUCT.getCode(),
                    product.getProductId(), product.getProductTitle(), null, ShopifyTaskStep.PRODUCT_UPSERT.getCode(),
                    ShopifyTaskDetailStatus.FAILED.getCode(), ShopifyTaskErrorCode.PRODUCT_UPSERT_FAILED.getCode(), null, null, e.getMessage());
            return;
        }
        for (ShopifyUserError error : e.getUserErrors()) {
            recordDetail(taskId, storeId, shopName, product.getProductId(), ShopifyTaskDetailItemType.PRODUCT.getCode(),
                    product.getProductId(), product.getProductTitle(), null, ShopifyTaskStep.PRODUCT_UPSERT.getCode(),
                    ShopifyTaskDetailStatus.FAILED.getCode(), error.getCode(), error.getField(),
                    error.getInputIndex(), error.getMessage());
        }
    }

    private void recordVariantApiFailures(Long taskId, Long storeId, String shopName, Long productId,
                                          List<ProductVariant> variants, ShopifyApiException e) {
        if (e.getUserErrors().isEmpty()) {
            recordVariantFailures(taskId, storeId, shopName, productId, variants,
                    ShopifyTaskErrorCode.VARIANT_CREATE_FAILED.getCode(), null, null, e.getMessage());
            return;
        }

        Set<Integer> recordedIndexes = new HashSet<>();
        for (ShopifyUserError error : e.getUserErrors()) {
            Integer inputIndex = error.getInputIndex();
            if (inputIndex != null && inputIndex >= 0 && inputIndex < variants.size()) {
                ProductVariant variant = variants.get(inputIndex);
                recordVariantDetail(taskId, storeId, shopName, productId, variant,
                        ShopifyTaskStep.VARIANT_CREATE.getCode(), ShopifyTaskDetailStatus.FAILED.getCode(),
                        null, error.getCode(), error.getField(), inputIndex, error.getMessage());
                recordedIndexes.add(inputIndex);
            }
        }

        for (int i = 0; i < variants.size(); i++) {
            if (recordedIndexes.contains(i)) {
                continue;
            }
            recordVariantDetail(taskId, storeId, shopName, productId, variants.get(i),
                    ShopifyTaskStep.VARIANT_CREATE.getCode(), ShopifyTaskDetailStatus.FAILED.getCode(),
                    null, ShopifyTaskErrorCode.VARIANT_CREATE_FAILED.getCode(), null, i,
                    "同批次变体创建失败，Shopify 未返回该变体的单项错误");
        }
    }

    private void recordVariantFailures(Long taskId, Long storeId, String shopName, Long productId,
                                       List<ProductVariant> variants, String errorCode, String errorField,
                                       Integer inputIndex, String errorMessage) {
        for (int i = 0; i < variants.size(); i++) {
            ProductVariant variant = variants.get(i);
            recordVariantDetail(taskId, storeId, shopName, productId, variant,
                    ShopifyTaskStep.VARIANT_CREATE.getCode(), ShopifyTaskDetailStatus.FAILED.getCode(),
                    null, errorCode, errorField, inputIndex == null ? i : inputIndex, errorMessage);
        }
    }

    private void recordVariantDetail(Long taskId, Long storeId, String shopName, Long productId, ProductVariant variant,
                                     String step, String status, String shopifyId, String errorCode,
                                     String errorField, Integer inputIndex, String errorMessage) {
        recordDetail(taskId, storeId, shopName, productId, ShopifyTaskDetailItemType.VARIANT.getCode(),
                variant.getVariantId(), resolveVariantName(variant), shopifyId, step, status,
                errorCode, errorField, inputIndex, errorMessage);
    }

    private void recordMediaFailure(Long taskId, Long storeId, String shopName, Long productId, Media media,
                                    String step, String errorCode, String errorMessage) {
        recordMediaDetail(taskId, storeId, shopName, productId, media, step,
                ShopifyTaskDetailStatus.FAILED.getCode(), null, errorCode, null, null, errorMessage);
    }

    private void recordMediaDetail(Long taskId, Long storeId, String shopName, Long productId, Media media,
                                   String step, String status, String shopifyId, String errorCode,
                                   String errorField, Integer inputIndex, String errorMessage) {
        recordDetail(taskId, storeId, shopName, productId, ShopifyTaskDetailItemType.MEDIA.getCode(),
                media.getMediaId(), media.getFilename(), shopifyId, step, status,
                errorCode, errorField, inputIndex, errorMessage);
    }

    private void recordDetail(Long taskId, Long storeId, String shopName, Long productId, String itemType,
                              Long itemId, String itemName, String shopifyId, String step, String status,
                              String errorCode, String errorField, Integer inputIndex, String errorMessage) {
        if (taskId == null || shopifyTaskDetailService == null) {
            return;
        }
        ShopifyTaskDetail detail = new ShopifyTaskDetail();
        detail.setTaskId(taskId);
        detail.setStoreId(storeId);
        detail.setShopName(shopName);
        detail.setProductId(productId);
        detail.setItemType(itemType);
        detail.setItemId(itemId);
        detail.setItemName(itemName);
        detail.setShopifyId(shopifyId);
        detail.setStep(step);
        detail.setStatus(status);
        detail.setErrorCode(errorCode);
        detail.setErrorField(errorField);
        detail.setInputIndex(inputIndex);
        detail.setErrorMessage(errorMessage);
        detail.setCreateTime(DateUtils.getNowDate());
        shopifyTaskDetailService.save(detail);
    }

    private InventoryItemInput buildInventoryItem(ShopifyStore store, ProductVariant variant) {
        return InventoryItemInput.builder()
                .sku(variant.getSku())
                .tracked("1".equals(store.getInventoryTracked()))
                .build();
    }

    private List<InventoryQuantity> buildInventoryQuantities(ShopifyStore store) {
        if (!"1".equals(store.getInventoryTracked())) {
            return null;
        }
        if (StringUtils.isEmpty(store.getInventoryLocationId())) {
            throw new RuntimeException("店铺启用库存跟踪但未配置库存仓库 Location ID");
        }
        return List.of(InventoryQuantity.builder()
                .locationId(store.getInventoryLocationId())
                .availableQuantity(store.getDefaultInventoryQuantity() == null ? 100 : store.getDefaultInventoryQuantity())
                .build());
    }

    private String resolveInventoryPolicy(ShopifyStore store) {
        return StringUtils.isEmpty(store.getInventoryPolicy()) ? "DENY" : store.getInventoryPolicy();
    }

    private List<Media> filterCreateMediaList(List<Media> uploadMediaList) {
        if (uploadMediaList == null || uploadMediaList.isEmpty()) {
            return List.of();
        }
        return uploadMediaList.stream()
                .filter(media -> media != null
                        && StringUtils.isEmpty(media.getShopifyMediaId())
                        && StringUtils.isNotEmpty(media.getShopifyMediaUrl()))
                .toList();
    }

    private List<CreateMediaInput> buildMediaInputs(List<Media> uploadMediaList, Product product) {
        List<CreateMediaInput> mediaInputs = new ArrayList<>();
        if (uploadMediaList != null && !uploadMediaList.isEmpty()) {
            for (Media media : uploadMediaList) {
                if (media != null && StringUtils.isNotEmpty(media.getShopifyMediaUrl())) {
                    mediaInputs.add(CreateMediaInput.builder()
                            .originalSource(media.getShopifyMediaUrl())
                            .alt(product.getProductTitle() + ProductConstants.PRODUCT_MEDIA_SUFFIX)
                            .mediaContentType(media.getMediaContentType())
                            .build());
                }
            }
        }
        return mediaInputs;
    }

    private List<OptionValueInput> parseOptionValuesAsList(String optionValuesJson) {
        List<OptionValueInput> options = new ArrayList<>();
        if (StringUtils.isEmpty(optionValuesJson)) {
            return options;
        }
        List<ProductVariantOption> optionValueList = JSON.parseArray(optionValuesJson, ProductVariantOption.class);

        try {
            for (ProductVariantOption opt : optionValueList) {
                options.add(OptionValueInput.builder()
                        .name(opt.getEnglishValue())
                        .optionName(opt.getEnglishName())
                        .build());
            }
        } catch (Exception e) {
            log.warn("解析 optionValues 失败: {}", optionValuesJson, e);
        }
        return options;
    }

    private ProductInput buildProductInput(Product product, List<Media> uploadMediaList,
                                                                List<String> tagNames, String defaultProductStatus) {
        ProductInput input = ProductInput.builder()
                .title(product.getProductTitle())
                .descriptionHtml(product.getBodyHtml())
                .productType(product.getProductType())
                .vendor(ProductConstants.DEFAULT_PRODUCT_VENDER)
                .category(product.getCategory())
                .seo(SeoInput.builder()
                        .title(product.getProductTitle() + ProductConstants.SEO_TITTLE_SUFFIX)
                        .description(product.getDescription())
                        .build())
                .tags(tagNames)
                .status(defaultProductStatus)
                .build();

        if (StringUtils.isNotEmpty(product.getOptionJson())) {
            try {
                List<ProductOption> optionList = JSON.parseArray(product.getOptionJson(), ProductOption.class);
                List<ProductOptionInput> options = new ArrayList<>();
                for (int i = 0; i < optionList.size(); i++) {
                    ProductOption opt = optionList.get(i);
                    List<OptionValueInput> valueInputs = opt.getValues().stream()
                            .map(value -> OptionValueInput.builder()
                                    .name(value.getEnglishValue())
                                    .build())
                            .collect(Collectors.toList());

                    options.add(ProductOptionInput.builder()
                            .name(opt.getEnglishName())
                            .position(i + 1)
                            .values(valueInputs)
                            .build());
                }
                input.setProductOptions(options);
            } catch (Exception e) {
                log.warn("解析 optionJson 失败: {}", product.getOptionJson(), e);
            }
        }

        if (StringUtils.isNotBlank(product.getSpu())) {
            List<ProductMetafield> metafieldInput = new ArrayList<>();
            metafieldInput.add(ProductMetafield.builder()
                    .key("SPU")
                    .namespace("custom")
                    .value(product.getSpu())
                    .build());
            input.setMetafields(metafieldInput);
        }
        return input;
    }

    private String resolveDefaultProductStatus(ShopifyStore store) {
        if (store == null || StringUtils.isEmpty(store.getDefaultProductStatus())) {
            return ShopifyProductStatus.DRAFT.name();
        }
        String status = store.getDefaultProductStatus().trim().toUpperCase();
        if (StoreConstants.PRODUCT_STATUS_ACTIVE.equals(status)) {
            return ShopifyProductStatus.ACTIVE.name();
        }
        if (StoreConstants.PRODUCT_STATUS_DRAFT.equals(status)) {
            return ShopifyProductStatus.DRAFT.name();
        }
        return ShopifyProductStatus.DRAFT.name();
    }

    private String resolveVariantName(ProductVariant variant) {
        if (variant == null) {
            return "";
        }
        if (StringUtils.isNotEmpty(variant.getSku())) {
            return variant.getSku();
        }
        return String.valueOf(variant.getVariantId());
    }

    @Getter
    public static class ProductSyncResult {
        private final StringBuilder summary;
        private boolean productCreatedOrUpdated;
        private boolean childFailure;

        public ProductSyncResult() {
            this(new StringBuilder());
        }

        private ProductSyncResult(StringBuilder summary) {
            this.summary = summary;
        }

        public ProductSyncResult append(Object value) {
            summary.append(value);
            return this;
        }

        public String getSummary() {
            return summary.toString();
        }

        public boolean hasChildFailure() {
            return childFailure;
        }

        public void setProductCreatedOrUpdated(boolean productCreatedOrUpdated) {
            this.productCreatedOrUpdated = productCreatedOrUpdated;
        }

        public void setChildFailure(boolean childFailure) {
            this.childFailure = childFailure;
        }
    }

    @Getter
    private static class MediaUploadResult {
        private final List<Media> mediaList = new ArrayList<>();
        private boolean failure;

        void add(Media media) {
            mediaList.add(media);
        }

        void markFailure() {
            failure = true;
        }

        boolean hasFailure() {
            return failure;
        }
    }
}

package com.ruoyi.erp.executor;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.enums.ShopifyProductStatusEnum;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.config.ShopifyGraphQLClient;
import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.constant.ShopifyTaskContants;
import com.ruoyi.erp.exception.ShopifyApiException;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ShopifyStoreMapper;
import com.ruoyi.erp.model.domain.*;
import com.ruoyi.erp.service.*;
import com.ruoyi.erp.utils.MediaFileUtil;
import com.ruoyi.erp.utils.PriceUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shopify 批量同步执行器
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
    private IShopifyTaskService shopifyTaskService;
    @Resource
    private ITagDictService tagDictService;
    @Resource
    private IProductTagRelService productTagRelService;
    @Resource
    private ShopifyStoreMapper shopifyStoreMapper;
    @Resource
    private ProductMapper productMapper;

    /**
     * 异步执行批量推送
     */
    @Async("threadPoolTaskExecutor")
    public void execute(Long taskId, List<Long> productIds) {
        long startTime = System.currentTimeMillis();
        ShopifyTask task = shopifyTaskService.selectShopifyTaskByTaskId(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return;
        }

        // 获取店铺 ID: 优先从任务获取
        Long storeId = resolveStoreId(task);
        if (storeId == null) {
            log.error("无法确定店铺ID，请检查任务或商品的店铺配置: taskId={}", taskId);
            task.setTaskStatus(ProductConstants.SYNC_STATUS_FAILED);
            task.setErrorMessage("无法确定店铺ID，请检查任务或商品的店铺配置");
            task.setEndTime(new Date());
            shopifyTaskService.updateShopifyTask(task);
            return;
        }

        // 获取店铺名称用于日志
        ShopifyStore store = shopifyStoreMapper.selectById(storeId);
        String shopName = store != null ? store.getShopName() : "unknown";

        int total = productIds.size();
        int success = 0;
        int failed = 0;
        StringBuilder errorSummary = new StringBuilder();

        task.setTaskStatus(ProductConstants.SYNC_STATUS_RUNNING);
        task.setProgress(0);
        task.setStartTime(new Date());
        shopifyTaskService.updateShopifyTask(task);

        log.info("开始同步商品到 Shopify 店铺: {}, storeId={}, 商品数量={}", shopName, storeId, total);

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            int index = i + 1;

            try {
                String syncResult = syncProduct(storeId, productId, index, total);

                Product p = new Product();
                p.setProductId(productId);
                p.setSyncStatus(ProductConstants.SYNC_STATUS_SUCCESS);
                p.setSyncMessage(syncResult);
                p.setLastSyncTime(DateUtils.getNowDate());
                productMapper.updateById(p);

                success++;
            } catch (Exception e) {
                log.error("推送商品失败: productId={}", productId, e);

                Product p = new Product();
                p.setProductId(productId);
                p.setSyncStatus(ProductConstants.SYNC_STATUS_FAILED);
                p.setSyncMessage(e.getMessage());
                p.setLastSyncTime(DateUtils.getNowDate());
                productMapper.updateById(p);

                failed++;
                errorSummary.append(String.format("[商品%d] %s\n", index, e.getMessage()));
            }

            // 更新进度
            int progress = (int) ((i + 1) * 100.0 / total);
            task.setProgress(progress);
            task.setSuccessCount(success);
            task.setFailedCount(failed);
            shopifyTaskService.updateShopifyTask(task);
        }

        // 更新店铺同步统计
        if (store != null) {
            store.setLastSyncTime(new Date());
            store.setSyncCount((store.getSyncCount() == null ? 0 : store.getSyncCount()) + 1);
            shopifyStoreMapper.updateById(store);
        }

        // 最终状态
        task.setTaskStatus(failed == 0 ? ShopifyTaskContants.TASK_STATUS_SUCCESS : (success == 0 ? ShopifyTaskContants.TASK_STATUS_FAILED : ShopifyTaskContants.TASK_STATUS_PART_SUCCESS));
        task.setProgress(100);
        task.setEndTime(new Date());
        task.setExecutionTime(System.currentTimeMillis() - startTime);
        task.setErrorMessage(errorSummary.toString());
        shopifyTaskService.updateShopifyTask(task);

        log.info("商品同步完成: shop={}, 成功={}, 失败={}, 耗时={}ms", shopName, success, failed, System.currentTimeMillis() - startTime);
    }

    /**
     * 解析店铺 ID
     * 优先从任务获取，否则从商品列表的第一个商品获取
     */
    private Long resolveStoreId(ShopifyTask task) {
        // 优先从任务获取
        if (task.getStoreId() != null) {
            return task.getStoreId();
        }
        // 最后尝试从任务关联的店铺名获取默认店铺
        if (task.getShopName() != null) {
            ShopifyStore store = shopifyStoreMapper.selectByShopName(task.getShopName());
            if (store != null) {
                return store.getStoreId();
            }
        }

        // 尝试获取默认店铺
        ShopifyStore defaultStore = shopifyStoreMapper.selectDefaultStore();
        if (defaultStore != null) {
            return defaultStore.getStoreId();
        }

        return null;
    }

    /**
     * 同步单个商品到 Shopify
     * @param storeId 店铺 ID
     * @param productId 商品 ID
     * @param index 当前索引
     * @param total 总数
     * @return 同步结果摘要
     */
    public String syncProduct(Long storeId, Long productId, int index, int total) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("[商品「%s」] 第 %d/%d 个\n", product.getProductTitle(), index, total));

        // 1. 上传媒体
        log.info("[商品「{}」] 上传媒体", product.getProductTitle());
        List<Media> uploadMediaList = uploadMedia(storeId, productId, result);
        log.info("[商品「{}」] 上传媒体完成", product.getProductTitle());

        // 2. 获取标签名称
        List<String> tagNames = productTagRelService.selectTagCodeListByProductId(productId);

        // 3. 构建商品输入和媒体列表
        ShopifyGraphQLClient.ProductInput productInput = buildProductInput(product, uploadMediaList, tagNames);
        List<Media> createMediaList = filterCreateMediaList(uploadMediaList);
        List<ShopifyGraphQLClient.CreateMediaInput> mediaInputs = buildMediaInputs(createMediaList, product);

        // 4. 创建/更新商品
        String shopifyProductId;
        List<String> shopifyMediaIds = new ArrayList<>();
        boolean isCreateShopifyProduct = StringUtils.isEmpty(product.getShopifyProductId());
        log.info("[商品「{}」] 创建/更新商品", product.getProductTitle());
        if (!isCreateShopifyProduct) {
            ShopifyGraphQLClient.ProductCreateResult updateResult = shopifyGraphQLClient.updateProduct(storeId, product.getShopifyProductId(), productInput, mediaInputs);
            shopifyProductId = updateResult.getProductId();
            shopifyMediaIds = updateResult.getMediaIds();
            result.append("✅ 商品更新成功 → PID: ").append(shopifyProductId).append("\n");
        } else {
            ShopifyGraphQLClient.ProductCreateResult createResult = shopifyGraphQLClient.createProduct(storeId, productInput, mediaInputs);
            shopifyProductId = createResult.getProductId();
            shopifyMediaIds = createResult.getMediaIds();
            result.append("✅ 商品创建成功 → PID: ").append(shopifyProductId).append("\n");
        }
        log.info("[商品「{}」] 创建/更新商品完成", product.getProductTitle());

        // 更新商品的 Shopify ID
        if (!shopifyMediaIds.isEmpty() && !createMediaList.isEmpty()) {
            saveMediaIdsToDatabase(createMediaList, shopifyMediaIds);
            result.append("✅ 已保存 ").append(shopifyMediaIds.size()).append(" 个媒体 ID\n");
        }

        Product updateProduct = new Product();
        updateProduct.setProductId(productId);
        updateProduct.setShopifyProductId(shopifyProductId);
        productMapper.updateById(updateProduct);

        // 5. 批量创建变体
        List<ProductVariant> variants = productVariantService.selectListByProductId(productId);
        if (!variants.isEmpty()) {
            log.info("[商品「{}」] 批量创建变体", product.getProductTitle());
            String variantCreateStrategy = isCreateShopifyProduct ? VARIANT_CREATE_STRATEGY_REMOVE_STANDALONE : null;
            syncVariants(storeId, shopifyProductId, variants, result, variantCreateStrategy);
            log.info("[商品「{}」] 批量创建变体完成", product.getProductTitle());
        }

        return result.toString();
    }

    /**
     * 上传媒体文件
     */
    private List<Media> uploadMedia(Long storeId, Long productId, StringBuilder result) {
        List<Media> uploadMediaList = new ArrayList<>();

        List<Media> mediaList = mediaService.listByProductId(productId);
        if (mediaList == null || mediaList.isEmpty()) {
            return uploadMediaList;
        }

        for (Media media : mediaList) {
            if (media == null) continue;

            try {
                // 如果已经有 Shopify media ID，跳过上传
                if (StringUtils.isNotEmpty(media.getShopifyMediaId())) {
                    result.append("media ").append(media.getFilename()).append(" already has Shopify Media ID, skip\n");
                    continue;
                }

                if (StringUtils.isNotEmpty(media.getShopifyMediaUrl())) {
                    uploadMediaList.add(media);
                    result.append("⚠️ 图片「").append(media.getFilename()).append("」已存在，跳过\n");
                    continue;
                }

                // 获取本地文件路径并上传
                String localPath = media.getNasMediaUrl();
                if (StringUtils.isEmpty(localPath)) {
                    result.append("⚠️ 图片「").append(media.getFilename()).append("」无本地路径，跳过\n");
                    continue;
                }

                // 根据媒体类型确定 MIME
                String mimeType = "VIDEO".equals(media.getMediaContentType()) ? "video/mp4" : "image/png";
                if (localPath.toLowerCase().endsWith(".jpg") || localPath.toLowerCase().endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                } else if (localPath.toLowerCase().endsWith(".webp")) {
                    mimeType = "image/webp";
                }

                // 读取文件并上传
                String oldFilePath = MediaFileUtil.convertUrlToFilePath(media.getNasMediaUrl());
                File file = new File(oldFilePath);
                if (!file.exists()) {
                    result.append("❌ 图片「").append(media.getFilename()).append("」文件不存在: ").append(localPath).append("\n");
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    ShopifyGraphQLClient.StagedUploadResult uploadResult = shopifyGraphQLClient.stagedUploadMedia(
                            storeId,
                            media.getFilename(),
                            mimeType,
                            fis,
                            file.length()
                    );


                    // 更新 media 表的 shopify media ID
                    Media updateMedia = new Media();
                    updateMedia.setMediaId(media.getMediaId());
                    updateMedia.setShopifyMediaUrl(uploadResult.resourceUrl());
                    mediaService.updateMedia(updateMedia);
                    media.setShopifyMediaUrl(uploadResult.resourceUrl());
                    uploadMediaList.add(media);

                    result.append("✅ 图片「").append(media.getFilename()).append("」上传成功\n");
                }
            } catch (Exception e) {
                log.error("图片「" + media.getFilename() + "」上传失败", e);
                result.append("❌ 图片「").append(media.getFilename()).append("」上传失败: ").append(e.getMessage()).append("\n");
            }
        }
        return uploadMediaList;
    }

    /**
     * 同步变体
     */
    private void syncVariants(Long storeId, String shopifyProductId, List<ProductVariant> variants, StringBuilder result, String variantCreateStrategy) {
        List<ShopifyGraphQLClient.VariantInput> variantInputs = new ArrayList<>();
        List<ProductVariant> createVariants = new ArrayList<>();
        ShopifyStore store = shopifyStoreMapper.selectById(storeId);
        if (store == null) {
            throw new RuntimeException("店铺不存在: " + storeId);
        }

        for (ProductVariant variant : variants) {
            if (StringUtils.isNotEmpty(variant.getShopifyVariantId())) {
                result.append("变体").append(variant.getVariantId()).append("已存在 Shopify ID，跳过创建\n");
                continue;
            }

            BigDecimal price = PriceUtil.fenToYuan(variant.getPrice());
            BigDecimal compareAtPrice = PriceUtil.fenToYuan(variant.getCompareAtPrice());

            Long mediaId = variant.getMediaId();
            String shopifyMediaId = null;
            if (mediaId != null) {
                Media media = mediaService.getOne(new LambdaQueryWrapper<>(Media.class)
                        .select(Media::getShopifyMediaId, Media::getShopifyMediaUrl).eq(Media::getMediaId, mediaId));
                if (media != null) {
                    shopifyMediaId = media.getShopifyMediaId();
                }
            }

            ShopifyGraphQLClient.VariantInput input = ShopifyGraphQLClient.VariantInput.builder()
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
            return;
        }

        try {
            List<String> variantIds = shopifyGraphQLClient.createVariantsBulk(storeId, shopifyProductId, variantInputs, variantCreateStrategy);
            saveVariantIdsToDatabase(createVariants, variantIds, result);
        } catch (ShopifyApiException e) {
            result.append("❌ 变体批量创建失败: ").append(e.getMessage()).append("\n");
            throw e;
        }
    }

    /**
     * 保存 Shopify 变体 ID 到数据库
     */
    private void saveVariantIdsToDatabase(List<ProductVariant> variants, List<String> shopifyVariantIds, StringBuilder result) {
        if (shopifyVariantIds == null || shopifyVariantIds.size() != variants.size()) {
            throw new ShopifyApiException("Shopify 返回的变体 ID 数量与本地待同步变体数量不一致");
        }

        for (int i = 0; i < variants.size(); i++) {
            ProductVariant variant = variants.get(i);
            String shopifyVariantId = shopifyVariantIds.get(i);
            if (StringUtils.isEmpty(shopifyVariantId)) {
                throw new ShopifyApiException("Shopify 返回了空的变体 ID");
            }

            ProductVariant updateVariant = new ProductVariant();
            updateVariant.setVariantId(variant.getVariantId());
            updateVariant.setShopifyVariantId(shopifyVariantId);
            int updated = productVariantService.updateProductVariant(updateVariant);
            if (updated <= 0) {
                throw new ShopifyApiException("保存 Shopify 变体 ID 失败，variantId=" + variant.getVariantId());
            }

            result.append("✅ 变体").append(i + 1).append("创建成功 → VID: ").append(shopifyVariantId).append("\n");
        }
    }

    private ShopifyGraphQLClient.InventoryItemInput buildInventoryItem(ShopifyStore store, ProductVariant variant) {
        return ShopifyGraphQLClient.InventoryItemInput.builder()
                .sku(variant.getSku())
                .tracked("1".equals(store.getInventoryTracked()))
                .build();
    }

    private List<ShopifyGraphQLClient.InventoryQuantity> buildInventoryQuantities(ShopifyStore store) {
        if (!"1".equals(store.getInventoryTracked())) {
            return null;
        }
        if (StringUtils.isEmpty(store.getInventoryLocationId())) {
            throw new RuntimeException("店铺启用库存跟踪但未配置库存仓库 Location ID");
        }
        return List.of(ShopifyGraphQLClient.InventoryQuantity.builder()
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

    private void saveMediaIdsToDatabase(List<Media> uploadMediaList, List<String> shopifyMediaIds) {
        if (uploadMediaList == null || uploadMediaList.isEmpty() || shopifyMediaIds.isEmpty()) {
            return;
        }

        // 按顺序匹配 media 和 mediaId
        for (int i = 0; i < Math.min(uploadMediaList.size(), shopifyMediaIds.size()); i++) {
            Media media = uploadMediaList.get(i);
            String shopifyMediaId = shopifyMediaIds.get(i);

            if (media != null && StringUtils.isNotEmpty(shopifyMediaId)) {
                try {
                    Media updateMedia = new Media();
                    updateMedia.setMediaId(media.getMediaId());
                    updateMedia.setShopifyMediaId(shopifyMediaId);
                    mediaService.updateMedia(updateMedia);
                    log.info("已保存 media ID: mediaId={}, shopifyMediaId={}", media.getMediaId(), shopifyMediaId);
                } catch (Exception e) {
                    log.error("保存 media ID 失败: mediaId={}", media.getMediaId(), e);
                }
            }
        }
    }

    /**
     * 构建媒体输入列表
     */
    private List<ShopifyGraphQLClient.CreateMediaInput> buildMediaInputs(List<Media> uploadMediaList, Product product) {
        List<ShopifyGraphQLClient.CreateMediaInput> mediaInputs = new ArrayList<>();
        
        if (uploadMediaList != null && !uploadMediaList.isEmpty()) {
            for (Media media : uploadMediaList) {
                if (media != null && StringUtils.isNotEmpty(media.getShopifyMediaUrl())) {
                    mediaInputs.add(ShopifyGraphQLClient.CreateMediaInput.builder()
                            .originalSource(media.getShopifyMediaUrl())
                            .alt(product.getProductTitle() + ProductConstants.PRODUCT_MEDIA_SUFFIX)
                            .mediaContentType(media.getMediaContentType())
                            .build());
                }
            }
        }
        
        return mediaInputs;
    }

    /**
     * 解析 optionValues JSON 为 List<OptionValueInput>
     */
    private List<ShopifyGraphQLClient.OptionValueInput> parseOptionValuesAsList(String optionValuesJson) {
        List<ShopifyGraphQLClient.OptionValueInput> options = new ArrayList<>();
        if (StringUtils.isEmpty(optionValuesJson)) {
            return options;
        }
        List<ProductVariantOption> optionValueList = JSON.parseArray(
                optionValuesJson, ProductVariantOption.class);

        try {
            for (ProductVariantOption opt : optionValueList) {
                options.add(ShopifyGraphQLClient.OptionValueInput.builder()
                        .name(opt.getEnglishValue())
                        .optionName(opt.getEnglishName())
                        .build());
            }
        } catch (Exception e) {
            log.warn("解析 optionValues 失败: {}", optionValuesJson, e);
        }
        return options;
    }

    /**
     * 构建商品 GraphQL 输入
     */
    private ShopifyGraphQLClient.ProductInput buildProductInput(Product product, List<Media> uploadMediaList, List<String> tagNames) {
        ShopifyGraphQLClient.ProductInput input = ShopifyGraphQLClient.ProductInput.builder()
                .title(product.getProductTitle())
                .descriptionHtml(product.getBodyHtml())
                .productType(product.getProductType())
                .vendor(ProductConstants.DEFAULT_PRODUCT_VENDER)
                .category(product.getCategory())
                .seo(ShopifyGraphQLClient.SeoInput.builder()
                        .title(product.getProductTitle() + ProductConstants.SEO_TITTLE_SUFFIX)
                        .description(product.getDescription())
                        .build())
                .tags(tagNames)
                .status(ShopifyProductStatusEnum.DRAFT.name())
                .build();

        // 处理选项 - 根据 Shopify 2026-04 API，values 需要是 OptionValueInput 对象数组
        if (StringUtils.isNotEmpty(product.getOptionJson())) {
            try {
                List<ProductOption> optionList = JSON.parseArray(product.getOptionJson(), ProductOption.class);
                List<ShopifyGraphQLClient.ProductOptionInput> options = new ArrayList<>();
                for (int i = 0; i < optionList.size(); i++) {
                    ProductOption opt = optionList.get(i);
                    // 将字符串值转换为 OptionValueInput 对象
                    List<ShopifyGraphQLClient.OptionValueInput> valueInputs = opt.getValues().stream()
                            .map(value -> ShopifyGraphQLClient.OptionValueInput.builder()
                                    .name(value.getEnglishValue())
                                    .build())
                            .collect(Collectors.toList());
                    
                    options.add(ShopifyGraphQLClient.ProductOptionInput.builder()
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
        
        if(StringUtils.isNotBlank(product.getSpu())){
            List<ShopifyGraphQLClient.ProductMetafield> metafieldInput = new ArrayList<>();
            metafieldInput.add(ShopifyGraphQLClient.ProductMetafield.builder()
                    .key("SPU")
                    .namespace("custom")
                    .value(product.getSpu())
                    .build());
            input.setMetafields(metafieldInput);
        }
        return input;
    }
}

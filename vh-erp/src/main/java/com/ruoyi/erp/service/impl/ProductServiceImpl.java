package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.config.ShopifyGraphQLClient;
import com.ruoyi.erp.constant.ShopifyTaskContants;
import com.ruoyi.erp.executor.ShopifySyncExecutor;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.model.domain.*;
import com.ruoyi.erp.model.dto.product.ProductQuery;
import com.ruoyi.erp.model.vo.media.MediaVo;
import com.ruoyi.erp.model.vo.product.ProductVo;
import com.ruoyi.erp.model.vo.product.ProductWorkbenchSummaryVo;
import com.ruoyi.erp.service.*;
import com.ruoyi.erp.util.ProductListMetrics;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * erp商品Service业务层处理
 *
 * @author lwj
 * @date 2026-03-26
 */
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    @Resource
    private ProductMapper productMapper;
    @Resource
    private IProductTagRelService productTagRelService;
    @Resource
    private IProductVariantService productVariantService;
    @Resource
    private IMediaService mediaService;
    @Resource
    private IShopifyTaskService shopifyTaskService;
    @Resource
    private ShopifySyncExecutor shopifySyncExecutor;
    @Resource
    private IShopifyStoreService shopifyStoreService;
    @Resource
    private ShopifyGraphQLClient shopifyGraphQLClient;
    @Resource
    private IProductQualityService productQualityService;

    /**
     * 查询erp商品
     *
     * @param productId erp商品主键
     * @return erp商品
     */
    @Override
    public Product selectProductByProductId(Long productId) {
        Product product = this.getById(productId);
        if (StringUtils.isNull(product)) {
            return null;
        }
        product.setTagIds(productTagRelService.getTagIdListByProductId(productId));
        List<ProductVariant> productVariants = productVariantService.selectListByProductId(productId);
        List<Media> media = mediaService.listByProductId(productId);
        Map<Long, MediaVo> collect = media.stream().collect(Collectors.toMap(Media::getMediaId, MediaVo::objToVo, (oldVal, newVal) -> newVal));
        productVariants.forEach(x->x.setMedia(collect.get(x.getMediaId())));

        product.setProductVariantList(productVariants);
        product.setMediaList(media);
        return product;
    }

    private ProductVo enrichProductVo(Product product, Product query) {
        ProductVo vo = ProductVo.objToVo(product);
        Long productId = product.getProductId();
        vo.setTagCodeList(productTagRelService.selectTagCodeListByProductId(productId));

        List<ProductVariant> variants = productVariantService.selectListByProductId(productId);
        List<Media> medias = mediaService.listByProductId(productId);
        ShopifyTask latestTask = selectLatestProductTask(productId, query.getStoreId());
        ProductListMetrics.apply(vo, product, variants, medias, latestTask);
        return vo;
    }

    private ShopifyTask selectLatestProductTask(Long productId, Long storeId) {
        if (productId == null) {
            return null;
        }
        LambdaQueryWrapper<ShopifyTask> wrapper = new LambdaQueryWrapper<ShopifyTask>()
                .eq(ShopifyTask::getBusinessType, ShopifyTaskContants.TASK_BUSINESS_TYPE_PRODUCT)
                .like(ShopifyTask::getBusinessIds, String.valueOf(productId))
                .orderByDesc(ShopifyTask::getCreateTime)
                .last("limit 20");
        if (storeId != null) {
            wrapper.eq(ShopifyTask::getStoreId, storeId);
        }
        return shopifyTaskService.list(wrapper).stream()
                .filter(task -> businessIdsContain(task.getBusinessIds(), productId))
                .findFirst()
                .orElse(null);
    }

    private boolean businessIdsContain(String businessIds, Long productId) {
        if (StringUtils.isEmpty(businessIds) || productId == null) {
            return false;
        }
        return Arrays.stream(businessIds.split(","))
                .map(String::trim)
                .anyMatch(id -> String.valueOf(productId).equals(id));
    }

    /**
     * 查询erp商品列表
     *
     * @param product erp商品
     * @return erp商品
     */
    @Override
    public List<ProductVo> selectProductList(Product product) {
        Product query = product == null ? new Product() : product;
        List<Product> list = baseMapper.selectProductList(query);
        return list.stream()
                .map(item -> enrichProductVo(item, query))
                .collect(Collectors.toList());
    }

    @Override
    public ProductWorkbenchSummaryVo getWorkbenchSummary(Product product) {
        Product query = product == null ? new Product() : product;
        return baseMapper.selectWorkbenchSummary(query);
    }

    /**
     * 批量删除erp商品
     *
     * @param productIds 需要删除的erp商品主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteProductByProductIds(Long[] productIds) {
        productMapper.deleteProductVariantByProductIds(productIds);
        return productMapper.deleteProductByProductIds(productIds);
    }

    @Override
    public List<ProductVo> convertVoList(List<Product> productList) {
        if (StringUtils.isEmpty(productList)) {
            return Collections.emptyList();
        }
        return productList.stream().map(ProductVo::objToVo).collect(Collectors.toList());
    }

    /**
     * 导入erp商品数据
     *
     * @param productList     erp商品数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importProductData(List<Product> productList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isEmpty(productList)) {
            throw new ServiceException("导入erp商品数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Product product : productList) {
            try {
                // 验证是否存在这个erp商品
                Long productId = product.getProductId();
                Product productExist = null;
                if (StringUtils.isNotNull(productId)) {
                    productExist = this.getById(productId);
                }
                if (StringUtils.isNull(productExist)) {
                    product.setCreateTime(DateUtils.getNowDate());
                    this.save(product);
                    productQualityService.refreshProductMissingFields(product.getProductId());
                    successNum++;
                    String productIdStr = StringUtils.isNotNull(productId) ? productId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、erp商品 " + productIdStr + " 导入成功");
                } else if (isUpdateSupport) {
                    product.setUpdateTime(DateUtils.getNowDate());
                    this.updateById(product);
                    productQualityService.refreshProductMissingFields(productId);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp商品 " + productId.toString() + " 更新成功");
                } else {
                    failureNum++;
                    String productIdStr = StringUtils.isNotNull(productId) ? productId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp商品 " + productIdStr + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                Long productId = product.getProductId();
                String productIdStr = StringUtils.isNotNull(productId) ? productId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp商品 " + productIdStr + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    @Override
    public Long pushBatchAsync(List<Long> productIds) {
        return pushBatchAsync(productIds, null);
    }

    public Long pushBatchAsync(List<Long> productIds, Long storeId) {
        if (productIds == null || productIds.isEmpty()) {
            return null;
        }

        // 创建 ShopifyTask
        ShopifyStore store = resolvePublishStore(storeId);

        ShopifyTask task = new ShopifyTask();
        task.setStoreId(store.getStoreId());
        task.setShopName(store.getShopName());
        task.setTaskName("批量推送商品-" + DateUtils.getNowDate());
        task.setTaskGroup(ShopifyTaskContants.TASK_GROUP_PRODUCT);
        task.setTaskType(ShopifyTaskContants.TASK_TYPE_PRODUCT_SYNC_BATCH);
        task.setBusinessType(ShopifyTaskContants.TASK_BUSINESS_TYPE_PRODUCT);
        task.setBusinessIds(String.join(",", productIds.stream().map(String::valueOf).toList()));
        task.setTaskStatus(ShopifyTaskContants.TASK_STATUS_PENDING);
        task.setProgress(0);
        task.setTotalCount(productIds.size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setCreateTime(new Date());
        shopifyTaskService.insertShopifyTask(task);

        // 异步执行
        shopifySyncExecutor.execute(task.getTaskId(), productIds);

        return task.getTaskId();
    }

    @Override
    public Long pushBatchByCondition(ProductQuery productQuery, Long[] productIds, Long storeId) {
        List<Long> productIdList;

        // 优先使用传入的 productIds
        if (productIds != null && productIds.length > 0) {
            productIdList = Arrays.asList(productIds);
        } else {
            // 使用 productQuery 按条件查询
            Product product = ProductQuery.queryToObj(productQuery);
            List<Product> productList = this.baseMapper.selectProductList(product);
            productIdList = productList.stream().map(Product::getProductId).toList();
        }

        if (productIdList.isEmpty()) {
            return null;
        }

        ShopifyStore store = resolvePublishStore(storeId);

        // 创建 ShopifyTask
        ShopifyTask task = new ShopifyTask();
        task.setTaskName("批量推送商品-" + DateUtils.getNowDate());
        task.setStoreId(store.getStoreId());
        task.setShopName(store.getShopName());
        task.setTaskGroup("0");
        task.setTaskType("PRODUCT_CREATE_BATCH");
        task.setBusinessType("PRODUCT");
        task.setBusinessIds(String.join(",", productIdList.stream().map(String::valueOf).toList()));
        task.setTaskStatus("PENDING");
        task.setProgress(0);
        task.setTotalCount(productIdList.size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setCreateTime(new Date());
        shopifyTaskService.save(task);

        // 异步执行
        shopifySyncExecutor.execute(task.getTaskId(), productIdList);

        return task.getTaskId();
    }

    @Override
    public Object getPushResult(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return shopifyTaskService.selectShopifyTaskByTaskId(taskId);
    }

    @Override
    public Object publishToChannels(Long[] productIds, Long storeId) {
        if (productIds == null || productIds.length == 0) {
            throw new ServiceException("请选择需要发布的商品");
        }

        ShopifyStore store = resolvePublishStore(storeId);
        List<ShopifyGraphQLClient.PublicationInput> publications = buildPublicationInputs(store.getPublishPublicationIds());
        if (publications.isEmpty()) {
            throw new ServiceException("当前店铺未配置发布渠道，请先在店铺配置中维护 Publication ID");
        }

        int successCount = 0;
        int failedCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (Long productId : productIds) {
            Product product = this.getById(productId);
            if (product == null || StringUtils.isEmpty(product.getShopifyProductId())) {
                failedCount++;
                details.add(Map.of(
                        "productId", productId,
                        "success", false,
                        "message", "商品尚未同步到 Shopify"
                ));
                continue;
            }
            try {
                List<ShopifyGraphQLClient.PublicationResult> result = shopifyGraphQLClient.publishProduct(
                        store.getStoreId(),
                        product.getShopifyProductId(),
                        publications
                );
                successCount++;
                details.add(Map.of(
                        "productId", productId,
                        "success", true,
                        "publicationCount", result.size()
                ));
            } catch (Exception e) {
                failedCount++;
                details.add(Map.of(
                        "productId", productId,
                        "success", false,
                        "message", e.getMessage()
                ));
                log.warn("商品发布渠道失败，productId={}", productId, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("details", details);
        return result;
    }

    private List<ShopifyGraphQLClient.PublicationInput> buildPublicationInputs(String publicationIds) {
        if (StringUtils.isEmpty(publicationIds)) {
            return Collections.emptyList();
        }
        return Arrays.stream(publicationIds.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .map(publicationId -> ShopifyGraphQLClient.PublicationInput.builder()
                        .publicationId(publicationId)
                        .build())
                .toList();
    }

    private ShopifyStore resolvePublishStore(Long storeId) {
        ShopifyStore store = storeId != null
                ? shopifyStoreService.selectByStoreId(storeId)
                : shopifyStoreService.selectDefaultStore();
        if (store == null) {
            throw new ServiceException("未找到可用的店铺");
        }
        if ("0".equals(store.getIsActive())) {
            throw new ServiceException("店铺已禁用");
        }
        return store;
    }


}

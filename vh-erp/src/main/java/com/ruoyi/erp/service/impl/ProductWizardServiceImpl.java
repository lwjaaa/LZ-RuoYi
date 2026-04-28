package com.ruoyi.erp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.model.domain.*;
import com.ruoyi.erp.model.dto.product.ProductSaveByExtentionEdit;
import com.ruoyi.erp.model.dto.shipping.ShippingFeeQuery;
import com.ruoyi.erp.model.vo.shipping.ShippingFeeVo;
import com.ruoyi.erp.service.*;
import com.ruoyi.erp.utils.MediaDownloadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author lwj
 * @version 1.0.0
 * @description 商品选品-新增编辑向导逻辑实现类
 * @date 2026/4/2 15:45
 **/
@Service
@Slf4j
public class ProductWizardServiceImpl implements IProductWizardService {

    @Autowired
    private IProductService productService;
    @Autowired
    private IProductVariantService productVariantService;
    @Autowired
    private IProductTagRelService productTagRelService;
    @Autowired
    private ITagDictService tagDictService;
    @Autowired
    private IMediaService mediaService;
    @Autowired
    private MediaDownloadUtil mediaDownloadUtil;
    @Autowired
    private com.ruoyi.erp.utils.FpxApiUtil fpxApiUtil;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private IFormSuggestionService formSuggestionService;

    private static final Integer WIZARD_STEP_FIRST = 1;
    private static final Integer WIZARD_STEP_SECOND = 2;

    /**
     * 运费查询缓存key前缀
     */
    private static final String SHIPPING_FEE_CACHE_KEY = "shipping_fee:";

    /**
     * 运费查询缓存过期时间（1天）
     */
    private static final int SHIPPING_FEE_CACHE_EXPIRE_HOURS = 24;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveProductWithWizard(Product product, int step) {
        // 参数校验
        this.checkProductParam(product, step);

        // 调用通用保存方法
        return this.saveProductCommon(product, step);

    }

    /**
     * 商品保存前的必要条件检查
     *
     * @param product
     * @param step
     * @return void
     * @author lwj
     **/
    private void checkProductParam(Product product, Integer step) {
        if (step == null) {
            // 外部导入的校验
            List<String> collect = product.getMediaUrlList().stream().filter(StringUtils::isNotBlank)
                    .distinct().collect(Collectors.toList());
            product.setMediaUrlList(collect);
        } else {
            // 第一步的校验
            if (WIZARD_STEP_FIRST.equals(step)) {
                if (CollectionUtils.isEmpty(product.getTagIds())) {
                    throw new ServiceException("请选择标签");
                }
                if (StringUtils.isBlank(product.getSpu())) {
                    throw new ServiceException("SPU编号不能为空");
                }
                String prefix = extractSpuPrefix(product.getSpu());
                if (StringUtils.isBlank(prefix)) {
                    throw new IllegalArgumentException("SPU前缀不合法");
                }
            }

            // 商品编辑的第二步，
            if (WIZARD_STEP_SECOND.equals(step)) {
                // 已提前获生成图片列表，所以在此校验图片信息
                Set<Long> keepMediaIds = new HashSet<>();
                if (CollectionUtil.isNotEmpty(product.getMediaList())) {
                    keepMediaIds = product.getMediaList().stream()
                            .filter(x -> StringUtils.isNoneBlank(x.getNasMediaUrl(), x.getFilename()))
                            .map(Media::getMediaId)
                            .collect(Collectors.toSet());
                }
                List<ProductVariant> variantList = product.getProductVariantList();
                if (!CollectionUtils.isEmpty(variantList)) {
                    for (int i = 0; i < variantList.size(); i++) {
                        Long mediaId = variantList.get(i).getMediaId();
                        if (mediaId != null && !keepMediaIds.contains(mediaId)) {
                            throw new ServiceException("第" + (i + 1) + "个变体图片不存在！");
                        }
                    }
                }
                if (product.getMainMediaId() != null && !keepMediaIds.contains(product.getMainMediaId()))
                    throw new ServiceException("主图不存在！");
            }

        }

    }

    @Override
    public List<ShippingFeeVo> calculateShipping(ShippingFeeQuery shippingFeeQuery) {
        log.info("查询运费信息，请求参数：{}", shippingFeeQuery);

        // 参数校验
        if (shippingFeeQuery.getPkWidth() == null || shippingFeeQuery.getPkHeight() == null
                || shippingFeeQuery.getPkLength() == null || shippingFeeQuery.getPkWeight() == null) {
            throw new ServiceException("包裹尺寸和重量不能为空");
        }
        if (CollectionUtil.isEmpty(shippingFeeQuery.getLogisticsCode())) {
            throw new ServiceException("请选择物流渠道");
        }

        // 设置默认值
        String countryCode = shippingFeeQuery.getCountryCode() != null ? shippingFeeQuery.getCountryCode() : "US";

        // 构建缓存key：基于尺寸、重量和物流渠道（不包含国家代码和邮编，因为这些不影响运费计算）
        String cacheKey = buildShippingFeeCacheKey(
                shippingFeeQuery.getPkWidth(),
                shippingFeeQuery.getPkHeight(),
                shippingFeeQuery.getPkLength(),
                shippingFeeQuery.getPkWeight(),
                shippingFeeQuery.getLogisticsCode()
        );

        // 尝试从缓存获取
        List<ShippingFeeVo> cachedResult = redisCache.getCacheObject(cacheKey);
        if (cachedResult != null && !cachedResult.isEmpty()) {
            log.info("从缓存获取运费信息，cacheKey: {},返回结果：{}", cacheKey, cachedResult);
            return cachedResult;
        }

        try {
            // 调用4px API查询运费
            List<ShippingFeeVo> fpxResultList = fpxApiUtil.queryShippingFee(
                    shippingFeeQuery.getPkWidth(),
                    shippingFeeQuery.getPkHeight(),
                    shippingFeeQuery.getPkLength(),
                    shippingFeeQuery.getPkWeight(),
                    countryCode,
                    shippingFeeQuery.getPostalCode(),
                    shippingFeeQuery.getLogisticsCode()
            );

            // 将结果存入缓存，有效期1天
            if (fpxResultList != null && !fpxResultList.isEmpty()) {
                redisCache.setCacheObject(cacheKey, fpxResultList, SHIPPING_FEE_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                log.info("运费信息已缓存，cacheKey: {}, 过期时间: {}小时", cacheKey, SHIPPING_FEE_CACHE_EXPIRE_HOURS);
            }

            return fpxResultList;

        } catch (Exception e) {
            log.error("查询运费失败: {}", e.getMessage(), e);
            // 返回默认运费或抛出异常，根据业务需求决定
            throw new ServiceException("查询运费失败: " + e.getMessage());
        }
    }

    /**
     * 构建运费查询缓存key
     *
     * @param width          宽度
     * @param height         高度
     * @param length         长度
     * @param weight         重量
     * @param logisticsCodes 物流渠道代码列表
     * @return 缓存key
     */
    private String buildShippingFeeCacheKey(Integer width, Integer height, Integer length,
                                            Integer weight, List<String> logisticsCodes) {
        // 排序物流渠道代码，确保相同的内容生成相同的key
        String sortedLogisticsCodes = logisticsCodes.stream()
                .sorted()
                .collect(Collectors.joining(","));

        return SHIPPING_FEE_CACHE_KEY + width + "x" + height + "x" + length + "_" + weight + "g_" + sortedLogisticsCodes;
    }

    /**
     * 根据spu更新标签的当前最大流水号
     *
     * @param spu
     * @return void
     * @author lwj
     **/
    private void updateTagDictPrefix(String spu) {
        // 从 SPU 中提取前缀（假设 SPU 格式为：前缀 + 数字，如 "ABC0001"）
        String prefix = extractSpuPrefix(spu);
        // 提取 SPU 中的数字部分
        Integer seqNum = extractSpuSequence(spu, prefix);
        if (seqNum != null) {
            // 调用 tagDictService 封装的方法更新最大流水号
            tagDictService.updateMaxSeqBySpuPrefix(prefix, seqNum);
        }
    }

    /**
     * 从 SPU 中提取前缀
     *
     * @param spu SPU 编号
     * @return 前缀部分
     */
    private String extractSpuPrefix(String spu) {
        if (StringUtils.isEmpty(spu)) {
            return null;
        }
        // 假设 SPU 格式为：字母前缀 + 数字，如 "ABC0001"
        // 提取开头的字母部分作为前缀
        StringBuilder prefix = new StringBuilder();
        for (char c : spu.toCharArray()) {
            if (Character.isLetter(c)) {
                prefix.append(c);
            } else {
                break;
            }
        }
        return !prefix.isEmpty() ? prefix.toString() : null;
    }

    /**
     * 从 SPU 中提取序列号
     *
     * @param spu    SPU 编号
     * @param prefix 前缀
     * @return 数字序列号
     */
    private Integer extractSpuSequence(String spu, String prefix) {
        if (StringUtils.isEmpty(spu) || StringUtils.isEmpty(prefix)) {
            return null;
        }
        // 提取前缀后面的数字部分
        String seqStr = spu.substring(prefix.length());
        try {
            return Integer.parseInt(seqStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * 保存商品标签关联
     *
     * @param productId 商品 ID
     * @param tagIds    标签 ID 列表
     */
    private void saveProductTags(Long productId, List<Long> tagIds) {
        List<TagDict> tagDicts = tagDictService.listByIds(tagIds);
        if (StringUtils.isEmpty(tagDicts)) {
            throw new ServiceException("标签不存在");
        }

        List<ProductTagRel> tagRelList = new ArrayList<>();
        Date now = DateUtils.getNowDate();
        String createBy = null;
        try {
            createBy = SecurityUtils.getUsername();
        } catch (Exception e) {

        }

        for (Long tagId : tagIds) {
            ProductTagRel tagRel = new ProductTagRel();
            tagRel.setProductId(productId);
            tagRel.setTagId(tagId);
            tagRel.setCreateTime(now);
            tagRel.setCreateBy(createBy);
            tagRelList.add(tagRel);
        }
        // 批量插入
        productTagRelService.saveBatch(tagRelList);
    }

    /**
     * 根据商品 ID 删除标签关联
     *
     * @param productId 商品 ID
     */
    private void deleteProductTagsByProductId(Long productId) {
        LambdaQueryWrapper<ProductTagRel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductTagRel::getProductId, productId);
        productTagRelService.remove(queryWrapper);
    }

    /**
     * 根据选项 JSON 获取选项值Map
     *
     * @param optionJson
     * @return java.util.Map<java.lang.String,java.util.Set<java.lang.String>>
     * @author lwj
     **/
    private Map<String, Set<String>> getOptionMap(String optionJson) {
        List<ProductOption> optionList = JSON.parseArray(optionJson, ProductOption.class);
        Map<String, Set<String>> purchaseOptionMap = new HashMap<>();
        for (ProductOption productOption : optionList) {
            //productOption.getValues()转SET
            Set<String> values = productOption.getValues().stream().map(ProductOptionValue::getEnglishValue).collect(Collectors.toSet());
            purchaseOptionMap.put(productOption.getEnglishName(), values);
        }
        return purchaseOptionMap;
    }

    @Override
    public Long saveProductFromExtension(ProductSaveByExtentionEdit productSaveByExtentionEdit) {
        log.info("开始处理浏览器插件商品保存请求，商品名称: {}", productSaveByExtentionEdit.getProductName());
        long startTime = System.currentTimeMillis();

        // 转换为 Product 对象
        Product product = ProductSaveByExtentionEdit.editToObj(productSaveByExtentionEdit);

        // 参数校验
        this.checkProductParam(product, null);

        // 调用通用保存方法
        Long productId = this.saveProductCommon(product, null);

        long endTime = System.currentTimeMillis();
        log.info("浏览器插件商品保存完成，SPU: {}, 商品ID: {}, 总耗时: {}ms",
                product.getSpu(), productId, endTime - startTime);

        return productId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveProductsFromJsonFile(MultipartFile file) {
        log.info("开始处理JSON文件批量导入");

        if (file == null || file.isEmpty()) {
            throw new ServiceException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".json")) {
            throw new ServiceException("文件格式错误，仅支持JSON文件");
        }

        try {
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<ProductSaveByExtentionEdit> productList = JSON.parseArray(jsonContent, ProductSaveByExtentionEdit.class);

            if (CollectionUtils.isEmpty(productList)) {
                throw new ServiceException("JSON文件内容为空或格式错误");
            }

            int successCount = 0;
            int failCount = 0;
            List<String> errorMessages = new ArrayList<>();

            for (int i = 0; i < productList.size(); i++) {
                try {
                    ProductSaveByExtentionEdit productEdit = productList.get(i);
                    saveProductFromExtension(productEdit);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    String errorMsg = String.format("第%d个商品保存失败: %s", i + 1, e.getMessage());
                    errorMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }

            log.info("JSON文件批量导入完成，成功: {}个，失败: {}个", successCount, failCount);

            if (failCount > 0) {
                log.warn("部分商品导入失败，错误详情: {}", String.join("; ", errorMessages));
            }

            return successCount;

        } catch (IOException e) {
            log.error("读取JSON文件失败", e);
            throw new ServiceException("读取JSON文件失败: " + e.getMessage());
        }
    }

    /**
     * 根据标签ID列表查找叶子标签
     *
     * @param tagIds 标签ID列表
     * @return java.util.List<com.ruoyi.erp.domain.TagDict>
     * @author lwj
     **/
    private TagDict findMenuLeafTag(List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return null;
        }

        List<TagDict> tagDicts = tagDictService.listByIds(tagIds);
        if (CollectionUtils.isEmpty(tagDicts)) {
            return null;
        }

        List<TagDict> menuTags = tagDicts.stream()
                .filter(tag -> "MENU".equals(tag.getTagType()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(menuTags)) {
            return null;
        }

        List<Long> menuTagIds = menuTags.stream()
                .map(TagDict::getTagId)
                .collect(Collectors.toList());

        List<TagDict> childTags = tagDictService.lambdaQuery()
                .in(TagDict::getParentId, menuTagIds)
                .select(TagDict::getParentId)
                .list();

        Set<Long> parentIdsWithChildren = childTags.stream()
                .map(TagDict::getParentId)
                .collect(Collectors.toSet());

        return menuTags.stream()
                .filter(tag -> !parentIdsWithChildren.contains(tag.getTagId()))
                .findFirst()
                .orElse(null);
    }

    private String generateSpuByTag(TagDict tag) {
        if (tag == null || StringUtils.isEmpty(tag.getSpuPrefix())) {
            throw new ServiceException("标签或SPU前缀不能为空");
        }

        String spuPrefix = tag.getSpuPrefix();
        Integer currentMaxSeq = tag.getCurrentMaxSeq();
        if (currentMaxSeq == null) {
            currentMaxSeq = 0;
        }

        int nextSeq = currentMaxSeq + 1;
        String spu = String.format("%s%04d", spuPrefix, nextSeq);

        tagDictService.updateMaxSeqBySpuPrefix(spuPrefix, nextSeq);

        log.info("根据标签生成SPU: {}, 标签ID: {}, 前缀: {}, 流水号: {}",
                spu, tag.getTagId(), spuPrefix, nextSeq);

        return spu;
    }

    @Async
    protected void asyncDownloadMediaFiles(Product product, List<String> mediaUrlList, Map<String, ProductVariant> mediaUrlVarianMap) {
        String spu = product.getSpu();
        String imageSearchKeyword = product.getImageSearchKeyword();
        if (StringUtils.isBlank(imageSearchKeyword)) {
            imageSearchKeyword = StringUtils.isBlank(spu) ? product.getProductId().toString() : product.getSpu();
            product.setImageSearchKeyword(imageSearchKeyword);
        }
        log.info("开始异步下载媒体文件，spu: {}, 文件夹: {}, 文件数量: {}",
                spu, product.getImageSearchKeyword(), mediaUrlList.size());

        try {
            List<Media> downloadedFiles = mediaDownloadUtil.downloadMediaFiles(product, mediaUrlList, mediaUrlVarianMap);

            if (CollectionUtil.isNotEmpty(downloadedFiles)) {
                log.info("媒体文件下载完成，spu: {}, 文件夹: {}, 成功下载: {}个文件",
                        spu, imageSearchKeyword, downloadedFiles.size());
            }
        } catch (Exception e) {
            log.error("异步下载媒体文件失败，spu: {}, 文件夹: {}, 错误: {}",
                    spu, imageSearchKeyword, e.getMessage(), e);
        }
    }

    /**
     * 通用商品保存方法（不做参数校验）
     * <p>
     * 该方法封装了商品保存的核心逻辑，包括：
     * 1. SPU生成和管理
     * 2. 媒体文件处理（主图、规格图绑定、文件名统一）
     * 3. 规格数据处理和校验
     * 4. SKU生成
     * 5. 商品主表、变体、标签关联的持久化
     * </p>
     *
     * @param product 商品对象（包含完整的商品信息、变体列表、媒体列表、标签ID列表）
     * @return 商品ID
     * @author lwj
     * @date 2026-04-23
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveProductCommon(Product product, Integer wizardStep) {
        log.info("开始执行通用商品保存方法，商品名称: {}", product.getProductName());
        long startTime = System.currentTimeMillis();

        // ==================== 1. 确定是新增还是编辑 ====================
        Long productId = product.getProductId();
        boolean isInsert = StringUtils.isNull(productId);
        Product oldProduct = productService.getById(productId);

        if (isInsert) {
            // ==================== 1. 处理 SPU 编号 ====================
            this.handleSpu(product);
            if (StringUtils.isBlank(product.getPurchaseUrl())) {
                product.setPurchaseUrl(product.getSourceUrl());
            }
            // 新增逻辑
            product.setCreateTime(DateUtils.getNowDate());
            product.setImageSearchKeyword(product.getSpu());
            productService.save(product);
            productId = product.getProductId();
            log.info("商品主表新增成功，商品ID: {}", productId);
        } else {
            // 编辑逻辑
            product.setUpdateTime(DateUtils.getNowDate());
        }

        // ==================== 3. 处理商品标签关联 新增/向导第一步====================
        List<Long> tagIds = product.getTagIds();
        if (StringUtils.isNotEmpty(tagIds)) {
            if (isInsert) {
                // 新增时直接保存标签关联
                saveProductTags(productId, tagIds);
            } else if (WIZARD_STEP_FIRST.equals(wizardStep)) {
                // 编辑时先删除再新增
                deleteProductTagsByProductId(productId);
                saveProductTags(productId, tagIds);
            }
            log.info("商品标签关联保存成功，商品ID: {}, 标签数量: {}", productId, tagIds.size());
        }

        // ==================== 4. 处理商品变体（SKU生成） ====================
        List<ProductVariant> productVariants = handleProductVariants(product, productId, isInsert);

        // ==================== 5. 处理媒体文件（仅编辑时需要） ====================
        if (CollectionUtil.isNotEmpty(product.getMediaList())) {
            // 设置主图ID
            product.setMainMediaId(product.getMediaList().get(0).getMediaId());

            // 更新商品媒体（表和磁盘文件），根据主图和规格图重命名（表和磁盘文件），更新顺序
            mediaService.updateProductMedia(product);

            log.info("商品媒体更新成功，商品ID: {}, 媒体数量: {}", productId, product.getMediaList().size());

        } else if (CollectionUtil.isNotEmpty(product.getMediaUrlList())) {

            // ==================== 6. 处理媒体文件（外部导入） ====================
            Map<String, ProductVariant> mediaUrlVariantMap = productVariants.stream()
                    .collect(Collectors.toMap(ProductVariant::getMediaUrl, x -> x));
            // 异步下载媒体文件
            this.asyncDownloadMediaFiles(product, product.getMediaUrlList(), mediaUrlVariantMap);
        }

        // ==================== 7. 更新商品主表（编辑时） ====================
        if (!isInsert) {
            productService.updateById(product);
            log.info("商品主表更新成功，商品ID: {}", productId);
        }

        // ==================== 8. 更新表单提示词 ====================
        if(wizardStep != null){
            this.updateFormSuggestions(product, oldProduct, wizardStep);
        }

        long endTime = System.currentTimeMillis();
        log.info("通用商品保存方法执行完成，商品ID: {}, SPU: {}, 总耗时: {}ms",
                productId, product.getSpu(), endTime - startTime);

        return productId;
    }

    /**
     * 处理 SPU 编号
     * <p>
     * 如果 SPU 为空，根据标签自动生成；
     * 如果 SPU 不为空，更新标签字典的最大流水号。
     * </p>
     *
     * @param product 商品对象
     */
    private void handleSpu(Product product) {
        String spu = product.getSpu();

        if (StringUtils.isEmpty(spu)) {
            // SPU 为空，根据标签自动生成
            List<Long> tagIds = product.getTagIds();
            if (CollectionUtil.isNotEmpty(tagIds)) {
                TagDict leafTag = findMenuLeafTag(tagIds);
                if (leafTag != null) {
                    spu = generateSpuByTag(leafTag);
                    product.setSpu(spu);
                    log.info("根据标签自动生成 SPU: {}, 标签ID: {}", spu, leafTag.getTagId());
                }
            }
            // SPU 为空
            log.info("SPU 为空");
        } else {
            // SPU 不为空，更新标签字典的最大流水号
            updateTagDictPrefix(spu);
            log.info("使用已有 SPU: {}", spu);
        }
        Product one = productService.getOne(new LambdaQueryWrapper<Product>(Product.class)
                .select(Product::getProductId).eq(Product::getSpu, spu));
        if (one != null) {
            throw new RuntimeException("SPU 已存在");
        }
    }


    /**
     * 处理商品变体（包含 SKU 生成）
     * <p>
     * 1. 为每个变体生成 SKU（如果未提供）
     * 2. 序列化选项值
     * 3. 批量保存或更新变体数据
     * </p>
     *
     * @param product   商品对象
     * @param productId 商品ID
     * @param isInsert  是否为新增操作
     */
    private List<ProductVariant> handleProductVariants(Product product, Long productId, boolean isInsert) {
        List<ProductVariant> productVariantList = product.getProductVariantList();

        // 如果变体列表为空或 null，直接返回
        if (StringUtils.isEmpty(productVariantList)) {
            log.info("商品变体列表为空，跳过变体处理，商品ID: {}", productId);
            return Collections.emptyList();
        }

        Date now = DateUtils.getNowDate();
        String currentUsername = null;
        try {
            currentUsername = SecurityUtils.getUsername();
        } catch (Exception e) {
            log.warn("获取当前用户名失败，将不设置创建者/更新者字段");
        }

        List<ProductVariant> saveList = new ArrayList<>();
        List<Long> existList = new ArrayList<>();

        for (int i = 0; i < productVariantList.size(); i++) {
            ProductVariant variant = productVariantList.get(i);
            Long variantId = variant.getVariantId();

            // 生成 SKU（如果未提供）
            if (StringUtils.isEmpty(variant.getSku())) {
                String sku = this.generateSku(product, i);
                variant.setSku(sku);
                log.debug("为变体生成 SKU: {}, 变体索引: {}", sku, i);
            }

            if (variantId != null) {
                // 更新操作
                existList.add(variantId);
                variant.setUpdateBy(currentUsername);
                variant.setUpdateTime(now);
            } else {
                // 新增操作
                if (StringUtils.isBlank(variant.getPurchaseUrl())) {
                    variant.setPurchaseUrl(product.getPurchaseUrl());
                }
                variant.setProductId(productId);
                variant.setCreateBy(currentUsername);
                variant.setCreateTime(now);
            }
            if (variant.getIsActiveAvailable() == null) {
                variant.setIsActiveAvailable(ProductConstants.IS_ACTIVE_AVAILABLE_YES);
            }
            // 序列化选项值
            if (CollectionUtil.isNotEmpty(variant.getOptionValueList())) {
                variant.setOptionValues(JSON.toJSONString(variant.getOptionValueList()));
            }

            saveList.add(variant);
        }

        // 如果不是新增，删除不在当前列表中的变体
        if (!isInsert && !existList.isEmpty()) {
            productVariantService.remove(new LambdaQueryWrapper<ProductVariant>()
                    .eq(ProductVariant::getProductId, productId)
                    .notIn(ProductVariant::getVariantId, existList));
            log.info("删除其他变体成功，商品 ID: {}, 保留变体ID：{}", productId, existList);
        }

        // 批量插入或更新变体数据
        if (!saveList.isEmpty()) {
            productVariantService.saveOrUpdateBatch(saveList);
            log.info("批量保存商品变体成功，商品 ID: {}, 变体数量: {}", productId, saveList.size());
        }
        return saveList;
    }

    /**
     * 生成 SKU
     * <p>
     * SKU 格式：SPU + 变体索引（从1开始，补零到3位）
     * 例如：ABC0001-001, ABC0001-002
     * </p>
     *
     * @param product 商品
     * @param index   变体索引（从0开始）
     * @return 生成的 SKU
     */
    private String generateSku(Product product, int index) {
        String profix = product.getSpu();
        if (StringUtils.isEmpty(profix)) {
            // 如果 SPU 为空，使用时间戳作为前缀
            profix = String.valueOf(product.getProductId());
        }

        // SKU 格式：SPU + "-" + 三位序号
        return String.format("%s-%03d", profix, index + 1);
    }

    /**
     * 更新表单提示词
     * <p>
     * 在保存商品时，将 size、material、note、noteCn 字段的值以及 optionJson 中的
     * englishName 和 englishValue 更新到提示词表中。
     * 如果值未变化，则不更新使用时间。
     * </p>
     *
     * @param product  商品对象
     * @param isInsert 是否为新增操作
     */
    private void updateFormSuggestions(Product product, Product oldProduct, Integer wizardStep) {
        try {
            List<Map<String, String>> updates = new ArrayList<>();

            // 定义需要更新的字段列表
            String[] fields = {ProductConstants.SUGGESTIONS_KEY_SIZE, ProductConstants.SUGGESTIONS_KEY_MATERIAL,
                    ProductConstants.SUGGESTIONS_KEY_NOTE, ProductConstants.SUGGESTIONS_KEY_NOTECN};

            // 1. 处理普通字段
            if(wizardStep == 2){
                for (String fieldName : fields) {
                    String newValue = getFieldValue(product, fieldName);

                    // 如果新值为空，跳过
                    if (StringUtils.isEmpty(newValue)) {
                        continue;
                    }

                    String oldValue = null;
                    // 从缓存的旧商品信息中获取旧值
                    if (oldProduct != null) {
                        oldValue = getFieldValue(oldProduct, fieldName);
                    }

                    // 添加到更新列表
                    Map<String, String> update = new HashMap<>();
                    update.put("fieldName", fieldName);
                    update.put("oldValue", oldValue);
                    update.put("newValue", newValue);
                    updates.add(update);
                }
            }

            if(wizardStep == 1){
                // 2. 处理 optionJson 中的 englishName 和 englishValue
                extractOptionSuggestions(product, oldProduct, updates);
            }

            // 批量更新提示词
            if (!updates.isEmpty()) {
                formSuggestionService.batchUpdateUsage(updates);
                log.debug("表单提示词更新成功，商品ID: {}, 更新字段数: {}",
                        product.getProductId(), updates.size());
            }
        } catch (Exception e) {
            // 提示词更新失败不影响商品保存，只记录日志
            log.error("更新表单提示词失败，商品ID: {}, 错误: {}",
                    product.getProductId(), e.getMessage(), e);
        }
    }

    /**
     * 根据字段名获取商品对象的字段值
     *
     * @param product   商品对象
     * @param fieldName 字段名
     * @return 字段值
     */
    private String getFieldValue(Product product, String fieldName) {
        if (product == null || StringUtils.isEmpty(fieldName)) {
            return null;
        }

        switch (fieldName) {
            case ProductConstants.SUGGESTIONS_KEY_SIZE:
                return product.getSize();
            case ProductConstants.SUGGESTIONS_KEY_MATERIAL:
                return product.getMaterial();
            case ProductConstants.SUGGESTIONS_KEY_NOTE:
                return product.getNote();
            case ProductConstants.SUGGESTIONS_KEY_NOTECN:
                return product.getNoteCn();
            default:
                return null;
        }
    }

    /**
     * 从 optionJson 中提取 englishName 和 englishValue 作为提示词
     *
     * @param product    新商品对象
     * @param oldProduct 旧商品对象（编辑时）
     * @param updates    更新列表
     */
    private void extractOptionSuggestions(Product product, Product oldProduct, List<Map<String, String>> updates) {
        // 提取新商品的选项
        Set<String> newEnglishNames = new HashSet<>();
        Set<String> newEnglishValues = new HashSet<>();

        if (StringUtils.isNotEmpty(product.getOptionJson())) {
            try {
                List<ProductOption> options = JSON.parseArray(product.getOptionJson(), ProductOption.class);
                if (CollectionUtil.isNotEmpty(options)) {
                    for (ProductOption option : options) {
                        // 收集 englishName
                        if (StringUtils.isNotEmpty(option.getEnglishName())) {
                            newEnglishNames.add(option.getEnglishName());
                        }
                        // 收集 englishValue
                        if (CollectionUtil.isNotEmpty(option.getValues())) {
                            for (ProductOptionValue value : option.getValues()) {
                                if (StringUtils.isNotEmpty(value.getEnglishValue())) {
                                    newEnglishValues.add(value.getEnglishValue());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析新商品 optionJson 失败: {}", e.getMessage());
            }
        }

        // 提取旧商品的选项（用于对比）
        Set<String> oldEnglishNames = new HashSet<>();
        Set<String> oldEnglishValues = new HashSet<>();

        if (oldProduct != null && StringUtils.isNotEmpty(oldProduct.getOptionJson())) {
            try {
                List<ProductOption> oldOptions = JSON.parseArray(oldProduct.getOptionJson(), ProductOption.class);
                if (CollectionUtil.isNotEmpty(oldOptions)) {
                    for (ProductOption option : oldOptions) {
                        if (StringUtils.isNotEmpty(option.getEnglishName())) {
                            oldEnglishNames.add(option.getEnglishName());
                        }
                        if (CollectionUtil.isNotEmpty(option.getValues())) {
                            for (ProductOptionValue value : option.getValues()) {
                                if (StringUtils.isNotEmpty(value.getEnglishValue())) {
                                    oldEnglishValues.add(value.getEnglishValue());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析旧商品 optionJson 失败: {}", e.getMessage());
            }
        }

        // 处理 englishName：只添加新增的或变化的
        for (String englishName : newEnglishNames) {
            if (!oldEnglishNames.contains(englishName)) {
                Map<String, String> update = new HashMap<>();
                update.put("fieldName", ProductConstants.SUGGESTIONS_KEY_OPTION_ENGLISH_NAME);
                update.put("oldValue", null);
                update.put("newValue", englishName);
                updates.add(update);
            }
        }

        // 处理 englishValue：只添加新增的或变化的
        for (String englishValue : newEnglishValues) {
            if (!oldEnglishValues.contains(englishValue)) {
                Map<String, String> update = new HashMap<>();
                update.put("fieldName", ProductConstants.SUGGESTIONS_KEY_OPTION_ENGLISH_VALUE);
                update.put("oldValue", null);
                update.put("newValue", englishValue);
                updates.add(update);
            }
        }
    }

}

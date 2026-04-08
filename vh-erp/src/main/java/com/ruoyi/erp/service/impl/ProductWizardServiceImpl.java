package com.ruoyi.erp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.model.domain.*;
import com.ruoyi.erp.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveProductWithWizard(Product product) {
        Long productId = product.getProductId();
        List<Long> tagIds = product.getTagIds();
        if(CollectionUtils.isEmpty(tagIds)){
            throw new ServiceException("请选择标签");
        }

        if (StringUtils.isNull(productId)) {
            // ==================== 新增逻辑 ====================
            product.setCreateTime(DateUtils.getNowDate());

            // 1. 处理 SPU 编号：根据前缀找到 TagDict，更新当前最大流水号
            String spu = product.getSpu();
            this.updateTagDictPrefix(spu);

            // 2. 插入商品主表
            boolean save = productService.save(product);

            // 3. 获取生成的商品 ID
            Long generatedProductId = product.getProductId();

            // 4. 保存商品标签关联
            if (StringUtils.isNotEmpty(tagIds)) {
                this.saveProductTags(generatedProductId, tagIds);
            }

            // 5. 保存变体信息
            this.saveProductVariant(product);

            return product.getProductId();
        } else {
            // ==================== 编辑逻辑 ====================
            product.setUpdateTime(DateUtils.getNowDate());

            // 1. 删除所有商品标签关联
            this.deleteProductTagsByProductId(productId);

            // 2. 重新新增商品标签关联
            if (StringUtils.isNotEmpty(tagIds)) {
                this.saveProductTags(productId, tagIds);
            }

            // 5. 保存变体信息
            this.saveProductVariant(product);

            // 6. 更新商品主表
            productService.updateById(product);
            return product.getProductId();
        }
    }

    /**
     * 根据spu更新标签的当前最大流水号
     *
     * @param spu
     * @return void
     * @author lwj
     **/
    private void updateTagDictPrefix(String spu) {
        if (StringUtils.isEmpty(spu)) {
            throw new RuntimeException("SPU编号不能为空");
        }
        // 从 SPU 中提取前缀（假设 SPU 格式为：前缀 + 数字，如 "ABC0001"）
        String prefix = extractSpuPrefix(spu);
        if (StringUtils.isBlank(prefix)) {
            throw new RuntimeException("SPU前缀不合法");
        }
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
     * @param spu SPU 编号
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
     * 根据前缀查询标签字典
     *
     * @param prefix SPU 前缀
     * @return 标签字典对象
     */
    private TagDict lambdaQueryTagDictByPrefix(String prefix) {
        return tagDictService.lambdaQuery()
                .eq(TagDict::getSpuPrefix, prefix)
                .one();
    }

    /**
     * 保存商品标签关联
     *
     * @param productId 商品 ID
     * @param tagIds 标签 ID 列表
     */
    private void saveProductTags(Long productId, List<Long> tagIds) {
        List<TagDict> tagDicts = tagDictService.listByIds(tagIds);
        if (StringUtils.isEmpty(tagDicts)) {
            throw new ServiceException("标签不存在");
        }

        List<ProductTagRel> tagRelList = new ArrayList<>();
        Date now = DateUtils.getNowDate();
        String createBy = SecurityUtils.getUsername();

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
     * 保存商品变体信息
     *
     * @param product 商品对象（包含变体列表）
     * @author lwj
     **/
    private void saveProductVariant(Product product) {
        List<ProductVariant> productVariantList = product.getProductVariantList();

        // 如果变体列表为空或 null，直接返回
        if (StringUtils.isEmpty(productVariantList)) {
            return;
        }

        Long productId = product.getProductId();
        Date now = DateUtils.getNowDate();
        String currentUsername = SecurityUtils.getUsername();

        List<ProductVariant> saveList = new ArrayList<>();
        List<Long> existList = new ArrayList<>();
        for (ProductVariant variant : productVariantList) {
            // 如果变体 ID 不为空，则说明是更新
            Long variantId = variant.getVariantId();
            if(variantId != null){
                existList.add(variantId);
                variant.setUpdateBy(currentUsername);
                variant.setUpdateTime(now);
            }else{
                // 新增
                // 设置关联的商品 ID
                variant.setProductId(productId);

                // 设置创建者和创建时间
                variant.setCreateBy(currentUsername);
                variant.setCreateTime(now);
            }
            variant.setOptionValues(JSON.toJSONString(variant.getOptionValueList()));
            saveList.add(variant);
        }

        // 删除其他变体
        productVariantService.remove(new LambdaQueryWrapper<ProductVariant>()
                .eq(ProductVariant::getProductId, productId)
                // 如果existList不为空就删除其他变体
                .notIn(!existList.isEmpty(),ProductVariant::getVariantId, existList));
        log.info("删除其他变体成功，商品 ID: {}, 变体ID：{}", productId, existList);

        // 批量插入变体数据
        if (!saveList.isEmpty()) {
            productVariantService.saveOrUpdateBatch(saveList);
            log.info("批量保存商品变体成功，商品 ID: {}", productId);
        }
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
            Set<String> values = productOption.getValues().stream().map(ProductOptionValue::getValue).collect(Collectors.toSet());
            purchaseOptionMap.put(productOption.getName(), values);
        }
        return purchaseOptionMap;
    }

}

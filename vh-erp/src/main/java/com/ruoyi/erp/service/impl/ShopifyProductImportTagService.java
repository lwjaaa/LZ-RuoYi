package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.constant.TagConstants;
import com.ruoyi.erp.mapper.ProductTagRelMapper;
import com.ruoyi.erp.mapper.TagDictMapper;
import com.ruoyi.erp.model.domain.ProductTagRel;
import com.ruoyi.erp.model.domain.TagDict;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shopify 商品导入标签同步服务。
 */
@Slf4j
@Service
public class ShopifyProductImportTagService {

    @Resource
    private TagDictMapper tagDictMapper;
    @Resource
    private ProductTagRelMapper productTagRelMapper;

    /**
     * 同步 Shopify 标签到本地标签字典和商品标签关系，采用只增不删策略。
     *
     * @param remote Shopify 商品数据
     * @param productId 本地商品 ID
     * @param now 当前导入时间
     */
    public void syncProductTags(ShopifyImportedProduct remote, Long productId, Date now) {
        List<String> tagCodes = normalizeShopifyTags(remote.getTags());
        if (tagCodes.isEmpty()) {
            return;
        }
        int createdRelations = 0;
        for (String tagCode : tagCodes) {
            TagDict tag = resolveOrCreateTag(tagCode, now);
            if (tag == null || tag.getTagId() == null) {
                throw new ServiceException("Shopify 标签保存失败：" + tagCode);
            }
            if (insertProductTagRelIfAbsent(productId, tag.getTagId(), now)) {
                createdRelations++;
            }
        }
        log.debug("Shopify 商品标签导入完成，productId={}, tagCount={}, createdRelations={}",
                productId, tagCodes.size(), createdRelations);
    }

    /**
     * 按标签编码查找本地标签，不存在时创建 OTHER 类型顶级标签。
     *
     * @param tagCode Shopify 标签编码
     * @param now 当前导入时间
     * @return 本地标签
     */
    private TagDict resolveOrCreateTag(String tagCode, Date now) {
        TagDict existing = selectTagByCode(tagCode);
        if (existing != null) {
            return existing;
        }
        TagDict tag = new TagDict();
        tag.setTagName(tagCode);
        tag.setTagCode(tagCode);
        tag.setTagType(TagConstants.TYPE_OTHER);
        tag.setParentId(0L);
        tag.setAncestors("0");
        tag.setMenuLevel(1);
        tag.setSortOrder(selectNextOtherRootTagSortOrder());
        tag.setDelFlag("0");
        tag.setCreateTime(now);
        try {
            tagDictMapper.insert(tag);
        } catch (DataIntegrityViolationException e) {
            TagDict concurrentTag = selectTagByCode(tagCode);
            if (concurrentTag != null) {
                return concurrentTag;
            }
            throw e;
        }
        return tag;
    }

    /**
     * 清洗 Shopify 标签，去除空白并按原始顺序去重。
     *
     * @param tags Shopify 标签列表
     * @return 规范化后的标签编码列表
     */
    private List<String> normalizeShopifyTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Set<String> tagCodes = new LinkedHashSet<>();
        for (String tag : tags) {
            if (StringUtils.isEmpty(tag)) {
                continue;
            }
            String tagCode = tag.trim();
            if (StringUtils.isNotEmpty(tagCode)) {
                tagCodes.add(tagCode);
            }
        }
        return new ArrayList<>(tagCodes);
    }

    /**
     * 查询下一个顶级非 MENU 标签排序号。
     *
     * @return 新标签排序号
     */
    private Integer selectNextOtherRootTagSortOrder() {
        TagDict maxSortTag = tagDictMapper.selectOne(new LambdaQueryWrapper<TagDict>()
                .select(TagDict::getSortOrder)
                .eq(TagDict::getParentId, 0L)
                .ne(TagDict::getTagType, TagConstants.TYPE_MENU)
                .orderByDesc(TagDict::getSortOrder)
                .last("limit 1"));
        return (maxSortTag == null || maxSortTag.getSortOrder() == null ? 0 : maxSortTag.getSortOrder()) + 1;
    }

    /**
     * 按标签编码查询本地标签。
     *
     * @param tagCode 标签编码
     * @return 本地标签
     */
    private TagDict selectTagByCode(String tagCode) {
        return tagDictMapper.selectOne(new LambdaQueryWrapper<TagDict>()
                .eq(TagDict::getTagCode, tagCode)
                .last("limit 1"));
    }

    /**
     * 商品标签关系不存在时新增，保留本地已有标签关系。
     *
     * @param productId 商品 ID
     * @param tagId 标签 ID
     * @param now 当前导入时间
     * @return 是否新增关系
     */
    private boolean insertProductTagRelIfAbsent(Long productId, Long tagId, Date now) {
        ProductTagRel existing = productTagRelMapper.selectOne(new LambdaQueryWrapper<ProductTagRel>()
                .eq(ProductTagRel::getProductId, productId)
                .eq(ProductTagRel::getTagId, tagId)
                .last("limit 1"));
        if (existing != null) {
            return false;
        }
        ProductTagRel rel = new ProductTagRel();
        rel.setProductId(productId);
        rel.setTagId(tagId);
        rel.setCreateTime(now);
        try {
            productTagRelMapper.insertProductTagRel(rel);
            return true;
        } catch (DataIntegrityViolationException e) {
            ProductTagRel concurrentRel = productTagRelMapper.selectOne(new LambdaQueryWrapper<ProductTagRel>()
                    .eq(ProductTagRel::getProductId, productId)
                    .eq(ProductTagRel::getTagId, tagId)
                    .last("limit 1"));
            if (concurrentRel != null) {
                return false;
            }
            throw e;
        }
    }
}

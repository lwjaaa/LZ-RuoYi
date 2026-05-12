package com.ruoyi.erp.service;

import java.util.Collection;

/**
 * 商品资料质量服务。
 */
public interface IProductQualityService {

    /**
     * 刷新单个商品的资料缺失字段。
     *
     * @param productId 商品ID
     */
    void refreshProductMissingFields(Long productId);

    /**
     * 批量刷新商品资料缺失字段。
     *
     * @param productIds 商品ID集合
     */
    void refreshProductMissingFields(Collection<Long> productIds);
}

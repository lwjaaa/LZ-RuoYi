package com.ruoyi.erp.service;

import com.ruoyi.erp.model.domain.Product;

/**
 * erp商品Service接口
 *
 * @author lwj
 * @date 2026-03-26
 */
public interface IProductWizardService {

    /**
     * 保存商品及相关数据（选品向导）
     *
     * @param product 商品对象
     * @return 结果
     */
    boolean saveProductWithWizard(Product product);
}

package com.ruoyi.erp.service;

import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.dto.productVariant.ShippingFeeQurey;

import java.util.Map;

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
    Long saveProductWithWizard(Product product,int step);

    /**
     * 运费查询
     *
     * @param shippingFeeQurey
     * @return java.lang.Integer
     * @author lwj
     **/
    Integer calculateShipping(ShippingFeeQurey shippingFeeQurey);

    /**
     * 获取美元汇率
     *
     * @return java.lang.String
     * @author lwj
     **/
    Map<String,Object> getUsdRate();
}

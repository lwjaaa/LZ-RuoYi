package com.ruoyi.erp.model.dto.productVariant;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.erp.model.domain.ProductVariant;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
/**
 * erp商品变体Query对象 erp_product_variant
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class ProductVariantQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 关联商品主表ID */
    private Long productId;

    /** Shopify平台变体ID */
    private String shopifyVariantId;

    /** SKU */
    private String sku;

    /** 销售价格(美元*100) */
    private Integer price;

    /** 采购价（分） */
    private Integer purchasePrice;

    /** 运费是否来自实际发货数据(0:否, 1:是) */
    private String isActualShipment;

    /** 创建时间 */
    private Date createTime;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    /**
     * 对象转封装类
     *
     * @param productVariantQuery 查询对象
     * @return ProductVariant
     */
    public static ProductVariant queryToObj(ProductVariantQuery productVariantQuery) {
        if (productVariantQuery == null) {
            return null;
        }
        ProductVariant productVariant = new ProductVariant();
        BeanUtils.copyProperties(productVariantQuery, productVariant);
        return productVariant;
    }
}

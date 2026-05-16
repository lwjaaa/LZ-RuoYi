package com.ruoyi.erp.shopify.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 订单同步行项目
 */
@Data
public class ShopifyImportedOrderLineItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Shopify 订单行ID */
    private String id;

    /** Shopify 商品ID */
    private String productId;

    /** Shopify 变体ID */
    private String variantId;

    /** 商品标题 */
    private String title;

    /** 变体标题 */
    private String variantTitle;

    /** SKU */
    private String sku;

    /** 数量 */
    private Integer quantity;

    /** 单价，单位为分 */
    private Integer unitPrice;

    /** 行总额，单位为分 */
    private Integer totalPrice;
}

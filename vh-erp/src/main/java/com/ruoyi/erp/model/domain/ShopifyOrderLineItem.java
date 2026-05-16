package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 订单行对象 erp_shopify_order_line_item
 */
@TableName("erp_shopify_order_line_item")
@Data
public class ShopifyOrderLineItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本地订单行主键 */
    @TableId(value = "line_item_id", type = IdType.ASSIGN_ID)
    private Long lineItemId;

    /** 本地订单ID */
    private Long orderId;

    /** 店铺ID */
    private Long storeId;

    /** Shopify 订单行ID */
    private String shopifyLineItemId;

    /** Shopify 商品ID */
    private String shopifyProductId;

    /** Shopify 变体ID */
    private String shopifyVariantId;

    /** 本地商品ID */
    private Long productId;

    /** 本地变体ID */
    private Long variantId;

    /** SKU */
    @Excel(name = "SKU")
    private String sku;

    /** 商品标题 */
    @Excel(name = "商品标题")
    private String title;

    /** 变体标题 */
    private String variantTitle;

    /** 购买数量 */
    private Integer quantity;

    /** 单价，单位为分 */
    private Integer unitPrice;

    /** 行总额，单位为分 */
    private Integer totalPrice;

    /** 商品来源链接 */
    private String sourceUrl;

    /** 采购链接 */
    private String purchaseUrl;

    /** 采购单价，单位为分 */
    private Integer purchasePrice;

    /** 采购总额，单位为分 */
    private Integer purchaseAmount;

    /** 采购状态 */
    private String purchaseStatus;

    /** 采购备注 */
    private String purchaseRemark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}

package com.ruoyi.erp.shopify.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Shopify 订单同步聚合对象
 */
@Data
public class ShopifyImportedOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Shopify 订单ID */
    private String id;

    /** 订单名称 */
    private String name;

    /** 数字订单号 */
    private Long orderNumber;

    /** 邮箱 */
    private String email;

    /** 电话 */
    private String phone;

    /** 付款状态 */
    private String financialStatus;

    /** 履约状态 */
    private String fulfillmentStatus;

    /** 币种 */
    private String currencyCode;

    /** 订单总额，单位为分 */
    private Integer totalPrice;

    /** 商品小计，单位为分 */
    private Integer subtotalPrice;

    /** 税费，单位为分 */
    private Integer totalTax;

    /** Shopify 运费，单位为分 */
    private Integer totalShippingPrice;

    /** 退款总额，单位为分 */
    private Integer totalRefund;

    /** Shopify 下单时间 */
    private Date createdAt;

    /** Shopify 更新时间 */
    private Date updatedAt;

    /** Shopify 取消时间 */
    private Date cancelledAt;

    /** Shopify 关闭时间 */
    private Date closedAt;

    /** 收货人 */
    private String shippingName;

    /** 收货电话 */
    private String shippingPhone;

    /** 收货国家 */
    private String shippingCountry;

    /** 收货省份 */
    private String shippingProvince;

    /** 收货城市 */
    private String shippingCity;

    /** 收货邮编 */
    private String shippingZip;

    /** 收货地址1 */
    private String shippingAddress1;

    /** 收货地址2 */
    private String shippingAddress2;

    /** 客户快照 */
    private ShopifyImportedCustomer customer;

    /** 订单行 */
    private List<ShopifyImportedOrderLineItem> lineItems;

    /** 退款记录 */
    private List<ShopifyImportedRefund> refunds;
}

package com.ruoyi.erp.model.dto.shopifyOrder;

import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 履约回传请求对象
 */
@Data
public class FulfillmentSubmitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本地订单ID */
    private Long orderId;

    /** 物流公司 */
    private String trackingCompany;

    /** 运单号 */
    private String trackingNumber;

    /** 物流查询链接 */
    private String trackingUrl;

    /** 发货成本，单位为分 */
    private Integer shippingFee;
}

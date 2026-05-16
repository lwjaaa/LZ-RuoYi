package com.ruoyi.erp.shopify.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 履约创建结果
 */
@Data
@Builder
public class FulfillmentCreateResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Shopify Fulfillment ID */
    private String fulfillmentId;

    /** Shopify Fulfillment 状态 */
    private String status;
}

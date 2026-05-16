package com.ruoyi.erp.shopify.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 订单同步退款记录
 */
@Data
public class ShopifyImportedRefund implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Shopify 退款ID */
    private String id;

    /** 退款金额，单位为分 */
    private Integer amount;

    /** 币种 */
    private String currencyCode;

    /** 退款备注 */
    private String note;

    /** 退款时间 */
    private Date createdAt;
}

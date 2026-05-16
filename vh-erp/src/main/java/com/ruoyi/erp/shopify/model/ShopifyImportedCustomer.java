package com.ruoyi.erp.shopify.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 订单同步客户快照
 */
@Data
public class ShopifyImportedCustomer implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Shopify 客户ID */
    private String id;

    /** 邮箱 */
    private String email;

    /** 电话 */
    private String phone;

    /** 名 */
    private String firstName;

    /** 姓 */
    private String lastName;

    /** 展示名 */
    private String displayName;
}

package com.ruoyi.erp.model.dto.shopifyStore;

import lombok.Data;

import java.io.Serializable;

/**
 * Shopify Admin API 调用请求
 */
@Data
public class ShopifyApiCallRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String method;
    private String url;
    private String mode;
    private String query;
    private String variables;
    private String body;
}

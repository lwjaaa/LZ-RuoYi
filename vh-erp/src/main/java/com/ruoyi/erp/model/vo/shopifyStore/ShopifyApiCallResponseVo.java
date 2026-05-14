package com.ruoyi.erp.model.vo.shopifyStore;

import lombok.Data;

import java.io.Serializable;

/**
 * Shopify Admin API 调用响应
 */
@Data
public class ShopifyApiCallResponseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer statusCode;
    private String contentType;
    private Object body;
    private String rawBody;
    private Long durationMs;
}

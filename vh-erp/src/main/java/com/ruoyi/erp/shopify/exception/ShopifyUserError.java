package com.ruoyi.erp.shopify.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shopify GraphQL userErrors 的结构化信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopifyUserError {

    private String field;

    private String message;

    private String code;

    private Integer inputIndex;
}

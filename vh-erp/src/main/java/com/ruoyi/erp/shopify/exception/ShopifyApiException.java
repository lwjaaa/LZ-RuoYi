package com.ruoyi.erp.shopify.exception;

import java.util.Collections;
import java.util.List;

/**
 * Shopify API 异常。
 */
public class ShopifyApiException extends RuntimeException {

    private final List<ShopifyUserError> userErrors;

    public ShopifyApiException(String message) {
        super(message);
        this.userErrors = Collections.emptyList();
    }

    public ShopifyApiException(String message, Throwable cause) {
        super(message, cause);
        this.userErrors = Collections.emptyList();
    }

    public ShopifyApiException(String message, List<ShopifyUserError> userErrors) {
        super(message);
        this.userErrors = userErrors == null ? Collections.emptyList() : List.copyOf(userErrors);
    }

    public List<ShopifyUserError> getUserErrors() {
        return userErrors;
    }
}

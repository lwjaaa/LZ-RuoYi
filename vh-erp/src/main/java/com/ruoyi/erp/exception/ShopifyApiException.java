package com.ruoyi.erp.exception;

/**
 * Shopify API 异常
 */
public class ShopifyApiException extends RuntimeException {

    public ShopifyApiException(String message) {
        super(message);
    }

    public ShopifyApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

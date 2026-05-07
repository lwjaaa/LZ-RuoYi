package com.ruoyi.erp.exception;

/**
 * Shopify Token 过期异常
 * 用于触发 token 刷新机制
 */
public class ShopifyTokenExpiredException extends RuntimeException {
    
    public ShopifyTokenExpiredException(String message) {
        super(message);
    }
    
    public ShopifyTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.ruoyi.erp.shopify.constant;

/**
 * Shopify 同步配置常量，集中维护跨类共享的同步参数。
 */
public final class ShopifySyncConstants {

    private ShopifySyncConstants() {
    }

    public static final int PRODUCT_IMPORT_PAGE_SIZE = 100;
    public static final long PRODUCT_IMPORT_SAFETY_WINDOW_MINUTES = 5L;
    public static final String TOKEN_REFRESH_LOCK_KEY_PREFIX = "shopify:token_refresh_lock:";
    public static final String RETRY_COUNT_KEY_PREFIX = "shopify:retry_count:";
}

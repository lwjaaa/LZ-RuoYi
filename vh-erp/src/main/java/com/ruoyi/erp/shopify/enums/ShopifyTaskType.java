package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * Shopify 任务类型。code 必须保持数据库和前端现有字符串值不变。
 */
public enum ShopifyTaskType {
    PRODUCT_SYNC("PRODUCT_SYNC"),
    PRODUCT_SYNC_BATCH("PRODUCT_SYNC_BATCH"),
    MEDIA_SYNC_BATCH("MEDIA_SYNC_BATCH"),
    MEDIA_SYNC("MEDIA_SYNC"),
    PRODUCT_IMPORT_FULL("SHOPIFY_PRODUCT_IMPORT_FULL"),
    PRODUCT_IMPORT_INCREMENTAL("SHOPIFY_PRODUCT_IMPORT_INCREMENTAL"),
    UNKNOWN("UNKNOWN");

    private final String code;

    ShopifyTaskType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifyTaskType fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}

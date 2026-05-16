package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * Shopify 任务步骤，用于任务明细诊断，不改变已有 step 字符串。
 */
public enum ShopifyTaskStep {
    PRODUCT_SYNC("PRODUCT_SYNC"),
    PRODUCT_UPSERT("PRODUCT_UPSERT"),
    MEDIA_UPLOAD("MEDIA_UPLOAD"),
    VARIANT_CREATE("VARIANT_CREATE"),
    VARIANT_SAVE_ID("VARIANT_SAVE_ID"),
    MEDIA_REGISTER("MEDIA_REGISTER"),
    PRODUCT_IMPORT("PRODUCT_IMPORT"),
    PRODUCT_IMPORT_CREATE("PRODUCT_IMPORT_CREATE"),
    PRODUCT_IMPORT_UPDATE("PRODUCT_IMPORT_UPDATE"),
    PRODUCT_IMPORT_CONFLICT("PRODUCT_IMPORT_CONFLICT"),
    ORDER_SYNC("ORDER_SYNC"),
    UNKNOWN("UNKNOWN");

    private final String code;

    ShopifyTaskStep(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifyTaskStep fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}

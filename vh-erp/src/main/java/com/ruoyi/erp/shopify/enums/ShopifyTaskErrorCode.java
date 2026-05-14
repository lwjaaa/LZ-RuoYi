package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * 本地生成的 Shopify 任务错误码。Shopify userErrors.code 仍按远端原值保存。
 */
public enum ShopifyTaskErrorCode {
    PRODUCT_SYNC_FAILED("PRODUCT_SYNC_FAILED"),
    PRODUCT_UPSERT_FAILED("PRODUCT_UPSERT_FAILED"),
    MEDIA_PATH_EMPTY("MEDIA_PATH_EMPTY"),
    MEDIA_FILE_MISSING("MEDIA_FILE_MISSING"),
    MEDIA_URL_SAVE_FAILED("MEDIA_URL_SAVE_FAILED"),
    MEDIA_UPLOAD_FAILED("MEDIA_UPLOAD_FAILED"),
    VARIANT_CREATE_FAILED("VARIANT_CREATE_FAILED"),
    VARIANT_ID_COUNT_MISMATCH("VARIANT_ID_COUNT_MISMATCH"),
    VARIANT_ID_EMPTY("VARIANT_ID_EMPTY"),
    VARIANT_ID_SAVE_FAILED("VARIANT_ID_SAVE_FAILED"),
    MEDIA_ID_MISSING("MEDIA_ID_MISSING"),
    MEDIA_ID_SAVE_FAILED("MEDIA_ID_SAVE_FAILED"),
    ERP_PRIORITY_CONFLICT("ERP_PRIORITY_CONFLICT"),
    PRODUCT_IMPORT_FAILED("PRODUCT_IMPORT_FAILED"),
    UNKNOWN("UNKNOWN");

    private final String code;

    ShopifyTaskErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifyTaskErrorCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}

package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * Shopify 同步游标模式，区分不同反向同步链路。
 */
public enum ShopifySyncCursorMode {
    PRODUCT_IMPORT("PRODUCT_IMPORT"),
    UNKNOWN("UNKNOWN");

    private final String code;

    ShopifySyncCursorMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifySyncCursorMode fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}

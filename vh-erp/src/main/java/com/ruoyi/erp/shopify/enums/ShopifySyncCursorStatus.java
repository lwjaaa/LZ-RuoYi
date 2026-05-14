package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * Shopify 同步游标状态，和任务状态分离，避免游标推进逻辑误用任务状态。
 */
public enum ShopifySyncCursorStatus {
    RUNNING("RUNNING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    PART_SUCCESS("PART_SUCCESS"),
    UNKNOWN("UNKNOWN");

    private final String code;

    ShopifySyncCursorStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifySyncCursorStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}

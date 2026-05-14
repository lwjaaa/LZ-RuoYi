package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * Shopify 任务明细状态。明细状态与任务主状态分开，便于诊断单个商品或子项。
 */
public enum ShopifyTaskDetailStatus {
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    PART_SUCCESS("PART_SUCCESS"),
    SKIPPED("SKIPPED"),
    UNKNOWN("UNKNOWN");

    private final String code;

    ShopifyTaskDetailStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifyTaskDetailStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}

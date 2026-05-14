package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * Shopify 任务主状态。状态 code 与 erp_shopify_task.task_status 保存值一致。
 */
public enum ShopifyTaskStatus {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    PART_SUCCESS("PART_SUCCESS"),
    CANCELLED("CANCELLED"),
    UNKNOWN("UNKNOWN");

    private final String code;

    ShopifyTaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifyTaskStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public static ShopifyTaskStatus defaultValue() {
        return PENDING;
    }
}

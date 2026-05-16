package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * Shopify 任务明细对象类型，对应商品、变体、媒体等本地对象。
 */
public enum ShopifyTaskDetailItemType {
    PRODUCT("PRODUCT"),
    VARIANT("VARIANT"),
    MEDIA("MEDIA"),
    ORDER("ORDER"),
    ORDER_LINE("ORDER_LINE"),
    PURCHASE("PURCHASE"),
    FULFILLMENT("FULFILLMENT"),
    REFUND("REFUND"),
    UNKNOWN("UNKNOWN");

    private final String code;

    ShopifyTaskDetailItemType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifyTaskDetailItemType fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}

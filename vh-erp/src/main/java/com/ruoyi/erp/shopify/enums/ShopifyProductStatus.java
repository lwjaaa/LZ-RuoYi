package com.ruoyi.erp.shopify.enums;

import java.util.Arrays;

/**
 * ERP 内部保存的 Shopify 商品状态 code，远端状态名通过 fromName 转换。
 */
public enum ShopifyProductStatus {
    DRAFT("0"),
    ACTIVE("1"),
    ARCHIVED("2"),
    UNLISTED("3");

    private final String code;

    ShopifyProductStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ShopifyProductStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(DRAFT);
    }

    public static ShopifyProductStatus fromName(String name) {
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(DRAFT);
    }
}

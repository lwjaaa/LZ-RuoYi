package com.ruoyi.common.enums;

import lombok.Getter;

/**
 * @author lwj
 * @version 1.0.0
 * @description Shopify产品状态
 * @date 2026/5/6 11:56
 **/
@Getter
public enum ShopifyProductStatusEnum {
    /**
     * 草稿:该产品尚未准备好销售，客户无法通过销售渠道和应用程序访问该产品。默认情况下，重复和未归档的产品状态为草稿。
     */
    DRAFT("0"),
    /**
     * 产品已准备就绪，可以发布到销售渠道和应用程序。状态为“在售”的产品不会自动发布到销售渠道，例如在线商店或应用程序。默认情况下，现有产品设置为“在售”。
     */
    ACTIVE("1"),
    /**
     * 已存档:该产品已停止销售，顾客无法通过销售渠道和应用程序购买。
     */
    ARCHIVED("2"),
    /**
     * 未列出:该产品已上线，但您需要直接链接才能查看。该产品不会出现在搜索结果、产品系列或产品推荐中。只有通过句柄、ID 或元字段引用单独查询时，才会从 Storefront API 和 Liquid 中返回该产品。此状态仅在 2025-10 及更高版本中可见，在旧版本中会显示为“已上线”，且无法从旧版本中的“未上线”状态更改。
     */
    UNLISTED("3");

    private final String code;

    ShopifyProductStatusEnum(String code) {
        this.code = code;
    }

}

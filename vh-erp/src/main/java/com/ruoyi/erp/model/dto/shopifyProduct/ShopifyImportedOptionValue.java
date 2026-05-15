package com.ruoyi.erp.model.dto.shopifyProduct;

import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 商品选项值导入 DTO。
 */
@Data
public class ShopifyImportedOptionValue implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
}

package com.ruoyi.erp.model.dto.shopifyProduct;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Shopify 商品选项导入 DTO。
 */
@Data
public class ShopifyImportedOption implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private Integer position;
    private List<ShopifyImportedOptionValue> values = new ArrayList<>();
}

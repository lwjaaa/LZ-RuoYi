package com.ruoyi.erp.model.dto.shopifyProduct;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ShopifyImportedVariant implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String sku;
    private String price;
    private String compareAtPrice;
    private Integer position;
    private String inventoryItemId;
    private Map<String, String> selectedOptions;
    private List<String> mediaIds = new ArrayList<>();
}

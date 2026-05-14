package com.ruoyi.erp.shopify.model;

import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPage {
    private List<ShopifyImportedProduct> products;
    private boolean hasNextPage;
    private String endCursor;
}

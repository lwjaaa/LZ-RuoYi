package com.ruoyi.erp.shopify.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品创建结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateResult {
    private String productId;
    private List<String> mediaIds;
}

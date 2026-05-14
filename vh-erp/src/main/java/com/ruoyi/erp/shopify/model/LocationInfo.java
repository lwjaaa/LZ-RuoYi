package com.ruoyi.erp.shopify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存仓库信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationInfo {
    private String id;
    private String name;
}

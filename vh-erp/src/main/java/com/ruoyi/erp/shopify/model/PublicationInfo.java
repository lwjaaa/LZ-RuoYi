package com.ruoyi.erp.shopify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布渠道信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicationInfo {
    private String id;
    private String name;
}

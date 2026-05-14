package com.ruoyi.erp.shopify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 销售渠道信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelInfo {
    private String id;
    private String name;
    private Boolean isPublished;
}

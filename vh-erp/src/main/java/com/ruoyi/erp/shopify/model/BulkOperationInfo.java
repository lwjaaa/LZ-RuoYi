package com.ruoyi.erp.shopify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationInfo {
    private String id;
    private String status;
    private String errorCode;
    private Long objectCount;
    private Long rootObjectCount;
    private String url;
    private String partialDataUrl;
}

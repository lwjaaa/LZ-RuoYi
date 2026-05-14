package com.ruoyi.erp.model.vo.shopifyTask;

import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 同步任务诊断统计项。
 */
@Data
public class ShopifyTaskDiagnosticStatVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    private Long total;
}

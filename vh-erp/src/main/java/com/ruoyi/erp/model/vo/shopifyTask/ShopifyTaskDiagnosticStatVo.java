package com.ruoyi.erp.model.vo.shopifyTask;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 同步任务诊断统计项。
 */
@Data
@Schema(description = "Shopify 同步任务诊断统计项")
public class ShopifyTaskDiagnosticStatVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "统计名称")
    private String name;

    @Schema(description = "统计数量")
    private Long total;
}

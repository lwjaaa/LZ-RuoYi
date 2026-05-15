package com.ruoyi.erp.model.vo.shopifyTask;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Shopify 同步任务诊断响应对象。
 */
@Data
@Schema(description = "Shopify 同步任务诊断响应对象")
public class ShopifyTaskDiagnosticsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "明细类型统计")
    private List<ShopifyTaskDiagnosticStatVo> itemTypeStats = new ArrayList<>();

    @Schema(description = "明细状态统计")
    private List<ShopifyTaskDiagnosticStatVo> statusStats = new ArrayList<>();

    @Schema(description = "失败步骤统计")
    private List<ShopifyTaskDiagnosticStatVo> failedStepStats = new ArrayList<>();

    @Schema(description = "高频错误统计")
    private List<ShopifyTaskErrorStatVo> topErrors = new ArrayList<>();

    @Schema(description = "近期失败明细")
    private List<ShopifyTaskDetailVo> recentFailures = new ArrayList<>();
}

package com.ruoyi.erp.model.vo.shopifyTask;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Shopify 同步任务诊断响应对象。
 */
@Data
public class ShopifyTaskDiagnosticsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;

    private List<ShopifyTaskDiagnosticStatVo> itemTypeStats = new ArrayList<>();

    private List<ShopifyTaskDiagnosticStatVo> statusStats = new ArrayList<>();

    private List<ShopifyTaskDiagnosticStatVo> failedStepStats = new ArrayList<>();

    private List<ShopifyTaskErrorStatVo> topErrors = new ArrayList<>();

    private List<ShopifyTaskDetailVo> recentFailures = new ArrayList<>();
}

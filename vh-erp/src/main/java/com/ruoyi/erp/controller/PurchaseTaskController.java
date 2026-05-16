package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.model.domain.PurchaseTask;
import com.ruoyi.erp.model.dto.shopifyOrder.PurchaseTaskEdit;
import com.ruoyi.erp.model.dto.shopifyOrder.PurchaseTaskQuery;
import com.ruoyi.erp.service.IShopifyOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 人工采购任务 Controller。
 */
@RestController
@RequestMapping("/erp/purchase-task")
@Tag(name = "人工采购任务", description = "订单行采购辅助、采购金额和异常备注接口")
public class PurchaseTaskController extends BaseController {

    @Resource
    private IShopifyOrderService shopifyOrderService;

    /**
     * 分页查询人工采购任务。
     */
    @PreAuthorize("@ss.hasPermi('erp:purchase-task:list')")
    @GetMapping("/list")
    @Operation(summary = "查询采购任务列表", description = "按店铺、采购状态和关键词分页查询采购任务")
    @ApiResponse(responseCode = "200", description = "返回分页采购任务列表")
    public TableDataInfo list(@ParameterObject PurchaseTaskQuery query) {
        startPage();
        List<PurchaseTask> list = shopifyOrderService.selectPurchaseTaskList(query);
        return getDataTable(list);
    }

    /**
     * 更新采购任务状态、金额和备注。
     */
    @PreAuthorize("@ss.hasPermi('erp:purchase-task:edit')")
    @Log(title = "人工采购任务", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "更新采购任务", description = "更新采购状态、实际采购金额、异常原因和备注")
    @ApiResponse(responseCode = "200", description = "返回更新结果")
    public AjaxResult edit(@RequestBody PurchaseTaskEdit edit) {
        return toAjax(shopifyOrderService.updatePurchaseTask(edit));
    }
}

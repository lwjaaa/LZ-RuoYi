package com.ruoyi.erp.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.model.dto.shopifyOrder.ProfitReportQuery;
import com.ruoyi.erp.service.IShopifyOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shopify 订单利润报表 Controller。
 */
@RestController
@RequestMapping("/erp/profit")
@Tag(name = "Shopify订单利润报表", description = "订单销售额、采购成本、物流费、退款额和毛利统计接口")
public class OrderProfitReportController extends BaseController {

    @Resource
    private IShopifyOrderService shopifyOrderService;

    /**
     * 查询订单利润汇总。
     */
    @PreAuthorize("@ss.hasPermi('erp:profit:list')")
    @GetMapping("/summary")
    @Operation(summary = "查询订单利润汇总", description = "按店铺和下单时间范围统计订单利润")
    @ApiResponse(responseCode = "200", description = "返回利润汇总")
    public AjaxResult summary(@ParameterObject ProfitReportQuery query) {
        return success(shopifyOrderService.selectProfitSummary(query));
    }
}

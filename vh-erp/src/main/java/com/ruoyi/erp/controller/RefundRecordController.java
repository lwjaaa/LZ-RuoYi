package com.ruoyi.erp.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.erp.model.domain.RefundRecord;
import com.ruoyi.erp.model.dto.shopifyOrder.RefundRecordQuery;
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

import java.util.List;

/**
 * Shopify 售后退款记录 Controller。
 */
@RestController
@RequestMapping("/erp/refund")
@Tag(name = "Shopify售后记录", description = "取消、退款和售后责任记录接口")
public class RefundRecordController extends BaseController {

    @Resource
    private IShopifyOrderService shopifyOrderService;

    /**
     * 分页查询售后退款记录。
     */
    @PreAuthorize("@ss.hasPermi('erp:refund:list')")
    @GetMapping("/list")
    @Operation(summary = "查询售后记录列表", description = "按店铺、订单和关键词分页查询退款售后记录")
    @ApiResponse(responseCode = "200", description = "返回分页售后记录列表")
    public TableDataInfo list(@ParameterObject RefundRecordQuery query) {
        startPage();
        List<RefundRecord> list = shopifyOrderService.selectRefundRecordList(query);
        return getDataTable(list);
    }
}

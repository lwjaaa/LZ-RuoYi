package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.model.domain.FulfillmentRecord;
import com.ruoyi.erp.model.dto.shopifyOrder.FulfillmentRecordQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.FulfillmentSubmitRequest;
import com.ruoyi.erp.service.IShopifyOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Shopify 发货与履约回传 Controller。
 */
@RestController
@RequestMapping("/erp/fulfillment")
@Tag(name = "Shopify发货回传", description = "发货记录、运单号和 Shopify 履约回传接口")
public class FulfillmentRecordController extends BaseController {

    @Resource
    private IShopifyOrderService shopifyOrderService;

    /**
     * 分页查询发货记录。
     */
    @PreAuthorize("@ss.hasPermi('erp:fulfillment:list')")
    @GetMapping("/list")
    @Operation(summary = "查询发货记录列表", description = "按店铺、订单、同步状态和关键词分页查询发货记录")
    @ApiResponse(responseCode = "200", description = "返回分页发货记录列表")
    public TableDataInfo list(@ParameterObject FulfillmentRecordQuery query) {
        startPage();
        List<FulfillmentRecord> list = shopifyOrderService.selectFulfillmentRecordList(query);
        return getDataTable(list);
    }

    /**
     * 提交发货并回传 Shopify 履约信息。
     */
    @PreAuthorize("@ss.hasPermi('erp:fulfillment:add')")
    @Log(title = "Shopify发货回传", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    @Operation(summary = "提交发货回传", description = "录入运单和物流费用，并通过 Shopify fulfillmentCreate 回传运单号")
    @ApiResponse(responseCode = "200", description = "返回发货记录")
    public AjaxResult submit(@RequestBody FulfillmentSubmitRequest request) {
        return success(shopifyOrderService.submitFulfillment(request));
    }
}

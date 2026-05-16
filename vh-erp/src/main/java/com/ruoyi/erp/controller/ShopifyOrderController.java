package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.model.domain.ShopifyOrder;
import com.ruoyi.erp.model.dto.shopifyOrder.ShopifyOrderQuery;
import com.ruoyi.erp.service.IShopifyOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Shopify 订单中心 Controller。
 */
@RestController
@RequestMapping("/erp/order")
@Tag(name = "Shopify订单中心", description = "订单轮询、订单详情和订单同步游标接口")
public class ShopifyOrderController extends BaseController {

    @Resource
    private IShopifyOrderService shopifyOrderService;

    /**
     * 分页查询 Shopify 订单列表。
     */
    @PreAuthorize("@ss.hasPermi('erp:order:list')")
    @GetMapping("/list")
    @Operation(summary = "查询 Shopify 订单列表", description = "按店铺、订单号、付款状态、履约状态和采购状态分页查询订单")
    @ApiResponse(responseCode = "200", description = "返回分页订单列表")
    public TableDataInfo list(@ParameterObject ShopifyOrderQuery query) {
        startPage();
        List<ShopifyOrder> list = shopifyOrderService.selectOrderList(query);
        return getDataTable(list);
    }

    /**
     * 查询 Shopify 订单详情。
     */
    @PreAuthorize("@ss.hasPermi('erp:order:query')")
    @GetMapping("/{orderId}")
    @Operation(summary = "查询 Shopify 订单详情", description = "按本地订单ID查询订单和订单行")
    @ApiResponse(responseCode = "200", description = "返回订单详情")
    public AjaxResult getInfo(@Parameter(description = "本地订单ID", required = true) @PathVariable("orderId") Long orderId) {
        return success(shopifyOrderService.selectOrderById(orderId));
    }

    /**
     * 手动触发店铺订单增量轮询。
     */
    @PreAuthorize("@ss.hasPermi('erp:order:sync')")
    @Log(title = "Shopify订单轮询", businessType = BusinessType.OTHER)
    @PostMapping("/sync/{storeId}")
    @Operation(summary = "手动触发订单增量轮询", description = "为指定店铺创建订单增量同步任务")
    @ApiResponse(responseCode = "200", description = "返回任务ID")
    public AjaxResult sync(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(shopifyOrderService.startIncrementalSync(storeId));
    }

    /**
     * 手动触发店铺近 30 天订单补偿轮询。
     */
    @PreAuthorize("@ss.hasPermi('erp:order:sync')")
    @Log(title = "Shopify订单补偿轮询", businessType = BusinessType.OTHER)
    @PostMapping("/backfill/{storeId}")
    @Operation(summary = "手动触发订单补偿轮询", description = "为指定店铺创建近 30 天订单补偿同步任务")
    @ApiResponse(responseCode = "200", description = "返回任务ID")
    public AjaxResult backfill(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(shopifyOrderService.startBackfillSync(storeId));
    }

    /**
     * 查询店铺订单同步游标。
     */
    @PreAuthorize("@ss.hasPermi('erp:order:query')")
    @GetMapping("/cursor/{storeId}")
    @Operation(summary = "查询订单同步游标", description = "查询指定店铺的订单同步游标和最近错误")
    @ApiResponse(responseCode = "200", description = "返回订单同步游标")
    public AjaxResult cursor(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(shopifyOrderService.getCursor(storeId));
    }
}

package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyApiCallRequest;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyStoreEdit;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyStoreInsert;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyStoreQuery;
import com.ruoyi.erp.model.vo.shopifyStore.ShopifyStoreVo;
import com.ruoyi.erp.service.IShopifyProductImportService;
import com.ruoyi.erp.service.IShopifyStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Shopify 店铺管理 Controller
 */
@RestController
@RequestMapping("/erp/store")
@Tag(name = "Shopify店铺管理", description = "Shopify 店铺配置、连接测试、资源拉取和商品导入接口")
public class ShopifyStoreController extends BaseController {

    @Resource
    private IShopifyStoreService shopifyStoreService;
    @Resource
    private IShopifyProductImportService shopifyProductImportService;

    /**
     * 查询 Shopify 店铺列表
     */
    @PreAuthorize("@ss.hasPermi('erp:store:list')")
    @GetMapping("/list")
    @Operation(summary = "查询 Shopify 店铺列表", description = "按店铺名称、Shop 名称、启用状态、默认店铺、认证模式和连接状态分页查询店铺")
    @ApiResponse(responseCode = "200", description = "返回分页店铺列表")
    public TableDataInfo list(@ParameterObject ShopifyStoreQuery query) {
        startPage();
        List<ShopifyStore> list = shopifyStoreService.selectShopifyStoreList(query);
        TableDataInfo table = getDataTable(list);
        table.setRows(shopifyStoreService.convertVoList(list));
        return table;
    }

    /**
     * 查询启用店铺下拉选项
     */
    @PreAuthorize("@ss.hasAnyPermi('erp:store:list,erp:product:push')")
    @GetMapping("/active")
    @Operation(summary = "查询启用店铺下拉选项", description = "返回当前启用的 Shopify 店铺列表")
    @ApiResponse(responseCode = "200", description = "返回启用店铺列表")
    public AjaxResult active() {
        return success(shopifyStoreService.convertVoList(shopifyStoreService.selectActiveStores()));
    }

    /**
     * 导出 Shopify 店铺列表
     */
    @PreAuthorize("@ss.hasPermi('erp:store:export')")
    @Log(title = "Shopify 店铺", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出 Shopify 店铺列表", description = "按查询条件导出 Shopify 店铺数据")
    @ApiResponse(responseCode = "200", description = "返回 Excel 文件流")
    public void export(HttpServletResponse response, @ParameterObject ShopifyStoreQuery query) {
        List<ShopifyStore> list = shopifyStoreService.selectShopifyStoreList(query);
        ExcelUtil<ShopifyStore> util = new ExcelUtil<>(ShopifyStore.class);
        util.exportExcel(response, list, "Shopify 店铺数据");
    }

    /**
     * 获取 Shopify 店铺详情
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @GetMapping("/{storeId}")
    @Operation(summary = "获取 Shopify 店铺详情", description = "按店铺ID查询 Shopify 店铺配置详情")
    @ApiResponse(responseCode = "200", description = "返回店铺详情")
    public AjaxResult getInfo(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(ShopifyStoreVo.objToVo(shopifyStoreService.selectByStoreId(storeId)));
    }

    /**
     * 新增 Shopify 店铺
     */
    @PreAuthorize("@ss.hasPermi('erp:store:add')")
    @Log(title = "Shopify 店铺", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增 Shopify 店铺", description = "新增 Shopify 店铺配置")
    @ApiResponse(responseCode = "200", description = "返回新增结果")
    public AjaxResult add(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Shopify 店铺新增参数", required = true) @RequestBody ShopifyStoreInsert insert) {
        return toAjax(shopifyStoreService.insertShopifyStore(ShopifyStoreInsert.insertToObj(insert)));
    }

    /**
     * 修改 Shopify 店铺
     */
    @PreAuthorize("@ss.hasPermi('erp:store:edit')")
    @Log(title = "Shopify 店铺", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改 Shopify 店铺", description = "修改 Shopify 店铺配置")
    @ApiResponse(responseCode = "200", description = "返回修改结果")
    public AjaxResult edit(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Shopify 店铺编辑参数", required = true) @RequestBody ShopifyStoreEdit edit) {
        return toAjax(shopifyStoreService.updateShopifyStore(ShopifyStoreEdit.editToObj(edit)));
    }

    /**
     * 删除 Shopify 店铺
     */
    @PreAuthorize("@ss.hasPermi('erp:store:remove')")
    @Log(title = "Shopify 店铺", businessType = BusinessType.DELETE)
    @DeleteMapping("/{storeIds}")
    @Operation(summary = "删除 Shopify 店铺", description = "按店铺ID删除 Shopify 店铺配置，多个ID以英文逗号分隔")
    @ApiResponse(responseCode = "200", description = "返回删除结果")
    public AjaxResult remove(@Parameter(description = "店铺ID数组，多个ID以英文逗号分隔", required = true) @PathVariable Long[] storeIds) {
        return toAjax(shopifyStoreService.deleteShopifyStoreByStoreIds(storeIds));
    }

    /**
     * 测试 Shopify 店铺连接
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @PostMapping("/test/{storeId}")
    @Operation(summary = "测试 Shopify 店铺连接", description = "使用当前店铺配置测试 Shopify Admin API 连接")
    @ApiResponse(responseCode = "200", description = "连接成功时返回提示信息")
    public AjaxResult testConnection(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        shopifyStoreService.testConnection(storeId);
        return success("连接成功");
    }

    /**
     * 拉取 Shopify 库存仓库
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @GetMapping("/{storeId}/locations")
    @Operation(summary = "拉取 Shopify 库存仓库", description = "从 Shopify 查询当前店铺可用的库存仓库选项")
    @ApiResponse(responseCode = "200", description = "返回库存仓库选项列表")
    public AjaxResult locations(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(shopifyStoreService.fetchLocations(storeId));
    }

    /**
     * 拉取 Shopify 发布渠道
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @GetMapping("/{storeId}/publications")
    @Operation(summary = "拉取 Shopify 发布渠道", description = "从 Shopify 查询当前店铺可用的发布渠道选项")
    @ApiResponse(responseCode = "200", description = "返回发布渠道选项列表")
    public AjaxResult publications(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(shopifyStoreService.fetchPublications(storeId));
    }

    /**
     * 调用当前店铺 Shopify Admin API
     */
    @PreAuthorize("@ss.hasPermi('erp:store:edit')")
    @PostMapping("/{storeId}/api-call")
    @Operation(summary = "调用当前店铺 Shopify Admin API", description = "仅允许调用当前店铺对应的 Shopify Admin API 地址")
    @ApiResponse(responseCode = "200", description = "返回 Shopify Admin API 调用结果")
    public AjaxResult callAdminApi(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId,
                                   @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Shopify Admin API 调用参数", required = true) @RequestBody ShopifyApiCallRequest request) {
        return success(shopifyStoreService.callAdminApi(storeId, request));
    }

    /**
     * 手动触发 Shopify 商品全量导入
     */
    @PreAuthorize("@ss.hasPermi('erp:store:edit')")
    @PostMapping("/{storeId}/shopify-products/import-full")
    @Operation(summary = "手动触发 Shopify 商品全量导入", description = "为指定店铺创建并提交 Shopify 商品全量导入任务")
    @ApiResponse(responseCode = "200", description = "返回导入任务ID")
    public AjaxResult importShopifyProductsFull(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(shopifyProductImportService.startFullImport(storeId));
    }

    /**
     * 手动触发 Shopify 商品增量导入
     */
    @PreAuthorize("@ss.hasPermi('erp:store:edit')")
    @PostMapping("/{storeId}/shopify-products/import-incremental")
    @Operation(summary = "手动触发 Shopify 商品增量导入", description = "为指定店铺创建并提交 Shopify 商品增量导入任务")
    @ApiResponse(responseCode = "200", description = "返回导入任务ID")
    public AjaxResult importShopifyProductsIncremental(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(shopifyProductImportService.startIncrementalImport(storeId));
    }

    /**
     * 查询 Shopify 商品导入游标
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @GetMapping("/{storeId}/shopify-products/import-cursor")
    @Operation(summary = "查询 Shopify 商品导入游标", description = "查询指定店铺的 Shopify 商品反向同步游标")
    @ApiResponse(responseCode = "200", description = "返回商品导入游标")
    public AjaxResult shopifyProductImportCursor(@Parameter(description = "店铺ID", required = true) @PathVariable("storeId") Long storeId) {
        return success(shopifyProductImportService.getCursor(storeId));
    }
}

package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.dto.product.ProductPushRequest;
import com.ruoyi.erp.model.dto.product.ProductQuery;
import com.ruoyi.erp.model.vo.product.ProductVo;
import com.ruoyi.erp.service.IProductService;
import com.ruoyi.erp.service.IProductWizardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * erp商品Controller
 *
 * @author lwj
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/erp/product")
public class ProductController extends BaseController {
    @Resource
    private IProductService productService;
    @Resource
    private IProductWizardService productWizardService;

    /**
     * 查询erp商品列表
     */
    @PreAuthorize("@ss.hasPermi('erp:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductQuery productQuery) {
        Product product = ProductQuery.queryToObj(productQuery);
        startPage();
        List<ProductVo> list = productService.selectProductList(product);
        TableDataInfo table = getDataTable(list);
        table.setRows(list);
        return table;
    }

    /**
     * 查询商品工作台概览
     */
    @PreAuthorize("@ss.hasPermi('erp:product:list')")
    @GetMapping("/workbench-summary")
    public AjaxResult workbenchSummary(ProductQuery productQuery) {
        Product product = ProductQuery.queryToObj(productQuery);
        return success(productService.getWorkbenchSummary(product));
    }

    /**
     * 导出erp商品列表
     */
    @PreAuthorize("@ss.hasPermi('erp:product:export')")
    @Log(title = "erp商品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProductQuery productQuery) {
        Product product = ProductQuery.queryToObj(productQuery);
        List<ProductVo> list = productService.selectProductList(product);
        ExcelUtil<ProductVo> util = new ExcelUtil<ProductVo>(ProductVo.class);
        util.exportExcel(response, list, "erp商品数据");
    }

    /**
     * 获取erp商品详细信息
     */
    @PreAuthorize("@ss.hasPermi('erp:product:query')")
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable("productId") Long productId) {
        Product product = productService.selectProductByProductId(productId);
        return success(product);
    }

    /**
     * 删除erp商品
     */
    @PreAuthorize("@ss.hasPermi('erp:product:remove')")
    @Log(title = "erp商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds) {
        return toAjax(productService.deleteProductByProductIds(productIds));
    }

    /**
     * 导入erp商品数据
     */
    @PreAuthorize("@ss.hasPermi('erp:product:import')")
    @Log(title = "erp商品", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        List<Product> productList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = productService.importProductData(productList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载erp商品导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        util.importTemplateExcel(response, "erp商品数据");
    }

    /**
     * 批量推送商品到 Shopify
     * POST /erp/product/push-batch
     */
    @PreAuthorize("@ss.hasPermi('erp:product:push')")
    @Log(title = "批量推送", businessType = BusinessType.OTHER)
    @PostMapping("/push-batch")
    @Operation(summary = "批量推送商品到 Shopify", description = "按商品ID列表或查询条件创建 Shopify 商品批量推送任务", tags = {"Shopify商品同步"})
    @ApiResponse(responseCode = "200", description = "返回任务ID；未匹配到商品时返回普通成功结果")
    public AjaxResult pushBatch(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "商品批量推送请求", required = true) @RequestBody ProductPushRequest request) {
        Long taskId = productService.pushBatchByCondition(
                request.getProductQuery(),
                request.getProductIds(),
                request.getStoreId()
        );
        return taskId == null ? success() : success(java.util.Map.of("taskId", taskId));
    }

    /**
     * 批量发布已同步商品到店铺配置的渠道
     */
    @PreAuthorize("@ss.hasPermi('erp:product:push')")
    @Log(title = "批量发布渠道", businessType = BusinessType.OTHER)
    @PostMapping("/publish-channels")
    @Operation(summary = "批量发布已同步商品到 Shopify 渠道", description = "按商品ID列表将已同步商品发布到店铺配置的发布渠道", tags = {"Shopify商品同步"})
    @ApiResponse(responseCode = "200", description = "返回发布统计和明细")
    public AjaxResult publishChannels(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "商品发布渠道请求", required = true) @RequestBody ProductPushRequest request) {
        return success(productService.publishToChannels(request.getProductIds(), request.getStoreId()));
    }

    /**
     * 查询推送结果
     * GET /erp/product/push-result/{taskId}
     */
    @PreAuthorize("@ss.hasPermi('erp:product:push')")
    @GetMapping("/push-result/{taskId}")
    @Operation(summary = "查询 Shopify 商品推送结果", description = "按任务ID查询 Shopify 商品推送任务结果", tags = {"Shopify商品同步"})
    @ApiResponse(responseCode = "200", description = "返回任务结果")
    public AjaxResult pushResult(@Parameter(description = "任务ID", required = true) @PathVariable Long taskId) {
        return success(productService.getPushResult(taskId));
    }

}

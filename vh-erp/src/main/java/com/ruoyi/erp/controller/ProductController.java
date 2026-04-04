package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.dto.product.*;
import com.ruoyi.erp.model.vo.product.ProductVo;
import com.ruoyi.erp.service.IProductService;
import com.ruoyi.erp.service.IProductWizardService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

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
        List<Product> list = productService.selectProductList(product);
        List<ProductVo> listVo = list.stream().map(ProductVo::objToVo).collect(Collectors.toList());
        TableDataInfo table = getDataTable(list);
        table.setRows(listVo);
        return table;
    }

    /**
     * 导出erp商品列表
     */
    @PreAuthorize("@ss.hasPermi('erp:product:export')")
    @Log(title = "erp商品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProductQuery productQuery) {
        Product product = ProductQuery.queryToObj(productQuery);
        List<Product> list = productService.selectProductList(product);
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        util.exportExcel(response, list, "erp商品数据");
    }

    /**
     * 获取erp商品详细信息
     */
    @PreAuthorize("@ss.hasPermi('erp:product:query')")
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable("productId") Long productId) {
        Product product = productService.selectProductByProductId(productId);
        return success(ProductVo.objToVo(product));
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
     * 批量推送任务
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:push')")
    @Log(title = "批量推送", businessType = BusinessType.OTHER)
    @PostMapping("/vh-erp/product/push-batch")
    public AjaxResult pushBatch(@RequestBody List<Long> productIds) {
        Long taskId = productService.pushBatchAsync(productIds);
        return success().put("taskId", taskId);
    }

    /**
     * 查询推送结果
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:push')")
    @GetMapping("/push-result/{taskId}")
    public AjaxResult pushResult(@PathVariable Long taskId) {
        return success(productService.getPushResult(taskId));
    }

    /**
     * 编辑/新增 选品信息
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:add')")
    @Log(title = "编辑/新增 选品信息", businessType = BusinessType.INSERT)
    @PostMapping("/selectionInfo")
    public AjaxResult saveSelectionInfo(@RequestBody ProductSelectionEdit productSelectionEdit) {
        // 事务控制：保存 SPU -> 更新 Tag 流水号 -> 保存 Variants -> 保存 Media
        Product product = ProductSelectionEdit.editToObj(productSelectionEdit);
        return toAjax(productWizardService.saveProductWithWizard(product));
    }

    /**
     * 编辑 商品其他信息
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:edit')")
    @Log(title = "编辑/新增 商品其他信息", businessType = BusinessType.UPDATE)
    @PostMapping("/baseInfo")
    public AjaxResult saveProductBaseInfo(@RequestBody ProductBaseInfoEdit productSelectionEdit) {
        // 事务控制：保存 SPU -> 更新 Tag 流水号 -> 保存 Variants -> 保存 Media
        Product product = ProductBaseInfoEdit.editToObj(productSelectionEdit);
        return toAjax(productWizardService.saveProductWithWizard(product));
    }

}

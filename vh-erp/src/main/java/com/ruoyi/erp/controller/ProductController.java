package com.ruoyi.erp.controller;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.vo.product.ProductVo;
import com.ruoyi.erp.model.dto.product.ProductQuery;
import com.ruoyi.erp.model.dto.product.ProductInsert;
import com.ruoyi.erp.model.dto.product.ProductEdit;
import com.ruoyi.erp.service.IProductService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * erp商品Controller
 *
 * @author lwj
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/erp/product")
public class ProductController extends BaseController
{
    @Resource
    private IProductService productService;

    /**
     * 查询erp商品列表
     */
    @PreAuthorize("@ss.hasPermi('erp:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductQuery productQuery)
    {
        Product product = ProductQuery.queryToObj(productQuery);
        startPage();
        List<Product> list = productService.selectProductList(product);
        List<ProductVo> listVo= list.stream().map(ProductVo::objToVo).collect(Collectors.toList());
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
    public void export(HttpServletResponse response, ProductQuery productQuery)
    {
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
    public AjaxResult getInfo(@PathVariable("productId") Long productId)
    {
        Product product = productService.selectProductByProductId(productId);
        return success(ProductVo.objToVo(product));
    }

    /**
     * 新增erp商品
     */
    @PreAuthorize("@ss.hasPermi('erp:product:add')")
    @Log(title = "erp商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProductInsert productInsert)
    {
        Product product = ProductInsert.insertToObj(productInsert);
        return toAjax(productService.insertProduct(product));
    }

    /**
     * 修改erp商品
     */
    @PreAuthorize("@ss.hasPermi('erp:product:edit')")
    @Log(title = "erp商品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProductEdit productEdit)
    {
        Product product = ProductEdit.editToObj(productEdit);
        return toAjax(productService.updateProduct(product));
    }

    /**
     * 删除erp商品
     */
    @PreAuthorize("@ss.hasPermi('erp:product:remove')")
    @Log(title = "erp商品", businessType = BusinessType.DELETE)
	@DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(productService.deleteProductByProductIds(productIds));
    }

    /**
     * 导入erp商品数据
     */
    @PreAuthorize("@ss.hasPermi('erp:product:import')")
    @Log(title = "erp商品", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
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
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        util.importTemplateExcel(response, "erp商品数据");
    }

    /**
     * 分页查询商品，支持 tagIds 交集筛选
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:list')")
    @GetMapping("/vh-erp/product/list")
    public TableDataInfo vhList(ProductQuery productQuery)
    {
        // 实现 tagIds 交集筛选逻辑
        startPage();
        List<Product> list = productService.selectProductListByTags(productQuery);
        List<ProductVo> listVo = list.stream().map(ProductVo::objToVo).collect(Collectors.toList());
        TableDataInfo table = getDataTable(list);
        table.setRows(listVo);
        return table;
    }

    /**
     * 保存商品
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:add')")
    @Log(title = "保存商品", businessType = BusinessType.INSERT)
    @PostMapping("/vh-erp/product/save")
    public AjaxResult save(@RequestBody ProductInsert productInsert)
    {
        // 事务控制：保存 SPU -> 更新 Tag 流水号 -> 保存 Variants -> 保存 Media
        return toAjax(productService.saveProductWithTransaction(productInsert));
    }

    /**
     * 批量推送任务
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:push')")
    @Log(title = "批量推送", businessType = BusinessType.OTHER)
    @PostMapping("/vh-erp/product/push-batch")
    public AjaxResult pushBatch(@RequestBody List<Long> productIds)
    {
        Long taskId = productService.pushBatchAsync(productIds);
        return success().put("taskId", taskId);
    }

    /**
     * 查询推送结果
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:push')")
    @GetMapping("/vh-erp/product/push-result/{taskId}")
    public AjaxResult pushResult(@PathVariable Long taskId)
    {
        return success(productService.getPushResult(taskId));
    }
}

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
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.vo.productVariant.ProductVariantVo;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantQuery;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantInsert;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantEdit;
import com.ruoyi.erp.service.IProductVariantService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * erp商品变体Controller
 *
 * @author lwj
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/erp/variant")
public class ProductVariantController extends BaseController
{
    @Resource
    private IProductVariantService productVariantService;

    /**
     * 查询erp商品变体列表
     */
    @PreAuthorize("@ss.hasPermi('erp:variant:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductVariantQuery productVariantQuery)
    {
        ProductVariant productVariant = ProductVariantQuery.queryToObj(productVariantQuery);
        startPage();
        List<ProductVariant> list = productVariantService.selectProductVariantList(productVariant);
        List<ProductVariantVo> listVo= list.stream().map(ProductVariantVo::objToVo).collect(Collectors.toList());
        TableDataInfo table = getDataTable(list);
        table.setRows(listVo);
        return table;
    }

    /**
     * 导出erp商品变体列表
     */
    @PreAuthorize("@ss.hasPermi('erp:variant:export')")
    @Log(title = "erp商品变体", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProductVariantQuery productVariantQuery)
    {
        ProductVariant productVariant = ProductVariantQuery.queryToObj(productVariantQuery);
        List<ProductVariant> list = productVariantService.selectProductVariantList(productVariant);
        ExcelUtil<ProductVariant> util = new ExcelUtil<ProductVariant>(ProductVariant.class);
        util.exportExcel(response, list, "erp商品变体数据");
    }

    /**
     * 获取erp商品变体详细信息
     */
    @PreAuthorize("@ss.hasPermi('erp:variant:query')")
    @GetMapping(value = "/{variantId}")
    public AjaxResult getInfo(@PathVariable("variantId") Long variantId)
    {
        ProductVariant productVariant = productVariantService.selectProductVariantByVariantId(variantId);
        return success(ProductVariantVo.objToVo(productVariant));
    }

    /**
     * 新增erp商品变体
     */
    @PreAuthorize("@ss.hasPermi('erp:variant:add')")
    @Log(title = "erp商品变体", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProductVariantInsert productVariantInsert)
    {
        ProductVariant productVariant = ProductVariantInsert.insertToObj(productVariantInsert);
        return toAjax(productVariantService.insertProductVariant(productVariant));
    }

    /**
     * 修改erp商品变体
     */
    @PreAuthorize("@ss.hasPermi('erp:variant:edit')")
    @Log(title = "erp商品变体", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProductVariantEdit productVariantEdit)
    {
        ProductVariant productVariant = ProductVariantEdit.editToObj(productVariantEdit);
        return toAjax(productVariantService.updateProductVariant(productVariant));
    }

    /**
     * 删除erp商品变体
     */
    @PreAuthorize("@ss.hasPermi('erp:variant:remove')")
    @Log(title = "erp商品变体", businessType = BusinessType.DELETE)
	@DeleteMapping("/{variantIds}")
    public AjaxResult remove(@PathVariable Long[] variantIds)
    {
        return toAjax(productVariantService.deleteProductVariantByVariantIds(variantIds));
    }

    /**
     * 导入erp商品变体数据
     */
    @PreAuthorize("@ss.hasPermi('erp:variant:import')")
    @Log(title = "erp商品变体", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ProductVariant> util = new ExcelUtil<ProductVariant>(ProductVariant.class);
        List<ProductVariant> productVariantList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = productVariantService.importProductVariantData(productVariantList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载erp商品变体导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<ProductVariant> util = new ExcelUtil<ProductVariant>(ProductVariant.class);
        util.importTemplateExcel(response, "erp商品变体数据");
    }
}

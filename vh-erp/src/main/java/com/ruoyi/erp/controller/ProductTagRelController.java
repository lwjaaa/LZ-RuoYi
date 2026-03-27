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
import com.ruoyi.erp.model.domain.ProductTagRel;
import com.ruoyi.erp.model.vo.productTagRel.ProductTagRelVo;
import com.ruoyi.erp.model.dto.productTagRel.ProductTagRelQuery;
import com.ruoyi.erp.model.dto.productTagRel.ProductTagRelInsert;
import com.ruoyi.erp.model.dto.productTagRel.ProductTagRelEdit;
import com.ruoyi.erp.service.IProductTagRelService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * erp商品与标签关联Controller
 *
 * @author lwj
 * @date 2026-03-24
 */
@RestController
@RequestMapping("/vh-erp/ProductTag")
public class ProductTagRelController extends BaseController
{
    @Resource
    private IProductTagRelService productTagRelService;

    /**
     * 查询erp商品与标签关联列表
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:ProductTag:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductTagRelQuery productTagRelQuery)
    {
        ProductTagRel productTagRel = ProductTagRelQuery.queryToObj(productTagRelQuery);
        startPage();
        List<ProductTagRel> list = productTagRelService.selectProductTagRelList(productTagRel);
        List<ProductTagRelVo> listVo= list.stream().map(ProductTagRelVo::objToVo).collect(Collectors.toList());
        TableDataInfo table = getDataTable(list);
        table.setRows(listVo);
        return table;
    }

    /**
     * 导出erp商品与标签关联列表
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:ProductTag:export')")
    @Log(title = "erp商品与标签关联", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProductTagRelQuery productTagRelQuery)
    {
        ProductTagRel productTagRel = ProductTagRelQuery.queryToObj(productTagRelQuery);
        List<ProductTagRel> list = productTagRelService.selectProductTagRelList(productTagRel);
        ExcelUtil<ProductTagRel> util = new ExcelUtil<ProductTagRel>(ProductTagRel.class);
        util.exportExcel(response, list, "erp商品与标签关联数据");
    }

    /**
     * 获取erp商品与标签关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:ProductTag:query')")
    @GetMapping(value = "/{relId}")
    public AjaxResult getInfo(@PathVariable("relId") Long relId)
    {
        ProductTagRel productTagRel = productTagRelService.selectProductTagRelByRelId(relId);
        return success(ProductTagRelVo.objToVo(productTagRel));
    }

    /**
     * 新增erp商品与标签关联
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:ProductTag:add')")
    @Log(title = "erp商品与标签关联", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProductTagRelInsert productTagRelInsert)
    {
        ProductTagRel productTagRel = ProductTagRelInsert.insertToObj(productTagRelInsert);
        return toAjax(productTagRelService.insertProductTagRel(productTagRel));
    }

    /**
     * 修改erp商品与标签关联
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:ProductTag:edit')")
    @Log(title = "erp商品与标签关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProductTagRelEdit productTagRelEdit)
    {
        ProductTagRel productTagRel = ProductTagRelEdit.editToObj(productTagRelEdit);
        return toAjax(productTagRelService.updateProductTagRel(productTagRel));
    }

    /**
     * 删除erp商品与标签关联
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:ProductTag:remove')")
    @Log(title = "erp商品与标签关联", businessType = BusinessType.DELETE)
	@DeleteMapping("/{relIds}")
    public AjaxResult remove(@PathVariable Long[] relIds)
    {
        return toAjax(productTagRelService.deleteProductTagRelByRelIds(relIds));
    }

    /**
     * 导入erp商品与标签关联数据
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:ProductTag:import')")
    @Log(title = "erp商品与标签关联", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ProductTagRel> util = new ExcelUtil<ProductTagRel>(ProductTagRel.class);
        List<ProductTagRel> productTagRelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = productTagRelService.importProductTagRelData(productTagRelList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载erp商品与标签关联导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<ProductTagRel> util = new ExcelUtil<ProductTagRel>(ProductTagRel.class);
        util.importTemplateExcel(response, "erp商品与标签关联数据");
    }
}

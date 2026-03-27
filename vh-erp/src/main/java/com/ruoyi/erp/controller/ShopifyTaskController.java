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
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskVo;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskQuery;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskInsert;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskEdit;
import com.ruoyi.erp.service.IShopifyTaskService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * Shopify 任务配置Controller
 *
 * @author lwj
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/erp/task")
public class ShopifyTaskController extends BaseController
{
    @Resource
    private IShopifyTaskService shopifyTaskService;

    /**
     * 查询Shopify 任务配置列表
     */
    @PreAuthorize("@ss.hasPermi('erp:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(ShopifyTaskQuery shopifyTaskQuery)
    {
        ShopifyTask shopifyTask = ShopifyTaskQuery.queryToObj(shopifyTaskQuery);
        startPage();
        List<ShopifyTask> list = shopifyTaskService.selectShopifyTaskList(shopifyTask);
        List<ShopifyTaskVo> listVo= list.stream().map(ShopifyTaskVo::objToVo).collect(Collectors.toList());
        TableDataInfo table = getDataTable(list);
        table.setRows(listVo);
        return table;
    }

    /**
     * 导出Shopify 任务配置列表
     */
    @PreAuthorize("@ss.hasPermi('erp:task:export')")
    @Log(title = "Shopify 任务配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ShopifyTaskQuery shopifyTaskQuery)
    {
        ShopifyTask shopifyTask = ShopifyTaskQuery.queryToObj(shopifyTaskQuery);
        List<ShopifyTask> list = shopifyTaskService.selectShopifyTaskList(shopifyTask);
        ExcelUtil<ShopifyTask> util = new ExcelUtil<ShopifyTask>(ShopifyTask.class);
        util.exportExcel(response, list, "Shopify 任务配置数据");
    }

    /**
     * 获取Shopify 任务配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('erp:task:query')")
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") Long taskId)
    {
        ShopifyTask shopifyTask = shopifyTaskService.selectShopifyTaskByTaskId(taskId);
        return success(ShopifyTaskVo.objToVo(shopifyTask));
    }

    /**
     * 新增Shopify 任务配置
     */
    @PreAuthorize("@ss.hasPermi('erp:task:add')")
    @Log(title = "Shopify 任务配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ShopifyTaskInsert shopifyTaskInsert)
    {
        ShopifyTask shopifyTask = ShopifyTaskInsert.insertToObj(shopifyTaskInsert);
        return toAjax(shopifyTaskService.insertShopifyTask(shopifyTask));
    }

    /**
     * 修改Shopify 任务配置
     */
    @PreAuthorize("@ss.hasPermi('erp:task:edit')")
    @Log(title = "Shopify 任务配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ShopifyTaskEdit shopifyTaskEdit)
    {
        ShopifyTask shopifyTask = ShopifyTaskEdit.editToObj(shopifyTaskEdit);
        return toAjax(shopifyTaskService.updateShopifyTask(shopifyTask));
    }

    /**
     * 删除Shopify 任务配置
     */
    @PreAuthorize("@ss.hasPermi('erp:task:remove')")
    @Log(title = "Shopify 任务配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds)
    {
        return toAjax(shopifyTaskService.deleteShopifyTaskByTaskIds(taskIds));
    }

    /**
     * 导入Shopify 任务配置数据
     */
    @PreAuthorize("@ss.hasPermi('erp:task:import')")
    @Log(title = "Shopify 任务配置", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ShopifyTask> util = new ExcelUtil<ShopifyTask>(ShopifyTask.class);
        List<ShopifyTask> shopifyTaskList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = shopifyTaskService.importShopifyTaskData(shopifyTaskList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载Shopify 任务配置导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<ShopifyTask> util = new ExcelUtil<ShopifyTask>(ShopifyTask.class);
        util.importTemplateExcel(response, "Shopify 任务配置数据");
    }
}

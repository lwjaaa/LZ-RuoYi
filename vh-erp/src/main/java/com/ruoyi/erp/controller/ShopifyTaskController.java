package com.ruoyi.erp.controller;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.annotations.ParameterObject;
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
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskDetailQuery;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskVo;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDetailVo;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskQuery;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskInsert;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskEdit;
import com.ruoyi.erp.service.IShopifyTaskDetailService;
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
@Tag(name = "Shopify同步任务", description = "Shopify 同步任务、任务明细和诊断接口")
public class ShopifyTaskController extends BaseController
{
    @Resource
    private IShopifyTaskService shopifyTaskService;
    @Resource
    private IShopifyTaskDetailService shopifyTaskDetailService;

    /**
     * 查询Shopify 任务配置列表
     */
    @PreAuthorize("@ss.hasPermi('erp:task:list')")
    @GetMapping("/list")
    @Operation(summary = "查询 Shopify 同步任务列表", description = "按店铺、任务名称、任务类型、任务状态和开始时间分页查询 Shopify 同步任务")
    @ApiResponse(responseCode = "200", description = "返回分页任务列表")
    public TableDataInfo list(@ParameterObject ShopifyTaskQuery shopifyTaskQuery)
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
    @Operation(summary = "导出 Shopify 同步任务列表", description = "按查询条件导出 Shopify 同步任务数据")
    @ApiResponse(responseCode = "200", description = "返回 Excel 文件流")
    public void export(HttpServletResponse response, @ParameterObject ShopifyTaskQuery shopifyTaskQuery)
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
    @Operation(summary = "获取 Shopify 同步任务详情", description = "按任务ID查询 Shopify 同步任务详情")
    @ApiResponse(responseCode = "200", description = "返回任务详情")
    public AjaxResult getInfo(@Parameter(description = "任务ID", required = true) @PathVariable("taskId") Long taskId)
    {
        ShopifyTask shopifyTask = shopifyTaskService.selectShopifyTaskByTaskId(taskId);
        return success(ShopifyTaskVo.objToVo(shopifyTask));
    }

    /**
     * 获取 Shopify 同步任务诊断汇总
     */
    @PreAuthorize("@ss.hasPermi('erp:task:query')")
    @GetMapping(value = "/{taskId}/diagnostics")
    @Operation(summary = "获取 Shopify 同步任务诊断汇总", description = "按任务ID查询明细类型、状态、失败步骤、高频错误和近期失败明细")
    @ApiResponse(responseCode = "200", description = "返回任务诊断汇总")
    public AjaxResult diagnostics(@Parameter(description = "任务ID", required = true) @PathVariable("taskId") Long taskId)
    {
        return success(shopifyTaskDetailService.selectTaskDiagnostics(taskId));
    }

    /**
     * 查询 Shopify 同步任务明细
     */
    @PreAuthorize("@ss.hasPermi('erp:task:query')")
    @GetMapping(value = "/{taskId}/details")
    @Operation(summary = "查询 Shopify 同步任务明细", description = "按任务ID、商品ID、明细类型和明细状态分页查询同步任务明细")
    @ApiResponse(responseCode = "200", description = "返回分页任务明细")
    public TableDataInfo details(@Parameter(description = "任务ID", required = true) @PathVariable("taskId") Long taskId,
                                 @ParameterObject ShopifyTaskDetailQuery query)
    {
        query.setTaskId(taskId);
        ShopifyTaskDetail detail = ShopifyTaskDetailQuery.queryToObj(query);
        startPage();
        List<ShopifyTaskDetail> list = shopifyTaskDetailService.selectShopifyTaskDetailList(detail);
        List<ShopifyTaskDetailVo> listVo = list.stream().map(ShopifyTaskDetailVo::objToVo).collect(Collectors.toList());
        TableDataInfo table = getDataTable(list);
        table.setRows(listVo);
        return table;
    }

    /**
     * 新增Shopify 任务配置
     */
    @PreAuthorize("@ss.hasPermi('erp:task:add')")
    @Log(title = "Shopify 任务配置", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增 Shopify 同步任务", description = "新增 Shopify 同步任务记录")
    @ApiResponse(responseCode = "200", description = "返回新增结果")
    public AjaxResult add(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Shopify 同步任务新增参数", required = true) @RequestBody ShopifyTaskInsert shopifyTaskInsert)
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
    @Operation(summary = "修改 Shopify 同步任务", description = "修改 Shopify 同步任务记录")
    @ApiResponse(responseCode = "200", description = "返回修改结果")
    public AjaxResult edit(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Shopify 同步任务编辑参数", required = true) @RequestBody ShopifyTaskEdit shopifyTaskEdit)
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
    @Operation(summary = "删除 Shopify 同步任务", description = "按任务ID删除 Shopify 同步任务，多个ID以英文逗号分隔")
    @ApiResponse(responseCode = "200", description = "返回删除结果")
    public AjaxResult remove(@Parameter(description = "任务ID数组，多个ID以英文逗号分隔", required = true) @PathVariable Long[] taskIds)
    {
        return toAjax(shopifyTaskService.deleteShopifyTaskByTaskIds(taskIds));
    }

    /**
     * 导入Shopify 任务配置数据
     */
    @PreAuthorize("@ss.hasPermi('erp:task:import')")
    @Log(title = "Shopify 任务配置", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @Operation(summary = "导入 Shopify 同步任务数据", description = "通过 Excel 文件导入 Shopify 同步任务数据")
    @ApiResponse(responseCode = "200", description = "返回导入结果")
    public AjaxResult importData(@Parameter(description = "导入文件", required = true) MultipartFile file,
                                 @Parameter(description = "是否更新已存在数据") boolean updateSupport) throws Exception
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
    @Operation(summary = "下载 Shopify 同步任务导入模板", description = "下载 Shopify 同步任务 Excel 导入模板")
    @ApiResponse(responseCode = "200", description = "返回 Excel 模板文件流")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<ShopifyTask> util = new ExcelUtil<ShopifyTask>(ShopifyTask.class);
        util.importTemplateExcel(response, "Shopify 任务配置数据");
    }
}

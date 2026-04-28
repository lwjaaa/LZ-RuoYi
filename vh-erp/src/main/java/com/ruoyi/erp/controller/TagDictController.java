package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.constant.TagConstants;
import com.ruoyi.erp.model.domain.TagDict;
import com.ruoyi.erp.model.dto.tagDict.TagDictEdit;
import com.ruoyi.erp.model.dto.tagDict.TagDictInsert;
import com.ruoyi.erp.model.dto.tagDict.TagDictQuery;
import com.ruoyi.erp.model.dto.tagDict.TreeDragDTO;
import com.ruoyi.erp.model.vo.tagDict.TagDictMenuVo;
import com.ruoyi.erp.model.vo.tagDict.TagDictVo;
import com.ruoyi.erp.service.ITagDictService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * erp标签Controller
 *
 * @author lwj
 * @date 2026-03-25
 */
@RestController
@RequestMapping("/erp/tag")
public class TagDictController extends BaseController
{
    @Resource
    private ITagDictService tagDictService;

    /**
     * 查询erp标签列表
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:list')")
    @GetMapping("/list")
    public AjaxResult list(TagDictQuery tagDictQuery)
    {
        TagDict tagDict = TagDictQuery.queryToObj(tagDictQuery);
        List<TagDict> list = tagDictService.selectTagDictList(tagDict);
        List<TagDictVo> listVo= list.stream().map(TagDictVo::objToVo).collect(Collectors.toList());
        return success(listVo);
    }

    /**
     * 导出erp标签列表
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:export')")
    @Log(title = "erp标签", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TagDictQuery tagDictQuery)
    {
        TagDict tagDict = TagDictQuery.queryToObj(tagDictQuery);
        List<TagDict> list = tagDictService.selectTagDictList(tagDict);
        ExcelUtil<TagDict> util = new ExcelUtil<TagDict>(TagDict.class);
        util.exportExcel(response, list, "erp标签数据");
    }

    /**
     * 获取erp标签详细信息
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:query')")
    @GetMapping(value = "/{tagId}")
    public AjaxResult getInfo(@PathVariable("tagId") Long tagId)
    {
        TagDict tagDict = tagDictService.selectTagDictByTagId(tagId);
        return success(TagDictVo.objToVo(tagDict));
    }

    /**
     * 新增erp标签
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:add')")
    @Log(title = "erp标签", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TagDictInsert tagDictInsert)
    {
        TagDict tagDict = TagDictInsert.insertToObj(tagDictInsert);

        checkAndResetParam(tagDict);
        return toAjax(tagDictService.insertTagDict(tagDict));
    }

    /**
     * 修改erp标签
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:edit')")
    @Log(title = "erp标签", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TagDictEdit tagDictEdit)
    {
        TagDict tagDict = TagDictEdit.editToObj(tagDictEdit);
        checkAndResetParam(tagDict);
        return toAjax(tagDictService.updateTagDict(tagDict));
    }


    /**
     * 拖拽修改节点位置
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:edit')")
    @Log(title = "标签拖拽", businessType = BusinessType.UPDATE)
    @PostMapping("/dragNode")
    public AjaxResult dragNode(@RequestBody @Valid TreeDragDTO dto) {
        tagDictService.dragNode(dto);
        return AjaxResult.success("拖拽保存成功");
    }

    /**
     * 删除erp标签
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:remove')")
    @Log(title = "erp标签", businessType = BusinessType.DELETE)
	@DeleteMapping("/{tagIds}")
    public AjaxResult remove(@PathVariable Long[] tagIds)
    {
        return toAjax(tagDictService.deleteTagDictByTagIds(tagIds));
    }

    /**
     * 导入erp标签数据
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:import')")
    @Log(title = "erp标签", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<TagDict> util = new ExcelUtil<TagDict>(TagDict.class);
        List<TagDict> tagDictList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = tagDictService.importTagDictData(tagDictList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载erp标签导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<TagDict> util = new ExcelUtil<TagDict>(TagDict.class);
        util.importTemplateExcel(response, "erp标签数据");
    }

    /**
     * 获取菜单树（带缓存）
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:list')")
    @GetMapping("/treelist/{type}")
    public AjaxResult getMenuList(@PathVariable("type") String type)
    {
        List<TagDictMenuVo> tree = tagDictService.getTagMenuList(type);
        return success(tree);
    }

    /**
     * 刷新菜单缓存
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:edit')")
    @Log(title = "刷新菜单缓存", businessType = BusinessType.CLEAN)
    @DeleteMapping("/refreshCache")
    public AjaxResult refreshCache()
    {
        tagDictService.refreshMenuCache();
        return success("缓存已刷新");
    }
    /**
     * 置顶
     */
    @PreAuthorize("@ss.hasPermi('erp:tag:edit')")
    @Log(title = "置顶", businessType = BusinessType.UPDATE)
    @PostMapping("/top/{tagId}")
    public AjaxResult top(@PathVariable("tagId") Long tagId) {
        return toAjax(tagDictService.top(tagId));
    }


    /**
     * 参数校验
     */
    private void checkAndResetParam(TagDict tagDict){
        if(tagDict.getTagType() == null){
            throw new IllegalArgumentException("参数错误");
        }
        if(!tagDict.getTagType().equals(TagConstants.TYPE_MENU)){
            tagDict.setParentId(0L);
            tagDict.setMenuLevel(1);
            tagDict.setSpuPrefix(null);
            tagDict.setCurrentMaxSeq(null);
            tagDict.setAncestors("0");

        }
    }
}

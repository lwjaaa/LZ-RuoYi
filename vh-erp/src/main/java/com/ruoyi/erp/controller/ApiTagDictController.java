package com.ruoyi.erp.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.model.vo.tagDict.TagDictMenuVo;
import com.ruoyi.erp.service.ITagDictService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * erp标签Controller
 *
 * @author lwj
 * @date 2026-03-25
 */
@RestController
@RequestMapping("/api/erp/tag")
public class ApiTagDictController extends BaseController
{
    @Resource
    private ITagDictService tagDictService;

    /**
     * 获取菜单树（带缓存）
     */
    @GetMapping("/treelist/{type}")
    public AjaxResult getMenuList(@PathVariable("type") String type)
    {
        List<TagDictMenuVo> tree = tagDictService.getTagMenuList(type);
        return success(tree);
    }

}

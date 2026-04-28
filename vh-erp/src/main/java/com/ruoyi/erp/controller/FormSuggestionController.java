package com.ruoyi.erp.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.model.domain.FormSuggestion;
import com.ruoyi.erp.service.IFormSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表单快速提示词Controller
 *
 * @author ruoyi
 * @date 2026-04-27
 */
@RestController
@RequestMapping("/erp/formSuggestion")
public class FormSuggestionController extends BaseController {
    
    @Autowired
    private IFormSuggestionService formSuggestionService;

    /**
     * 批量获取多个字段的提示词列表
     * 
     * 请求示例：
     * {
     *   "fields": [
     *     {"fieldName": "size", "limit": 30},
     *     {"fieldName": "material", "limit": 30},
     *     {"fieldName": "note", "limit": 30},
     *     {"fieldName": "noteCn", "limit": 30}
     *   ]
     * }
     */
    @PostMapping("/batchList")
    public AjaxResult batchList(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldConfigs = (List<Map<String, Object>>) params.get("fields");
        
        if (fieldConfigs == null || fieldConfigs.isEmpty()) {
            return AjaxResult.error("请提供字段配置");
        }
        
        // 为每个字段设置默认限制为30
        for (Map<String, Object> config : fieldConfigs) {
            if (!config.containsKey("limit")) {
                config.put("limit", 30);
            }
        }
        
        List<FormSuggestion> suggestions = formSuggestionService.getSuggestionsByFields(fieldConfigs);
        return AjaxResult.success(suggestions);
    }

    /**
     * 批量更新提示词使用记录
     * 
     * 请求示例：
     * {
     *   "updates": [
     *     {"fieldName": "size", "oldValue": "M", "newValue": "L"},
     *     {"fieldName": "material", "oldValue": "棉", "newValue": "丝绸"}
     *   ]
     * }
     */
    @PostMapping("/batchUpdateUsage")
    public AjaxResult batchUpdateUsage(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> updates = (List<Map<String, String>>) params.get("updates");
        
        if (updates == null || updates.isEmpty()) {
            return AjaxResult.error("请提供更新数据");
        }
        
        formSuggestionService.batchUpdateUsage(updates);
        return AjaxResult.success();
    }
}

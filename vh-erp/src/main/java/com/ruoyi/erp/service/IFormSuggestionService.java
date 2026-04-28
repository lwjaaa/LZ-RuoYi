package com.ruoyi.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.erp.model.domain.FormSuggestion;

import java.util.List;
import java.util.Map;

/**
 * 表单快速提示词Service接口
 *
 * @author ruoyi
 * @date 2026-04-27
 */
public interface IFormSuggestionService extends IService<FormSuggestion> {
    
    /**
     * 批量查询多个字段的提示词
     * @param fieldConfigs 字段配置列表，每个配置包含 fieldName 和 limit
     * @return 提示词列表
     */
    List<FormSuggestion> getSuggestionsByFields(List<Map<String, Object>> fieldConfigs);

    /**
     * 更新提示词使用记录（如果值未变化则不更新时间）
     * @param fieldName 字段名
     * @param oldValue 旧值
     * @param newValue 新值
     */
    void updateUsage(String fieldName, String oldValue, String newValue);

    /**
     * 批量更新提示词使用记录
     * @param updates 更新列表，每个元素包含 fieldName, oldValue, newValue
     */
    void batchUpdateUsage(List<Map<String, String>> updates);
}

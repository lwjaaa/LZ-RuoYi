package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.erp.mapper.FormSuggestionMapper;
import com.ruoyi.erp.model.domain.FormSuggestion;
import com.ruoyi.erp.service.IFormSuggestionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 表单快速提示词Service业务层处理
 *
 * @author ruoyi
 * @date 2026-04-27
 */
@Service
public class FormSuggestionServiceImpl extends ServiceImpl<FormSuggestionMapper, FormSuggestion> implements IFormSuggestionService {

    @Resource
    private FormSuggestionMapper formSuggestionMapper;

    /**
     * 批量查询多个字段的提示词
     * @param fieldConfigs 字段配置列表，每个配置包含 fieldName 和 limit
     * @return 提示词列表
     */
    @Override
    public List<FormSuggestion> getSuggestionsByFields(List<Map<String, Object>> fieldConfigs) {
        if (fieldConfigs == null || fieldConfigs.isEmpty()) {
            return List.of();
        }
        return formSuggestionMapper.selectSuggestionsByFields(fieldConfigs);
    }

    /**
     * 更新提示词使用记录（如果值未变化则不更新时间）
     * @param fieldName 字段名
     * @param oldValue 旧值
     * @param newValue 新值
     */
    @Override
    public void updateUsage(String fieldName, String oldValue, String newValue) {
        // 如果值为空，不处理
        if (!StringUtils.hasText(newValue)) {
            return;
        }

        // 如果旧值和新值相同，不更新
        if (StringUtils.hasText(oldValue) && oldValue.equals(newValue)) {
            return;
        }

        // 查找是否已存在该值
        FormSuggestion existing = formSuggestionMapper.selectByFieldAndValue(fieldName, newValue);
        
        Date now = new Date();
        
        if (existing != null) {
            // 如果存在，更新最后使用时间
            existing.setLastUsedTime(now);
            existing.setUpdateTime(now);
            formSuggestionMapper.updateFormSuggestion(existing);
        } else {
            // 如果不存在，新增记录
            FormSuggestion suggestion = new FormSuggestion();
            suggestion.setFieldName(fieldName);
            suggestion.setFieldValue(newValue);
            suggestion.setLastUsedTime(now);
            suggestion.setCreateTime(now);
            suggestion.setUpdateTime(now);
            suggestion.setDelFlag("0");
            formSuggestionMapper.insertFormSuggestion(suggestion);
        }
    }

    /**
     * 批量更新提示词使用记录
     * @param updates 更新列表，每个元素包含 fieldName, oldValue, newValue
     */
    @Override
    public void batchUpdateUsage(List<Map<String, String>> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        
        for (Map<String, String> update : updates) {
            String fieldName = update.get("fieldName");
            String oldValue = update.get("oldValue");
            String newValue = update.get("newValue");
            updateUsage(fieldName, oldValue, newValue);
        }
    }
}

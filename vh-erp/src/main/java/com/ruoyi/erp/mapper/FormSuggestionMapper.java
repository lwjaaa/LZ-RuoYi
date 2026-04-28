package com.ruoyi.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.erp.model.domain.FormSuggestion;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 表单快速提示词Mapper接口
 *
 * @author ruoyi
 * @date 2026-04-27
 */
public interface FormSuggestionMapper extends BaseMapper<FormSuggestion> {
    
    /**
     * 批量查询多个字段的提示词
     * @param fieldConfigs 字段配置列表，每个配置包含 fieldName 和 limit
     * @return 提示词列表
     */
    List<FormSuggestion> selectSuggestionsByFields(@Param("fieldConfigs") List<Map<String, Object>> fieldConfigs);

    /**
     * 根据字段名和值查询
     * @param fieldName 字段名
     * @param fieldValue 字段值
     * @return 提示词对象
     */
    FormSuggestion selectByFieldAndValue(@Param("fieldName") String fieldName, 
                                       @Param("fieldValue") String fieldValue);

    /**
     * 新增提示词
     * @param suggestion 提示词对象
     * @return 结果
     */
    int insertFormSuggestion(FormSuggestion suggestion);

    /**
     * 修改提示词（更新最后使用时间）
     * @param suggestion 提示词对象
     * @return 结果
     */
    int updateFormSuggestion(FormSuggestion suggestion);
}

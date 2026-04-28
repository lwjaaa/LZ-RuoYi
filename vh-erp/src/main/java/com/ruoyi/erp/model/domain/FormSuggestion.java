package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 表单快速提示词对象 erp_form_suggestion
 *
 * @author ruoyi
 * @date 2026-04-27
 */
@TableName("erp_form_suggestion")
@Data
public class FormSuggestion implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "suggestion_id", type = IdType.AUTO)
    private Long suggestionId;

    /** 字段名称 (如: size, material, note, note_cn) */
    private String fieldName;

    /** 字段值内容 */
    private String fieldValue;

    /** 最后使用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUsedTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 删除标志 (0代表存在 1代表删除) */
    private String delFlag;
}

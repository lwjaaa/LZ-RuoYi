package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Shopify 同步任务摘要对象 erp_shopify_task
 *
 * @author lwj
 * @date 2026-03-26
 */
@TableName("erp_shopify_task")
@Data
public class ShopifyTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 任务主键 */
    @TableId(value = "task_id", type = IdType.ASSIGN_ID)
    private Long taskId;

    /** 关联的店铺ID */
    private Long storeId;

    /** Shopify 店铺名称 */
    private String shopName;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 任务类型 */
    @Excel(name = "任务类型", dictType = "erp_task_type")
    private String taskType;

    /** 任务状态 */
    @Excel(name = "任务状态", dictType = "erp_task_status")
    private String taskStatus;

    /** 执行进度（0-100） */
    @Excel(name = "执行进度")
    private Integer progress;

    /** 总数 */
    @Excel(name = "总数")
    private Integer totalCount;

    /** 成功数 */
    @Excel(name = "成功数")
    private Integer successCount;

    /** 部分成功数 */
    @Excel(name = "部分成功数")
    private Integer partialCount;

    /** 失败数 */
    @Excel(name = "失败数")
    private Integer failedCount;

    /** 错误摘要 */
    @Excel(name = "错误摘要")
    private String errorMessage;

    /** 执行耗时（毫秒） */
    @Excel(name = "执行耗时")
    private Long executionTime;

    /** 开始执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

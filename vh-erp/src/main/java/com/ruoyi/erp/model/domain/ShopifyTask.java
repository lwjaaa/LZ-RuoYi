package com.ruoyi.erp.model.domain;

import java.io.Serializable;
import java.util.Map;
import java.util.Date;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.ruoyi.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Shopify 任务配置对象 erp_shopify_task
 *
 * @author lwj
 * @date 2026-03-26
 */
@TableName("erp_shopify_task")
@Data
public class ShopifyTask implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务主键 */
    @TableId(value = "task_id", type = IdType.ASSIGN_ID)
    private Long taskId;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 任务分组（商品管理/媒体管理） */
    @Excel(name = "任务分组", readConverterExp = "商=品管理/媒体管理", dictType = "erp_task_group")
    private String taskGroup;

    /** 任务类型（PRODUCT_CREATE=商品创建，MEDIA_SYNC=媒体同步） */
    @Excel(name = "任务类型", readConverterExp = "P=RODUCT_CREATE=商品创建，MEDIA_SYNC=媒体同步", dictType = "erp_task_type")
    private String taskType;

    /** 关联业务类型（PRODUCT=商品，MEDIA=媒体） */
    @Excel(name = "关联业务类型", readConverterExp = "P=RODUCT=商品，MEDIA=媒体", dictType = "erp_task_business_type")
    private String businessType;

    /** 关联业务 ID 集合（英文逗号分割，如：123,456） */
    @Excel(name = "关联业务 ID 集合", readConverterExp = "英=文逗号分割，如：123,456")
    private String businessIds;

    /** 请求地址 */
    @Excel(name = "请求地址")
    private String requestPath;

    /** 请求参数 */
    @Excel(name = "请求参数")
    private String requestParams;

    /** 任务状态（PENDING=待执行，RUNNING=执行中，SUCCESS=成功，FAILED=失败，PARTIAL_SUCCESS=部分成功，CANCELLED=已取消） */
    @Excel(name = "任务状态", readConverterExp = "P=ENDING=待执行，RUNNING=执行中，SUCCESS=成功，FAILED=失败，PARTIAL_SUCCESS=部分成功，CANCELLED=已取消", dictType = "erp_task_status")
    private String taskStatus;

    /** 执行进度（0-100） */
    @Excel(name = "执行进度", readConverterExp = "0=-100")
    private Long progress;

    /** 错误信息 */
    @Excel(name = "错误信息")
    private String errorMessage;

    /** 执行结果数据（JSON 格式） */
    @Excel(name = "执行结果数据", readConverterExp = "J=SON,格=式")
    private String resultData;

    /** 执行耗时（毫秒） */
    @Excel(name = "执行耗时", readConverterExp = "毫=秒")
    private Long executionTime;

    /** 开始执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始执行时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    /** 父任务 ID（用于任务分解） */
    @Excel(name = "父任务 ID", readConverterExp = "用=于任务分解")
    private Long parentTaskId;

    /** 根任务 ID（用于追踪批量任务） */
    @Excel(name = "根任务 ID", readConverterExp = "用=于追踪批量任务")
    private Long rootTaskId;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 删除标志（0=存在，1=删除） */
    private String delFlag;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

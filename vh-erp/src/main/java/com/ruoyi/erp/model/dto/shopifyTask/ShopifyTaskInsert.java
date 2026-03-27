package com.ruoyi.erp.model.dto.shopifyTask;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import com.ruoyi.erp.model.domain.ShopifyTask;
/**
 * Shopify 任务配置Vo对象 erp_shopify_task
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class ShopifyTaskInsert implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务主键 */
    private Long taskId;

    /** 任务名称 */
    private String taskName;

    /** 任务分组（商品管理/媒体管理） */
    private String taskGroup;

    /** 任务类型（PRODUCT_CREATE=商品创建，MEDIA_SYNC=媒体同步） */
    private String taskType;

    /** 关联业务类型（PRODUCT=商品，MEDIA=媒体） */
    private String businessType;

    /** 关联业务 ID 集合（英文逗号分割，如：123,456） */
    private String businessIds;

    /** 请求地址 */
    private String requestPath;

    /** 请求参数 */
    private String requestParams;

    /** 任务状态（PENDING=待执行，RUNNING=执行中，SUCCESS=成功，FAILED=失败，PARTIAL_SUCCESS=部分成功，CANCELLED=已取消） */
    private String taskStatus;

    /** 执行进度（0-100） */
    private Long progress;

    /** 错误信息 */
    private String errorMessage;

    /** 执行结果数据（JSON 格式） */
    private String resultData;

    /** 执行耗时（毫秒） */
    private Long executionTime;

    /** 开始执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endTime;

    /** 父任务 ID（用于任务分解） */
    private Long parentTaskId;

    /** 根任务 ID（用于追踪批量任务） */
    private Long rootTaskId;

    /** 备注 */
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

    /**
     * 对象转封装类
     *
     * @param shopifyTaskInsert 插入对象
     * @return ShopifyTaskInsert
     */
    public static ShopifyTask insertToObj(ShopifyTaskInsert shopifyTaskInsert) {
        if (shopifyTaskInsert == null) {
            return null;
        }
        ShopifyTask shopifyTask = new ShopifyTask();
        BeanUtils.copyProperties(shopifyTaskInsert, shopifyTask);
        return shopifyTask;
    }
}

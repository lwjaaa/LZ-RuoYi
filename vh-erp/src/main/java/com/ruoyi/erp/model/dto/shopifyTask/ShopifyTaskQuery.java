package com.ruoyi.erp.model.dto.shopifyTask;

import java.util.Map;
import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.BeanUtils;
import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.erp.model.domain.ShopifyTask;
/**
 * Shopify 任务配置Query对象 erp_shopify_task
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class ShopifyTaskQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

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

    /** 任务状态（PENDING=待执行，RUNNING=执行中，SUCCESS=成功，FAILED=失败，PARTIAL_SUCCESS=部分成功，CANCELLED=已取消） */
    private String taskStatus;

    /** 开始执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;

    /** 父任务 ID（用于任务分解） */
    private Long parentTaskId;

    /** 根任务 ID（用于追踪批量任务） */
    private Long rootTaskId;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    /**
     * 对象转封装类
     *
     * @param shopifyTaskQuery 查询对象
     * @return ShopifyTask
     */
    public static ShopifyTask queryToObj(ShopifyTaskQuery shopifyTaskQuery) {
        if (shopifyTaskQuery == null) {
            return null;
        }
        ShopifyTask shopifyTask = new ShopifyTask();
        BeanUtils.copyProperties(shopifyTaskQuery, shopifyTask);
        return shopifyTask;
    }
}

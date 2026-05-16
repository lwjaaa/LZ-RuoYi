package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 订单轮询游标对象 erp_shopify_order_sync_cursor
 */
@TableName("erp_shopify_order_sync_cursor")
@Data
public class ShopifyOrderSyncCursor implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 游标主键 */
    @TableId(value = "cursor_id", type = IdType.ASSIGN_ID)
    private Long cursorId;

    /** 店铺ID */
    private Long storeId;

    /** 游标状态 */
    private String status;

    /** 上次成功推进到的 Shopify 订单更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessUpdatedAt;

    /** 上次成功同步完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessSyncTime;

    /** 最近一次任务ID */
    private Long lastTaskId;

    /** 最近一次错误摘要 */
    private String lastErrorSummary;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}

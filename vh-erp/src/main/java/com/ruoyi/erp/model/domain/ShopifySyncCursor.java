package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 反向同步游标 erp_shopify_sync_cursor
 */
@TableName("erp_shopify_sync_cursor")
@Data
public class ShopifySyncCursor implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "cursor_id", type = IdType.ASSIGN_ID)
    private Long cursorId;

    private Long storeId;

    private String syncMode;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessUpdatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessSyncTime;

    private String lastBulkOperationId;

    private Long lastTaskId;

    private String lastErrorSummary;

    private Date createTime;

    private Date updateTime;
}

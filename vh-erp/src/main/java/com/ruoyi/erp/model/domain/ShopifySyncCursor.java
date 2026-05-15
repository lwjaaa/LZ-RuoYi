package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 反向同步游标 erp_shopify_sync_cursor
 */
@TableName("erp_shopify_sync_cursor")
@Data
@Schema(description = "Shopify 反向同步游标")
public class ShopifySyncCursor implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "游标主键")
    @TableId(value = "cursor_id", type = IdType.ASSIGN_ID)
    private Long cursorId;

    @Schema(description = "店铺ID")
    private Long storeId;

    @Schema(description = "同步模式", allowableValues = {"PRODUCT_IMPORT"})
    private String syncMode;

    @Schema(description = "游标状态", allowableValues = {"RUNNING", "SUCCESS", "FAILED", "PART_SUCCESS"})
    private String status;

    @Schema(description = "上次成功推进到的 Shopify updatedAt", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessUpdatedAt;

    @Schema(description = "上次成功同步完成时间", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessSyncTime;

    @Schema(description = "最近一次 Bulk Operation ID")
    private String lastBulkOperationId;

    @Schema(description = "最近一次任务ID")
    private Long lastTaskId;

    @Schema(description = "最近一次错误摘要")
    private String lastErrorSummary;

    @Schema(description = "创建时间", type = "string", format = "date-time")
    private Date createTime;

    @Schema(description = "更新时间", type = "string", format = "date-time")
    private Date updateTime;
}

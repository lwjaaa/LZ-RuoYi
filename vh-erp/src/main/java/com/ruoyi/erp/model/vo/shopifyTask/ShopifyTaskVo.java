package com.ruoyi.erp.model.vo.shopifyTask;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 同步任务响应对象
 */
@Data
@Schema(description = "Shopify 同步任务响应对象")
public class ShopifyTaskVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务主键")
    private Long taskId;

    @Schema(description = "店铺ID")
    private Long storeId;

    @Schema(description = "Shopify 店铺名称")
    private String shopName;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型", allowableValues = {"PRODUCT_SYNC", "PRODUCT_SYNC_BATCH", "MEDIA_SYNC_BATCH", "MEDIA_SYNC", "SHOPIFY_PRODUCT_IMPORT_FULL", "SHOPIFY_PRODUCT_IMPORT_INCREMENTAL"})
    private String taskType;

    @Schema(description = "任务状态", allowableValues = {"PENDING", "RUNNING", "SUCCESS", "FAILED", "PART_SUCCESS", "CANCELLED"})
    private String taskStatus;

    @Schema(description = "执行进度，范围 0-100")
    private Integer progress;

    @Schema(description = "总数，批量任务总数")
    private Integer totalCount;

    @Schema(description = "成功数")
    private Integer successCount;

    @Schema(description = "部分成功数")
    private Integer partialCount;

    @Schema(description = "失败数")
    private Integer failedCount;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "执行耗时，单位毫秒")
    private Long executionTime;

    @Schema(description = "开始执行时间", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "结束时间", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @Schema(description = "创建者")
    private String createBy;

    @Schema(description = "创建时间", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新者")
    private String updateBy;

    @Schema(description = "更新时间", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public static ShopifyTaskVo objToVo(ShopifyTask task) {
        if (task == null) {
            return null;
        }
        ShopifyTaskVo vo = new ShopifyTaskVo();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}

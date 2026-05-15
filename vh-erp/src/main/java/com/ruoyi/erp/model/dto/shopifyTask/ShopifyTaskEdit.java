package com.ruoyi.erp.model.dto.shopifyTask;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 同步任务编辑对象
 */
@Data
@Schema(description = "Shopify 同步任务编辑对象")
public class ShopifyTaskEdit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务主键", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @Schema(description = "任务名称")
    private String taskName;

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

    public static ShopifyTask editToObj(ShopifyTaskEdit edit) {
        if (edit == null) {
            return null;
        }
        ShopifyTask task = new ShopifyTask();
        BeanUtils.copyProperties(edit, task);
        return task;
    }
}

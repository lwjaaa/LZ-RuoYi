package com.ruoyi.erp.model.dto.shopifyTask;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.erp.model.domain.ShopifyTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Shopify 同步任务查询对象
 */
@Data
@Schema(description = "Shopify 同步任务查询对象")
public class ShopifyTaskQuery implements Serializable {
    private static final long serialVersionUID = 1L;

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

    @Schema(description = "开始执行日期", type = "string", format = "date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;

    @Schema(description = "请求参数，可包含 beginStartTime、endStartTime 等范围筛选参数")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    public static ShopifyTask queryToObj(ShopifyTaskQuery query) {
        if (query == null) {
            return null;
        }
        ShopifyTask task = new ShopifyTask();
        BeanUtils.copyProperties(query, task);
        return task;
    }
}

package com.ruoyi.erp.model.dto.shopifyTask;

import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * Shopify 同步任务明细查询对象
 */
@Data
@Schema(description = "Shopify 同步任务明细查询对象")
public class ShopifyTaskDetailQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "明细类型", allowableValues = {"PRODUCT", "VARIANT", "MEDIA"})
    private String itemType;

    @Schema(description = "明细状态", allowableValues = {"SUCCESS", "FAILED", "PART_SUCCESS", "SKIPPED"})
    private String status;

    public static ShopifyTaskDetail queryToObj(ShopifyTaskDetailQuery query) {
        if (query == null) {
            return null;
        }
        ShopifyTaskDetail detail = new ShopifyTaskDetail();
        BeanUtils.copyProperties(query, detail);
        return detail;
    }
}

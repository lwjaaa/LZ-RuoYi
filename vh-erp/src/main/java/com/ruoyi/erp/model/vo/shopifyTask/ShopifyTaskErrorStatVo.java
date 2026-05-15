package com.ruoyi.erp.model.vo.shopifyTask;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 同步任务高频错误统计项。
 */
@Data
@Schema(description = "Shopify 同步任务高频错误统计项")
public class ShopifyTaskErrorStatVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "错误编码")
    private String errorCode;

    @Schema(description = "Shopify userErrors.field")
    private String errorField;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "出现次数")
    private Long total;
}

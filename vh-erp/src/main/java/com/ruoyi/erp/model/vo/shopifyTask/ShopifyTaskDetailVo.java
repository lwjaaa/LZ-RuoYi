package com.ruoyi.erp.model.vo.shopifyTask;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 同步任务明细响应对象
 */
@Data
@Schema(description = "Shopify 同步任务明细响应对象")
public class ShopifyTaskDetailVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "明细主键")
    private Long detailId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "店铺ID")
    private Long storeId;

    @Schema(description = "Shopify 店铺名称")
    private String shopName;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "明细类型", allowableValues = {"PRODUCT", "VARIANT", "MEDIA"})
    private String itemType;

    @Schema(description = "本地商品、变体或媒体ID")
    private Long itemId;

    @Schema(description = "商品标题、SKU 或媒体文件名")
    private String itemName;

    @Schema(description = "Shopify 资源ID")
    private String shopifyId;

    @Schema(description = "同步步骤", allowableValues = {"PRODUCT_SYNC", "PRODUCT_UPSERT", "MEDIA_UPLOAD", "VARIANT_CREATE", "VARIANT_SAVE_ID", "MEDIA_REGISTER", "PRODUCT_IMPORT", "PRODUCT_IMPORT_CREATE", "PRODUCT_IMPORT_UPDATE", "PRODUCT_IMPORT_CONFLICT"})
    private String step;

    @Schema(description = "明细状态", allowableValues = {"SUCCESS", "FAILED", "PART_SUCCESS", "SKIPPED"})
    private String status;

    @Schema(description = "错误编码")
    private String errorCode;

    @Schema(description = "Shopify userErrors.field")
    private String errorField;

    @Schema(description = "批量输入下标")
    private Integer inputIndex;

    @Schema(description = "失败原因")
    private String errorMessage;

    @Schema(description = "创建时间", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public static ShopifyTaskDetailVo objToVo(ShopifyTaskDetail detail) {
        if (detail == null) {
            return null;
        }
        ShopifyTaskDetailVo vo = new ShopifyTaskDetailVo();
        BeanUtils.copyProperties(detail, vo);
        return vo;
    }
}

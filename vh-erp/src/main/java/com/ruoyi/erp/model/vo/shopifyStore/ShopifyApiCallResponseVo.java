package com.ruoyi.erp.model.vo.shopifyStore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Shopify Admin API 调用响应
 */
@Data
@Schema(description = "Shopify Admin API 调用响应")
public class ShopifyApiCallResponseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "HTTP 状态码")
    private Integer statusCode;

    @Schema(description = "响应内容类型")
    private String contentType;

    @Schema(description = "解析后的响应体；结构由 Shopify Admin API 返回内容决定")
    private Object body;

    @Schema(description = "原始响应体")
    private String rawBody;

    @Schema(description = "接口调用耗时，单位毫秒")
    private Long durationMs;
}

package com.ruoyi.erp.model.dto.shopifyStore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Shopify Admin API 调用请求
 */
@Data
@Schema(description = "Shopify Admin API 调用请求")
public class ShopifyApiCallRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "HTTP 请求方法，默认 POST", allowableValues = {"GET", "POST", "PUT", "DELETE"}, defaultValue = "POST")
    private String method;

    @Schema(description = "Shopify Admin API 地址；为空时使用当前店铺 GraphQL 地址")
    private String url;

    @Schema(description = "请求模式，默认 GRAPHQL", allowableValues = {"GRAPHQL", "JSON"}, defaultValue = "GRAPHQL")
    private String mode;

    @Schema(description = "GraphQL 查询语句；mode=GRAPHQL 时必填")
    private String query;

    @Schema(description = "GraphQL variables，必须是 JSON 对象字符串")
    private String variables;

    @Schema(description = "JSON 请求体；mode=JSON 时使用")
    private String body;
}

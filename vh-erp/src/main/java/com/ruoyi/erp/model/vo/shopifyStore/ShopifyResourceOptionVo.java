package com.ruoyi.erp.model.vo.shopifyStore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Shopify 可配置资源选项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Shopify 可配置资源选项")
public class ShopifyResourceOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Shopify 资源ID")
    private String id;

    @Schema(description = "Shopify 资源名称")
    private String name;
}

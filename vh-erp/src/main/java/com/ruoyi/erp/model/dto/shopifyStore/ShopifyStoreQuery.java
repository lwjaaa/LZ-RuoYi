package com.ruoyi.erp.model.dto.shopifyStore;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.erp.model.domain.ShopifyStore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * Shopify 店铺查询对象
 */
@Data
@Schema(description = "Shopify 店铺查询对象")
public class ShopifyStoreQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 店铺名称 */
    @Schema(description = "店铺名称")
    private String storeName;

    /** Shop 名称 */
    @Schema(description = "Shop 名称")
    private String shopName;

    /** 是否启用 */
    @Schema(description = "是否启用", allowableValues = {"0", "1"})
    private String isActive;

    /** 是否默认店铺 */
    @Schema(description = "是否默认店铺", allowableValues = {"0", "1"})
    private String isDefault;

    /** 认证模式 */
    @Schema(description = "认证模式", allowableValues = {"PRIVATE_APP", "OAUTH"})
    private String authMode;

    /** 连接状态 */
    @Schema(description = "连接状态", allowableValues = {"CONNECTED", "DISCONNECTED", "EXPIRED"})
    private String status;

    /** 请求参数 */
    @Schema(description = "请求参数")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    public static ShopifyStore queryToObj(ShopifyStoreQuery query) {
        if (query == null) {
            return null;
        }
        ShopifyStore store = new ShopifyStore();
        BeanUtils.copyProperties(query, store);
        return store;
    }
}

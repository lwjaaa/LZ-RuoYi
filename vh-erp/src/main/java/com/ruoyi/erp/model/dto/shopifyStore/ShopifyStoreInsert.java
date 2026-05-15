package com.ruoyi.erp.model.dto.shopifyStore;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyStore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 店铺新增对象
 */
@Data
@Schema(description = "Shopify 店铺新增对象")
public class ShopifyStoreInsert implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "店铺名称，用于后台展示", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeName;

    @Schema(description = "Shop 名称，myshopify.com 前的名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String shopName;

    @Schema(description = "Shopify Admin API 版本", defaultValue = "2026-04")
    private String apiVersion;

    @Schema(description = "API Key，用于 OAuth 或 Private App")
    private String apiKey;

    @Schema(description = "API Secret，用于 OAuth")
    private String apiSecret;

    @Schema(description = "Access Token，Private App 或 OAuth 存储")
    private String accessToken;

    @Schema(description = "Refresh Token，OAuth 刷新令牌")
    private String refreshToken;

    @Schema(description = "Token 过期时间", type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date tokenExpiresAt;

    @Schema(description = "自定义 API 端点，通常不需要，Shopify 会自动生成")
    private String baseUrl;

    @Schema(description = "Shopify 库存仓库 Location ID")
    private String inventoryLocationId;

    @Schema(description = "Shopify 库存仓库名称")
    private String inventoryLocationName;

    @Schema(description = "是否跟踪库存", allowableValues = {"0", "1"}, defaultValue = "0")
    private String inventoryTracked;

    @Schema(description = "默认库存数量", defaultValue = "100")
    private Integer defaultInventoryQuantity;

    @Schema(description = "缺货销售策略", allowableValues = {"DENY", "CONTINUE"}, defaultValue = "DENY")
    private String inventoryPolicy;

    @Schema(description = "自动发布 Publication ID，英文逗号分隔")
    private String publishPublicationIds;

    @Schema(description = "自动发布渠道名称，英文逗号分隔")
    private String publishPublicationNames;

    @Schema(description = "推送到 Shopify 时的默认商品状态", allowableValues = {"DRAFT", "ACTIVE"}, defaultValue = "DRAFT")
    private String defaultProductStatus;

    @Schema(description = "商品资料必填字段编码，英文逗号分隔")
    private String requiredProductFields;

    @Schema(description = "本次从 Shopify 拉取到的 Publication ID，英文逗号分隔，不落库")
    private String availablePublicationIds;

    @Schema(description = "是否启用", allowableValues = {"0", "1"}, defaultValue = "1")
    private String isActive;

    @Schema(description = "是否默认店铺", allowableValues = {"0", "1"}, defaultValue = "0")
    private String isDefault;

    @Schema(description = "认证模式", allowableValues = {"PRIVATE_APP", "OAUTH"}, defaultValue = "PRIVATE_APP")
    private String authMode;

    @Schema(description = "连接状态", allowableValues = {"CONNECTED", "DISCONNECTED", "EXPIRED"})
    private String status;

    @Schema(description = "备注")
    private String remark;

    public static ShopifyStore insertToObj(ShopifyStoreInsert insert) {
        if (insert == null) {
            return null;
        }
        ShopifyStore store = new ShopifyStore();
        BeanUtils.copyProperties(insert, store);
        return store;
    }
}

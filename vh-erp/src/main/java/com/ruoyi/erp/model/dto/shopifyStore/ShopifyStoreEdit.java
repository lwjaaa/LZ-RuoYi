package com.ruoyi.erp.model.dto.shopifyStore;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyStore;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 店铺编辑对象
 */
@Data
public class ShopifyStoreEdit implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long storeId;
    private String storeName;
    private String shopName;
    private String apiVersion;
    private String apiKey;
    private String apiSecret;
    private String accessToken;
    private String refreshToken;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date tokenExpiresAt;

    private String baseUrl;
    private String inventoryLocationId;
    private String inventoryLocationName;
    private String inventoryTracked;
    private Integer defaultInventoryQuantity;
    private String inventoryPolicy;
    private String publishPublicationIds;
    private String publishPublicationNames;
    private String isActive;
    private String isDefault;
    private String authMode;
    private String status;
    private String remark;

    public static ShopifyStore editToObj(ShopifyStoreEdit edit) {
        if (edit == null) {
            return null;
        }
        ShopifyStore store = new ShopifyStore();
        BeanUtils.copyProperties(edit, store);
        return store;
    }
}

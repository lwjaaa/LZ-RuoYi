package com.ruoyi.erp.model.dto.shopifyStore;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyStore;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 店铺新增对象
 */
@Data
public class ShopifyStoreInsert implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public static ShopifyStore insertToObj(ShopifyStoreInsert insert) {
        if (insert == null) {
            return null;
        }
        ShopifyStore store = new ShopifyStore();
        BeanUtils.copyProperties(insert, store);
        return store;
    }
}

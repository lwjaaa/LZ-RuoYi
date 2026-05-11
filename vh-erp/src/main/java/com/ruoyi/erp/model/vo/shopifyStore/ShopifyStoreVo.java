package com.ruoyi.erp.model.vo.shopifyStore;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyStore;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 店铺响应对象
 */
@Data
public class ShopifyStoreVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long storeId;
    private String storeName;
    private String shopName;
    private String apiVersion;
    private String apiKey;
    private String apiSecret;
    private String accessToken;
    private Boolean hasApiSecret;
    private Boolean hasAccessToken;
    private Boolean hasRefreshToken;

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
    private String defaultProductStatus;
    private String isActive;
    private String isDefault;
    private String authMode;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncTime;

    private Integer syncCount;
    private String remark;
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public static ShopifyStoreVo objToVo(ShopifyStore store) {
        if (store == null) {
            return null;
        }
        ShopifyStoreVo vo = new ShopifyStoreVo();
        BeanUtils.copyProperties(store, vo);
        vo.setHasApiSecret(store.getApiSecret() != null && !store.getApiSecret().isEmpty());
        vo.setHasAccessToken(store.getAccessToken() != null && !store.getAccessToken().isEmpty());
        vo.setHasRefreshToken(store.getRefreshToken() != null && !store.getRefreshToken().isEmpty());
        return vo;
    }
}

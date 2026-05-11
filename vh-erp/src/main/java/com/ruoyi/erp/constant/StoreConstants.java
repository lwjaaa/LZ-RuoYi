package com.ruoyi.erp.constant;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/5/2 22:44
 **/
public interface StoreConstants {
    String STATUS_DISCONNECTED = "DISCONNECTED";
    String STATUS_CONNECTED = "CONNECTED";
    String STATUS_EXPIRED = "EXPIRED";

    String YES = "1";
    String NO = "0";
    String DEL_FLAG_NORMAL = "0";
    String DEL_FLAG_DELETED = "2";

    String INVENTORY_POLICY_DENY = "DENY";
    String INVENTORY_POLICY_CONTINUE = "CONTINUE";

    String PRODUCT_STATUS_DRAFT = "DRAFT";
    String PRODUCT_STATUS_ACTIVE = "ACTIVE";

    /**
     * OAuth 模式
     */
    String AUTH_MODE_OAUTH = "OAUTH";
    /**
     * 私有应用模式
     */
    String AUTH_MODE_PRIVATE = "PRIVATE_APP";
}

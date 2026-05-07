package com.ruoyi.erp.model.dto.shopifyStore;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.erp.model.domain.ShopifyStore;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * Shopify 店铺查询对象
 */
@Data
public class ShopifyStoreQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 店铺名称 */
    private String storeName;

    /** Shop 名称 */
    private String shopName;

    /** 是否启用 */
    private String isActive;

    /** 是否默认店铺 */
    private String isDefault;

    /** 认证模式 */
    private String authMode;

    /** 连接状态 */
    private String status;

    /** 请求参数 */
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

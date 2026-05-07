package com.ruoyi.erp.model.vo.shopifyStore;

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
public class ShopifyResourceOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
}

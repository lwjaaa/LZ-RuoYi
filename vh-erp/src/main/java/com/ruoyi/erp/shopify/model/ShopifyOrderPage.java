package com.ruoyi.erp.shopify.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Shopify 订单分页同步结果
 */
@Data
public class ShopifyOrderPage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 订单列表 */
    private List<ShopifyImportedOrder> orders;

    /** 是否还有下一页 */
    private boolean hasNextPage;

    /** 下一页游标 */
    private String endCursor;
}

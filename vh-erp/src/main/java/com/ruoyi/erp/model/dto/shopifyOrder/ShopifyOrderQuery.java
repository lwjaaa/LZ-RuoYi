package com.ruoyi.erp.model.dto.shopifyOrder;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Shopify 订单查询对象
 */
@Data
public class ShopifyOrderQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 店铺ID */
    private Long storeId;

    /** 订单号或 SKU 关键字 */
    private String searchKeyword;

    /** 订单名称 */
    private String orderName;

    /** 付款状态 */
    private String financialStatus;

    /** 履约状态 */
    private String fulfillmentStatus;

    /** 采购状态 */
    private String purchaseStatus;

    /** 发货回传状态 */
    private String fulfillmentSyncStatus;

    /** 下单日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date placedAt;

    /** 下单开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date beginDate;

    /** 下单结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

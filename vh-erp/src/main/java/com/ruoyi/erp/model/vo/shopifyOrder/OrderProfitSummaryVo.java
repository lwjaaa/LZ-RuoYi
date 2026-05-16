package com.ruoyi.erp.model.vo.shopifyOrder;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单利润汇总响应对象
 */
@Data
public class OrderProfitSummaryVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 订单数 */
    private Long orderCount;

    /** 销售额，单位为分 */
    private Integer salesAmount;

    /** 退款额，单位为分 */
    private Integer refundAmount;

    /** 采购成本，单位为分 */
    private Integer purchaseCost;

    /** 发货成本，单位为分 */
    private Integer shippingCost;

    /** 毛利，单位为分 */
    private Integer grossProfit;

    /** 毛利率 */
    private BigDecimal grossProfitRate;
}

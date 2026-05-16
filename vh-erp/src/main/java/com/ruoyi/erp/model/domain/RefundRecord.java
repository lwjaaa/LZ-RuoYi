package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 售后退款记录对象 erp_refund_record
 */
@TableName("erp_refund_record")
@Data
public class RefundRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 退款记录主键 */
    @TableId(value = "refund_id", type = IdType.ASSIGN_ID)
    private Long refundId;

    /** 本地订单ID */
    private Long orderId;

    /** 店铺ID */
    private Long storeId;

    /** Shopify 退款ID */
    private String shopifyRefundId;

    /** 退款金额，单位为分 */
    private Integer refundAmount;

    /** 币种 */
    private String currencyCode;

    /** 退款原因 */
    private String reason;

    /** Shopify 备注 */
    private String note;

    /** 责任归类 */
    private String responsibility;

    /** 退款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date refundTime;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}

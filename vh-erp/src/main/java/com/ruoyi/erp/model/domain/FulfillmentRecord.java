package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 发货回传记录对象 erp_fulfillment_record
 */
@TableName("erp_fulfillment_record")
@Data
public class FulfillmentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 发货记录主键 */
    @TableId(value = "fulfillment_id", type = IdType.ASSIGN_ID)
    private Long fulfillmentId;

    /** 本地订单ID */
    private Long orderId;

    /** 店铺ID */
    private Long storeId;

    /** Shopify 订单ID */
    private String shopifyOrderId;

    /** Shopify FulfillmentOrder ID */
    private String shopifyFulfillmentOrderId;

    /** Shopify Fulfillment ID */
    private String shopifyFulfillmentId;

    /** 物流公司 */
    @Excel(name = "物流公司")
    private String trackingCompany;

    /** 运单号 */
    @Excel(name = "运单号")
    private String trackingNumber;

    /** 物流查询链接 */
    private String trackingUrl;

    /** 物流费用，单位为分 */
    private Integer shippingFee;

    /** 回传状态 */
    @Excel(name = "回传状态")
    private String syncStatus;

    /** 错误信息 */
    private String errorMessage;

    /** 发货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date fulfilledAt;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}

package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Shopify 订单对象 erp_shopify_order
 */
@TableName("erp_shopify_order")
@Data
public class ShopifyOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本地订单主键 */
    @TableId(value = "order_id", type = IdType.ASSIGN_ID)
    private Long orderId;

    /** 店铺ID */
    @Excel(name = "店铺ID")
    private Long storeId;

    /** Shopify 店铺名称 */
    @Excel(name = "Shopify 店铺")
    private String shopName;

    /** Shopify 订单ID */
    @Excel(name = "Shopify 订单ID")
    private String shopifyOrderId;

    /** 订单名称，如 #1001 */
    @Excel(name = "订单号")
    private String orderName;

    /** 数字订单号 */
    private Long orderNumber;

    /** 客户本地主键 */
    private Long customerId;

    /** 客户展示名 */
    @Excel(name = "客户")
    private String customerName;

    /** 客户邮箱 */
    private String email;

    /** 客户电话 */
    private String phone;

    /** 收货人 */
    private String shippingName;

    /** 收货电话 */
    private String shippingPhone;

    /** 收货国家 */
    private String shippingCountry;

    /** 收货省份 */
    private String shippingProvince;

    /** 收货城市 */
    private String shippingCity;

    /** 收货邮编 */
    private String shippingZip;

    /** 收货地址1 */
    private String shippingAddress1;

    /** 收货地址2 */
    private String shippingAddress2;

    /** 付款状态 */
    @Excel(name = "付款状态")
    private String financialStatus;

    /** 履约状态 */
    @Excel(name = "履约状态")
    private String fulfillmentStatus;

    /** 采购状态 */
    @Excel(name = "采购状态")
    private String purchaseStatus;

    /** 发货回传状态 */
    @Excel(name = "发货回传状态")
    private String fulfillmentSyncStatus;

    /** 币种 */
    private String currencyCode;

    /** 订单总额，单位为分 */
    @Excel(name = "订单总额")
    private Integer totalPrice;

    /** 商品小计，单位为分 */
    private Integer subtotalPrice;

    /** 税费，单位为分 */
    private Integer totalTax;

    /** Shopify 收取的运费，单位为分 */
    private Integer totalShippingPrice;

    /** 退款金额，单位为分 */
    private Integer totalRefund;

    /** 采购成本，单位为分 */
    private Integer purchaseCost;

    /** 实际发货成本，单位为分 */
    private Integer shippingCost;

    /** 毛利，单位为分 */
    private Integer grossProfit;

    /** 毛利率 */
    private BigDecimal grossProfitRate;

    /** Shopify 下单时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date placedAt;

    /** Shopify 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shopifyUpdatedAt;

    /** Shopify 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelledAt;

    /** Shopify 关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closedAt;

    /** 最近一次订单轮询导入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastShopifyImportTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 订单行列表 */
    @TableField(exist = false)
    private List<ShopifyOrderLineItem> lineItems;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

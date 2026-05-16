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
 * 人工采购任务对象 erp_purchase_task
 */
@TableName("erp_purchase_task")
@Data
public class PurchaseTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 采购任务主键 */
    @TableId(value = "purchase_task_id", type = IdType.ASSIGN_ID)
    private Long purchaseTaskId;

    /** 本地订单ID */
    private Long orderId;

    /** 本地订单行ID */
    private Long orderLineItemId;

    /** 店铺ID */
    private Long storeId;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderName;

    /** SKU */
    @Excel(name = "SKU")
    private String sku;

    /** 商品标题 */
    private String itemTitle;

    /** 采购数量 */
    private Integer quantity;

    /** 采购链接 */
    private String purchaseUrl;

    /** 预计采购金额，单位为分 */
    private Integer expectedPurchaseAmount;

    /** 实际采购金额，单位为分 */
    private Integer actualPurchaseAmount;

    /** 采购状态 */
    @Excel(name = "采购状态")
    private String purchaseStatus;

    /** 采购异常原因 */
    private String exceptionReason;

    /** 采购备注 */
    private String remark;

    /** 采购完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date purchasedAt;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}

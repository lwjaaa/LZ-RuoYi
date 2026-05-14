package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 同步任务明细对象 erp_shopify_task_detail
 */
@TableName("erp_shopify_task_detail")
@Data
public class ShopifyTaskDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 明细主键 */
    @TableId(value = "detail_id", type = IdType.ASSIGN_ID)
    private Long detailId;

    /** 任务主键 */
    private Long taskId;

    /** 店铺ID */
    private Long storeId;

    /** Shopify 店铺名称 */
    private String shopName;

    /** 商品ID */
    private Long productId;

    /** 明细类型：PRODUCT、VARIANT、MEDIA */
    private String itemType;

    /** 明细对应的本地ID */
    private Long itemId;

    /** 明细名称：商品标题、SKU 或文件名 */
    private String itemName;

    /** Shopify 返回的资源ID */
    private String shopifyId;

    /** 执行步骤 */
    private String step;

    /** 明细状态 */
    private String status;

    /** Shopify userErrors.code 或本地错误编码 */
    private String errorCode;

    /** Shopify userErrors.field */
    private String errorField;

    /** 批量输入下标 */
    private Integer inputIndex;

    /** 错误原因 */
    private String errorMessage;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}

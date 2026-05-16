package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 客户快照对象 erp_shopify_customer
 */
@TableName("erp_shopify_customer")
@Data
public class ShopifyCustomer implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本地客户主键 */
    @TableId(value = "customer_id", type = IdType.ASSIGN_ID)
    private Long customerId;

    /** 店铺ID */
    private Long storeId;

    /** Shopify 客户ID */
    private String shopifyCustomerId;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 名 */
    private String firstName;

    /** 姓 */
    private String lastName;

    /** Shopify 展示名 */
    private String displayName;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}

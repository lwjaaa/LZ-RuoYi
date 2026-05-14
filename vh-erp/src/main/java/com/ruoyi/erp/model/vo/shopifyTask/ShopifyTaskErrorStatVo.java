package com.ruoyi.erp.model.vo.shopifyTask;

import lombok.Data;

import java.io.Serializable;

/**
 * Shopify 同步任务高频错误统计项。
 */
@Data
public class ShopifyTaskErrorStatVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String errorCode;

    private String errorField;

    private String errorMessage;

    private Long total;
}

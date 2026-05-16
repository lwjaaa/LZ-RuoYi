package com.ruoyi.erp.model.dto.shopifyOrder;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 人工采购任务查询对象
 */
@Data
public class PurchaseTaskQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 店铺ID */
    private Long storeId;

    /** 采购状态 */
    private String purchaseStatus;

    /** 订单号或 SKU 关键字 */
    private String searchKeyword;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

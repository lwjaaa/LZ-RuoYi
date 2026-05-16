package com.ruoyi.erp.model.dto.shopifyOrder;

import lombok.Data;

import java.io.Serializable;

/**
 * 人工采购任务编辑对象
 */
@Data
public class PurchaseTaskEdit implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 采购任务ID */
    private Long purchaseTaskId;

    /** 采购状态 */
    private String purchaseStatus;

    /** 实际采购金额，单位为分 */
    private Integer actualPurchaseAmount;

    /** 采购异常原因 */
    private String exceptionReason;

    /** 采购备注 */
    private String remark;
}

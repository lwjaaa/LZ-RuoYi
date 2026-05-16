package com.ruoyi.erp.model.dto.shopifyOrder;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 退款记录查询对象
 */
@Data
public class RefundRecordQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 店铺ID */
    private Long storeId;

    /** 本地订单ID */
    private Long orderId;

    /** 订单号或责任归类关键字 */
    private String searchKeyword;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

package com.ruoyi.erp.model.dto.shopifyOrder;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 订单利润报表查询对象
 */
@Data
public class ProfitReportQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 店铺ID */
    private Long storeId;

    /** 统计开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date beginDate;

    /** 统计结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

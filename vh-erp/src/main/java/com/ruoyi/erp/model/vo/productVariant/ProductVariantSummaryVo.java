package com.ruoyi.erp.model.vo.productVariant;

import lombok.Data;

import java.io.Serializable;

/**
 * SKU 运营台汇总指标。
 */
@Data
public class ProductVariantSummaryVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** SKU 总数 */
    private Long totalCount;

    /** 待同步 SKU 数 */
    private Long needSyncCount;

    /** 低毛利 SKU 数 */
    private Long lowProfitCount;

    /** 缺采购链接 SKU 数 */
    private Long missingPurchaseUrlCount;

    /** 近 30 天有订单的 SKU 数 */
    private Long orderedSkuCount30d;
}

package com.ruoyi.erp.model.dto.productVariant;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * SKU 运营台批量编辑请求。
 */
@Data
public class ProductVariantBatchEdit implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 需要批量更新的 SKU 主键 */
    private List<Long> variantIds;

    /** 销售价格，单位为美分 */
    private Integer price;

    /** 原价/对比价，单位为美分 */
    private Integer compareAtPrice;

    /** 采购价，单位为分 */
    private Integer purchasePrice;

    /** 采购链接 */
    private String purchaseUrl;

    /** 预估运费，单位为分 */
    private Integer freight;

    /** 包装宽度 */
    private Integer pkWidth;

    /** 包装高度 */
    private Integer pkHeight;

    /** 包装长度 */
    private Integer pkLength;

    /** 常规包装重量 */
    private Integer pkWeight;

    /** 是否可售 */
    private String isActiveAvailable;

    /** 备注 */
    private String remark;
}

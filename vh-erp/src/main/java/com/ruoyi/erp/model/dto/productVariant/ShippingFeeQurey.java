package com.ruoyi.erp.model.dto.productVariant;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lwj
 * @version 1.0.0
 * @description 运费查询传输对象
 * @date 2026/4/12 20:58
 **/
@Data
public class ShippingFeeQurey implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 包装宽度 */
    private Integer pkWidth;

    /** 包装高度 */
    private Integer pkHeight;

    /** 包装长度 */
    private Integer pkLength;

    /** 材积重 */
    private Integer materialWeight;

    /** 常规包装重量 */
    private Integer pkWeight;
}

package com.ruoyi.erp.model.dto.shipping;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author lwj
 * @version 1.0.0
 * @description 运费查询传输对象
 * @date 2026/4/12 20:58
 **/
@Data
public class ShippingFeeQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 包装宽度(cm) */
    private Integer pkWidth;

    /** 包装高度(cm) */
    private Integer pkHeight;

    /** 包装长度(cm) */
    private Integer pkLength;

    /** 材积重(g) */
    private Integer materialWeight;

    /** 常规包装重量(g) */
    private Integer pkWeight;
    
    /** 国家代码（如US、CN等） */
    private String countryCode;
    
    /** 邮政编码 */
    private String postalCode;
    
    /** 物流服务代码（可选） */
    private List<String> logisticsCode;
}

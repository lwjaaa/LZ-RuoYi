package com.ruoyi.erp.model.vo.product;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author lwj
 * @version 1.0.0
 * @description 商品选项对象
 * @date 2026/3/26 10:28
 **/
@Data
public class ProductOptionVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 商品选项名称
     */
    private String name;
    /**
     * 商品选项值列表
     */
    private List<ProductOptionValueVo> values;
    // private String optionId;
}

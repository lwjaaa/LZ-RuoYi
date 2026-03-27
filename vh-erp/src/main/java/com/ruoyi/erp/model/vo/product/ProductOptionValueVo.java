package com.ruoyi.erp.model.vo.product;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author lwj
 * @version 1.0.0
 * @description 商品选项值
 * @date 2026/3/26 10:29
 **/
@Data
public class ProductOptionValueVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 选项值
     */
    private String value;
}

package com.ruoyi.erp.model.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author lwj
 * @version 1.0.0
 * @description 变体对应的商品选项对象
 * @date 2026/3/26 10:39
 **/
@Data
public class ProductVariantOption implements Serializable{
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 商品选项ID
     */
    private String optionId;
    /**
     * 商品选项名称
     */
    private String name;
    /**
     * 商品选项值
     */
    private String value;
}

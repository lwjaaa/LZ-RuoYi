package com.ruoyi.erp.model.dto.product;

import com.ruoyi.erp.model.domain.Product;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品批量推送请求对象
 */
@Data
public class ProductPushRequest implements Serializable {

    /** 自定义查询条件 */
    private ProductQuery productQuery;

    /** 要推送的商品ID列表 */
    private Long[] productIds;
}

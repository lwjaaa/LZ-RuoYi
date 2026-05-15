package com.ruoyi.erp.model.dto.product;

import com.ruoyi.erp.model.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品批量推送请求对象
 */
@Data
@Schema(description = "商品批量推送请求对象")
public class ProductPushRequest implements Serializable {

    /** 自定义查询条件 */
    @Schema(description = "自定义查询条件；未传 productIds 时按该条件筛选商品")
    private ProductQuery productQuery;

    /** 要推送的商品ID列表 */
    @Schema(description = "要推送的商品ID列表")
    private Long[] productIds;

    /** 店铺ID */
    @Schema(description = "店铺ID；为空时按现有业务规则解析发布店铺")
    private Long storeId;
}

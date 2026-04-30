package com.ruoyi.erp.model.dto.product;

import com.ruoyi.erp.model.domain.Product;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品批量推送请求对象
 */
@Data
public class ProductPushRequest implements Serializable {

    /** 分类 */
    private String category;

    /** 标签ID列表，逗号分隔 */
    private String tagIds;

    /** 同步状态 */
    private String syncStatus;

    /** 是否全量（忽略其他条件） */
    private Boolean selectAll;

    /** 自定义查询条件 */
    private ProductQuery productQuery;
}

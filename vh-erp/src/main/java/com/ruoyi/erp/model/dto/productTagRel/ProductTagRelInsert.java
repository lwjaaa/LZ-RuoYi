package com.ruoyi.erp.model.dto.productTagRel;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import com.ruoyi.erp.model.domain.ProductTagRel;
/**
 * erp商品与标签关联Vo对象 erp_product_tag_rel
 *
 * @author lwj
 * @date 2026-03-24
 */
@Data
public class ProductTagRelInsert implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 关联主键 */
    private Long relId;

    /** 商品主表ID */
    private Long productId;

    /** 标签字典表ID */
    private Long tagId;

    /** 创建时间 */
    private Date createTime;

    /** 创建者 */
    private String createBy;

    /**
     * 对象转封装类
     *
     * @param productTagRelInsert 插入对象
     * @return ProductTagRelInsert
     */
    public static ProductTagRel insertToObj(ProductTagRelInsert productTagRelInsert) {
        if (productTagRelInsert == null) {
            return null;
        }
        ProductTagRel productTagRel = new ProductTagRel();
        BeanUtils.copyProperties(productTagRelInsert, productTagRel);
        return productTagRel;
    }
}

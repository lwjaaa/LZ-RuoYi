package com.ruoyi.erp.model.dto.productTagRel;

import com.ruoyi.erp.model.domain.ProductTagRel;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
/**
 * erp商品与标签关联Vo对象 erp_product_tag_rel
 *
 * @author lwj
 * @date 2026-03-24
 */
@Data
public class ProductTagRelEdit implements Serializable
{
    private static final long serialVersionUID = 1L;


    /** 关联主键 */
    private Long relId;

    /** 商品主表ID */
    private Long productId;

    /** 标签字典表ID */
    private Long tagId;

    /**
     * 对象转封装类
     *
     * @param productTagRelEdit 编辑对象
     * @return ProductTagRel
     */
    public static ProductTagRel editToObj(ProductTagRelEdit productTagRelEdit) {
        if (productTagRelEdit == null) {
            return null;
        }
        ProductTagRel productTagRel = new ProductTagRel();
        BeanUtils.copyProperties(productTagRelEdit, productTagRel);
        return productTagRel;
    }
}

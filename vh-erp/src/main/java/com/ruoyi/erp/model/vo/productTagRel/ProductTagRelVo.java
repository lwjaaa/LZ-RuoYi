package com.ruoyi.erp.model.vo.productTagRel;

import java.io.Serializable;
import java.util.Date;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.ruoyi.common.annotation.Excel;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ProductTagRel;
/**
 * erp商品与标签关联Vo对象 erp_product_tag_rel
 *
 * @author lwj
 * @date 2026-03-24
 */
@Data
public class ProductTagRelVo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 商品主表ID */
    private Long productId;

    /** 标签字典表ID */
    private Long tagId;


     /**
     * 对象转封装类
     *
     * @param productTagRel ProductTagRel实体对象
     * @return ProductTagRelVo
     */
    public static ProductTagRelVo objToVo(ProductTagRel productTagRel) {
        if (productTagRel == null) {
            return null;
        }
        ProductTagRelVo productTagRelVo = new ProductTagRelVo();
        BeanUtils.copyProperties(productTagRel, productTagRelVo);
        return productTagRelVo;
    }
}

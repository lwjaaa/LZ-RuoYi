package com.ruoyi.erp.model.dto.productTagRel;

import java.util.Map;
import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.BeanUtils;
import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.erp.model.domain.ProductTagRel;
/**
 * erp商品与标签关联Query对象 erp_product_tag_rel
 *
 * @author lwj
 * @date 2026-03-24
 */
@Data
public class ProductTagRelQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 商品主表ID */
    private Long productId;

    /** 标签字典表ID */
    private Long tagId;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    /**
     * 对象转封装类
     *
     * @param productTagRelQuery 查询对象
     * @return ProductTagRel
     */
    public static ProductTagRel queryToObj(ProductTagRelQuery productTagRelQuery) {
        if (productTagRelQuery == null) {
            return null;
        }
        ProductTagRel productTagRel = new ProductTagRel();
        BeanUtils.copyProperties(productTagRelQuery, productTagRel);
        return productTagRel;
    }
}

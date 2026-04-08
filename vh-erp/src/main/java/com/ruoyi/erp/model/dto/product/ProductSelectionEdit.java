package com.ruoyi.erp.model.dto.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductOption;
import com.ruoyi.erp.model.domain.ProductVariant;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * erp商品Vo对象 erp_product
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class ProductSelectionEdit implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 本地主键 */
    private Long productId;

    /** Shopify平台商品ID (唯一映射) */
    private String shopifyProductId;

    /** SPU */
    private String spu;

    /** 来源URL */
    private String sourceUrl;

    /** 采购链接 */
    private String purchaseUrl;

    /** 商品选项 */
    private String optionJson;

    /** 主图ID，仅用户erp后台展示 */
    private Long mainMediaId;


    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** erp商品变体信息 */
    private List<ProductVariant> productVariantList;

    /** 商品标签ID列表 */
    private List<Long> tagIds;

    /**
     * 对象转封装类
     *
     * @param productEdit 编辑对象
     * @return Product
     */
    public static Product editToObj(ProductSelectionEdit productEdit) {
        if (productEdit == null) {
            return null;
        }
        Product product = new Product();
        BeanUtils.copyProperties(productEdit, product);
        return product;
    }
}

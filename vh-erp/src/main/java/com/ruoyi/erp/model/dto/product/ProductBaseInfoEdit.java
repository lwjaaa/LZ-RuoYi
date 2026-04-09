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
public class ProductBaseInfoEdit implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 本地主键 */
    private Long productId;

    /** Shopify平台商品ID (唯一映射) */
    private String shopifyProductId;

    /** 商品标题 */
    private String productTitle;

    /** 商品类别ID (Category) */
    private String category;

    /** 商品类型 */
    private String productType;

    /** 商品选项 */
    private String optionJson;
    private List<ProductOption> optionList;

    /** 商品详情描述 */
    private String bodyHtml;

    /** 主图ID，仅用户erp后台展示 */
    private Long mainMediaId;

    /** 描述 */
    private String description;

    /** 大小 */
    private String size;

    /** 材质 */
    private String material;

    /** 备注 */
    private String note;

    /** 包含的包材 */
    private String packageInclude;

    /** 图片搜索关键字 */
    private String imageSearchKeyword;

    /** 备注 */
    private String remark;

    /** erp商品变体信息 */
    private List<ProductVariant> productVariantList;
    /** erp商品媒体信息 */
    private List<Long> mediaIdList;

    /**
     * 对象转封装类
     *
     * @param productEdit 编辑对象
     * @return Product
     */
    public static Product editToObj(ProductBaseInfoEdit productEdit) {
        if (productEdit == null) {
            return null;
        }
        Product product = new Product();
        BeanUtils.copyProperties(productEdit, product);
        return product;
    }
}

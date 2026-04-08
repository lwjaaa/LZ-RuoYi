package com.ruoyi.erp.model.vo.productVariant;

import com.ruoyi.erp.model.domain.ProductVariant;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
/**
 * erp商品变体Vo对象 erp_product_variant
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class ProductVariantVo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 关联商品主表ID */
    private Long productId;

    /** Shopify平台变体ID */
    private String shopifyVariantId;

    /** SKU */
    private String sku;

    /** 销售价格(美元*100) */
    private Long price;

    /** 原价/对比价(美元*100) */
    private Long compareAtPrice;

    /** 采购价（分） */
    private Long purchasePrice;

    /** 采购链接 */
    private String purchaseUrl;

    /** 变体对应的选项 */
    private String optionValues;

    /** 关联的图片ID (若有) */
    private Long mediaId;

    /** 排序位置 列表中的第一个位置是 1 */
    private Long position;

    /** 包装宽度 */
    private Long pkWidth;

    /** 包装高度 */
    private Long pkHeight;

    /** 包装长度 */
    private Long pkLength;

    /** 材积重 */
    private Long materialWeight;

    /** 常规包装重量 */
    private Long pkWeight;

    /** 运费 */
    private Long freight;

    /** 运费是否来自实际发货数据(0:否, 1:是) */
    private String isActualShipment;

    /** 商品成本价（分） */
    private Long unitCostPrice;

    /** 备注 */
    private String remark;


     /**
     * 对象转封装类
     *
     * @param productVariant ProductVariant实体对象
     * @return ProductVariantVo
     */
    public static ProductVariantVo objToVo(ProductVariant productVariant) {
        if (productVariant == null) {
            return null;
        }
        ProductVariantVo productVariantVo = new ProductVariantVo();
        BeanUtils.copyProperties(productVariant, productVariantVo);
        return productVariantVo;
    }
}

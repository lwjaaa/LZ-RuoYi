package com.ruoyi.erp.model.dto.productVariant;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import com.ruoyi.erp.model.domain.ProductVariant;
/**
 * erp商品变体Vo对象 erp_product_variant
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class ProductVariantEdit implements Serializable
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

    /** 采购产品名称 */
    private String purchaseProductName;

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

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;

    /**
     * 对象转封装类
     *
     * @param productVariantEdit 编辑对象
     * @return ProductVariant
     */
    public static ProductVariant editToObj(ProductVariantEdit productVariantEdit) {
        if (productVariantEdit == null) {
            return null;
        }
        ProductVariant productVariant = new ProductVariant();
        BeanUtils.copyProperties(productVariantEdit, productVariant);
        return productVariant;
    }
}

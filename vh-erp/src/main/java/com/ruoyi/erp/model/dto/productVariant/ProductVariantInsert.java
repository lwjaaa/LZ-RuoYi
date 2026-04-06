package com.ruoyi.erp.model.dto.productVariant;

import com.ruoyi.erp.model.domain.ProductVariant;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
/**
 * erp商品变体Vo对象 erp_product_variant
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class ProductVariantInsert implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 本地主键 */
    private Long variantId;

    /** 关联商品主表ID */
    private Long productId;

    /** Shopify平台变体ID */
    private String shopifyVariantId;

    /** SKU */
    private String sku;

    /** 销售价格(美元*100) */
    private Integer price;

    /** 原价/对比价(美元*100) */
    private Integer compareAtPrice;

    /** 采购价（分） */
    private Integer purchasePrice;

    /** 采购链接 */
    private String purchaseUrl;

    /** 采购产品名称 */
    private String purchaseProductName;

    /** 变体对应的选项 */
    private String optionValues;

    /** 关联的图片ID (若有) */
    private Long mediaId;

    /** 排序位置 列表中的第一个位置是 1 */
    private Integer position;

    /** 包装宽度 */
    private Integer pkWidth;

    /** 包装高度 */
    private Integer pkHeight;

    /** 包装长度 */
    private Integer pkLength;

    /** 材积重 */
    private Integer materialWeight;

    /** 常规包装重量 */
    private Integer pkWeight;

    /** 运费 */
    private Integer freight;

    /** 运费是否来自实际发货数据(0:否, 1:是) */
    private String isActualShipment;

    /** 商品成本价（分） */
    private Integer unitCostPrice;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** 删除标志 (0代表存在 2代表删除) */
    private String delFlag;

    /**
     * 对象转封装类
     *
     * @param productVariantInsert 插入对象
     * @return ProductVariantInsert
     */
    public static ProductVariant insertToObj(ProductVariantInsert productVariantInsert) {
        if (productVariantInsert == null) {
            return null;
        }
        ProductVariant productVariant = new ProductVariant();
        BeanUtils.copyProperties(productVariantInsert, productVariant);
        return productVariant;
    }
}

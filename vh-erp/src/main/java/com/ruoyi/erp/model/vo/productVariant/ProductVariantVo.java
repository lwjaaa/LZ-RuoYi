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

    /** SKU 主键 */
    private Long variantId;

    /** 关联商品主表ID */
    private Long productId;

    /** 商品标题 */
    private String productTitle;

    /** SPU */
    private String spu;

    /** Shopify 店铺名称 */
    private String shopName;

    /** 商品主图 */
    private String mainMediaUrl;

    /** 父商品同步状态 */
    private String productSyncStatus;

    /** 所属 Shopify 店铺 ID */
    private Long storeId;

    /** Shopify平台变体ID */
    private String shopifyVariantId;

    /** Shopify InventoryItem ID */
    private String shopifyInventoryItemId;

    /** 最近一次 Shopify 反向导入成功时间 */
    private java.util.Date lastShopifyImportTime;

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

    /** 变体对应的选项 */
    private String optionValues;

    /** 关联的图片ID (若有) */
    private Long mediaId;

    /** SKU 图片 */
    private String mediaUrl;

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

    /** 建议售价(美元*100) */
    private Integer suggestedPrice;

    /** 实际利润率 */
    private java.math.BigDecimal profitRate;

    /** 利润 */
    private java.math.BigDecimal profit;

    /** 是否可用 */
    private String isActiveAvailable;

    /** 资料缺失标记，英文逗号分隔 */
    private String missingFlags;

    /** 是否需要同步到 Shopify */
    private Boolean needSync;

    /** 近 30 天订单数 */
    private Long orderCount30d;

    /** 近 30 天销售额 */
    private Integer salesAmount30d;

    /** 近 30 天退款额 */
    private Integer refundAmount30d;

    /** 待采购数量 */
    private Long pendingPurchaseCount;

    /** 履约异常数量 */
    private Long fulfillmentExceptionCount;

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

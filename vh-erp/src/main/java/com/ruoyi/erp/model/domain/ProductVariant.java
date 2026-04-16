package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.erp.model.vo.media.MediaVo;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * erp商品变体对象 erp_product_variant
 *
 * @author lwj
 * @date 2026-03-26
 */
@TableName("erp_product_variant")
@Data
public class ProductVariant implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 本地主键 */
    @TableId(value = "variant_id", type = IdType.ASSIGN_ID)
    private Long variantId;

    /** 关联商品主表ID */
    @Excel(name = "关联商品主表ID")
    private Long productId;

    /** Shopify平台变体ID */
    @Excel(name = "Shopify平台变体ID")
    private String shopifyVariantId;

    /** SKU */
    @Excel(name = "SKU")
    private String sku;

    /** 销售价格(美元*100) */
    @Excel(name = "销售价格(美元*100)")
    private Integer price;

    /** 原价/对比价(美元*100) */
    @Excel(name = "原价/对比价(美元*100)")
    private Integer compareAtPrice;

    /** 采购价（分） */
    @Excel(name = "采购价", readConverterExp = "分=")
    private Integer purchasePrice;

    /** 采购链接 */
    @Excel(name = "采购链接")
    private String purchaseUrl;

    /** 变体对应的选项 */
    @Excel(name = "变体对应的选项")
    private String optionValues;

    @TableField(exist = false)
    private List<ProductVariantOption> optionValueList;

    /** 关联的图片ID (若有) */
    @Excel(name = "关联的图片ID (若有)")
    private Long mediaId;
    @TableField(exist = false)
    private MediaVo media;

    /** 排序位置 列表中的第一个位置是 1 */
    @Excel(name = "排序位置 列表中的第一个位置是 1")
    private Integer position;

    /** 包装宽度 */
    @Excel(name = "包装宽度")
    private Integer pkWidth;

    /** 包装高度 */
    @Excel(name = "包装高度")
    private Integer pkHeight;

    /** 包装长度 */
    @Excel(name = "包装长度")
    private Integer pkLength;

    /** 材积重 */
    @Excel(name = "材积重")
    private Integer materialWeight;

    /** 常规包装重量 */
    @Excel(name = "常规包装重量")
    private Integer pkWeight;

    /** 运费 */
    @Excel(name = "运费")
    private Integer freight;

    /** 运费是否来自实际发货数据(0:否, 1:是) */
    @Excel(name = "运费是否来自实际发货数据(0:否, 1:是)", dictType = "erp_product_variant_is_actual_shipment")
    private String isActualShipment;

    /** 美国汇率 */
    @Excel(name = "美国汇率")
    private BigDecimal exchangeRate;

    /** 商品成本价（分） */
    @Excel(name = "商品成本价", readConverterExp = "分=")
    private Integer unitCostPrice;

    /** 建议售价(美元*100) */
    @Excel(name = "建议售价(美元*100)")
    private Integer suggestedPrice;

    /** 实际利润率 */
    @Excel(name = "实际利润率")
    private BigDecimal profitRate;
    /** 利润 */
    @Excel(name = "利润 （分）")
    private BigDecimal profit;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 删除标志 (0代表存在 2代表删除) */
    private String delFlag;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

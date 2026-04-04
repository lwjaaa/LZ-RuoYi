package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * erp商品对象 erp_product
 *
 * @author lwj
 * @date 2026-03-26
 */
@TableName("erp_product")
@Data
public class Product implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 本地主键 */
    @TableId(value = "product_id", type = IdType.ASSIGN_ID)
    private Long productId;

    /** Shopify平台商品ID (唯一映射) */
    @Excel(name = "Shopify平台商品ID (唯一映射)")
    private String shopifyProductId;

    /** 商品标题 */
    @Excel(name = "商品标题")
    private String productTitle;

    /** SPU */
    @Excel(name = "SPU")
    private String spu;

    /** 商品类别ID (Category) */
    @Excel(name = "商品类别ID (Category)")
    private Long category;

    /** 商品类型 */
    @Excel(name = "商品类型")
    private String productType;

    /** 来源URL */
    @Excel(name = "来源URL")
    private String sourceUrl;

    /** 采购链接 */
    @Excel(name = "采购链接")
    private String purchaseUrl;

    /** 商品选项 */
    @Excel(name = "商品选项")
    private String optionJson;

    /** 采购商品选项 */
    @Excel(name = "采购商品选项")
    private String purchaseOptionJson;

    /** 发布状态 */
    @Excel(name = "发布状态", dictType = "erp_product_status")
    private String status;

    /** 商品详情描述 */
    @Excel(name = "商品详情描述")
    private String bodyHtml;

    /** 主图ID，仅用户erp后台展示 */
    @Excel(name = "主图ID，仅用户erp后台展示")
    private Long mainMediaId;

    /** 同步状态 */
    @Excel(name = "同步状态", dictType = "erp_product_sync_status")
    private String syncStatus;

    /** 最后一次同步错误信息或结果 */
    @Excel(name = "最后一次同步错误信息或结果")
    private String syncMessage;

    /** 最后同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "最后同步时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date lastSyncTime;

    /** 乐观锁版本号 */
    @Excel(name = "乐观锁版本号")
    private Long version;

    /** 描述 */
    @Excel(name = "DESCRIPTION")
    private String description;

    /** 大小 */
    @Excel(name = "SIZE")
    private String size;

    /** 材质 */
    @Excel(name = "MATERIAL")
    private String material;

    /** 备注 */
    @Excel(name = "NOTE")
    private String note;

    /** 包含的包材 */
    @Excel(name = "PACKAGEINCLUDE")
    private String packageInclude;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updateTime;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 删除标志 (0代表存在 2代表删除) */
    private String delFlag;

    /** erp商品变体信息,非数据库字段 */
    private List<ProductVariant> productVariantList;
    /** 商品标签ID列表,非数据库字段 */
    private List<Long> tagIds;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}

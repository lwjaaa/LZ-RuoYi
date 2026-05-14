package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.utils.StringUtils;
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

    // 商品名称
    @Excel(name = "商品名称")
    private String productName;
    /** 本地主键 */
    @TableId(value = "product_id", type = IdType.ASSIGN_ID)
    private Long productId;

    /** Shopify平台商品ID (唯一映射) */
    @Excel(name = "Shopify平台商品ID (唯一映射)")
    private String shopifyProductId;

    /** 所属 Shopify 店铺 ID */
    @Excel(name = "所属店铺ID")
    private Long storeId;

    /** Shopify 远端商品更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shopifyUpdatedAt;

    /** 最近一次 Shopify 反向导入成功时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastShopifyImportTime;

    /** 商品标题 */
    @Excel(name = "商品标题")
    private String productTitle;

    /** SPU */
    @Excel(name = "SPU")
    private String spu;

    /** 商品类别ID (Category) */
    @Excel(name = "商品类别ID (Category)")
    private String category;

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

    /** 商品资料缺失字段编码，英文逗号分隔 */
    @TableField("missing_fields")
    @Excel(name = "商品资料缺失字段")
    private String missingFieldCodes;

    /** 最后同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后同步时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncTime;

    /** 乐观锁版本号 */
    @Excel(name = "乐观锁版本号")
    private Long version;

    /** 描述 */
    @Excel(name = "DESCRIPTION")
    private String description;

    /** 描述 */
    @Excel(name = "中文DESCRIPTION")
    private String descriptionCn;

    /** 大小 */
    @Excel(name = "SIZE")
    private String size;

    /** 材质 */
    @Excel(name = "MATERIAL")
    private String material;

    /** 备注 */
    @Excel(name = "NOTE")
    private String note;

    /** 中文备注 */
    @Excel(name = "中文NOTE")
    private String noteCn;

    /** 包含的包材 */
    @Excel(name = "PACKAGEINCLUDE")
    private String packageInclude;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 删除标志 (0代表存在 2代表删除) */
    private String delFlag;

    /** erp商品变体信息,非数据库字段 */
    @TableField(exist = false)
    private List<ProductVariant> productVariantList;
    /** 商品标签ID列表,非数据库字段 */
    @TableField(exist = false)
    private List<Long> tagIds;

    /** 工作台关键词，匹配标题、SPU、SKU 或 Shopify ID */
    @TableField(exist = false)
    private String searchKeyword;

    /** 资料完整度快捷分组：incomplete */
    @TableField(exist = false)
    private String qualityState;

    /** 更新人筛选 */
    @TableField(exist = false)
    private String updatedBy;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    @TableField(exist = false)
    private List<Media> mediaList;

    @TableField(exist = false)
    private List<String> mediaUrlList;

    /**
     * 获取关键词，用于 SKU前缀、文件夹、文件名前缀
     * 优先使用 SPU，如果 SPU 为空则使用 productId
     */
    public String getKeyWord(){
        return StringUtils.isNotBlank(spu)? spu : String.valueOf(productId);
    }
}

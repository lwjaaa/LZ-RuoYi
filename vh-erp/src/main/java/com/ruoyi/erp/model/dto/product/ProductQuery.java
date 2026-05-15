package com.ruoyi.erp.model.dto.product;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * erp商品Query对象 erp_product
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
@Schema(description = "ERP 商品查询对象")
public class ProductQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** Shopify平台商品ID (唯一映射) */
    @Schema(description = "Shopify平台商品ID，唯一映射")
    private String shopifyProductId;

    /** 商品标题 */
    @Schema(description = "商品标题")
    private String productTitle;

    /** 工作台关键词，匹配标题、SPU、SKU 或 Shopify ID */
    @Schema(description = "工作台关键词，匹配标题、SPU、SKU 或 Shopify ID")
    private String searchKeyword;

    /** SPU */
    @Schema(description = "SPU")
    private String spu;

    /** 商品类别 */
    @Schema(description = "商品类别")
    private String category;

    /** 商品类型 */
    @Schema(description = "商品类型")
    private String productType;

    /** 来源URL */
    @Schema(description = "来源URL")
    private String sourceUrl;

    /** 采购链接 */
    @Schema(description = "采购链接")
    private String purchaseUrl;

    /** 发布状态 */
    @Schema(description = "发布状态")
    private String status;

    /** 同步状态 */
    @Schema(description = "同步状态")
    private String syncStatus;

    /** 资料完整度快捷分组：incomplete/complete */
    @Schema(description = "资料完整度快捷分组", allowableValues = {"incomplete", "complete"})
    private String qualityState;

    /** 当前目标店铺，仅用于店铺上下文和最新任务查询 */
    @Schema(description = "当前目标店铺，仅用于店铺上下文和最新任务查询")
    private Long storeId;

    /** 标签筛选 */
    @Schema(description = "标签筛选")
    private List<Long> tagIds;

    /** 更新人 */
    @Schema(description = "更新人")
    private String updatedBy;

    /** 最后同步时间 */
    @Schema(description = "最后同步时间", type = "string", format = "date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date lastSyncTime;

    /** 创建者 */
    @Schema(description = "创建者")
    private String createBy;

    /** erp商品变体信息 */
    @Schema(description = "ERP 商品变体信息")
    private List<ProductVariant> productVariantList;

    /** 请求参数 */
    @Schema(description = "请求参数")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    /**
     * 对象转封装类
     *
     * @param productQuery 查询对象
     * @return Product
     */
    public static Product queryToObj(ProductQuery productQuery) {
        if (productQuery == null) {
            return null;
        }
        Product product = new Product();
        BeanUtils.copyProperties(productQuery, product);
        return product;
    }
}

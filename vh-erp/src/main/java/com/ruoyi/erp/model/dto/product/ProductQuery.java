package com.ruoyi.erp.model.dto.product;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
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
public class ProductQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** Shopify平台商品ID (唯一映射) */
    private String shopifyProductId;

    /** 商品标题 */
    private String productTitle;

    /** SPU */
    private String spu;

    /** 发布状态 */
    private String status;

    /** 同步状态 */
    private String syncStatus;

    /** 最后同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date lastSyncTime;

    /** 创建者 */
    private String createBy;

    /** erp商品变体信息 */
    private List<ProductVariant> productVariantList;

    /** 请求参数 */
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

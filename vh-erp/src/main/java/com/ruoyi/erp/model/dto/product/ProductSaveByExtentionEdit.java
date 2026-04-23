package com.ruoyi.erp.model.dto.product;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductOption;
import com.ruoyi.erp.model.domain.ProductVariant;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.List;

/**
 * 通过插件创建商品
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class ProductSaveByExtentionEdit implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long productId;

    /** SPU */
    private String spu;

    /** 来源URL */
    private String sourceUrl;

    /** 采购链接 */
    private String purchaseUrl;

    /** 商品选项 */
    private String optionJson;
    private List<ProductOption> optionList;

    /** erp商品变体信息 */
    private List<ProductVariant> productVariantList;

    /** 商品标签ID列表 */
    private List<Long> tagIds;

    private List<String> mediaUrlList;

    // 商品名称
    private String productName;

    /**
     * 对象转封装类
     *
     * @param productEdit 编辑对象
     * @return Product
     */
    public static Product editToObj(ProductSaveByExtentionEdit productEdit) {
        if (productEdit == null) {
            return null;
        }
        Product product = new Product();
        BeanUtils.copyProperties(productEdit, product);
        product.setOptionJson(JSONObject.toJSONString(productEdit.getOptionList()));
        return product;
    }
}

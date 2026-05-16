package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantQuery;
import com.ruoyi.erp.model.vo.productVariant.ProductVariantSummaryVo;
import com.ruoyi.erp.model.vo.productVariant.ProductVariantVo;

/**
 * erp商品变体Mapper接口
 * 
 * @author lwj
 * @date 2026-03-26
 */
public interface ProductVariantMapper extends BaseMapper<ProductVariant>
{
    /**
     * 查询erp商品变体
     * 
     * @param variantId erp商品变体主键
     * @return erp商品变体
     */
    public ProductVariant selectProductVariantByVariantId(Long variantId);

    /**
     * 查询erp商品变体列表
     * 
     * @param productVariant erp商品变体
     * @return erp商品变体集合
     */
    public List<ProductVariant> selectProductVariantList(ProductVariant productVariant);

    /**
     * 查询 SKU 运营台列表。
     *
     * @param query SKU 查询条件
     * @return SKU 运营列表
     */
    public List<ProductVariantVo> selectSkuOperationList(ProductVariantQuery query);

    /**
     * 查询 SKU 运营台汇总指标。
     *
     * @param query SKU 查询条件
     * @return SKU 汇总指标
     */
    public ProductVariantSummaryVo selectSkuOperationSummary(ProductVariantQuery query);

    /**
     * 新增erp商品变体
     * 
     * @param productVariant erp商品变体
     * @return 结果
     */
    public int insertProductVariant(ProductVariant productVariant);

    /**
     * 修改erp商品变体
     * 
     * @param productVariant erp商品变体
     * @return 结果
     */
    public int updateProductVariant(ProductVariant productVariant);

    /**
     * 删除erp商品变体
     * 
     * @param variantId erp商品变体主键
     * @return 结果
     */
    public int deleteProductVariantByVariantId(Long variantId);

    /**
     * 批量删除erp商品变体
     * 
     * @param variantIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProductVariantByVariantIds(Long[] variantIds);
}

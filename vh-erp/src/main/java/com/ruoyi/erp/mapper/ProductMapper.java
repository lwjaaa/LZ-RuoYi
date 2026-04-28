package com.ruoyi.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.erp.model.domain.Product;

import java.util.List;

/**
 * erp商品Mapper接口
 * 
 * @author lwj
 * @date 2026-03-26
 */
public interface ProductMapper extends BaseMapper<Product>
{

    /**
     * 查询erp商品列表
     * 
     * @param product erp商品
     * @return erp商品集合
     */
    public List<Product> selectProductList(Product product);

    /**
     * 删除erp商品
     * 
     * @param productId erp商品主键
     * @return 结果
     */
    public int deleteProductByProductId(Long productId);

    /**
     * 批量删除erp商品
     * 
     * @param productIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProductByProductIds(Long[] productIds);

    /**
     * 批量删除erp商品变体
     * 
     * @param productIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProductVariantByProductIds(Long[] productIds);

    /**
     * 通过erp商品主键删除erp商品变体信息
     * 
     * @param productId erp商品ID
     * @return 结果
     */
    public int deleteProductVariantByProductId(Long productId);
}

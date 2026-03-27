package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.vo.productVariant.ProductVariantVo;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantQuery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
/**
 * erp商品变体Service接口
 * 
 * @author lwj
 * @date 2026-03-26
 */
public interface IProductVariantService extends IService<ProductVariant>
{
    //region mybatis代码
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
     * 批量删除erp商品变体
     * 
     * @param variantIds 需要删除的erp商品变体主键集合
     * @return 结果
     */
    public int deleteProductVariantByVariantIds(Long[] variantIds);

    /**
     * 删除erp商品变体信息
     * 
     * @param variantId erp商品变体主键
     * @return 结果
     */
    public int deleteProductVariantByVariantId(Long variantId);
    //endregion
    /**
     * 获取查询条件
     *
     * @param productVariantQuery 查询条件对象
     * @return 查询条件
     */
    QueryWrapper<ProductVariant> getQueryWrapper(ProductVariantQuery productVariantQuery);

    /**
     * 转换vo
     *
     * @param productVariantList ProductVariant集合
     * @return ProductVariantVO集合
     */
    List<ProductVariantVo> convertVoList(List<ProductVariant> productVariantList);

    /**
     * 导入erp商品变体数据
     *
     * @param productVariantList erp商品变体数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    public String importProductVariantData(List<ProductVariant> productVariantList, Boolean isUpdateSupport, String operName);
}

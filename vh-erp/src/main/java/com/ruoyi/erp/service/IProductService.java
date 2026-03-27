package com.ruoyi.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.dto.product.ProductQuery;
import com.ruoyi.erp.model.vo.product.ProductVo;

import java.util.List;
/**
 * erp商品Service接口
 * 
 * @author lwj
 * @date 2026-03-26
 */
public interface IProductService extends IService<Product>
{
    //region mybatis代码
    /**
     * 查询erp商品
     * 
     * @param productId erp商品主键
     * @return erp商品
     */
    public Product selectProductByProductId(Long productId);

    /**
     * 查询erp商品列表
     * 
     * @param product erp商品
     * @return erp商品集合
     */
    public List<Product> selectProductList(Product product);

    /**
     * 新增erp商品
     * 
     * @param product erp商品
     * @return 结果
     */
    public int insertProduct(Product product);

    /**
     * 修改erp商品
     * 
     * @param product erp商品
     * @return 结果
     */
    public int updateProduct(Product product);

    /**
     * 批量删除erp商品
     * 
     * @param productIds 需要删除的erp商品主键集合
     * @return 结果
     */
    public int deleteProductByProductIds(Long[] productIds);

    /**
     * 删除erp商品信息
     * 
     * @param productId erp商品主键
     * @return 结果
     */
    public int deleteProductByProductId(Long productId);
    //endregion
    /**
     * 获取查询条件
     *
     * @param productQuery 查询条件对象
     * @return 查询条件
     */
    QueryWrapper<Product> getQueryWrapper(ProductQuery productQuery);

    /**
     * 转换vo
     *
     * @param productList Product集合
     * @return ProductVO集合
     */
    List<ProductVo> convertVoList(List<Product> productList);

    /**
     * 导入erp商品数据
     *
     * @param productList erp商品数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    public String importProductData(List<Product> productList, Boolean isUpdateSupport, String operName);

    /**
     * 根据标签交集查询商品列表
     *
     * @param productQuery 查询条件
     * @return 商品列表
     */
    public List<Product> selectProductListByTags(ProductQuery productQuery);

    /**
     * 保存商品及相关数据（事务）
     *
     * @param productInsert 商品插入对象
     * @return 结果
     */
    public int saveProductWithTransaction(com.ruoyi.erp.model.dto.product.ProductInsert productInsert);

    /**
     * 异步批量推送商品到Shopify
     *
     * @param productIds 商品ID列表
     * @return 任务ID
     */
    public Long pushBatchAsync(List<Long> productIds);

    /**
     * 获取推送结果
     *
     * @param taskId 任务ID
     * @return 结果
     */
    public Object getPushResult(Long taskId);
}

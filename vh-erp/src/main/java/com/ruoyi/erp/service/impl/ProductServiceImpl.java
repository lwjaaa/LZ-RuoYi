package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.dto.product.ProductInsert;
import com.ruoyi.erp.model.dto.product.ProductQuery;
import com.ruoyi.erp.model.vo.product.ProductVo;
import com.ruoyi.erp.service.IProductService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * erp商品Service业务层处理
 *
 * @author lwj
 * @date 2026-03-26
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService
{

    @Resource
    private ProductMapper productMapper;

    //region mybatis代码
    /**
     * 查询erp商品
     *
     * @param productId erp商品主键
     * @return erp商品
     */
    @Override
    public Product selectProductByProductId(Long productId)
    {
        return productMapper.selectProductByProductId(productId);
    }

    /**
     * 查询erp商品列表
     *
     * @param product erp商品
     * @return erp商品
     */
    @Override
    public List<Product> selectProductList(Product product)
    {
        return productMapper.selectProductList(product);
    }

    /**
     * 新增erp商品
     *
     * @param product erp商品
     * @return 结果
     */
    @Transactional
    @Override
    public int insertProduct(Product product)
    {
        product.setCreateTime(DateUtils.getNowDate());
        int rows = productMapper.insertProduct(product);
        insertProductVariant(product);
        return rows;
    }

    /**
     * 修改erp商品
     *
     * @param product erp商品
     * @return 结果
     */
    @Transactional
    @Override
    public int updateProduct(Product product)
    {
        product.setUpdateTime(DateUtils.getNowDate());
        productMapper.deleteProductVariantByProductId(product.getProductId());
        insertProductVariant(product);
        return productMapper.updateProduct(product);
    }

    /**
     * 批量删除erp商品
     *
     * @param productIds 需要删除的erp商品主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteProductByProductIds(Long[] productIds)
    {
        productMapper.deleteProductVariantByProductIds(productIds);
        return productMapper.deleteProductByProductIds(productIds);
    }

    /**
     * 删除erp商品信息
     *
     * @param productId erp商品主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteProductByProductId(Long productId)
    {
        productMapper.deleteProductVariantByProductId(productId);
        return productMapper.deleteProductByProductId(productId);
    }

    /**
     * 新增erp商品变体信息
     *
     * @param product erp商品对象
     */
    public void insertProductVariant(Product product)
    {
        List<ProductVariant> productVariantList = product.getProductVariantList();
        Long productId = product.getProductId();
        if (StringUtils.isNotNull(productVariantList))
        {
            List<ProductVariant> list = new ArrayList<ProductVariant>();
            for (ProductVariant productVariant : productVariantList)
            {
                productVariant.setProductId(productId);
                list.add(productVariant);
            }
            if (list.size() > 0)
            {
                productMapper.batchProductVariant(list);
            }
        }
    }
    //endregion
    @Override
    public QueryWrapper<Product> getQueryWrapper(ProductQuery productQuery){
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        //如果不使用params可以删除
        Map<String, Object> params = productQuery.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        String shopifyProductId = productQuery.getShopifyProductId();
        queryWrapper.eq(StringUtils.isNotEmpty(shopifyProductId) ,"shopify_product_id",shopifyProductId);

        String productTitle = productQuery.getProductTitle();
        queryWrapper.like(StringUtils.isNotEmpty(productTitle) ,"product_title",productTitle);

        String spu = productQuery.getSpu();
        queryWrapper.like(StringUtils.isNotEmpty(spu) ,"spu",spu);

        String status = productQuery.getStatus();
        queryWrapper.eq(StringUtils.isNotEmpty(status) ,"status",status);

        String syncStatus = productQuery.getSyncStatus();
        queryWrapper.eq(StringUtils.isNotEmpty(syncStatus) ,"sync_status",syncStatus);

        Date lastSyncTime = productQuery.getLastSyncTime();
        queryWrapper.between(StringUtils.isNotNull(params.get("beginLastSyncTime"))&&StringUtils.isNotNull(params.get("endLastSyncTime")),"last_sync_time",params.get("beginLastSyncTime"),params.get("endLastSyncTime"));

        String createBy = productQuery.getCreateBy();
        queryWrapper.between(StringUtils.isNotNull(params.get("beginCreateBy"))&&StringUtils.isNotNull(params.get("endCreateBy")),"create_by",params.get("beginCreateBy"),params.get("endCreateBy"));

        return queryWrapper;
    }

    @Override
    public List<ProductVo> convertVoList(List<Product> productList) {
        if (StringUtils.isEmpty(productList)) {
            return Collections.emptyList();
        }
        return productList.stream().map(ProductVo::objToVo).collect(Collectors.toList());
    }

    /**
     * 导入erp商品数据
     *
     * @param productList erp商品数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importProductData(List<Product> productList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isEmpty(productList))
        {
            throw new ServiceException("导入erp商品数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Product product : productList)
        {
            try
            {
                // 验证是否存在这个erp商品
                Long productId = product.getProductId();
                Product productExist = null;
                if (StringUtils.isNotNull(productId))
                {
                    productExist = productMapper.selectProductByProductId(productId);
                }
                if (StringUtils.isNull(productExist))
                {
                    product.setCreateTime(DateUtils.getNowDate());
                    productMapper.insertProduct(product);
                    successNum++;
                    String productIdStr = StringUtils.isNotNull(productId) ? productId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、erp商品 " + productIdStr + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    product.setUpdateTime(DateUtils.getNowDate());
                    productMapper.updateProduct(product);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp商品 " + productId.toString() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    String productIdStr = StringUtils.isNotNull(productId) ? productId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp商品 " + productIdStr + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                Long productId = product.getProductId();
                String productIdStr = StringUtils.isNotNull(productId) ? productId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp商品 " + productIdStr + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    @Override
    public List<Product> selectProductListByTags(ProductQuery productQuery) {
        return List.of();
    }

    @Override
    public int saveProductWithTransaction(ProductInsert productInsert) {
        return 0;
    }

    @Override
    public Long pushBatchAsync(List<Long> productIds) {
        return 0L;
    }

    @Override
    public Object getPushResult(Long taskId) {
        return null;
    }
}

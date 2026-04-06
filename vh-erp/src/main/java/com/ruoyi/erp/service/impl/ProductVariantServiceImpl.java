package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantQuery;
import com.ruoyi.erp.model.vo.productVariant.ProductVariantVo;
import com.ruoyi.erp.service.IProductVariantService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * erp商品变体Service业务层处理
 *
 * @author lwj
 * @date 2026-03-26
 */
@Service
public class ProductVariantServiceImpl extends ServiceImpl<ProductVariantMapper, ProductVariant> implements IProductVariantService
{

    @Resource
    private ProductVariantMapper productVariantMapper;

    //region mybatis代码
    /**
     * 查询erp商品变体
     *
     * @param variantId erp商品变体主键
     * @return erp商品变体
     */
    @Override
    public ProductVariant selectProductVariantByVariantId(Long variantId)
    {
        return productVariantMapper.selectProductVariantByVariantId(variantId);
    }

    /**
     * 查询erp商品变体列表
     *
     * @param productVariant erp商品变体
     * @return erp商品变体
     */
    @Override
    public List<ProductVariant> selectProductVariantList(ProductVariant productVariant)
    {
        return productVariantMapper.selectProductVariantList(productVariant);
    }

    /**
     * 新增erp商品变体
     *
     * @param productVariant erp商品变体
     * @return 结果
     */
    @Override
    public int insertProductVariant(ProductVariant productVariant)
    {
        productVariant.setCreateTime(DateUtils.getNowDate());
        return productVariantMapper.insertProductVariant(productVariant);
    }

    /**
     * 修改erp商品变体
     *
     * @param productVariant erp商品变体
     * @return 结果
     */
    @Override
    public int updateProductVariant(ProductVariant productVariant)
    {
        productVariant.setUpdateTime(DateUtils.getNowDate());
        return productVariantMapper.updateProductVariant(productVariant);
    }

    /**
     * 批量删除erp商品变体
     *
     * @param variantIds 需要删除的erp商品变体主键
     * @return 结果
     */
    @Override
    public int deleteProductVariantByVariantIds(Long[] variantIds)
    {
        return productVariantMapper.deleteProductVariantByVariantIds(variantIds);
    }

    /**
     * 删除erp商品变体信息
     *
     * @param variantId erp商品变体主键
     * @return 结果
     */
    @Override
    public int deleteProductVariantByVariantId(Long variantId)
    {
        return productVariantMapper.deleteProductVariantByVariantId(variantId);
    }
    //endregion
    @Override
    public QueryWrapper<ProductVariant> getQueryWrapper(ProductVariantQuery productVariantQuery){
        QueryWrapper<ProductVariant> queryWrapper = new QueryWrapper<>();
        //如果不使用params可以删除
        Map<String, Object> params = productVariantQuery.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        Long productId = productVariantQuery.getProductId();
        queryWrapper.eq( StringUtils.isNotNull(productId),"product_id",productId);

        String shopifyVariantId = productVariantQuery.getShopifyVariantId();
        queryWrapper.eq(StringUtils.isNotEmpty(shopifyVariantId) ,"shopify_variant_id",shopifyVariantId);

        String sku = productVariantQuery.getSku();
        queryWrapper.eq(StringUtils.isNotEmpty(sku) ,"sku",sku);

        Integer price = productVariantQuery.getPrice();
        queryWrapper.eq( StringUtils.isNotNull(price),"price",price);

        Integer purchasePrice = productVariantQuery.getPurchasePrice();
        queryWrapper.eq( StringUtils.isNotNull(purchasePrice),"purchase_price",purchasePrice);

        String isActualShipment = productVariantQuery.getIsActualShipment();
        queryWrapper.eq(StringUtils.isNotEmpty(isActualShipment) ,"is_actual_shipment",isActualShipment);

        Date createTime = productVariantQuery.getCreateTime();
        queryWrapper.between(StringUtils.isNotNull(params.get("beginCreateTime"))&&StringUtils.isNotNull(params.get("endCreateTime")),"create_time",params.get("beginCreateTime"),params.get("endCreateTime"));

        return queryWrapper;
    }

    @Override
    public List<ProductVariantVo> convertVoList(List<ProductVariant> productVariantList) {
        if (StringUtils.isEmpty(productVariantList)) {
            return Collections.emptyList();
        }
        return productVariantList.stream().map(ProductVariantVo::objToVo).collect(Collectors.toList());
    }

    /**
     * 导入erp商品变体数据
     *
     * @param productVariantList erp商品变体数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importProductVariantData(List<ProductVariant> productVariantList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isEmpty(productVariantList))
        {
            throw new ServiceException("导入erp商品变体数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (ProductVariant productVariant : productVariantList)
        {
            try
            {
                // 验证是否存在这个erp商品变体
                Long variantId = productVariant.getVariantId();
                ProductVariant productVariantExist = null;
                if (StringUtils.isNotNull(variantId))
                {
                    productVariantExist = productVariantMapper.selectProductVariantByVariantId(variantId);
                }
                if (StringUtils.isNull(productVariantExist))
                {
                    productVariant.setCreateTime(DateUtils.getNowDate());
                    productVariantMapper.insertProductVariant(productVariant);
                    successNum++;
                    String variantIdStr = StringUtils.isNotNull(variantId) ? variantId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、erp商品变体 " + variantIdStr + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    productVariant.setUpdateTime(DateUtils.getNowDate());
                    productVariantMapper.updateProductVariant(productVariant);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp商品变体 " + variantId.toString() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    String variantIdStr = StringUtils.isNotNull(variantId) ? variantId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp商品变体 " + variantIdStr + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                Long variantId = productVariant.getVariantId();
                String variantIdStr = StringUtils.isNotNull(variantId) ? variantId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp商品变体 " + variantIdStr + " 导入失败：";
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
    public List<ProductVariant> selectListByProductId(Long productId) {
        return this.list(new LambdaQueryWrapper<>(ProductVariant.class).eq(ProductVariant::getProductId, productId));
    }
}

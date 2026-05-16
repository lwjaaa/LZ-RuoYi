package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantBatchEdit;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantQuery;
import com.ruoyi.erp.model.vo.productVariant.ProductVariantSummaryVo;
import com.ruoyi.erp.model.vo.productVariant.ProductVariantVo;
import com.ruoyi.erp.service.IProductQualityService;
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
    @Resource
    private ProductMapper productMapper;
    @Resource
    private IProductQualityService productQualityService;

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
     * 查询 SKU 运营台列表。
     *
     * @param query SKU 查询条件
     * @return SKU 运营列表
     */
    @Override
    public List<ProductVariantVo> selectSkuOperationList(ProductVariantQuery query) {
        return productVariantMapper.selectSkuOperationList(query);
    }

    /**
     * 查询 SKU 运营台汇总指标。
     *
     * @param query SKU 查询条件
     * @return SKU 汇总指标
     */
    @Override
    public ProductVariantSummaryVo selectSkuOperationSummary(ProductVariantQuery query) {
        ProductVariantSummaryVo summary = productVariantMapper.selectSkuOperationSummary(query);
        return summary == null ? new ProductVariantSummaryVo() : summary;
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
        validateSkuUnique(productVariant, null);
        productVariant.setCreateTime(DateUtils.getNowDate());
        int rows = productVariantMapper.insertProductVariant(productVariant);
        markProductsWaitingSync(List.of(productVariant.getProductId()));
        return rows;
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
        ProductVariant oldVariant = productVariant.getVariantId() == null ? null : productVariantMapper.selectById(productVariant.getVariantId());
        if (oldVariant != null) {
            if (productVariant.getStoreId() == null) {
                productVariant.setStoreId(oldVariant.getStoreId());
            }
            if (productVariant.getProductId() == null) {
                productVariant.setProductId(oldVariant.getProductId());
            }
        }
        validateSkuUnique(productVariant, productVariant.getVariantId());
        productVariant.setUpdateTime(DateUtils.getNowDate());
        int rows = productVariantMapper.updateProductVariant(productVariant);
        List<Long> productIds = new ArrayList<>();
        if (oldVariant != null) {
            productIds.add(oldVariant.getProductId());
        }
        productIds.add(productVariant.getProductId());
        markProductsWaitingSync(productIds);
        return rows;
    }

    /**
     * 批量更新 SKU 运营字段，并标记父商品待同步。
     *
     * @param edit 批量编辑请求
     * @return 影响 SKU 数
     */
    @Override
    public int batchUpdateSkuOperations(ProductVariantBatchEdit edit) {
        validateBatchEdit(edit);
        List<ProductVariant> variants = productVariantMapper.selectBatchIds(edit.getVariantIds());
        if (StringUtils.isEmpty(variants)) {
            throw new ServiceException("未找到可更新的 SKU");
        }
        int rows = 0;
        Date now = DateUtils.getNowDate();
        for (ProductVariant variant : variants) {
            ProductVariant update = new ProductVariant();
            update.setVariantId(variant.getVariantId());
            copyBatchFields(edit, update);
            update.setUpdateTime(now);
            rows += productVariantMapper.updateById(update);
        }
        markProductsWaitingSync(variants.stream().map(ProductVariant::getProductId).toList());
        return rows;
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
        List<Long> productIds = listByIds(List.of(variantIds)).stream()
                .map(ProductVariant::getProductId)
                .filter(Objects::nonNull)
                .toList();
        int rows = productVariantMapper.deleteProductVariantByVariantIds(variantIds);
        markProductsWaitingSync(productIds);
        return rows;
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
        ProductVariant variant = productVariantMapper.selectById(variantId);
        int rows = productVariantMapper.deleteProductVariantByVariantId(variantId);
        if (variant != null) {
            markProductsWaitingSync(List.of(variant.getProductId()));
        }
        return rows;
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

        Long storeId = productVariantQuery.getStoreId();
        queryWrapper.eq(StringUtils.isNotNull(storeId), "store_id", storeId);

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

    /**
     * 校验同店铺 SKU 唯一性。
     *
     * @param variant SKU 实体
     * @param currentVariantId 当前 SKU 主键
     */
    private void validateSkuUnique(ProductVariant variant, Long currentVariantId) {
        if (variant == null || StringUtils.isEmpty(variant.getSku()) || variant.getStoreId() == null) {
            return;
        }
        QueryWrapper<ProductVariant> wrapper = new QueryWrapper<ProductVariant>()
                .eq("store_id", variant.getStoreId())
                .eq("sku", variant.getSku())
                .ne(currentVariantId != null, "variant_id", currentVariantId);
        ProductVariant duplicate = productVariantMapper.selectOne(wrapper);
        if (duplicate != null) {
            throw new ServiceException("同一店铺下 SKU 已存在，请更换 SKU");
        }
    }

    /**
     * 校验 SKU 批量编辑请求。
     *
     * @param edit 批量编辑请求
     */
    private void validateBatchEdit(ProductVariantBatchEdit edit) {
        if (edit == null || StringUtils.isEmpty(edit.getVariantIds())) {
            throw new ServiceException("请选择需要更新的 SKU");
        }
        validateNonNegative(edit.getPrice(), "销售价格不能小于0");
        validateNonNegative(edit.getCompareAtPrice(), "对比价不能小于0");
        validateNonNegative(edit.getPurchasePrice(), "采购价不能小于0");
        validateNonNegative(edit.getFreight(), "运费不能小于0");
        validateNonNegative(edit.getPkWidth(), "包装宽度不能小于0");
        validateNonNegative(edit.getPkHeight(), "包装高度不能小于0");
        validateNonNegative(edit.getPkLength(), "包装长度不能小于0");
        validateNonNegative(edit.getPkWeight(), "包装重量不能小于0");
    }

    /**
     * 校验非负数字。
     *
     * @param value 字段值
     * @param message 错误提示
     */
    private void validateNonNegative(Integer value, String message) {
        if (value != null && value < 0) {
            throw new ServiceException(message);
        }
    }

    /**
     * 复制批量编辑中填写的字段。
     *
     * @param edit 批量编辑请求
     * @param update SKU 更新实体
     */
    private void copyBatchFields(ProductVariantBatchEdit edit, ProductVariant update) {
        if (edit.getPrice() != null) {
            update.setPrice(edit.getPrice());
        }
        if (edit.getCompareAtPrice() != null) {
            update.setCompareAtPrice(edit.getCompareAtPrice());
        }
        if (edit.getPurchasePrice() != null) {
            update.setPurchasePrice(edit.getPurchasePrice());
        }
        if (edit.getPurchaseUrl() != null) {
            update.setPurchaseUrl(edit.getPurchaseUrl());
        }
        if (edit.getFreight() != null) {
            update.setFreight(edit.getFreight());
        }
        if (edit.getPkWidth() != null) {
            update.setPkWidth(edit.getPkWidth());
        }
        if (edit.getPkHeight() != null) {
            update.setPkHeight(edit.getPkHeight());
        }
        if (edit.getPkLength() != null) {
            update.setPkLength(edit.getPkLength());
        }
        if (edit.getPkWeight() != null) {
            update.setPkWeight(edit.getPkWeight());
        }
        if (edit.getIsActiveAvailable() != null) {
            update.setIsActiveAvailable(edit.getIsActiveAvailable());
        }
        if (edit.getRemark() != null) {
            update.setRemark(edit.getRemark());
        }
    }

    /**
     * 将父商品标记为待同步。
     *
     * @param productIds 父商品ID列表
     */
    private void markProductsWaitingSync(List<Long> productIds) {
        if (StringUtils.isEmpty(productIds)) {
            return;
        }
        productIds.stream().filter(Objects::nonNull).distinct().forEach(productId -> {
            Product product = new Product();
            product.setProductId(productId);
            product.setSyncStatus(ProductConstants.SYNC_STATUS_WAITING);
            product.setUpdateTime(DateUtils.getNowDate());
            productMapper.updateById(product);
        });
    }
}

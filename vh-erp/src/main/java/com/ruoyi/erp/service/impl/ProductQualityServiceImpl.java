package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.erp.mapper.MediaMapper;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.service.IProductQualityService;
import com.ruoyi.erp.util.ProductListMetrics;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 维护商品资料缺失字段缓存，避免列表每次跨表实时计算。
 */
@Service
public class ProductQualityServiceImpl implements IProductQualityService {

    @Resource
    private ProductMapper productMapper;
    @Resource
    private ProductVariantMapper productVariantMapper;
    @Resource
    private MediaMapper mediaMapper;

    @Override
    public void refreshProductMissingFields(Long productId) {
        if (productId == null) {
            return;
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return;
        }
        List<ProductVariant> variants = productVariantMapper.selectList(new LambdaQueryWrapper<ProductVariant>()
                .eq(ProductVariant::getProductId, productId));
        List<Media> medias = mediaMapper.selectList(new LambdaQueryWrapper<Media>()
                .eq(Media::getProductId, productId));
        String missingFieldCodes = ProductListMetrics.joinMissingFields(
                ProductListMetrics.resolveMissingFields(product, variants, medias)
        );

        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .set(Product::getMissingFieldCodes, missingFieldCodes)
                .eq(Product::getProductId, productId));
    }

    @Override
    public void refreshProductMissingFields(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::refreshProductMissingFields);
    }
}

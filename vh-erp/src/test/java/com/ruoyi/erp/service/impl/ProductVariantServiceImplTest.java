package com.ruoyi.erp.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.dto.productVariant.ProductVariantBatchEdit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductVariantServiceImplTest {

    @Test
    void batchUpdateSkuOperationsUpdatesSelectedVariantsAndMarksParentProductsWaitingSync() {
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);

        ProductVariant first = variant(1L, 10L, "SKU-A");
        ProductVariant second = variant(2L, 20L, "SKU-B");
        when(variantMapper.selectBatchIds(any())).thenReturn(List.of(first, second));
        when(variantMapper.updateById(any(ProductVariant.class))).thenReturn(1);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        ProductVariantServiceImpl service = service(variantMapper, productMapper);
        ProductVariantBatchEdit edit = new ProductVariantBatchEdit();
        edit.setVariantIds(List.of(1L, 2L));
        edit.setPrice(1299);
        edit.setPurchasePrice(500);
        edit.setFreight(220);
        edit.setIsActiveAvailable("1");

        int rows = service.batchUpdateSkuOperations(edit);

        assertEquals(2, rows);
        ArgumentCaptor<ProductVariant> variantCaptor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantMapper, times(2)).updateById(variantCaptor.capture());
        assertEquals(List.of(1L, 2L), variantCaptor.getAllValues().stream().map(ProductVariant::getVariantId).toList());
        assertEquals(1299, variantCaptor.getAllValues().get(0).getPrice());
        assertEquals(500, variantCaptor.getAllValues().get(0).getPurchasePrice());
        assertEquals(220, variantCaptor.getAllValues().get(0).getFreight());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper, times(2)).updateById(productCaptor.capture());
        assertEquals(List.of(10L, 20L), productCaptor.getAllValues().stream().map(Product::getProductId).toList());
        assertEquals(ProductConstants.SYNC_STATUS_WAITING, productCaptor.getAllValues().get(0).getSyncStatus());
    }

    @Test
    void updateProductVariantRejectsDuplicateSkuWithinSameStore() {
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariant oldVariant = variant(1L, 10L, "SKU-OLD");
        oldVariant.setStoreId(7L);
        ProductVariant duplicate = variant(2L, 20L, "SKU-NEW");
        duplicate.setStoreId(7L);

        when(variantMapper.selectById(1L)).thenReturn(oldVariant);
        when(variantMapper.selectOne(any())).thenReturn(duplicate);

        ProductVariantServiceImpl service = service(variantMapper, productMapper);
        ProductVariant update = variant(1L, 10L, "SKU-NEW");
        update.setStoreId(7L);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.updateProductVariant(update));

        assertEquals("同一店铺下 SKU 已存在，请更换 SKU", exception.getMessage());
    }

    private ProductVariantServiceImpl service(ProductVariantMapper variantMapper, ProductMapper productMapper) {
        ProductVariantServiceImpl service = new ProductVariantServiceImpl();
        ReflectionTestUtils.setField(service, "productVariantMapper", variantMapper);
        ReflectionTestUtils.setField(service, "baseMapper", variantMapper);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        return service;
    }

    private ProductVariant variant(Long variantId, Long productId, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(variantId);
        variant.setProductId(productId);
        variant.setStoreId(1L);
        variant.setSku(sku);
        return variant;
    }
}

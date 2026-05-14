package com.ruoyi.erp.service.impl;

import com.ruoyi.erp.mapper.MediaMapper;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ShopifySyncCursor;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedMedia;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedVariant;
import com.ruoyi.erp.service.IProductQualityService;
import com.ruoyi.erp.service.IShopifyTaskDetailService;
import com.ruoyi.erp.shopify.enums.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopifyProductImportServiceImplTest {

    @Test
    void calculateQuerySinceSubtractsFiveMinuteSafetyWindowFromLastSuccessfulCursor() {
        ShopifyProductImportServiceImpl service = new ShopifyProductImportServiceImpl();
        ShopifySyncCursor cursor = new ShopifySyncCursor();
        cursor.setLastSuccessUpdatedAt(Date.from(Instant.parse("2026-05-13T10:15:30Z")));

        Date since = service.calculateQuerySince(cursor);

        assertEquals(Date.from(Instant.parse("2026-05-13T10:10:30Z")), since);
    }

    @Test
    void upsertProductCreatesLocalProductVariantsMediaAndStoreScopedMappings() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);
        IProductQualityService productQualityService = mock(IProductQualityService.class);

        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Product.class).setProductId(100L);
            return 1;
        });
        when(variantMapper.insert(any(ProductVariant.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ProductVariant.class).setVariantId(200L);
            return 1;
        });
        when(mediaMapper.insert(any(Media.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Media.class).setMediaId(300L);
            return 1;
        });

        ShopifyProductImportServiceImpl service = service(productMapper, variantMapper, mediaMapper,
                productQualityService, mock(IShopifyTaskDetailService.class));

        ShopifyImportedProduct remote = remoteProduct("gid://shopify/Product/9001", "Remote title");
        remote.setSpu("SPU-9001");
        remote.setVariants(List.of(remoteVariant()));
        remote.setMedia(List.of(remoteMedia()));

        ShopifyProductImportServiceImpl.ImportContext context = context();
        ShopifyProductImportServiceImpl.ProductImportItemResult result = service.upsertProduct(remote, context);

        assertTrue(result.isSuccess());
        assertFalse(result.isConflict());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertEquals("Remote title", savedProduct.getProductTitle());
        assertEquals("SPU-9001", savedProduct.getSpu());
        assertEquals("1", savedProduct.getStatus());
        assertEquals("gid://shopify/Product/9001", savedProduct.getShopifyProductId());
        assertEquals(1L, savedProduct.getStoreId());
        assertEquals(remote.getUpdatedAt(), savedProduct.getShopifyUpdatedAt());
        assertTrue(savedProduct.getLastShopifyImportTime() != null);

        ArgumentCaptor<ProductVariant> variantCaptor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantMapper).insert(variantCaptor.capture());
        ProductVariant savedVariant = variantCaptor.getValue();
        assertEquals(100L, savedVariant.getProductId());
        assertEquals(1L, savedVariant.getStoreId());
        assertEquals("SKU-RED", savedVariant.getSku());
        assertEquals(1234, savedVariant.getPrice());
        assertEquals("gid://shopify/InventoryItem/6001", savedVariant.getShopifyInventoryItemId());
        assertTrue(savedVariant.getLastShopifyImportTime() != null);
        assertTrue(savedVariant.getOptionValues().contains("Color"));

        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        verify(mediaMapper).insert(mediaCaptor.capture());
        Media savedMedia = mediaCaptor.getValue();
        assertEquals(100L, savedMedia.getProductId());
        assertEquals(1L, savedMedia.getStoreId());
        assertEquals("gid://shopify/MediaImage/7001", savedMedia.getShopifyMediaId());
        assertEquals("https://cdn.shopify.com/image.jpg", savedMedia.getShopifyMediaUrl());
        assertTrue(savedMedia.getLastShopifyImportTime() != null);
    }

    @Test
    void upsertProductKeepsLocalCoreFieldsWhenErpWasEditedAfterLastShopifyImport() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);
        IShopifyTaskDetailService taskDetailService = mock(IShopifyTaskDetailService.class);

        Product local = new Product();
        local.setProductId(100L);
        local.setStoreId(1L);
        local.setShopifyProductId("gid://shopify/Product/9001");
        local.setProductTitle("Local title");
        local.setUpdateTime(Date.from(Instant.parse("2026-05-13T09:05:00Z")));
        local.setLastShopifyImportTime(Date.from(Instant.parse("2026-05-13T09:00:00Z")));
        when(productMapper.selectOne(any())).thenReturn(local);

        ShopifyProductImportServiceImpl service = service(productMapper, variantMapper, mediaMapper,
                mock(IProductQualityService.class), taskDetailService);

        ShopifyProductImportServiceImpl.ProductImportItemResult result = service.upsertProduct(
                remoteProduct("gid://shopify/Product/9001", "Remote title"), context());

        assertTrue(result.isSuccess());
        assertTrue(result.isConflict());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(productCaptor.capture());
        Product productUpdate = productCaptor.getValue();
        assertEquals(100L, productUpdate.getProductId());
        assertEquals("gid://shopify/Product/9001", productUpdate.getShopifyProductId());
        assertEquals(1L, productUpdate.getStoreId());
        assertEquals(Date.from(Instant.parse("2026-05-13T10:00:00Z")), productUpdate.getShopifyUpdatedAt());
        assertFalse("Remote title".equals(productUpdate.getProductTitle()));

        verify(taskDetailService).insertShopifyTaskDetail(any());
    }

    private ShopifyProductImportServiceImpl service(ProductMapper productMapper,
                                                    ProductVariantMapper variantMapper,
                                                    MediaMapper mediaMapper,
                                                    IProductQualityService productQualityService,
                                                    IShopifyTaskDetailService taskDetailService) {
        ShopifyProductImportServiceImpl service = new ShopifyProductImportServiceImpl();
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "productVariantMapper", variantMapper);
        ReflectionTestUtils.setField(service, "mediaMapper", mediaMapper);
        ReflectionTestUtils.setField(service, "productQualityService", productQualityService);
        ReflectionTestUtils.setField(service, "shopifyTaskDetailService", taskDetailService);
        return service;
    }

    private ShopifyProductImportServiceImpl.ImportContext context() {
        return new ShopifyProductImportServiceImpl.ImportContext(
                900L,
                1L,
                "demo-shop",
                ShopifyTaskType.PRODUCT_IMPORT_INCREMENTAL.getCode()
        );
    }

    private ShopifyImportedProduct remoteProduct(String id, String title) {
        ShopifyImportedProduct product = new ShopifyImportedProduct();
        product.setId(id);
        product.setTitle(title);
        product.setHandle("remote-title");
        product.setDescriptionHtml("<p>Remote body</p>");
        product.setProductType("Chair");
        product.setVendor("Velar");
        product.setTags(List.of("new", "sale"));
        product.setStatus("ACTIVE");
        product.setUpdatedAt(Date.from(Instant.parse("2026-05-13T10:00:00Z")));
        product.setSeo(Map.of("title", "SEO title", "description", "SEO description"));
        return product;
    }

    private ShopifyImportedVariant remoteVariant() {
        ShopifyImportedVariant variant = new ShopifyImportedVariant();
        variant.setId("gid://shopify/ProductVariant/8001");
        variant.setSku("SKU-RED");
        variant.setPrice("12.34");
        variant.setCompareAtPrice("19.99");
        variant.setPosition(1);
        variant.setInventoryItemId("gid://shopify/InventoryItem/6001");
        variant.setSelectedOptions(Map.of("Color", "Red"));
        variant.setMediaIds(List.of("gid://shopify/MediaImage/7001"));
        return variant;
    }

    private ShopifyImportedMedia remoteMedia() {
        ShopifyImportedMedia media = new ShopifyImportedMedia();
        media.setId("gid://shopify/MediaImage/7001");
        media.setOriginalUrl("https://cdn.shopify.com/image.jpg");
        media.setAlt("Red chair");
        media.setMediaContentType("IMAGE");
        media.setPosition(1);
        return media;
    }
}

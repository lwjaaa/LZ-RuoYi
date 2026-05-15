package com.ruoyi.erp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.erp.mapper.MediaMapper;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ProductTagRelMapper;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.mapper.TagDictMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductOption;
import com.ruoyi.erp.model.domain.ProductTagRel;
import com.ruoyi.erp.model.domain.ProductVariantOption;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ShopifySyncCursor;
import com.ruoyi.erp.model.domain.TagDict;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedMedia;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedOption;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedOptionValue;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedVariant;
import com.ruoyi.erp.service.IProductQualityService;
import com.ruoyi.erp.service.IShopifyTaskDetailService;
import com.ruoyi.erp.shopify.enums.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.mockito.Mockito.never;
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
        List<ProductOption> savedOptions = JSON.parseArray(savedProduct.getOptionJson(), ProductOption.class);
        assertEquals("gid://shopify/ProductOption/1", savedOptions.get(0).getOptionId());
        assertEquals("gid://shopify/ProductOptionValue/1", savedOptions.get(0).getValues().get(0).getValueId());

        ArgumentCaptor<ProductVariant> variantCaptor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantMapper).insert(variantCaptor.capture());
        ProductVariant savedVariant = variantCaptor.getValue();
        assertEquals(100L, savedVariant.getProductId());
        assertEquals(1L, savedVariant.getStoreId());
        assertEquals("SKU-RED", savedVariant.getSku());
        assertEquals(1234, savedVariant.getPrice());
        assertEquals("gid://shopify/InventoryItem/6001", savedVariant.getShopifyInventoryItemId());
        assertTrue(savedVariant.getLastShopifyImportTime() != null);
        List<ProductVariantOption> savedVariantOptions = JSON.parseArray(savedVariant.getOptionValues(), ProductVariantOption.class);
        assertEquals("gid://shopify/ProductOption/1", savedVariantOptions.get(0).getOptionId());
        assertEquals("gid://shopify/ProductOptionValue/1", savedVariantOptions.get(0).getValueId());
        assertEquals("Color", savedVariantOptions.get(0).getEnglishName());
        assertEquals("Red", savedVariantOptions.get(0).getEnglishValue());

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
    void upsertProductCreatesMissingShopifyTagsAndRelationsWithoutRemovingLocalTags() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);
        TagDictMapper tagDictMapper = mock(TagDictMapper.class);
        ProductTagRelMapper productTagRelMapper = mock(ProductTagRelMapper.class);

        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Product.class).setProductId(100L);
            return 1;
        });
        when(tagDictMapper.insertTagDict(any(TagDict.class))).thenAnswer(invocation -> {
            TagDict tag = invocation.getArgument(0, TagDict.class);
            tag.setTagId("summer".equals(tag.getTagCode()) ? 10L : 11L);
            return 1;
        });

        ShopifyProductImportServiceImpl service = service(productMapper, variantMapper, mediaMapper,
                tagDictMapper, productTagRelMapper, mock(IProductQualityService.class), mock(IShopifyTaskDetailService.class));

        ShopifyImportedProduct remote = remoteProduct("gid://shopify/Product/9001", "Remote title");
        remote.setTags(List.of(" summer ", "", "summer", "sale"));

        service.upsertProduct(remote, context());

        ArgumentCaptor<TagDict> tagCaptor = ArgumentCaptor.forClass(TagDict.class);
        verify(tagDictMapper, org.mockito.Mockito.times(2)).insertTagDict(tagCaptor.capture());
        List<TagDict> savedTags = tagCaptor.getAllValues();
        assertEquals("summer", savedTags.get(0).getTagCode());
        assertEquals("summer", savedTags.get(0).getTagName());
        assertEquals("OTHER", savedTags.get(0).getTagType());
        assertEquals(0L, savedTags.get(0).getParentId());
        assertEquals("0", savedTags.get(0).getAncestors());
        assertEquals(1, savedTags.get(0).getMenuLevel());
        assertEquals("0", savedTags.get(0).getDelFlag());
        assertEquals("sale", savedTags.get(1).getTagCode());

        ArgumentCaptor<ProductTagRel> relCaptor = ArgumentCaptor.forClass(ProductTagRel.class);
        verify(productTagRelMapper, org.mockito.Mockito.times(2)).insertProductTagRel(relCaptor.capture());
        List<ProductTagRel> savedRels = relCaptor.getAllValues();
        assertEquals(100L, savedRels.get(0).getProductId());
        assertEquals(10L, savedRels.get(0).getTagId());
        assertEquals(100L, savedRels.get(1).getProductId());
        assertEquals(11L, savedRels.get(1).getTagId());
    }

    @Test
    void upsertProductIgnoresEmptyShopifyTags() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);
        TagDictMapper tagDictMapper = mock(TagDictMapper.class);
        ProductTagRelMapper productTagRelMapper = mock(ProductTagRelMapper.class);

        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Product.class).setProductId(100L);
            return 1;
        });

        ShopifyProductImportServiceImpl service = service(productMapper, variantMapper, mediaMapper,
                tagDictMapper, productTagRelMapper, mock(IProductQualityService.class), mock(IShopifyTaskDetailService.class));

        ShopifyImportedProduct remote = remoteProduct("gid://shopify/Product/9001", "Remote title");
        remote.setTags(List.of("", "   "));

        service.upsertProduct(remote, context());

        verify(tagDictMapper, never()).insertTagDict(any(TagDict.class));
        verify(productTagRelMapper, never()).insertProductTagRel(any(ProductTagRel.class));
    }

    @Test
    void upsertProductRecoversWhenTagWasConcurrentlyCreated() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);
        TagDictMapper tagDictMapper = mock(TagDictMapper.class);
        ProductTagRelMapper productTagRelMapper = mock(ProductTagRelMapper.class);

        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Product.class).setProductId(100L);
            return 1;
        });
        when(tagDictMapper.selectOne(any())).thenReturn(null, null, tag("sale", 10L));
        when(tagDictMapper.insertTagDict(any(TagDict.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        ShopifyProductImportServiceImpl service = service(productMapper, variantMapper, mediaMapper,
                tagDictMapper, productTagRelMapper, mock(IProductQualityService.class), mock(IShopifyTaskDetailService.class));

        ShopifyImportedProduct remote = remoteProduct("gid://shopify/Product/9001", "Remote title");
        remote.setTags(List.of("sale"));

        service.upsertProduct(remote, context());

        ArgumentCaptor<ProductTagRel> relCaptor = ArgumentCaptor.forClass(ProductTagRel.class);
        verify(productTagRelMapper).insertProductTagRel(relCaptor.capture());
        assertEquals(100L, relCaptor.getValue().getProductId());
        assertEquals(10L, relCaptor.getValue().getTagId());
    }

    @Test
    void upsertProductReusesExistingTagAndSkipsExistingRelation() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);
        TagDictMapper tagDictMapper = mock(TagDictMapper.class);
        ProductTagRelMapper productTagRelMapper = mock(ProductTagRelMapper.class);
        TagDict existingTag = tag("sale", 10L);
        ProductTagRel existingRel = new ProductTagRel();
        existingRel.setRelId(99L);
        existingRel.setProductId(100L);
        existingRel.setTagId(10L);

        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Product.class).setProductId(100L);
            return 1;
        });
        when(tagDictMapper.selectOne(any())).thenReturn(existingTag);
        when(productTagRelMapper.selectOne(any())).thenReturn(existingRel);

        ShopifyProductImportServiceImpl service = service(productMapper, variantMapper, mediaMapper,
                tagDictMapper, productTagRelMapper, mock(IProductQualityService.class), mock(IShopifyTaskDetailService.class));

        ShopifyImportedProduct remote = remoteProduct("gid://shopify/Product/9001", "Remote title");
        remote.setTags(List.of("sale"));

        service.upsertProduct(remote, context());

        verify(tagDictMapper, never()).insertTagDict(any(TagDict.class));
        verify(productTagRelMapper, never()).insertProductTagRel(any(ProductTagRel.class));
    }

    @Test
    void upsertProductKeepsLocalCoreFieldsWhenErpWasEditedAfterLastShopifyImport() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);
        TagDictMapper tagDictMapper = mock(TagDictMapper.class);
        ProductTagRelMapper productTagRelMapper = mock(ProductTagRelMapper.class);
        IShopifyTaskDetailService taskDetailService = mock(IShopifyTaskDetailService.class);

        Product local = new Product();
        local.setProductId(100L);
        local.setStoreId(1L);
        local.setShopifyProductId("gid://shopify/Product/9001");
        local.setProductTitle("Local title");
        local.setUpdateTime(Date.from(Instant.parse("2026-05-13T09:05:00Z")));
        local.setLastShopifyImportTime(Date.from(Instant.parse("2026-05-13T09:00:00Z")));
        when(productMapper.selectOne(any())).thenReturn(local);
        when(tagDictMapper.selectOne(any())).thenReturn(tag("new", 10L));

        ShopifyProductImportServiceImpl service = service(productMapper, variantMapper, mediaMapper,
                tagDictMapper, productTagRelMapper, mock(IProductQualityService.class), taskDetailService);

        ShopifyImportedProduct remote = remoteProduct("gid://shopify/Product/9001", "Remote title");
        remote.setTags(List.of("new"));
        ShopifyProductImportServiceImpl.ProductImportItemResult result = service.upsertProduct(remote, context());

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
        assertEquals(null, productUpdate.getOptionJson());

        verify(taskDetailService).insertShopifyTaskDetail(any());
        verify(productTagRelMapper).insertProductTagRel(any(ProductTagRel.class));
    }

    @Test
    void upsertProductGeneratesStableLocalOptionIdsWhenShopifyIdsAreMissing() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);

        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Product.class).setProductId(100L);
            return 1;
        });
        when(variantMapper.insert(any(ProductVariant.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ProductVariant.class).setVariantId(200L);
            return 1;
        });

        ShopifyProductImportServiceImpl service = service(productMapper, variantMapper, mediaMapper,
                mock(IProductQualityService.class), mock(IShopifyTaskDetailService.class));

        ShopifyImportedProduct remote = remoteProduct("gid://shopify/Product/9001", "Remote title");
        remote.setOptions(List.of());
        remote.setVariants(List.of(remoteVariant()));

        service.upsertProduct(remote, context());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        List<ProductOption> savedOptions = JSON.parseArray(productCaptor.getValue().getOptionJson(), ProductOption.class);
        assertTrue(savedOptions.get(0).getOptionId().length() > 0);
        assertTrue(savedOptions.get(0).getValues().get(0).getValueId().length() > 0);

        ArgumentCaptor<ProductVariant> variantCaptor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantMapper).insert(variantCaptor.capture());
        List<ProductVariantOption> variantOptions = JSON.parseArray(variantCaptor.getValue().getOptionValues(), ProductVariantOption.class);
        assertEquals(savedOptions.get(0).getOptionId(), variantOptions.get(0).getOptionId());
        assertEquals(savedOptions.get(0).getValues().get(0).getValueId(), variantOptions.get(0).getValueId());
    }

    private ShopifyProductImportServiceImpl service(ProductMapper productMapper,
                                                    ProductVariantMapper variantMapper,
                                                    MediaMapper mediaMapper,
                                                    IProductQualityService productQualityService,
                                                    IShopifyTaskDetailService taskDetailService) {
        TagDictMapper tagDictMapper = mock(TagDictMapper.class);
        ProductTagRelMapper productTagRelMapper = mock(ProductTagRelMapper.class);
        long[] tagId = {10L};
        when(tagDictMapper.insertTagDict(any(TagDict.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, TagDict.class).setTagId(tagId[0]++);
            return 1;
        });
        return service(productMapper, variantMapper, mediaMapper, tagDictMapper,
                productTagRelMapper, productQualityService, taskDetailService);
    }

    private ShopifyProductImportServiceImpl service(ProductMapper productMapper,
                                                    ProductVariantMapper variantMapper,
                                                    MediaMapper mediaMapper,
                                                    TagDictMapper tagDictMapper,
                                                    ProductTagRelMapper productTagRelMapper,
                                                    IProductQualityService productQualityService,
                                                    IShopifyTaskDetailService taskDetailService) {
        ShopifyProductImportServiceImpl service = new ShopifyProductImportServiceImpl();
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "productVariantMapper", variantMapper);
        ReflectionTestUtils.setField(service, "mediaMapper", mediaMapper);
        ReflectionTestUtils.setField(service, "productImportTagService", tagService(tagDictMapper, productTagRelMapper));
        ReflectionTestUtils.setField(service, "productImportOptionService", new ShopifyProductImportOptionService());
        ReflectionTestUtils.setField(service, "productQualityService", productQualityService);
        ReflectionTestUtils.setField(service, "shopifyTaskDetailService", taskDetailService);
        return service;
    }

    private ShopifyProductImportTagService tagService(TagDictMapper tagDictMapper,
                                                      ProductTagRelMapper productTagRelMapper) {
        ShopifyProductImportTagService service = new ShopifyProductImportTagService();
        ReflectionTestUtils.setField(service, "tagDictMapper", tagDictMapper);
        ReflectionTestUtils.setField(service, "productTagRelMapper", productTagRelMapper);
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
        product.setOptions(List.of(remoteOption()));
        return product;
    }

    private TagDict tag(String tagCode, Long tagId) {
        TagDict tag = new TagDict();
        tag.setTagId(tagId);
        tag.setTagCode(tagCode);
        tag.setTagName(tagCode);
        return tag;
    }

    private ShopifyImportedOption remoteOption() {
        ShopifyImportedOption option = new ShopifyImportedOption();
        option.setId("gid://shopify/ProductOption/1");
        option.setName("Color");
        option.setPosition(1);
        ShopifyImportedOptionValue value = new ShopifyImportedOptionValue();
        value.setId("gid://shopify/ProductOptionValue/1");
        value.setName("Red");
        option.setValues(List.of(value));
        return option;
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

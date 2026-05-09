package com.ruoyi.erp.executor;

import com.ruoyi.erp.config.ShopifyGraphQLClient;
import com.ruoyi.erp.mapper.ShopifyStoreMapper;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.service.IProductVariantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopifySyncExecutorTest {

    private ShopifyGraphQLClient shopifyGraphQLClient;
    private IProductVariantService productVariantService;
    private ShopifySyncExecutor executor;

    @BeforeEach
    void setUp() {
        shopifyGraphQLClient = mock(ShopifyGraphQLClient.class);
        productVariantService = mock(IProductVariantService.class);
        ShopifyStoreMapper shopifyStoreMapper = mock(ShopifyStoreMapper.class);

        ShopifyStore store = new ShopifyStore();
        store.setStoreId(1L);
        store.setInventoryTracked("0");
        when(shopifyStoreMapper.selectById(1L)).thenReturn(store);

        executor = new ShopifySyncExecutor();
        ReflectionTestUtils.setField(executor, "shopifyGraphQLClient", shopifyGraphQLClient);
        ReflectionTestUtils.setField(executor, "productVariantService", productVariantService);
        ReflectionTestUtils.setField(executor, "shopifyStoreMapper", shopifyStoreMapper);
        ReflectionTestUtils.setField(executor, "mediaService", mock(IMediaService.class));
    }

    @Test
    void syncVariantsSavesReturnedShopifyVariantIdsToLocalVariants() throws Exception {
        ProductVariant first = buildVariant(101L);
        ProductVariant second = buildVariant(102L);
        when(shopifyGraphQLClient.createVariantsBulk(eq(1L), eq("gid://shopify/Product/2001"), any()))
                .thenReturn(List.of("gid://shopify/ProductVariant/1001", "gid://shopify/ProductVariant/1002"));
        when(productVariantService.updateProductVariant(any())).thenReturn(1);

        StringBuilder result = new StringBuilder();
        invokeSyncVariants(List.of(first, second), result);

        ArgumentCaptor<ProductVariant> captor = forClass(ProductVariant.class);
        verify(productVariantService, times(2)).updateProductVariant(captor.capture());

        List<ProductVariant> updates = captor.getAllValues();
        assertEquals(101L, updates.get(0).getVariantId());
        assertEquals("gid://shopify/ProductVariant/1001", updates.get(0).getShopifyVariantId());
        assertEquals(102L, updates.get(1).getVariantId());
        assertEquals("gid://shopify/ProductVariant/1002", updates.get(1).getShopifyVariantId());
        assertTrue(result.toString().contains("gid://shopify/ProductVariant/1002"));
    }

    @Test
    void syncVariantsDoesNotSilentlySkipDatabaseSaveWhenShopifyReturnsFewerIds() throws Exception {
        ProductVariant first = buildVariant(101L);
        ProductVariant second = buildVariant(102L);
        when(shopifyGraphQLClient.createVariantsBulk(any(), any(), any()))
                .thenReturn(List.of("gid://shopify/ProductVariant/1001"));

        StringBuilder result = new StringBuilder();
        invokeSyncVariants(List.of(first, second), result);

        assertTrue(result.toString().contains("Shopify 返回的变体 ID 数量"));
        verify(productVariantService, never()).updateProductVariant(any());
    }

    private ProductVariant buildVariant(Long variantId) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(variantId);
        variant.setProductId(10L);
        variant.setPrice(1000);
        variant.setOptionValues("[]");
        return variant;
    }

    private void invokeSyncVariants(List<ProductVariant> variants, StringBuilder result) throws Exception {
        Method method = ShopifySyncExecutor.class.getDeclaredMethod(
                "syncVariants", Long.class, String.class, List.class, StringBuilder.class);
        method.setAccessible(true);
        method.invoke(executor, 1L, "gid://shopify/Product/2001", variants, result);
    }
}

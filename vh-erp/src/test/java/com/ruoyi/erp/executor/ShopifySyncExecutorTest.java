package com.ruoyi.erp.executor;

import com.ruoyi.erp.config.ShopifyGraphQLClient;
import com.ruoyi.erp.exception.ShopifyApiException;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ShopifyStoreMapper;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.service.IProductTagRelService;
import com.ruoyi.erp.service.IProductVariantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.Invocation;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopifySyncExecutorTest {

    private ShopifyGraphQLClient shopifyGraphQLClient;
    private IProductVariantService productVariantService;
    private ProductMapper productMapper;
    private List<String> variantBulkResult;
    private ShopifySyncExecutor executor;

    @BeforeEach
    void setUp() {
        variantBulkResult = List.of("gid://shopify/ProductVariant/1001", "gid://shopify/ProductVariant/1002");
        shopifyGraphQLClient = mock(ShopifyGraphQLClient.class, invocation -> {
            if ("createVariantsBulk".equals(invocation.getMethod().getName())) {
                return variantBulkResult;
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        productVariantService = mock(IProductVariantService.class);
        productMapper = mock(ProductMapper.class);
        IProductTagRelService productTagRelService = mock(IProductTagRelService.class);
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
        ReflectionTestUtils.setField(executor, "productMapper", productMapper);
        ReflectionTestUtils.setField(executor, "productTagRelService", productTagRelService);
    }

    @Test
    void syncVariantsSavesReturnedShopifyVariantIdsToLocalVariants() throws Exception {
        ProductVariant first = buildVariant(101L);
        ProductVariant second = buildVariant(102L);
        when(productVariantService.updateProductVariant(any())).thenReturn(1);

        StringBuilder result = new StringBuilder();
        invokeSyncVariants(List.of(first, second), result, null);

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
    void syncVariantsThrowsWhenShopifyReturnsFewerIds() throws Exception {
        ProductVariant first = buildVariant(101L);
        ProductVariant second = buildVariant(102L);
        variantBulkResult = List.of("gid://shopify/ProductVariant/1001");

        StringBuilder result = new StringBuilder();
        ShopifyApiException exception = assertThrows(ShopifyApiException.class,
                () -> invokeSyncVariants(List.of(first, second), result, null));

        assertTrue(exception.getMessage().contains("Shopify 返回的变体 ID 数量"));
        verify(productVariantService, never()).updateProductVariant(any());
    }

    @Test
    void syncProductUsesRemoveStandaloneVariantStrategyForNewShopifyProduct() {
        Product product = buildProduct(null);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(shopifyGraphQLClient.createProduct(eq(1L), any(), any()))
                .thenReturn(new ShopifyGraphQLClient.ProductCreateResult("gid://shopify/Product/2001", List.of()));
        when(productVariantService.selectListByProductId(10L)).thenReturn(List.of(buildVariant(101L), buildVariant(102L)));
        when(productVariantService.updateProductVariant(any())).thenReturn(1);

        executor.syncProduct(1L, 10L, 1, 1);

        Object[] args = singleCreateVariantsBulkArguments();
        assertEquals(4, args.length);
        assertEquals("REMOVE_STANDALONE_VARIANT", args[3]);
    }

    @Test
    void syncProductDoesNotUseRemoveStandaloneVariantStrategyForExistingShopifyProduct() {
        Product product = buildProduct("gid://shopify/Product/2001");
        when(productMapper.selectById(10L)).thenReturn(product);
        when(shopifyGraphQLClient.updateProduct(eq(1L), eq("gid://shopify/Product/2001"), any(), any()))
                .thenReturn(new ShopifyGraphQLClient.ProductCreateResult("gid://shopify/Product/2001", List.of()));
        when(productVariantService.selectListByProductId(10L)).thenReturn(List.of(buildVariant(101L), buildVariant(102L)));
        when(productVariantService.updateProductVariant(any())).thenReturn(1);

        executor.syncProduct(1L, 10L, 1, 1);

        Object[] args = singleCreateVariantsBulkArguments();
        assertEquals(4, args.length);
        assertNull(args[3]);
    }

    private Product buildProduct(String shopifyProductId) {
        Product product = new Product();
        product.setProductId(10L);
        product.setProductTitle("Test Product");
        product.setShopifyProductId(shopifyProductId);
        product.setBodyHtml("<p>body</p>");
        product.setDescription("description");
        return product;
    }

    private ProductVariant buildVariant(Long variantId) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(variantId);
        variant.setProductId(10L);
        variant.setPrice(1000);
        variant.setOptionValues("[]");
        return variant;
    }

    private void invokeSyncVariants(List<ProductVariant> variants, StringBuilder result, String variantCreateStrategy) throws Exception {
        Method method = ShopifySyncExecutor.class.getDeclaredMethod(
                "syncVariants", Long.class, String.class, List.class, StringBuilder.class, String.class);
        method.setAccessible(true);
        try {
            method.invoke(executor, 1L, "gid://shopify/Product/2001", variants, result, variantCreateStrategy);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private Object[] singleCreateVariantsBulkArguments() {
        return mockingDetails(shopifyGraphQLClient).getInvocations().stream()
                .filter(invocation -> "createVariantsBulk".equals(invocation.getMethod().getName()))
                .map(Invocation::getArguments)
                .findFirst()
                .orElseThrow();
    }
}

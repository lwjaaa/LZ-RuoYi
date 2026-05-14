package com.ruoyi.erp.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.erp.shopify.client.ShopifyGraphQLClient;
import com.ruoyi.erp.mapper.ShopifyStoreMapper;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyApiCallRequest;
import com.ruoyi.erp.model.vo.shopifyStore.ShopifyApiCallResponseVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopifyStoreServiceImplApiCallTest {

    private ShopifyStoreMapper shopifyStoreMapper;
    private ShopifyGraphQLClient shopifyGraphQLClient;
    private ShopifyStoreServiceImpl shopifyStoreService;

    @BeforeEach
    void setUp() {
        shopifyStoreMapper = mock(ShopifyStoreMapper.class);
        shopifyGraphQLClient = mock(ShopifyGraphQLClient.class);
        shopifyStoreService = new ShopifyStoreServiceImpl();
        ReflectionTestUtils.setField(shopifyStoreService, "baseMapper", shopifyStoreMapper);
        ReflectionTestUtils.setField(shopifyStoreService, "shopifyGraphQLClient", shopifyGraphQLClient);
    }

    @Test
    void callAdminApiBuildsGraphqlJsonRequestForCurrentStore() {
        ShopifyStore store = store();
        when(shopifyStoreMapper.selectById(10L)).thenReturn(store);
        ShopifyApiCallResponseVo response = new ShopifyApiCallResponseVo();
        response.setStatusCode(200);
        when(shopifyGraphQLClient.callAdminApi(eq(10L), eq("POST"), eq(store.getFullGraphQLUrl()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);

        ShopifyApiCallRequest request = new ShopifyApiCallRequest();
        request.setMethod("POST");
        request.setMode("GRAPHQL");
        request.setUrl(store.getFullGraphQLUrl());
        request.setQuery("{ shop { name } }");
        request.setVariables("{\"first\":1}");

        ShopifyApiCallResponseVo result = shopifyStoreService.callAdminApi(10L, request);

        assertEquals(200, result.getStatusCode());
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(shopifyGraphQLClient).callAdminApi(eq(10L), eq("POST"), eq(store.getFullGraphQLUrl()), bodyCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) bodyCaptor.getValue();
        assertEquals("{ shop { name } }", body.get("query"));
        assertEquals(Map.of("first", 1), body.get("variables"));
    }

    @Test
    void callAdminApiRejectsUrlOutsideCurrentStoreAdminApi() {
        ShopifyStore store = store();
        when(shopifyStoreMapper.selectById(10L)).thenReturn(store);

        ShopifyApiCallRequest request = new ShopifyApiCallRequest();
        request.setMethod("GET");
        request.setMode("JSON");
        request.setUrl("https://other-shop.myshopify.com/admin/api/2026-04/products.json");

        ServiceException exception = assertThrows(ServiceException.class, () -> shopifyStoreService.callAdminApi(10L, request));

        assertEquals("只能调用当前店铺的 Shopify Admin API", exception.getMessage());
        verify(shopifyGraphQLClient, never()).callAdminApi(eq(10L), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    private ShopifyStore store() {
        ShopifyStore store = new ShopifyStore();
        store.setStoreId(10L);
        store.setShopName("velar-home");
        store.setApiVersion("2026-04");
        return store;
    }
}

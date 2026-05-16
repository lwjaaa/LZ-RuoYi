package com.ruoyi.erp.service.impl;

import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.mapper.PurchaseTaskMapper;
import com.ruoyi.erp.mapper.RefundRecordMapper;
import com.ruoyi.erp.mapper.ShopifyOrderLineItemMapper;
import com.ruoyi.erp.mapper.ShopifyOrderMapper;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.PurchaseTask;
import com.ruoyi.erp.model.domain.ShopifyOrder;
import com.ruoyi.erp.model.domain.ShopifyOrderLineItem;
import com.ruoyi.erp.model.domain.ShopifyOrderSyncCursor;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.shopify.model.ShopifyImportedOrder;
import com.ruoyi.erp.shopify.model.ShopifyImportedOrderLineItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopifyOrderServiceImplTest {

    @Test
    void calculateQuerySinceUsesThirtyDayBackfillWhenCursorIsMissing() {
        ShopifyOrderServiceImpl service = new ShopifyOrderServiceImpl();
        Instant now = Instant.parse("2026-05-15T12:00:00Z");

        Date since = service.calculateQuerySince(null, now, true);

        assertEquals(Date.from(now.minus(30, ChronoUnit.DAYS)), since);
    }

    @Test
    void calculateQuerySinceSubtractsFiveMinuteSafetyWindowFromLastSuccessfulCursor() {
        ShopifyOrderServiceImpl service = new ShopifyOrderServiceImpl();
        ShopifyOrderSyncCursor cursor = new ShopifyOrderSyncCursor();
        cursor.setLastSuccessUpdatedAt(Date.from(Instant.parse("2026-05-15T10:15:30Z")));

        Date since = service.calculateQuerySince(cursor, Instant.parse("2026-05-15T12:00:00Z"), false);

        assertEquals(Date.from(Instant.parse("2026-05-15T10:10:30Z")), since);
    }

    @Test
    void upsertOrderLinksLineItemToLocalVariantAndCreatesPurchaseTask() {
        ShopifyOrderMapper orderMapper = mock(ShopifyOrderMapper.class);
        ShopifyOrderLineItemMapper lineItemMapper = mock(ShopifyOrderLineItemMapper.class);
        ProductVariantMapper variantMapper = mock(ProductVariantMapper.class);
        PurchaseTaskMapper purchaseTaskMapper = mock(PurchaseTaskMapper.class);
        RefundRecordMapper refundRecordMapper = mock(RefundRecordMapper.class);

        when(orderMapper.insert(any(ShopifyOrder.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ShopifyOrder.class).setOrderId(500L);
            return 1;
        });
        when(lineItemMapper.insert(any(ShopifyOrderLineItem.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ShopifyOrderLineItem.class).setLineItemId(600L);
            return 1;
        });

        ProductVariant localVariant = new ProductVariant();
        localVariant.setVariantId(20L);
        localVariant.setProductId(10L);
        localVariant.setStoreId(1L);
        localVariant.setShopifyVariantId("gid://shopify/ProductVariant/200");
        localVariant.setSku("SKU-RED");
        localVariant.setPurchaseUrl("https://detail.1688.com/offer/100.html");
        localVariant.setPurchasePrice(1234);
        when(variantMapper.selectOne(any())).thenReturn(localVariant);

        ShopifyOrderServiceImpl service = service(orderMapper, lineItemMapper, variantMapper,
                purchaseTaskMapper, refundRecordMapper);

        ShopifyStore store = new ShopifyStore();
        store.setStoreId(1L);
        store.setShopName("demo-shop");

        ShopifyImportedOrder remote = remoteOrder();
        ShopifyOrderServiceImpl.OrderUpsertResult result = service.upsertOrder(remote, store);

        assertEquals(1, result.getLineItemCount());

        ArgumentCaptor<ShopifyOrderLineItem> lineItemCaptor = ArgumentCaptor.forClass(ShopifyOrderLineItem.class);
        verify(lineItemMapper).insert(lineItemCaptor.capture());
        ShopifyOrderLineItem savedLine = lineItemCaptor.getValue();
        assertEquals(500L, savedLine.getOrderId());
        assertEquals(10L, savedLine.getProductId());
        assertEquals(20L, savedLine.getVariantId());
        assertEquals("SKU-RED", savedLine.getSku());
        assertEquals("https://detail.1688.com/offer/100.html", savedLine.getPurchaseUrl());
        assertEquals(1234, savedLine.getPurchasePrice());
        assertEquals(2468, savedLine.getPurchaseAmount());

        ArgumentCaptor<PurchaseTask> purchaseCaptor = ArgumentCaptor.forClass(PurchaseTask.class);
        verify(purchaseTaskMapper).insert(purchaseCaptor.capture());
        PurchaseTask purchaseTask = purchaseCaptor.getValue();
        assertEquals(500L, purchaseTask.getOrderId());
        assertEquals(600L, purchaseTask.getOrderLineItemId());
        assertEquals("SKU-RED", purchaseTask.getSku());
        assertEquals("PENDING", purchaseTask.getPurchaseStatus());
        assertEquals("https://detail.1688.com/offer/100.html", purchaseTask.getPurchaseUrl());
        assertEquals(2468, purchaseTask.getExpectedPurchaseAmount());
    }

    @Test
    void refreshProfitSubtractsRefundPurchaseAndShippingCosts() {
        ShopifyOrderServiceImpl service = new ShopifyOrderServiceImpl();
        ShopifyOrder order = new ShopifyOrder();
        order.setTotalPrice(10000);
        order.setTotalRefund(1000);
        order.setPurchaseCost(3000);
        order.setShippingCost(1200);

        service.refreshProfit(order);

        assertEquals(4800, order.getGrossProfit());
        assertEquals("0.4800", order.getGrossProfitRate().toPlainString());
    }

    private ShopifyOrderServiceImpl service(ShopifyOrderMapper orderMapper,
                                            ShopifyOrderLineItemMapper lineItemMapper,
                                            ProductVariantMapper variantMapper,
                                            PurchaseTaskMapper purchaseTaskMapper,
                                            RefundRecordMapper refundRecordMapper) {
        ShopifyOrderServiceImpl service = new ShopifyOrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "lineItemMapper", lineItemMapper);
        ReflectionTestUtils.setField(service, "productVariantMapper", variantMapper);
        ReflectionTestUtils.setField(service, "purchaseTaskMapper", purchaseTaskMapper);
        ReflectionTestUtils.setField(service, "refundRecordMapper", refundRecordMapper);
        return service;
    }

    private ShopifyImportedOrder remoteOrder() {
        ShopifyImportedOrder order = new ShopifyImportedOrder();
        order.setId("gid://shopify/Order/1000");
        order.setName("#1000");
        order.setOrderNumber(1000L);
        order.setCurrencyCode("USD");
        order.setFinancialStatus("PAID");
        order.setFulfillmentStatus("UNFULFILLED");
        order.setCreatedAt(Date.from(Instant.parse("2026-05-15T08:00:00Z")));
        order.setUpdatedAt(Date.from(Instant.parse("2026-05-15T08:30:00Z")));
        order.setTotalPrice(5998);
        order.setSubtotalPrice(5998);
        order.setTotalRefund(0);
        order.setLineItems(List.of(remoteLineItem()));
        return order;
    }

    private ShopifyImportedOrderLineItem remoteLineItem() {
        ShopifyImportedOrderLineItem lineItem = new ShopifyImportedOrderLineItem();
        lineItem.setId("gid://shopify/LineItem/300");
        lineItem.setProductId("gid://shopify/Product/100");
        lineItem.setVariantId("gid://shopify/ProductVariant/200");
        lineItem.setTitle("Red Lamp");
        lineItem.setVariantTitle("Red");
        lineItem.setSku("SKU-RED");
        lineItem.setQuantity(2);
        lineItem.setUnitPrice(2999);
        lineItem.setTotalPrice(5998);
        return lineItem;
    }
}

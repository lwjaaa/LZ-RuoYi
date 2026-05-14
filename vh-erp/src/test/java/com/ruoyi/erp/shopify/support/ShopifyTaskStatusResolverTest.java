package com.ruoyi.erp.shopify.support;

import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.shopify.enums.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopifyTaskStatusResolverTest {

    @Test
    void resolveTaskStatusReturnsPartialSuccessWhenAnyProductIsPartial() {
        assertEquals(ShopifyTaskStatus.PART_SUCCESS.getCode(),
                ShopifyTaskStatusResolver.resolveTaskStatus(3, 2, 1, 0));
    }

    @Test
    void resolveTaskStatusReturnsPartialSuccessWhenSuccessAndFailureAreMixed() {
        assertEquals(ShopifyTaskStatus.PART_SUCCESS.getCode(),
                ShopifyTaskStatusResolver.resolveTaskStatus(3, 1, 0, 2));
    }

    @Test
    void resolveTaskStatusReturnsFailedWhenNothingSucceededOrPartiallySucceeded() {
        assertEquals(ShopifyTaskStatus.FAILED.getCode(),
                ShopifyTaskStatusResolver.resolveTaskStatus(2, 0, 0, 2));
    }

    @Test
    void resolveProductSyncStatusReturnsPartialWhenProductCreatedButChildItemsFailed() {
        assertEquals(ProductConstants.SYNC_STATUS_PART_SUCCESS,
                ShopifyTaskStatusResolver.resolveProductSyncStatus(true, true));
    }
}

package com.ruoyi.erp.shopify.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopifyEnumsTest {

    @Test
    void taskEnumsResolveKnownCodesAndFallbackForUnknownCodes() {
        assertEquals(ShopifyTaskType.PRODUCT_SYNC_BATCH, ShopifyTaskType.fromCode("PRODUCT_SYNC_BATCH"));
        assertEquals(ShopifyTaskType.UNKNOWN, ShopifyTaskType.fromCode("NOT_EXISTS"));
        assertEquals(ShopifyTaskStatus.RUNNING, ShopifyTaskStatus.fromCode("RUNNING"));
        assertEquals(ShopifyTaskStatus.UNKNOWN, ShopifyTaskStatus.fromCode("NOT_EXISTS"));
        assertEquals(ShopifyTaskDetailStatus.SKIPPED, ShopifyTaskDetailStatus.fromCode("SKIPPED"));
        assertEquals(ShopifyTaskDetailStatus.UNKNOWN, ShopifyTaskDetailStatus.fromCode("NOT_EXISTS"));
    }

    @Test
    void taskDiagnosticEnumsKeepDatabaseCodesStable() {
        assertEquals("VARIANT", ShopifyTaskDetailItemType.VARIANT.getCode());
        assertEquals("VARIANT_CREATE", ShopifyTaskStep.VARIANT_CREATE.getCode());
        assertEquals("ERP_PRIORITY_CONFLICT", ShopifyTaskErrorCode.ERP_PRIORITY_CONFLICT.getCode());
        assertEquals("PRODUCT_IMPORT", ShopifySyncCursorMode.PRODUCT_IMPORT.getCode());
        assertEquals("PART_SUCCESS", ShopifySyncCursorStatus.PART_SUCCESS.getCode());
    }

    @Test
    void productStatusMapsShopifyNamesToLocalCodes() {
        assertEquals("1", ShopifyProductStatus.fromName("ACTIVE").getCode());
        assertEquals("0", ShopifyProductStatus.fromName("unknown").getCode());
        assertEquals(ShopifyProductStatus.ARCHIVED, ShopifyProductStatus.fromCode("2"));
    }
}

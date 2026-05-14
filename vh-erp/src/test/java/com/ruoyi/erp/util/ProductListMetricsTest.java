package com.ruoyi.erp.util;

import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.vo.product.ProductVo;
import com.ruoyi.erp.shopify.enums.ShopifyTaskStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductListMetricsTest {

    @Test
    void applyBuildsVariantPriceMediaAndResyncMetrics() {
        Product product = product(100L);
        product.setShopifyProductId("gid://shopify/Product/100");
        product.setSyncStatus("1");
        product.setLastSyncTime(date(1_000));
        product.setUpdateTime(date(900));
        product.setMainMediaId(10L);

        ProductVariant first = variant(1L, 12_900, 4_500, 12);
        first.setSku("CHAIR-001");
        first.setProfitRate(new BigDecimal("0.31"));
        first.setUpdateTime(date(1_500));
        ProductVariant second = variant(2L, 8_900, 3_200, 18);
        second.setSku("CHAIR-002");
        second.setProfitRate(new BigDecimal("0.22"));

        Media mainMedia = media(10L, "/profile/chair/main.jpg");
        Media detailMedia = media(11L, "/profile/chair/detail.jpg");
        ShopifyTask task = new ShopifyTask();
        task.setTaskId(300L);
        task.setTaskStatus(ShopifyTaskStatus.FAILED.getCode());
        task.setErrorMessage("media upload failed");

        ProductVo vo = ProductVo.objToVo(product);
        ProductListMetrics.apply(vo, product, List.of(first, second), List.of(mainMedia, detailMedia), task);

        assertEquals(2, vo.getVariantCount());
        assertEquals(2, vo.getMediaCount());
        assertEquals("/profile/chair/main.jpg", vo.getMainMediaUrl());
        assertEquals("CHAIR-001", vo.getSkuPreview());
        assertEquals(8_900, vo.getPriceMin());
        assertEquals(12_900, vo.getPriceMax());
        assertEquals(3_200, vo.getPurchasePriceMin());
        assertEquals(4_500, vo.getPurchasePriceMax());
        assertEquals(new BigDecimal("0.22"), vo.getProfitRateMin());
        assertEquals(new BigDecimal("0.31"), vo.getProfitRateMax());
        assertTrue(vo.getNeedResync());
        assertEquals(300L, vo.getLatestTaskId());
        assertEquals(ShopifyTaskStatus.FAILED.getCode(), vo.getLatestTaskStatus());
        assertEquals("media upload failed", vo.getLatestTaskError());
    }

    @Test
    void applyReportsMissingPublishInputsForProductMaintenance() {
        Product product = product(101L);
        product.setProductTitle("");
        product.setBodyHtml("");
        product.setProductType("");

        ProductVariant variant = variant(1L, null, null, null);
        variant.setSku("");

        ProductVo vo = ProductVo.objToVo(product);
        ProductListMetrics.apply(vo, product, List.of(variant), List.of(), null);

        assertTrue(vo.getMissingFields().contains("TITLE"));
        assertTrue(vo.getMissingFields().contains("MAIN_MEDIA"));
        assertTrue(vo.getMissingFields().contains("DESCRIPTION"));
        assertTrue(vo.getMissingFields().contains("PRODUCT_TYPE"));
        assertTrue(vo.getMissingFields().contains("SKU"));
        assertTrue(vo.getMissingFields().contains("PRICE"));
        assertTrue(vo.getMissingFields().contains("FREIGHT"));
    }

    private Product product(Long productId) {
        Product product = new Product();
        product.setProductId(productId);
        product.setProductTitle("Modern Chair");
        product.setProductType("Chair");
        product.setBodyHtml("<p>Comfortable chair</p>");
        return product;
    }

    private ProductVariant variant(Long variantId, Integer price, Integer purchasePrice, Integer freight) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(variantId);
        variant.setPrice(price);
        variant.setPurchasePrice(purchasePrice);
        variant.setFreight(freight);
        variant.setIsActiveAvailable("1");
        return variant;
    }

    private Media media(Long mediaId, String url) {
        Media media = new Media();
        media.setMediaId(mediaId);
        media.setNasMediaUrl(url);
        media.setMediaContentType("IMAGE");
        return media;
    }

    private Date date(long millis) {
        return new Date(millis);
    }
}

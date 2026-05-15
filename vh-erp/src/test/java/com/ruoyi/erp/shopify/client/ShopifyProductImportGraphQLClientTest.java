package com.ruoyi.erp.shopify.client;

import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedMedia;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedVariant;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShopifyProductImportGraphQLClientTest {

    /**
     * 验证 Shopify Bulk JSONL 拆成父子行时，商品、变体、媒体和变体媒体引用会被重新组装。
     */
    @Test
    void parseLocalJsonlProductsGroupsBulkChildRowsUnderProducts() throws IOException {
        Path jsonl = Path.of("target", "shopify-product-import-test", "shopify-products.jsonl");
        Files.createDirectories(jsonl.getParent());
        Files.writeString(jsonl, String.join(System.lineSeparator(),
                "{\"__typename\":\"Product\",\"id\":\"gid://shopify/Product/10136228659546\",\"title\":\"Wave Ear Glass Cup\",\"handle\":\"wave-ear-glass-cup\",\"descriptionHtml\":\"<p>Wave</p>\",\"productType\":\"\",\"vendor\":\"VELART HOME\",\"tags\":[\"CUPS\",\"GLASS\"],\"status\":\"ACTIVE\",\"updatedAt\":\"2026-04-20T21:51:09Z\",\"seo\":{\"title\":\"Wave Ear Glass Cup - Velart Home\",\"description\":\"Wave description\"},\"metafield\":{\"value\":\"ACUGL004\"}}",
                "{\"id\":\"gid://shopify/ProductVariant/50913751892314\",\"sku\":\"ACUGL004-PU\",\"price\":\"23.00\",\"compareAtPrice\":null,\"position\":1,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941660356954\",\"sku\":\"ACUGL004-PU\"},\"selectedOptions\":[{\"name\":\"STYLE\",\"value\":\"Purple-Green\"}],\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667357102426\",\"__parentId\":\"gid://shopify/ProductVariant/50913751892314\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913751925082\",\"sku\":\"ACUGL004-BR\",\"price\":\"23.00\",\"compareAtPrice\":null,\"position\":2,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941660389722\",\"sku\":\"ACUGL004-BR\"},\"selectedOptions\":[{\"name\":\"STYLE\",\"value\":\"Brown-Pink\"}],\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667356971354\",\"__parentId\":\"gid://shopify/ProductVariant/50913751925082\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913751957850\",\"sku\":\"ACUGL004-PK\",\"price\":\"23.00\",\"compareAtPrice\":null,\"position\":3,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941660422490\",\"sku\":\"ACUGL004-PK\"},\"selectedOptions\":[{\"name\":\"STYLE\",\"value\":\"Pink-Brown\"}],\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667357135194\",\"__parentId\":\"gid://shopify/ProductVariant/50913751957850\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667356905818\",\"alt\":\"Wave Ear Glass Cup - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-0.jpg\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-0.jpg\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUGL004-0.jpg\"},\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667356938586\",\"alt\":\"Wave Ear Glass Cup - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-1.jpg\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-1.jpg\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUGL004-1.jpg\"},\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667357004122\",\"alt\":\"Wave Ear Glass Cup - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-2.jpg\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-2.jpg\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUGL004-2.jpg\"},\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667357036890\",\"alt\":\"Wave Ear Glass Cup - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-3.jpg\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-3.jpg\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUGL004-3.jpg\"},\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667357069658\",\"alt\":\"Wave Ear Glass Cup - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-4.jpg\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-4.jpg\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUGL004-4.jpg\"},\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667357102426\",\"alt\":\"Wave Ear Glass Cup - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-5.jpg\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-5.jpg\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUGL004-5.jpg\"},\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667356971354\",\"alt\":\"Wave Ear Glass Cup - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-11.jpg\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-11.jpg\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUGL004-11.jpg\"},\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"id\":\"gid://shopify/MediaImage/52667357135194\",\"alt\":\"Wave Ear Glass Cup - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-7.jpg\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUGL004-7.jpg\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUGL004-7.jpg\"},\"__parentId\":\"gid://shopify/Product/10136228659546\"}",
                "{\"__typename\":\"Product\",\"id\":\"gid://shopify/Product/10136230560090\",\"title\":\"Glorious Mugs\",\"handle\":\"glorious-mugs\",\"descriptionHtml\":\"<p>Glorious</p>\",\"productType\":\"\",\"vendor\":\"VELART HOME\",\"tags\":[\"CUPS\",\"MUG\"],\"status\":\"ACTIVE\",\"updatedAt\":\"2026-04-20T21:51:10Z\",\"seo\":{\"title\":\"Glorious Mugs - Velart Home\",\"description\":\"Glorious description\"},\"metafield\":{\"value\":\"ACUMU001\"}}",
                "{\"id\":\"gid://shopify/ProductVariant/50913767293274\",\"sku\":\"ACUMU001-BL-S\",\"price\":\"39.00\",\"compareAtPrice\":null,\"position\":1,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941675200858\",\"sku\":\"ACUMU001-BL-S\"},\"selectedOptions\":[{\"name\":\"COLOR\",\"value\":\"Blue\"},{\"name\":\"STYLE\",\"value\":\"Stripes\"}],\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094573402\",\"__parentId\":\"gid://shopify/ProductVariant/50913767293274\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913767326042\",\"sku\":\"ACUMU001-BL-D\",\"price\":\"39.00\",\"compareAtPrice\":null,\"position\":2,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941675233626\",\"sku\":\"ACUMU001-BL-D\"},\"selectedOptions\":[{\"name\":\"COLOR\",\"value\":\"Blue\"},{\"name\":\"STYLE\",\"value\":\"Diamonds\"}],\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094540634\",\"__parentId\":\"gid://shopify/ProductVariant/50913767326042\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913767358810\",\"sku\":\"ACUMU001-BL-A\",\"price\":\"39.00\",\"compareAtPrice\":null,\"position\":3,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941675266394\",\"sku\":\"ACUMU001-BL-A\"},\"selectedOptions\":[{\"name\":\"COLOR\",\"value\":\"Blue\"},{\"name\":\"STYLE\",\"value\":\"Arcs\"}],\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094409562\",\"__parentId\":\"gid://shopify/ProductVariant/50913767358810\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913767391578\",\"sku\":\"ACUMU001-BL-C\",\"price\":\"39.00\",\"compareAtPrice\":null,\"position\":4,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941675299162\",\"sku\":\"ACUMU001-BL-C\"},\"selectedOptions\":[{\"name\":\"COLOR\",\"value\":\"Blue\"},{\"name\":\"STYLE\",\"value\":\"Circle\"}],\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/59067739308378\",\"__parentId\":\"gid://shopify/ProductVariant/50913767391578\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913767424346\",\"sku\":\"ACUMU001-PK-S\",\"price\":\"39.00\",\"compareAtPrice\":null,\"position\":5,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941675331930\",\"sku\":\"ACUMU001-PK-S\"},\"selectedOptions\":[{\"name\":\"COLOR\",\"value\":\"Pink\"},{\"name\":\"STYLE\",\"value\":\"Stripes\"}],\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094507866\",\"__parentId\":\"gid://shopify/ProductVariant/50913767424346\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913767457114\",\"sku\":\"ACUMU001-PK-D\",\"price\":\"39.00\",\"compareAtPrice\":null,\"position\":6,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941675364698\",\"sku\":\"ACUMU001-PK-D\"},\"selectedOptions\":[{\"name\":\"COLOR\",\"value\":\"Pink\"},{\"name\":\"STYLE\",\"value\":\"Diamonds\"}],\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094442330\",\"__parentId\":\"gid://shopify/ProductVariant/50913767457114\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913767489882\",\"sku\":\"ACUMU001-PK-A\",\"price\":\"39.00\",\"compareAtPrice\":null,\"position\":7,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941675397466\",\"sku\":\"ACUMU001-PK-A\"},\"selectedOptions\":[{\"name\":\"COLOR\",\"value\":\"Pink\"},{\"name\":\"STYLE\",\"value\":\"Arcs\"}],\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094475098\",\"__parentId\":\"gid://shopify/ProductVariant/50913767489882\"}",
                "{\"id\":\"gid://shopify/ProductVariant/50913767522650\",\"sku\":\"ACUMU001-PK-C\",\"price\":\"39.00\",\"compareAtPrice\":null,\"position\":8,\"inventoryItem\":{\"id\":\"gid://shopify/InventoryItem/52941675430234\",\"sku\":\"ACUMU001-PK-C\"},\"selectedOptions\":[{\"name\":\"COLOR\",\"value\":\"Pink\"},{\"name\":\"STYLE\",\"value\":\"Circle\"}],\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/59067739079002\",\"__parentId\":\"gid://shopify/ProductVariant/50913767522650\"}",
                "{\"id\":\"gid://shopify/MediaImage/59067739341146\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-0.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-0.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-0.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/59067739406682\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-7.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-7.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-7.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094573402\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-1.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-1.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-1.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094540634\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-3.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-3.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-3.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094409562\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-2.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-2.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-2.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/59067739308378\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-8.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-8.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-8.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094507866\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-4.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-4.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-4.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094442330\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-5.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-5.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-5.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/58411094475098\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-6.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-6.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-6.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}",
                "{\"id\":\"gid://shopify/MediaImage/59067739079002\",\"alt\":\"Glorious Mugs - Velart Home\",\"mediaContentType\":\"IMAGE\",\"preview\":{\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-9.webp\"}},\"image\":{\"url\":\"https://cdn.shopify.com/files/ACUMU001-9.webp\",\"originalSrc\":\"https://cdn.shopify.com/files/ACUMU001-9.webp\"},\"__parentId\":\"gid://shopify/Product/10136230560090\"}"
        ), StandardCharsets.UTF_8);

        ShopifyProductImportGraphQLClient client = new ShopifyProductImportGraphQLClient();
        List<ShopifyImportedProduct> products = client.parseLocalJsonlProducts(jsonl.toString());

        assertEquals(2, products.size());
        ShopifyImportedProduct waveCup = products.get(0);
        assertEquals("Wave Ear Glass Cup", waveCup.getTitle());
        assertEquals("ACUGL004", waveCup.getSpu());
        assertEquals(List.of("CUPS", "GLASS"), waveCup.getTags());
        assertEquals(3, waveCup.getVariants().size());
        assertEquals(8, waveCup.getMedia().size());

        ShopifyImportedVariant firstWaveVariant = waveCup.getVariants().get(0);
        assertEquals("gid://shopify/ProductVariant/50913751892314", firstWaveVariant.getId());
        assertEquals("ACUGL004-PU", firstWaveVariant.getSku());
        assertEquals("23.00", firstWaveVariant.getPrice());
        assertEquals(1, firstWaveVariant.getPosition());
        assertEquals("gid://shopify/InventoryItem/52941660356954", firstWaveVariant.getInventoryItemId());
        assertEquals("Purple-Green", firstWaveVariant.getSelectedOptions().get("STYLE"));
        assertEquals(List.of("gid://shopify/MediaImage/52667357102426"), firstWaveVariant.getMediaIds());
        assertEquals(List.of("gid://shopify/MediaImage/52667356971354"), waveCup.getVariants().get(1).getMediaIds());
        assertEquals(List.of("gid://shopify/MediaImage/52667357135194"), waveCup.getVariants().get(2).getMediaIds());

        ShopifyImportedMedia firstWaveMedia = waveCup.getMedia().get(0);
        assertEquals("gid://shopify/MediaImage/52667356905818", firstWaveMedia.getId());
        assertEquals("https://cdn.shopify.com/files/ACUGL004-0.jpg", firstWaveMedia.getOriginalUrl());
        assertEquals(1, firstWaveMedia.getPosition());
        assertEquals(8, waveCup.getMedia().get(7).getPosition());

        ShopifyImportedProduct gloriousMugs = products.get(1);
        assertEquals("Glorious Mugs", gloriousMugs.getTitle());
        assertEquals("ACUMU001", gloriousMugs.getSpu());
        assertEquals(8, gloriousMugs.getVariants().size());
        assertEquals(10, gloriousMugs.getMedia().size());
        ShopifyImportedVariant firstMugVariant = gloriousMugs.getVariants().get(0);
        assertEquals("Blue", firstMugVariant.getSelectedOptions().get("COLOR"));
        assertEquals("Stripes", firstMugVariant.getSelectedOptions().get("STYLE"));
        assertEquals(List.of("gid://shopify/MediaImage/58411094573402"), firstMugVariant.getMediaIds());
        assertEquals("https://cdn.shopify.com/files/ACUMU001-9.webp",
                gloriousMugs.getMedia().get(9).getOriginalUrl());
    }

    /**
     * 验证 Bulk JSONL 下载在首次连接被远端断开时会重新建立连接并解析成功。
     */
    @Test
    void downloadBulkJsonlProductsWithAgentRetriesAfterConnectionReset() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = startBulkJsonlServer(exchange -> {
            if (requestCount.incrementAndGet() == 1) {
                exchange.close();
                return;
            }
            writeBulkJsonlResponse(exchange, 200, sampleProductJsonl());
        });

        try {
            ShopifyProductImportGraphQLClient client = new ShopifyProductImportGraphQLClient();
            List<ShopifyImportedProduct> products = client.downloadBulkJsonlProductsWithAgent(bulkJsonlUrl(server));

            assertEquals(1, products.size());
            assertEquals("Retry Product", products.get(0).getTitle());
            assertEquals(2, requestCount.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证 Bulk JSONL 下载遇到临时 HTTP 503 时会按策略重试。
     */
    @Test
    void downloadBulkJsonlProductsWithAgentRetriesRetryableHttpStatus() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = startBulkJsonlServer(exchange -> {
            if (requestCount.incrementAndGet() == 1) {
                writeBulkJsonlResponse(exchange, 503, "");
                return;
            }
            writeBulkJsonlResponse(exchange, 200, sampleProductJsonl());
        });

        try {
            ShopifyProductImportGraphQLClient client = new ShopifyProductImportGraphQLClient();
            List<ShopifyImportedProduct> products = client.downloadBulkJsonlProductsWithAgent(bulkJsonlUrl(server));

            assertEquals(1, products.size());
            assertEquals(2, requestCount.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证 Bulk JSONL 下载遇到明确不存在的地址时不做无意义重试。
     */
    @Test
    void downloadBulkJsonlProductsWithAgentDoesNotRetryNotFound() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = startBulkJsonlServer(exchange -> {
            requestCount.incrementAndGet();
            writeBulkJsonlResponse(exchange, 404, "");
        });

        try {
            ShopifyProductImportGraphQLClient client = new ShopifyProductImportGraphQLClient();

            assertThrows(ShopifyApiException.class,
                    () -> client.downloadBulkJsonlProductsWithAgent(bulkJsonlUrl(server)));
            assertEquals(1, requestCount.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证 Bulk JSONL 内容格式错误时直接暴露解析失败，不按网络异常重试。
     */
    @Test
    void downloadBulkJsonlProductsWithAgentDoesNotRetryMalformedJsonl() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = startBulkJsonlServer(exchange -> {
            requestCount.incrementAndGet();
            writeBulkJsonlResponse(exchange, 200, "{bad-json");
        });

        try {
            ShopifyProductImportGraphQLClient client = new ShopifyProductImportGraphQLClient();

            assertThrows(ShopifyApiException.class,
                    () -> client.downloadBulkJsonlProductsWithAgent(bulkJsonlUrl(server)));
            assertEquals(1, requestCount.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证分页接口返回的嵌套 connection 结构仍沿用原解析逻辑。
     */
    @Test
    void parseImportedProductStillSupportsNestedConnectionNodes() throws Exception {
        String productJson = """
                {
                  "id": "gid://shopify/Product/9001",
                  "title": "Nested Product",
                  "handle": "nested-product",
                  "descriptionHtml": "<p>Nested</p>",
                  "productType": "Cup",
                  "vendor": "VELART HOME",
                  "tags": ["CUPS"],
                  "status": "ACTIVE",
                  "updatedAt": "2026-04-20T21:51:09Z",
                  "seo": {"title": "SEO title", "description": "SEO description"},
                  "metafield": {"value": "SPU-9001"},
                  "options": [{
                    "id": "gid://shopify/ProductOption/1",
                    "name": "Color",
                    "position": 1,
                    "optionValues": [{"id": "gid://shopify/ProductOptionValue/1", "name": "Red"}]
                  }],
                  "variants": {
                    "edges": [{
                      "node": {
                        "id": "gid://shopify/ProductVariant/8001",
                        "sku": "SKU-RED",
                        "price": "12.34",
                        "compareAtPrice": "19.99",
                        "position": 1,
                        "inventoryItem": {"id": "gid://shopify/InventoryItem/6001", "sku": "SKU-RED"},
                        "selectedOptions": [{"name": "Color", "value": "Red"}],
                        "media": {"edges": [{"node": {"id": "gid://shopify/MediaImage/7001"}}]}
                      }
                    }]
                  },
                  "media": {
                    "edges": [{
                      "node": {
                        "id": "gid://shopify/MediaImage/7001",
                        "alt": "Red cup",
                        "mediaContentType": "IMAGE",
                        "preview": {"image": {"url": "https://cdn.shopify.com/preview.jpg"}},
                        "image": {"url": "https://cdn.shopify.com/image.jpg", "originalSrc": "https://cdn.shopify.com/original.jpg"}
                      }
                    }]
                  }
                }
                """;

        ShopifyProductImportGraphQLClient client = new ShopifyProductImportGraphQLClient();
        Method readTree = client.getClass().getDeclaredMethod("parseImportedProduct",
                com.fasterxml.jackson.databind.JsonNode.class);
        readTree.setAccessible(true);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        ShopifyImportedProduct product = (ShopifyImportedProduct) readTree.invoke(client, mapper.readTree(productJson));

        assertEquals("Nested Product", product.getTitle());
        assertEquals(1, product.getOptions().size());
        assertEquals("gid://shopify/ProductOption/1", product.getOptions().get(0).getId());
        assertEquals("Color", product.getOptions().get(0).getName());
        assertEquals("gid://shopify/ProductOptionValue/1", product.getOptions().get(0).getValues().get(0).getId());
        assertEquals("Red", product.getOptions().get(0).getValues().get(0).getName());
        assertEquals(1, product.getVariants().size());
        assertEquals("SKU-RED", product.getVariants().get(0).getSku());
        assertEquals(List.of("gid://shopify/MediaImage/7001"), product.getVariants().get(0).getMediaIds());
        assertEquals(1, product.getMedia().size());
        assertEquals("https://cdn.shopify.com/original.jpg", product.getMedia().get(0).getOriginalUrl());
    }

    /**
     * 启动本地 Bulk JSONL 测试服务，避免单测依赖真实 Shopify 网络。
     */
    private HttpServer startBulkJsonlServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bulk.jsonl", handler);
        server.start();
        return server;
    }

    /**
     * 生成带查询参数的本地 Bulk JSONL URL，用于覆盖签名 URL 脱敏场景。
     */
    private String bulkJsonlUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/bulk.jsonl?signature=secret";
    }

    /**
     * 写出 Bulk JSONL 测试响应。
     */
    private void writeBulkJsonlResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/jsonl; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /**
     * 返回最小可解析的 Shopify 商品 JSONL 内容。
     */
    private String sampleProductJsonl() {
        return "{\"__typename\":\"Product\",\"id\":\"gid://shopify/Product/1\",\"title\":\"Retry Product\"}";
    }
}

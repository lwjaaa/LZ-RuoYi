package com.ruoyi.erp.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedMedia;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedVariant;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.model.BulkOperationInfo;
import com.ruoyi.erp.shopify.model.ProductPage;
import com.ruoyi.erp.shopify.query.ShopifyGraphQLQueries;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shopify 商品反向导入 GraphQL 客户端。
 */
@Slf4j
@Component
public class ShopifyProductImportGraphQLClient {
    private static final String PATH_ID = "id";
    private static final String PATH_MEDIA = "media";
    private static final String PATH_NAME = "name";
    private static final String PATH_VALUE = "value";
    private static final String PATH_URL = "url";
    private static final String MUTATION_BULK_OPERATION_RUN_QUERY = "bulkOperationRunQuery";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ShopifyGraphQLTransport transport;
    // ==================== Shopify 商品反向导入查询 ====================

    /**
     * 按 Shopify updated_at 增量分页拉取商品。
     */
    public ProductPage queryUpdatedProducts(Long storeId, Date since, String cursor, int first) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("first", first);
        variables.put("after", cursor);
        variables.put("query", since == null ? null : "updated_at:>" + DateTimeFormatter.ISO_INSTANT.format(since.toInstant()));

        log.info("查询 Shopify 增量商品分页，storeId={}, since={}, cursor={}, first={}", storeId, since, cursor, first);
        JsonNode data = transport.execute(storeId, ShopifyGraphQLQueries.PRODUCT_IMPORT_PAGE.getQuery(), variables);
        JsonNode productsNode = data.path("products");
        List<ShopifyImportedProduct> products = new ArrayList<>();
        for (JsonNode edge : productsNode.path("edges")) {
            products.add(parseImportedProduct(edge.path("node")));
        }
        JsonNode pageInfo = productsNode.path("pageInfo");
        log.info("Shopify 增量商品分页解析完成，storeId={}, count={}, hasNextPage={}",
                storeId, products.size(), pageInfo.path("hasNextPage").asBoolean(false));
        return ProductPage.builder()
                .products(products)
                .hasNextPage(pageInfo.path("hasNextPage").asBoolean(false))
                .endCursor(pageInfo.path("endCursor").asText(null))
                .build();
    }

    /**
     * 创建商品全量导入 Bulk Operation，只负责启动和返回 operation ID。
     */
    public BulkOperationInfo startProductBulkImport(Long storeId) {
        log.info("创建 Shopify 商品全量导入 Bulk Operation，storeId={}", storeId);
        JsonNode data = transport.execute(storeId, ShopifyGraphQLQueries.BULK_OPERATION_RUN_QUERY.getQuery(), Map.of(
                "query", ShopifyGraphQLQueries.BULK_PRODUCT_IMPORT.getQuery(),
                "groupObjects", true
        ));
        transport.checkUserErrors(data, MUTATION_BULK_OPERATION_RUN_QUERY);
        JsonNode operation = data.path(MUTATION_BULK_OPERATION_RUN_QUERY).path("bulkOperation");
        BulkOperationInfo info = parseBulkOperation(operation);
        log.info("Shopify 商品全量导入 Bulk Operation 创建成功，storeId={}, operationId={}, status={}",
                storeId, info.getId(), info.getStatus());
        return info;
    }

    public BulkOperationInfo pollBulkOperation(Long storeId, String bulkOperationId) {
        JsonNode data = transport.execute(storeId, ShopifyGraphQLQueries.BULK_OPERATION_STATUS.getQuery(), Map.of("id", bulkOperationId));
        JsonNode node = data.path("node");
        if (node.isMissingNode() || node.isNull()) {
            throw new ShopifyApiException("Shopify Bulk Operation 不存在: " + bulkOperationId);
        }
        BulkOperationInfo info = parseBulkOperation(node);
        log.debug("轮询 Shopify Bulk Operation，storeId={}, operationId={}, status={}, objectCount={}",
                storeId, bulkOperationId, info.getStatus(), info.getObjectCount());
        return info;
    }

    /**
     * 轮询 Bulk Operation 直到终态，避免上层服务散落状态判断。
     */
    public BulkOperationInfo waitForBulkOperation(Long storeId, String bulkOperationId, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        BulkOperationInfo operation;
        do {
            operation = pollBulkOperation(storeId, bulkOperationId);
            String status = operation.getStatus();
            if ("COMPLETED".equalsIgnoreCase(status)
                    || "FAILED".equalsIgnoreCase(status)
                    || "CANCELED".equalsIgnoreCase(status)
                    || "CANCELLED".equalsIgnoreCase(status)
                    || "EXPIRED".equalsIgnoreCase(status)) {
                log.info("Shopify Bulk Operation 到达终态，storeId={}, operationId={}, status={}, objectCount={}, urlPresent={}",
                        storeId, bulkOperationId, status, operation.getObjectCount(), StringUtils.isNotEmpty(operation.getUrl()));
                return operation;
            }
            try {
                Thread.sleep(5_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ShopifyApiException("等待 Shopify Bulk Operation 被中断", e);
            }
        } while (System.currentTimeMillis() < deadline);
        throw new ShopifyApiException("等待 Shopify Bulk Operation 超时: " + bulkOperationId);
    }

    /**
     * 下载并解析 Bulk JSONL。当前只处理 Product 根对象，子对象依赖 groupObjects=true 聚合在商品节点内。
     */
    public List<ShopifyImportedProduct> downloadBulkJsonlProducts(String url) {
        List<ShopifyImportedProduct> products = new ArrayList<>();
        log.info("开始下载 Shopify Bulk JSONL 商品文件，url={}", url);
        try (InputStream inputStream = URI.create(url).toURL().openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            log.info("开始解析 Shopify Bulk JSONL 商品文件");
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                if (isProductJsonlNode(node)) {
                    ShopifyImportedProduct product = parseImportedProduct(node);
                    products.add(product);
                    log.debug("成功解析商品, title={}", product.getTitle());
                }
            }
            log.info("Shopify Bulk JSONL 商品文件解析完成，productCount={}", products.size());
            return products;
        } catch (Exception e) {
            throw new ShopifyApiException("下载 Shopify Bulk JSONL 失败: " + e.getMessage(), e);
        }

    }

    private boolean isProductJsonlNode(JsonNode node) {
        String typename = node.path("__typename").asText(null);
        String id = node.path(PATH_ID).asText("");
        if ("Product".equals(typename)) {
            return true;
        }
        return id.contains("/Product/") && !id.contains("/ProductVariant/");
    }

    private BulkOperationInfo parseBulkOperation(JsonNode operation) {
        return BulkOperationInfo.builder()
                .id(operation.path(PATH_ID).asText(null))
                .status(operation.path("status").asText(null))
                .errorCode(operation.path("errorCode").asText(null))
                .objectCount(operation.path("objectCount").asLong(0))
                .rootObjectCount(operation.path("rootObjectCount").asLong(0))
                .url(operation.path(PATH_URL).asText(null))
                .partialDataUrl(operation.path("partialDataUrl").asText(null))
                .build();
    }

    /**
     * 把 Shopify 商品数据转换成内部使用的数据结构。
     *
     * @param node
     * @return com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct
     * @author lwj
     **/
    private ShopifyImportedProduct parseImportedProduct(JsonNode node) {
        ShopifyImportedProduct product = new ShopifyImportedProduct();
        product.setId(node.path(PATH_ID).asText(null));
        product.setTitle(node.path("title").asText(null));
        product.setHandle(node.path("handle").asText(null));
        product.setDescriptionHtml(node.path("descriptionHtml").asText(null));
        product.setProductType(node.path("productType").asText(null));
        product.setVendor(node.path("vendor").asText(null));
        product.setStatus(node.path("status").asText(null));
        product.setUpdatedAt(parseDate(node.path("updatedAt").asText(null)));
        product.setSpu(parseSpu(node));
        product.setTags(parseTextList(node.path("tags")));
        product.setSeo(parseSeo(node.path("seo")));
        product.setVariants(parseImportedVariants(node.path("variants")));
        product.setMedia(parseImportedMedia(node.path("media")));
        return product;
    }

    private Date parseDate(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return Date.from(java.time.Instant.parse(value));
    }

    /**
     * 从元字段获取SPU
     *
     * @param node
     * @return java.lang.String
     * @author lwj
     **/
    private String parseSpu(JsonNode node) {
        JsonNode metafield = node.path("metafield");
        if (!metafield.isMissingNode() && !metafield.isNull()) {
            return metafield.path(PATH_VALUE).asText(null);
        }
        for (JsonNode metafieldNode : connectionNodes(node.path("metafieds"))) {

            if ("custom".equals(metafieldNode.path("namespace").asText())
                    && "SPU".equalsIgnoreCase(metafieldNode.path("key").asText())) {
                return metafieldNode.path(PATH_VALUE).asText(null);
            }
        }
        return null;
    }

    private List<String> parseTextList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private Map<String, String> parseSeo(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        Map<String, String> seo = new LinkedHashMap<>();
        seo.put("title", node.path("title").asText(null));
        seo.put("description", node.path("description").asText(null));
        return seo;
    }

    private List<ShopifyImportedVariant> parseImportedVariants(JsonNode variantsNode) {
        List<ShopifyImportedVariant> variants = new ArrayList<>();
        for (JsonNode node : connectionNodes(variantsNode)) {
            ShopifyImportedVariant variant = new ShopifyImportedVariant();
            variant.setId(node.path(PATH_ID).asText(null));
            variant.setSku(firstNonEmpty(node.path("sku").asText(null), node.path("inventoryItem").path("sku").asText(null)));
            variant.setPrice(node.path("price").asText(null));
            variant.setCompareAtPrice(node.path("compareAtPrice").asText(null));
            variant.setPosition(node.path("position").isMissingNode() ? null : node.path("position").asInt());
            variant.setInventoryItemId(node.path("inventoryItem").path(PATH_ID).asText(null));
            variant.setSelectedOptions(parseSelectedOptions(node.path("selectedOptions")));
            variant.setMediaIds(parseVariantMediaIds(node.path(PATH_MEDIA)));
            variants.add(variant);
        }
        return variants;
    }

    private Map<String, String> parseSelectedOptions(JsonNode selectedOptionsNode) {
        Map<String, String> selectedOptions = new LinkedHashMap<>();
        if (selectedOptionsNode.isArray()) {
            for (JsonNode selectedOption : selectedOptionsNode) {
                selectedOptions.put(selectedOption.path(PATH_NAME).asText(), selectedOption.path(PATH_VALUE).asText());
            }
        }
        return selectedOptions;
    }

    private List<String> parseVariantMediaIds(JsonNode mediaNode) {
        List<String> mediaIds = new ArrayList<>();
        for (JsonNode node : connectionNodes(mediaNode)) {
            String mediaId = node.path(PATH_ID).asText(null);
            if (StringUtils.isNotEmpty(mediaId)) {
                mediaIds.add(mediaId);
            }
        }
        return mediaIds;
    }

    private List<ShopifyImportedMedia> parseImportedMedia(JsonNode mediaNode) {
        List<ShopifyImportedMedia> media = new ArrayList<>();
        int position = 1;
        for (JsonNode node : connectionNodes(mediaNode)) {
            ShopifyImportedMedia item = new ShopifyImportedMedia();
            item.setId(node.path(PATH_ID).asText(null));
            item.setAlt(node.path("alt").asText(null));
            item.setMediaContentType(node.path("mediaContentType").asText(null));
            item.setOriginalUrl(resolveOriginalMediaUrl(node));
            item.setPreviewUrl(node.path("preview").path("image").path(PATH_URL).asText(null));
            item.setPosition(position++);
            media.add(item);
        }
        return media;
    }

    private String resolveOriginalMediaUrl(JsonNode node) {
        String imageUrl = node.path("image").path("originalSrc").asText(null);
        if (StringUtils.isNotEmpty(imageUrl)) {
            return imageUrl;
        }
        imageUrl = node.path("image").path(PATH_URL).asText(null);
        if (StringUtils.isNotEmpty(imageUrl)) {
            return imageUrl;
        }
        JsonNode sources = node.path("sources");
        if (sources.isArray() && !sources.isEmpty()) {
            return sources.get(0).path(PATH_URL).asText(null);
        }
        return null;
    }

    private List<JsonNode> connectionNodes(JsonNode connectionNode) {
        List<JsonNode> nodes = new ArrayList<>();
        if (connectionNode == null || connectionNode.isMissingNode() || connectionNode.isNull()) {
            return nodes;
        }
        if (connectionNode.has("edges")) {
            for (JsonNode edge : connectionNode.path("edges")) {
                nodes.add(edge.path("node"));
            }
            return nodes;
        }
        if (connectionNode.has("nodes")) {
            for (JsonNode node : connectionNode.path("nodes")) {
                nodes.add(node);
            }
            return nodes;
        }
        if (connectionNode.isArray()) {
            for (JsonNode node : connectionNode) {
                nodes.add(node.has("node") ? node.path("node") : node);
            }
        }
        return nodes;
    }

    private String firstNonEmpty(String first, String second) {
        return StringUtils.isNotEmpty(first) ? first : second;
    }


}

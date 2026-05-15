package com.ruoyi.erp.shopify.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.model.dto.shopifyProduct.*;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.model.BulkOperationInfo;
import com.ruoyi.erp.shopify.model.ProductPage;
import com.ruoyi.erp.shopify.query.ShopifyGraphQLQueries;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Shopify 商品反向导入 GraphQL 客户端。
 */
@Slf4j
@Component
public class ShopifyProductImportGraphQLClient {
    private static final String PATH_ID = "id";
    private static final String PATH_MEDIA = "media";
    private static final String PATH_NAME = "name";
    private static final String PATH_PARENT_ID = "__parentId";
    private static final String PATH_VALUE = "value";
    private static final String PATH_URL = "url";
    private static final String MUTATION_BULK_OPERATION_RUN_QUERY = "bulkOperationRunQuery";
    private static final int BULK_DOWNLOAD_CONNECT_TIMEOUT_MILLIS = 30_000;
    private static final int BULK_DOWNLOAD_READ_TIMEOUT_MILLIS = 120_000;
    private static final int BULK_DOWNLOAD_MAX_RETRY_COUNT = 3;
    private static final int[] BULK_DOWNLOAD_RETRY_STATUSES = {408, 429, 500, 502, 503, 504};
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
     * 下载并解析 Bulk JSONL。
     */
    public List<ShopifyImportedProduct> downloadBulkJsonlProducts(String url) {
        log.info("开始下载 Shopify Bulk JSONL 商品文件，url={}", url);
        try (InputStream inputStream = URI.create(url).toURL().openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return parseBulkJsonlProducts(reader);
        } catch (Exception e) {
            throw new ShopifyApiException("下载 Shopify Bulk JSONL 失败: " + e.getMessage(), e);
        }

    }


    /**
     * 下载并解析 Bulk JSONL。
     */
    public List<ShopifyImportedProduct> downloadBulkJsonlProductsWithAgent(String url) {
        String safeUrl = maskSignedBulkUrl(url);
        log.info("开始下载 Shopify Bulk JSONL 商品文件，url={}", safeUrl);

        URL jsonlUrl;
        try {
            jsonlUrl = URI.create(url).toURL();
        } catch (Exception e) {
            throw new ShopifyApiException("下载 Shopify Bulk JSONL 失败: URL 无效", e);
        }

        int maxAttempts = BULK_DOWNLOAD_MAX_RETRY_COUNT + 1;
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = openBulkJsonlDownloadConnection(jsonlUrl);
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (isRetryableBulkDownloadStatus(responseCode) && attempt < maxAttempts) {
                        log.warn("下载 Shopify Bulk JSONL 返回可重试状态，attempt={}/{}, status={}, url={}",
                                attempt, maxAttempts, responseCode, safeUrl);
                        sleepBeforeBulkDownloadRetry(attempt);
                        continue;
                    }
                    throw new ShopifyApiException("下载 Shopify Bulk JSONL 失败，HTTP状态码: " + responseCode);
                }

                try (InputStream inputStream = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    return parseBulkJsonlProducts(reader);
                } catch (JsonProcessingException e) {
                    throw new ShopifyApiException("解析 Shopify Bulk JSONL 失败: " + e.getOriginalMessage(), e);
                }
            } catch (ShopifyApiException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (isRetryableBulkDownloadException(e) && attempt < maxAttempts) {
                    log.warn("下载 Shopify Bulk JSONL 发生可重试异常，attempt={}/{}, exception={}, url={}, message={}",
                            attempt, maxAttempts, e.getClass().getSimpleName(), safeUrl, e.getMessage());
                    sleepBeforeBulkDownloadRetry(attempt);
                    continue;
                }
                throw new ShopifyApiException("下载 Shopify Bulk JSONL 失败: " + e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        throw new ShopifyApiException("下载 Shopify Bulk JSONL 失败: " +
                (lastException == null ? "超过最大重试次数" : lastException.getMessage()), lastException);

    }

    /**
     * 创建 Bulk JSONL 下载连接，每次重试都新建连接，避免复用已经被远端重置的连接。
     */
    private HttpURLConnection openBulkJsonlDownloadConnection(URL jsonlUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) jsonlUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(BULK_DOWNLOAD_CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(BULK_DOWNLOAD_READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
        connection.setRequestProperty("Accept-Encoding", "identity");
        return connection;
    }

    /**
     * 判断 Bulk JSONL 下载的 HTTP 状态码是否适合重试。
     */
    private boolean isRetryableBulkDownloadStatus(int responseCode) {
        return Arrays.stream(BULK_DOWNLOAD_RETRY_STATUSES).anyMatch(status -> status == responseCode);
    }

    /**
     * 判断 Bulk JSONL 下载异常是否属于临时网络中断。
     */
    private boolean isRetryableBulkDownloadException(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.net.SocketException
                    || current instanceof EOFException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.contains("Connection reset")
                    || message.contains("Read timed out")
                    || message.contains("Connect timed out")
                    || message.contains("Broken pipe")
                    || message.contains("Unexpected end of file"))) {
                return true;
            }
            if (current instanceof SSLException && message != null && (message.contains("closed")
                    || message.contains("shutdown")
                    || message.contains("reset"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 按指数退避等待下一次 Bulk JSONL 下载重试。
     */
    private void sleepBeforeBulkDownloadRetry(int attempt) {
        long sleepMillis = (long) Math.pow(2, attempt) * 1000L;
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShopifyApiException("下载 Shopify Bulk JSONL 重试被中断", e);
        }
    }

    /**
     * 隐藏 Shopify Bulk JSONL 签名 URL 的查询参数，避免日志泄露临时下载凭证。
     */
    private String maskSignedBulkUrl(String url) {
        try {
            URI uri = URI.create(url);
            StringBuilder builder = new StringBuilder();
            builder.append(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() >= 0) {
                builder.append(':').append(uri.getPort());
            }
            if (StringUtils.isNotEmpty(uri.getPath())) {
                builder.append(uri.getPath());
            }
            return builder.toString();
        } catch (Exception e) {
            return "invalid-url";
        }
    }



    /**
     * 解析本地 JSONL 文件，并按 Shopify Bulk JSONL 父子行结构归并商品、变体和媒体。
     * @param filePath
     * @return
     */
    public List<ShopifyImportedProduct> parseLocalJsonlProducts(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new ShopifyApiException("文件不存在 " + filePath);
        }

        try (FileInputStream fileInputStream = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8))) {
            return parseBulkJsonlProducts(reader);
        } catch (Exception e) {
            throw new ShopifyApiException("下载 Shopify Bulk JSONL 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按 Bulk JSONL 的父子行结构解析商品，并把紧跟商品后的变体和媒体归并回商品对象。
     */
    private List<ShopifyImportedProduct> parseBulkJsonlProducts(BufferedReader reader) throws IOException {
        Map<String, ShopifyImportedProduct> productMap = new LinkedHashMap<>();
        Map<String, ShopifyImportedVariant> variantMap = new HashMap<>();
        Map<String, Integer> productMediaPositionMap = new HashMap<>();

        log.info("开始解析 Shopify Bulk JSONL 商品文件");
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode node = objectMapper.readTree(line);
            if (isProductJsonlNode(node)) {
                ShopifyImportedProduct product = parseImportedProduct(node);
                productMap.put(product.getId(), product);
                productMediaPositionMap.put(product.getId(), product.getMedia().size());
                for (ShopifyImportedVariant variant : product.getVariants()) {
                    if (StringUtils.isNotEmpty(variant.getId())) {
                        variantMap.put(variant.getId(), variant);
                    }
                }
                log.debug("成功解析商品, title={}", product.getTitle());
                continue;
            }
            if (isVariantJsonlNode(node)) {
                appendBulkVariant(node, productMap, variantMap);
                continue;
            }
            if (isVariantMediaReferenceNode(node)) {
                appendBulkVariantMediaReference(node, variantMap);
                continue;
            }
            if (isProductMediaJsonlNode(node)) {
                appendBulkProductMedia(node, productMap, productMediaPositionMap);
            }
        }

        List<ShopifyImportedProduct> products = new ArrayList<>(productMap.values());
        log.info("Shopify Bulk JSONL 商品文件解析完成，productCount={}", products.size());
        return products;
    }

    /**
     * 把变体行追加到父商品，并缓存变体用于后续媒体引用绑定。
     */
    private void appendBulkVariant(JsonNode node, Map<String, ShopifyImportedProduct> productMap,
                                   Map<String, ShopifyImportedVariant> variantMap) {
        String parentId = node.path(PATH_PARENT_ID).asText(null);
        ShopifyImportedProduct product = productMap.get(parentId);
        if (product == null) {
            log.warn("Shopify Bulk JSONL 变体缺少父商品，parentId={}, variantId={}",
                    parentId, node.path(PATH_ID).asText(null));
            return;
        }
        ShopifyImportedVariant variant = parseImportedVariant(node);
        product.getVariants().add(variant);
        variantMap.put(variant.getId(), variant);
    }

    /**
     * 把只有媒体 id 的变体媒体引用行追加到父变体。
     */
    private void appendBulkVariantMediaReference(JsonNode node, Map<String, ShopifyImportedVariant> variantMap) {
        String parentId = node.path(PATH_PARENT_ID).asText(null);
        ShopifyImportedVariant variant = variantMap.get(parentId);
        String mediaId = node.path(PATH_ID).asText(null);
        if (variant == null) {
            log.warn("Shopify Bulk JSONL 变体媒体引用缺少父变体，parentId={}, mediaId={}", parentId, mediaId);
            return;
        }
        if (StringUtils.isNotEmpty(mediaId)) {
            variant.getMediaIds().add(mediaId);
        }
    }

    /**
     * 把商品媒体详情行追加到父商品，并按 JSONL 出现顺序生成媒体排序。
     */
    private void appendBulkProductMedia(JsonNode node, Map<String, ShopifyImportedProduct> productMap,
                                        Map<String, Integer> productMediaPositionMap) {
        String parentId = node.path(PATH_PARENT_ID).asText(null);
        ShopifyImportedProduct product = productMap.get(parentId);
        if (product == null) {
            log.warn("Shopify Bulk JSONL 商品媒体缺少父商品，parentId={}, mediaId={}",
                    parentId, node.path(PATH_ID).asText(null));
            return;
        }
        int position = productMediaPositionMap.getOrDefault(parentId, 0) + 1;
        productMediaPositionMap.put(parentId, position);
        product.getMedia().add(parseImportedMedia(node, position));
    }

    private boolean isProductJsonlNode(JsonNode node) {
        String typename = node.path("__typename").asText(null);
        String id = node.path(PATH_ID).asText("");
        if ("Product".equals(typename)) {
            return true;
        }
        return id.contains("/Product/") && !id.contains("/ProductVariant/");
    }

    /**
     * 判断当前行是否是 Bulk JSONL 中的商品变体行。
     */
    private boolean isVariantJsonlNode(JsonNode node) {
        String id = node.path(PATH_ID).asText("");
        String parentId = node.path(PATH_PARENT_ID).asText("");
        return id.contains("/ProductVariant/") && parentId.contains("/Product/");
    }

    /**
     * 判断当前行是否是变体下的媒体引用行，该行只提供媒体 id 和父变体 id。
     */
    private boolean isVariantMediaReferenceNode(JsonNode node) {
        String id = node.path(PATH_ID).asText("");
        String parentId = node.path(PATH_PARENT_ID).asText("");
        return id.contains("/Media") && parentId.contains("/ProductVariant/")
                && !node.has("mediaContentType")
                && !node.has("image")
                && !node.has("preview")
                && !node.has("sources");
    }

    /**
     * 判断当前行是否是商品下的媒体详情行，该行包含图片或视频的可下载地址。
     */
    private boolean isProductMediaJsonlNode(JsonNode node) {
        String id = node.path(PATH_ID).asText("");
        String parentId = node.path(PATH_PARENT_ID).asText("");
        return id.contains("/Media") && parentId.contains("/Product/")
                && (node.has("mediaContentType") || node.has("image") || node.has("preview") || node.has("sources"));
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
        product.setOptions(parseImportedOptions(node.path("options")));
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

    /**
     * 解析 Shopify 商品选项节点。
     */
    private List<ShopifyImportedOption> parseImportedOptions(JsonNode optionsNode) {
        List<ShopifyImportedOption> options = new ArrayList<>();
        for (JsonNode node : optionNodes(optionsNode)) {
            String name = node.path(PATH_NAME).asText(null);
            if (StringUtils.isEmpty(name)) {
                continue;
            }
            ShopifyImportedOption option = new ShopifyImportedOption();
            option.setId(node.path(PATH_ID).asText(null));
            option.setName(name);
            option.setPosition(node.path("position").isMissingNode() ? null : node.path("position").asInt());
            option.setValues(parseImportedOptionValues(node.path("optionValues")));
            options.add(option);
        }
        return options;
    }

    /**
     * 读取选项或选项值节点，兼容数组和 connection 两种结构。
     */
    private List<JsonNode> optionNodes(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            node.forEach(nodes::add);
            return nodes;
        }
        return connectionNodes(node);
    }

    /**
     * 解析 Shopify 商品选项值节点。
     */
    private List<ShopifyImportedOptionValue> parseImportedOptionValues(JsonNode valuesNode) {
        List<ShopifyImportedOptionValue> values = new ArrayList<>();
        for (JsonNode node : optionNodes(valuesNode)) {
            String name = node.path(PATH_NAME).asText(null);
            if (StringUtils.isEmpty(name)) {
                continue;
            }
            ShopifyImportedOptionValue value = new ShopifyImportedOptionValue();
            value.setId(node.path(PATH_ID).asText(null));
            value.setName(name);
            values.add(value);
        }
        return values;
    }

    private List<ShopifyImportedVariant> parseImportedVariants(JsonNode variantsNode) {
        List<ShopifyImportedVariant> variants = new ArrayList<>();
        for (JsonNode node : connectionNodes(variantsNode)) {
            variants.add(parseImportedVariant(node));
        }
        return variants;
    }

    /**
     * 把 Shopify 变体节点转换成导入 DTO，兼容嵌套 connection 和 Bulk JSONL 子行。
     */
    private ShopifyImportedVariant parseImportedVariant(JsonNode node) {
        ShopifyImportedVariant variant = new ShopifyImportedVariant();
        variant.setId(node.path(PATH_ID).asText(null));
        variant.setSku(firstNonEmpty(node.path("sku").asText(null), node.path("inventoryItem").path("sku").asText(null)));
        variant.setPrice(node.path("price").asText(null));
        variant.setCompareAtPrice(node.path("compareAtPrice").asText(null));
        variant.setPosition(node.path("position").isMissingNode() ? null : node.path("position").asInt());
        variant.setInventoryItemId(node.path("inventoryItem").path(PATH_ID).asText(null));
        variant.setSelectedOptions(parseSelectedOptions(node.path("selectedOptions")));
        variant.setMediaIds(parseVariantMediaIds(node.path(PATH_MEDIA)));
        return variant;
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
            media.add(parseImportedMedia(node, position++));
        }
        return media;
    }

    /**
     * 把 Shopify 媒体节点转换成导入 DTO，并写入调用方确定的排序。
     */
    private ShopifyImportedMedia parseImportedMedia(JsonNode node, int position) {
        ShopifyImportedMedia item = new ShopifyImportedMedia();
        item.setId(node.path(PATH_ID).asText(null));
        item.setAlt(node.path("alt").asText(null));
        item.setMediaContentType(node.path("mediaContentType").asText(null));
        item.setOriginalUrl(resolveOriginalMediaUrl(node));
        item.setPreviewUrl(node.path("preview").path("image").path(PATH_URL).asText(null));
        item.setPosition(position);
        return item;
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

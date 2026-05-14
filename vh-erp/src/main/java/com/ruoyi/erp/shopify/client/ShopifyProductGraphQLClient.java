package com.ruoyi.erp.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.model.CreateMediaInput;
import com.ruoyi.erp.shopify.model.ProductCreateResult;
import com.ruoyi.erp.shopify.model.ProductInput;
import com.ruoyi.erp.shopify.model.VariantInput;
import com.ruoyi.erp.shopify.query.ShopifyGraphQLQueries;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shopify 商品和变体 GraphQL 客户端。
 */
@Slf4j
@Component
public class ShopifyProductGraphQLClient {
    private static final String PATH_ID = "id";
    private static final String PATH_PRODUCT = "product";
    private static final String PATH_PRODUCT_VARIANTS = "productVariants";
    private static final String MUTATION_PRODUCT_CREATE = "productCreate";
    private static final String MUTATION_PRODUCT_UPDATE = "productUpdate";
    private static final String MUTATION_PRODUCT_VARIANTS_BULK_CREATE = "productVariantsBulkCreate";

    @Resource
    private ShopifyGraphQLTransport transport;
    /**
     * 创建 Shopify 商品 (指定店铺)
     */
    public ProductCreateResult createProduct(Long storeId, ProductInput productInput, List<CreateMediaInput> mediaInputs) {
        String mutation = ShopifyGraphQLQueries.PRODUCT_CREATE.getQuery();

        Map<String, Object> variables = new HashMap<>();
        variables.put("product", productInput);
        if (mediaInputs != null && !mediaInputs.isEmpty()) {
            variables.put("media", mediaInputs);
        }

        JsonNode data = transport.execute(storeId, mutation, variables);
        transport.checkUserErrors(data, MUTATION_PRODUCT_CREATE);

        JsonNode productNode = data.path(MUTATION_PRODUCT_CREATE).path(PATH_PRODUCT);
        String productId = productNode.path(PATH_ID).asText();
        if (StringUtils.isEmpty(productId)) {
            throw new ShopifyApiException("商品创建成功但未返回 ID");
        }

        // 解析 media ID 列表
        List<String> mediaIds = new ArrayList<>();
        JsonNode mediaEdges = productNode.path("media").path("edges");
        if (mediaEdges.isArray()) {
            for (JsonNode edge : mediaEdges) {
                String mediaId = edge.path("node").path(PATH_ID).asText();
                if (StringUtils.isNotEmpty(mediaId)) {
                    mediaIds.add(mediaId);
                }
            }
        }
        if (mediaInputs == null || mediaInputs.isEmpty()) {
            mediaIds = List.of();
        } else if (mediaIds.size() > mediaInputs.size()) {
            mediaIds = new ArrayList<>(mediaIds.subList(mediaIds.size() - mediaInputs.size(), mediaIds.size()));
        }

        return new ProductCreateResult(productId, mediaIds);
    }

    /**
     * 更新 Shopify 商品 (指定店铺)
     */
    public ProductCreateResult updateProduct(Long storeId, String shopifyProductId, ProductInput input, List<CreateMediaInput> mediaInputs) {
        String mutation = ShopifyGraphQLQueries.PRODUCT_UPDATE.getQuery();

        // Shopify API 不允许在更新时包含 productOptions 字段
        ProductInput inputWithId = ProductInput.builderFrom(input)
                .id(shopifyProductId)
                .productOptions(null)  // 清空 productOptions
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("product", inputWithId);
        if (mediaInputs != null && !mediaInputs.isEmpty()) {
            variables.put("media", mediaInputs);
        }

        JsonNode data = transport.execute(storeId, mutation, variables);
        transport.checkUserErrors(data, MUTATION_PRODUCT_UPDATE);

        JsonNode productNode = data.path(MUTATION_PRODUCT_UPDATE).path(PATH_PRODUCT);
        String productId = productNode.path(PATH_ID).asText();
        if (StringUtils.isEmpty(productId)) {
            throw new ShopifyApiException("Product updated but Shopify did not return product ID");
        }

        List<String> mediaIds = new ArrayList<>();
        JsonNode mediaEdges = productNode.path("media").path("edges");
        if (mediaEdges.isArray()) {
            for (JsonNode edge : mediaEdges) {
                String mediaId = edge.path("node").path(PATH_ID).asText();
                if (StringUtils.isNotEmpty(mediaId)) {
                    mediaIds.add(mediaId);
                }
            }
        }
        if (mediaInputs == null || mediaInputs.isEmpty()) {
            mediaIds = List.of();
        } else if (mediaIds.size() > mediaInputs.size()) {
            mediaIds = new ArrayList<>(mediaIds.subList(mediaIds.size() - mediaInputs.size(), mediaIds.size()));
        }

        return new ProductCreateResult(productId, mediaIds);
    }

    /**
     * 批量创建变体 (指定店铺)
     */
    public List<String> createVariantsBulk(Long storeId, String shopifyProductId, List<VariantInput> variants) {
        return createVariantsBulk(storeId, shopifyProductId, variants, null);
    }

    /**
     * 批量创建变体 (指定店铺)
     */
    public List<String> createVariantsBulk(Long storeId, String shopifyProductId, List<VariantInput> variants, String strategy) {
        if (variants.isEmpty()) {
            return List.of();
        }

        String mutation = ShopifyGraphQLQueries.PRODUCT_VARIANTS_BULK_CREATE.getQuery();

        Map<String, Object> variables = new HashMap<>();
        variables.put("productId", shopifyProductId);
        variables.put("variants", variants);
        if (StringUtils.isNotEmpty(strategy)) {
            variables.put("strategy", strategy);
        }

        JsonNode data = transport.execute(storeId, mutation, variables);
        log.info("批量创建变体成功: storeId={}, shopifyProductId={}, data={}", storeId, shopifyProductId, data);
        transport.checkUserErrors(data, MUTATION_PRODUCT_VARIANTS_BULK_CREATE);
        JsonNode variantsNode = data.path(MUTATION_PRODUCT_VARIANTS_BULK_CREATE).path(PATH_PRODUCT_VARIANTS);
        List<String> variantIds = new ArrayList<>();
        for (JsonNode v : variantsNode) {
            variantIds.add(v.path(PATH_ID).asText());
        }
        return variantIds;
    }

}

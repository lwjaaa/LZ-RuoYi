package com.ruoyi.erp.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.model.ChannelInfo;
import com.ruoyi.erp.shopify.model.LocationInfo;
import com.ruoyi.erp.shopify.model.PublicationInfo;
import com.ruoyi.erp.shopify.model.PublicationInput;
import com.ruoyi.erp.shopify.model.PublicationResult;
import com.ruoyi.erp.shopify.model.PublicationUpdateInput;
import com.ruoyi.erp.shopify.query.ShopifyGraphQLQueries;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shopify 发布渠道和店铺资源 GraphQL 客户端。
 */
@Component
public class ShopifyPublicationGraphQLClient {
    private static final String MUTATION_PUBLICATION_UPDATE = "publicationUpdate";

    @Resource
    private ShopifyGraphQLTransport transport;
    /**
     * 测试店铺 GraphQL 连接。
     */
    public void testConnection(Long storeId) {
        transport.execute(storeId, ShopifyGraphQLQueries.SHOP_INFO.getQuery(), Map.of());
    }

    /**
     * 查询可用的库存仓库 Location。
     */
    public List<LocationInfo> getLocations(Long storeId) {
        String query = ShopifyGraphQLQueries.LOCATIONS.getQuery();
        JsonNode data = transport.execute(storeId, query, Map.of("first", 100));

        List<LocationInfo> locations = new ArrayList<>();
        JsonNode edges = data.path("locations").path("edges");
        if (edges.isArray()) {
            for (JsonNode edge : edges) {
                JsonNode node = edge.path("node");
                locations.add(LocationInfo.builder()
                        .id(node.path("id").asText())
                        .name(node.path("name").asText())
                        .build());
            }
        }
        return locations;
    }

    /**
     * 查询可发布的 Publication。
     */
    public List<PublicationInfo> getPublications(Long storeId) {
        String query = ShopifyGraphQLQueries.PUBLICATIONS.getQuery();
        JsonNode data = transport.execute(storeId, query, Map.of("first", 100));

        List<PublicationInfo> publications = new ArrayList<>();
        JsonNode edges = data.path("publications").path("edges");
        if (edges.isArray()) {
            for (JsonNode edge : edges) {
                JsonNode node = edge.path("node");
                publications.add(PublicationInfo.builder()
                        .id(node.path("id").asText())
                        .name(node.path("name").asText())
                        .build());
            }
        }
        return publications;
    }

    /**
     * 设置 Publication 是否自动发布新商品。
     */
    public void updatePublicationAutoPublish(Long storeId, String publicationId, boolean autoPublish) {
        if (StringUtils.isEmpty(publicationId)) {
            throw new ShopifyApiException("Publication ID 不能为空");
        }
        JsonNode data = transport.execute(storeId, ShopifyGraphQLQueries.PUBLICATION_UPDATE.getQuery(), Map.of(
                "id", publicationId,
                "input", PublicationUpdateInput.builder().autoPublish(autoPublish).build()
        ));
        transport.checkUserErrors(data, MUTATION_PUBLICATION_UPDATE);
    }

    /**
     * 查询可用的销售渠道。
     */
    public List<ChannelInfo> getChannels(Long storeId) {
        String query = ShopifyGraphQLQueries.CHANNELS.getQuery();
        JsonNode data = transport.execute(storeId, query, Map.of("first", 50));

        List<ChannelInfo> channels = new ArrayList<>();
        JsonNode edges = data.path("channels").path("edges");
        if (edges.isArray()) {
            for (JsonNode edge : edges) {
                JsonNode node = edge.path("node");
                channels.add(ChannelInfo.builder()
                        .id(node.path("id").asText())
                        .name(node.path("name").asText())
                        .isPublished(node.path("isPublished").asBoolean())
                        .build());
            }
        }
        return channels;
    }

    /**
     * 使用 Publication ID 发布商品。
     */
    public List<PublicationResult> publishProduct(Long storeId, String shopifyProductId, List<PublicationInput> publications) {
        String mutation = ShopifyGraphQLQueries.PUBLISHABLE_PUBLISH.getQuery();

        Map<String, Object> variables = Map.of(
                "id", shopifyProductId,
                "input", publications
        );

        JsonNode data = transport.execute(storeId, mutation, variables);
        transport.checkUserErrors(data, "publishablePublish");

        return publications.stream()
                .map(input -> PublicationResult.builder()
                        .publicationId(input.getPublicationId())
                        .isPublished(true)
                        .build())
                .toList();
    }
}

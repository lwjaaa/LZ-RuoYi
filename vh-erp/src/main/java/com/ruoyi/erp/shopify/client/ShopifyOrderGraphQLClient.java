package com.ruoyi.erp.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.model.FulfillmentCreateResult;
import com.ruoyi.erp.shopify.model.ShopifyImportedCustomer;
import com.ruoyi.erp.shopify.model.ShopifyImportedOrder;
import com.ruoyi.erp.shopify.model.ShopifyImportedOrderLineItem;
import com.ruoyi.erp.shopify.model.ShopifyImportedRefund;
import com.ruoyi.erp.shopify.model.ShopifyOrderPage;
import com.ruoyi.erp.shopify.query.ShopifyGraphQLQueries;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shopify 订单轮询和履约回传 GraphQL 客户端。
 */
@Slf4j
@Component
public class ShopifyOrderGraphQLClient {

    private static final String MUTATION_FULFILLMENT_CREATE = "fulfillmentCreate";

    @Resource
    private ShopifyGraphQLTransport transport;

    /**
     * 按 Shopify updated_at 分页拉取订单。
     *
     * @param storeId 店铺ID
     * @param since 更新时间下限
     * @param cursor Shopify 分页游标
     * @param first 每页数量
     * @return 订单分页结果
     */
    public ShopifyOrderPage queryUpdatedOrders(Long storeId, Date since, String cursor, int first) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("first", first);
        variables.put("after", cursor);
        variables.put("query", since == null ? null : "updated_at:>" + DateTimeFormatter.ISO_INSTANT.format(since.toInstant()));
        JsonNode data = transport.execute(storeId, ShopifyGraphQLQueries.ORDER_IMPORT_PAGE.getQuery(), variables);
        JsonNode ordersNode = data.path("orders");
        List<ShopifyImportedOrder> orders = new ArrayList<>();
        for (JsonNode edge : ordersNode.path("edges")) {
            orders.add(parseOrder(edge.path("node")));
        }
        ShopifyOrderPage page = new ShopifyOrderPage();
        page.setOrders(orders);
        page.setHasNextPage(ordersNode.path("pageInfo").path("hasNextPage").asBoolean(false));
        page.setEndCursor(ordersNode.path("pageInfo").path("endCursor").asText(null));
        log.info("Shopify 订单分页解析完成，storeId={}, count={}, hasNextPage={}", storeId, orders.size(), page.isHasNextPage());
        return page;
    }

    /**
     * 查询订单可用的 FulfillmentOrder ID。
     *
     * @param storeId 店铺ID
     * @param shopifyOrderId Shopify 订单ID
     * @return FulfillmentOrder ID 列表
     */
    public List<String> getFulfillmentOrderIds(Long storeId, String shopifyOrderId) {
        JsonNode data = transport.execute(storeId, ShopifyGraphQLQueries.FULFILLMENT_ORDERS_BY_ORDER.getQuery(),
                Map.of("id", shopifyOrderId));
        List<String> ids = new ArrayList<>();
        JsonNode edges = data.path("node").path("fulfillmentOrders").path("edges");
        for (JsonNode edge : edges) {
            JsonNode node = edge.path("node");
            String id = node.path("id").asText(null);
            String status = node.path("status").asText("");
            if (StringUtils.isNotEmpty(id) && !"CLOSED".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(status)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * 创建 Shopify 履约并回传物流信息。
     *
     * @param storeId 店铺ID
     * @param fulfillmentOrderId FulfillmentOrder ID
     * @param trackingCompany 物流公司
     * @param trackingNumber 运单号
     * @param trackingUrl 物流查询链接
     * @return Shopify 履约创建结果
     */
    public FulfillmentCreateResult createFulfillment(Long storeId, String fulfillmentOrderId,
                                                     String trackingCompany, String trackingNumber,
                                                     String trackingUrl) {
        Map<String, Object> trackingInfo = new HashMap<>();
        trackingInfo.put("company", trackingCompany);
        trackingInfo.put("number", trackingNumber);
        if (StringUtils.isNotEmpty(trackingUrl)) {
            trackingInfo.put("url", trackingUrl);
        }
        Map<String, Object> fulfillment = Map.of(
                "lineItemsByFulfillmentOrder", List.of(Map.of("fulfillmentOrderId", fulfillmentOrderId)),
                "trackingInfo", trackingInfo,
                "notifyCustomer", true
        );
        JsonNode data = transport.execute(storeId, ShopifyGraphQLQueries.FULFILLMENT_CREATE.getQuery(), Map.of(
                "fulfillment", fulfillment,
                "message", "ERP 发货回传"
        ));
        transport.checkUserErrors(data, MUTATION_FULFILLMENT_CREATE);
        JsonNode fulfillmentNode = data.path(MUTATION_FULFILLMENT_CREATE).path("fulfillment");
        return FulfillmentCreateResult.builder()
                .fulfillmentId(fulfillmentNode.path("id").asText(null))
                .status(fulfillmentNode.path("status").asText(null))
                .build();
    }

    /**
     * 解析 Shopify 订单节点。
     *
     * @param node GraphQL 订单节点
     * @return 本地同步订单对象
     */
    private ShopifyImportedOrder parseOrder(JsonNode node) {
        ShopifyImportedOrder order = new ShopifyImportedOrder();
        order.setId(node.path("id").asText(null));
        order.setName(node.path("name").asText(null));
        order.setOrderNumber(node.path("orderNumber").isMissingNode() ? null : node.path("orderNumber").asLong());
        order.setEmail(node.path("email").asText(null));
        order.setPhone(node.path("phone").asText(null));
        order.setFinancialStatus(node.path("displayFinancialStatus").asText(null));
        order.setFulfillmentStatus(node.path("displayFulfillmentStatus").asText(null));
        order.setCurrencyCode(node.path("currencyCode").asText(null));
        order.setCreatedAt(parseDate(node.path("createdAt").asText(null)));
        order.setUpdatedAt(parseDate(node.path("updatedAt").asText(null)));
        order.setCancelledAt(parseDate(node.path("cancelledAt").asText(null)));
        order.setClosedAt(parseDate(node.path("closedAt").asText(null)));
        order.setTotalPrice(parseMoney(node.path("totalPriceSet")));
        order.setSubtotalPrice(parseMoney(node.path("subtotalPriceSet")));
        order.setTotalTax(parseMoney(node.path("totalTaxSet")));
        order.setTotalShippingPrice(parseMoney(node.path("totalShippingPriceSet")));
        order.setTotalRefund(parseMoney(node.path("totalRefundedSet")));
        parseShippingAddress(node.path("shippingAddress"), order);
        order.setCustomer(parseCustomer(node.path("customer")));
        order.setLineItems(parseLineItems(node.path("lineItems").path("edges")));
        order.setRefunds(parseRefunds(node.path("refunds")));
        return order;
    }

    /**
     * 解析收货地址字段。
     *
     * @param node Shopify 收货地址节点
     * @param order 订单对象
     */
    private void parseShippingAddress(JsonNode node, ShopifyImportedOrder order) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        order.setShippingName(node.path("name").asText(null));
        order.setShippingPhone(node.path("phone").asText(null));
        order.setShippingCountry(node.path("country").asText(null));
        order.setShippingProvince(node.path("province").asText(null));
        order.setShippingCity(node.path("city").asText(null));
        order.setShippingZip(node.path("zip").asText(null));
        order.setShippingAddress1(node.path("address1").asText(null));
        order.setShippingAddress2(node.path("address2").asText(null));
    }

    /**
     * 解析客户快照。
     *
     * @param node Shopify 客户节点
     * @return 客户快照
     */
    private ShopifyImportedCustomer parseCustomer(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        ShopifyImportedCustomer customer = new ShopifyImportedCustomer();
        customer.setId(node.path("id").asText(null));
        customer.setEmail(node.path("email").asText(null));
        customer.setPhone(node.path("phone").asText(null));
        customer.setFirstName(node.path("firstName").asText(null));
        customer.setLastName(node.path("lastName").asText(null));
        customer.setDisplayName(node.path("displayName").asText(null));
        return customer;
    }

    /**
     * 解析订单行列表。
     *
     * @param edges Shopify 订单行 edges
     * @return 订单行列表
     */
    private List<ShopifyImportedOrderLineItem> parseLineItems(JsonNode edges) {
        List<ShopifyImportedOrderLineItem> lineItems = new ArrayList<>();
        for (JsonNode edge : edges) {
            JsonNode node = edge.path("node");
            ShopifyImportedOrderLineItem lineItem = new ShopifyImportedOrderLineItem();
            lineItem.setId(node.path("id").asText(null));
            lineItem.setTitle(node.path("title").asText(null));
            lineItem.setVariantTitle(node.path("variantTitle").asText(null));
            lineItem.setSku(node.path("sku").asText(null));
            lineItem.setQuantity(node.path("quantity").asInt(0));
            lineItem.setUnitPrice(parseMoney(node.path("originalUnitPriceSet")));
            lineItem.setTotalPrice(parseMoney(node.path("discountedTotalSet")));
            lineItem.setProductId(node.path("product").path("id").asText(null));
            JsonNode variant = node.path("variant");
            lineItem.setVariantId(variant.path("id").asText(null));
            if (StringUtils.isEmpty(lineItem.getSku())) {
                lineItem.setSku(variant.path("sku").asText(null));
            }
            lineItems.add(lineItem);
        }
        return lineItems;
    }

    /**
     * 解析退款列表。
     *
     * @param nodes Shopify 退款数组
     * @return 退款列表
     */
    private List<ShopifyImportedRefund> parseRefunds(JsonNode nodes) {
        List<ShopifyImportedRefund> refunds = new ArrayList<>();
        for (JsonNode node : nodes) {
            ShopifyImportedRefund refund = new ShopifyImportedRefund();
            refund.setId(node.path("id").asText(null));
            refund.setCreatedAt(parseDate(node.path("createdAt").asText(null)));
            refund.setNote(node.path("note").asText(null));
            refund.setAmount(parseMoney(node.path("totalRefundedSet")));
            refund.setCurrencyCode(node.path("totalRefundedSet").path("shopMoney").path("currencyCode").asText(null));
            refunds.add(refund);
        }
        return refunds;
    }

    /**
     * 解析 Shopify MoneyBag 到分。
     *
     * @param moneySet MoneyBag 节点
     * @return 分单位金额
     */
    private Integer parseMoney(JsonNode moneySet) {
        String amount = moneySet.path("shopMoney").path("amount").asText(null);
        if (StringUtils.isEmpty(amount)) {
            return null;
        }
        return new BigDecimal(amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 解析 Shopify ISO 时间。
     *
     * @param value ISO 时间字符串
     * @return Date 对象
     */
    private Date parseDate(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Date.from(Instant.parse(value));
        } catch (Exception e) {
            throw new ShopifyApiException("解析 Shopify 订单时间失败: " + value, e);
        }
    }
}

package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.mapper.FulfillmentRecordMapper;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.mapper.PurchaseTaskMapper;
import com.ruoyi.erp.mapper.RefundRecordMapper;
import com.ruoyi.erp.mapper.ShopifyCustomerMapper;
import com.ruoyi.erp.mapper.ShopifyOrderLineItemMapper;
import com.ruoyi.erp.mapper.ShopifyOrderMapper;
import com.ruoyi.erp.mapper.ShopifyOrderSyncCursorMapper;
import com.ruoyi.erp.order.ShopifyOrderTaskSupport;
import com.ruoyi.erp.model.domain.FulfillmentRecord;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.PurchaseTask;
import com.ruoyi.erp.model.domain.RefundRecord;
import com.ruoyi.erp.model.domain.ShopifyCustomer;
import com.ruoyi.erp.model.domain.ShopifyOrder;
import com.ruoyi.erp.model.domain.ShopifyOrderLineItem;
import com.ruoyi.erp.model.domain.ShopifyOrderSyncCursor;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.dto.shopifyOrder.FulfillmentRecordQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.FulfillmentSubmitRequest;
import com.ruoyi.erp.model.dto.shopifyOrder.ProfitReportQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.PurchaseTaskEdit;
import com.ruoyi.erp.model.dto.shopifyOrder.PurchaseTaskQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.RefundRecordQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.ShopifyOrderQuery;
import com.ruoyi.erp.model.vo.shopifyOrder.OrderProfitSummaryVo;
import com.ruoyi.erp.service.IShopifyOrderService;
import com.ruoyi.erp.service.IShopifyStoreService;
import com.ruoyi.erp.service.IShopifyTaskService;
import com.ruoyi.erp.shopify.client.ShopifyGraphQLClient;
import com.ruoyi.erp.shopify.enums.ShopifyTaskDetailStatus;
import com.ruoyi.erp.shopify.enums.ShopifyTaskStatus;
import com.ruoyi.erp.shopify.enums.ShopifyTaskType;
import com.ruoyi.erp.shopify.model.FulfillmentCreateResult;
import com.ruoyi.erp.shopify.model.ShopifyImportedCustomer;
import com.ruoyi.erp.shopify.model.ShopifyImportedOrder;
import com.ruoyi.erp.shopify.model.ShopifyImportedOrderLineItem;
import com.ruoyi.erp.shopify.model.ShopifyImportedRefund;
import com.ruoyi.erp.shopify.model.ShopifyOrderPage;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Shopify 订单轮询、人工采购、履约回传和利润统计 Service 实现。
 */
@Slf4j
@Service
public class ShopifyOrderServiceImpl implements IShopifyOrderService {

    private static final int ORDER_PAGE_SIZE = 50;
    private static final int MAX_PAGE_COUNT = 100;
    private static final int SAFETY_WINDOW_MINUTES = 5;
    private static final int BACKFILL_DAYS = 30;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PURCHASED = "PURCHASED";
    private static final String STATUS_EXCEPTION = "EXCEPTION";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PART_SUCCESS = "PART_SUCCESS";

    @Resource
    ShopifyOrderMapper orderMapper;
    @Resource
    ShopifyOrderLineItemMapper lineItemMapper;
    @Resource
    ShopifyCustomerMapper customerMapper;
    @Resource
    ProductVariantMapper productVariantMapper;
    @Resource
    PurchaseTaskMapper purchaseTaskMapper;
    @Resource
    FulfillmentRecordMapper fulfillmentRecordMapper;
    @Resource
    RefundRecordMapper refundRecordMapper;
    @Resource
    ShopifyOrderSyncCursorMapper cursorMapper;
    @Resource
    private IShopifyStoreService shopifyStoreService;
    @Resource
    private IShopifyTaskService shopifyTaskService;
    @Resource
    private ShopifyGraphQLClient shopifyGraphQLClient;
    @Resource
    private ShopifyOrderTaskSupport taskSupport;
    @Resource(name = "shopifySyncThreadPool")
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 根据游标计算订单轮询查询起点。
     *
     * @param cursor 店铺订单游标
     * @param now 当前时间
     * @param backfill 是否执行近 30 天补偿同步
     * @return Shopify updated_at 查询起点
     */
    Date calculateQuerySince(ShopifyOrderSyncCursor cursor, Instant now, boolean backfill) {
        if (backfill || cursor == null || cursor.getLastSuccessUpdatedAt() == null) {
            return Date.from(now.minus(BACKFILL_DAYS, ChronoUnit.DAYS));
        }
        return Date.from(cursor.getLastSuccessUpdatedAt().toInstant().minus(SAFETY_WINDOW_MINUTES, ChronoUnit.MINUTES));
    }

    /**
     * 刷新订单利润字段。
     *
     * @param order 订单对象
     */
    void refreshProfit(ShopifyOrder order) {
        int total = nvl(order.getTotalPrice());
        int refund = nvl(order.getTotalRefund());
        int purchase = nvl(order.getPurchaseCost());
        int shipping = nvl(order.getShippingCost());
        int grossProfit = total - refund - purchase - shipping;
        order.setGrossProfit(grossProfit);
        if (total > 0) {
            order.setGrossProfitRate(BigDecimal.valueOf(grossProfit)
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP));
        } else {
            order.setGrossProfitRate(BigDecimal.ZERO.setScale(4));
        }
    }

    @Override
    public List<ShopifyOrder> selectOrderList(ShopifyOrderQuery query) {
        QueryWrapper<ShopifyOrder> wrapper = buildOrderQuery(query);
        List<ShopifyOrder> orders = orderMapper.selectList(wrapper);
        for (ShopifyOrder order : orders) {
            order.setLineItems(selectLineItemsByOrderId(order.getOrderId()));
        }
        return orders;
    }

    @Override
    public ShopifyOrder selectOrderById(Long orderId) {
        ShopifyOrder order = orderMapper.selectById(orderId);
        if (order != null) {
            order.setLineItems(selectLineItemsByOrderId(orderId));
        }
        return order;
    }

    @Override
    public Long startIncrementalSync(Long storeId) {
        return startOrderSync(storeId, false);
    }

    @Override
    public Long startBackfillSync(Long storeId) {
        return startOrderSync(storeId, true);
    }

    @Override
    public void syncIncrementalAllActiveStores() {
        for (ShopifyStore store : shopifyStoreService.selectActiveStores()) {
            startIncrementalSync(store.getStoreId());
        }
    }

    @Override
    public void backfillRecentOrdersAllActiveStores() {
        for (ShopifyStore store : shopifyStoreService.selectActiveStores()) {
            startBackfillSync(store.getStoreId());
        }
    }

    @Override
    public ShopifyOrderSyncCursor getCursor(Long storeId) {
        return cursorMapper.selectOne(new QueryWrapper<ShopifyOrderSyncCursor>().eq("store_id", storeId));
    }

    @Override
    public List<PurchaseTask> selectPurchaseTaskList(PurchaseTaskQuery query) {
        QueryWrapper<PurchaseTask> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getStoreId() != null, "store_id", query.getStoreId())
                .eq(StringUtils.isNotEmpty(query.getPurchaseStatus()), "purchase_status", query.getPurchaseStatus())
                .and(StringUtils.isNotEmpty(query.getSearchKeyword()), w -> w
                        .like("order_name", query.getSearchKeyword())
                        .or()
                        .like("sku", query.getSearchKeyword())
                        .or()
                        .like("item_title", query.getSearchKeyword()))
                .orderByDesc("create_time");
        return purchaseTaskMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePurchaseTask(PurchaseTaskEdit edit) {
        if (edit == null || edit.getPurchaseTaskId() == null) {
            throw new ServiceException("采购任务ID不能为空");
        }
        PurchaseTask oldTask = purchaseTaskMapper.selectById(edit.getPurchaseTaskId());
        if (oldTask == null) {
            throw new ServiceException("采购任务不存在");
        }
        PurchaseTask task = new PurchaseTask();
        task.setPurchaseTaskId(edit.getPurchaseTaskId());
        task.setPurchaseStatus(StringUtils.isEmpty(edit.getPurchaseStatus()) ? oldTask.getPurchaseStatus() : edit.getPurchaseStatus());
        task.setActualPurchaseAmount(edit.getActualPurchaseAmount());
        task.setExceptionReason(edit.getExceptionReason());
        task.setRemark(edit.getRemark());
        if (STATUS_PURCHASED.equals(task.getPurchaseStatus())) {
            task.setPurchasedAt(DateUtils.getNowDate());
        }
        task.setUpdateTime(DateUtils.getNowDate());
        int rows = purchaseTaskMapper.updateById(task);
        syncLinePurchaseFromTask(oldTask, task);
        recalculateOrderCosts(oldTask.getOrderId());
        return rows;
    }

    @Override
    public List<FulfillmentRecord> selectFulfillmentRecordList(FulfillmentRecordQuery query) {
        QueryWrapper<FulfillmentRecord> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getStoreId() != null, "store_id", query.getStoreId())
                .eq(query.getOrderId() != null, "order_id", query.getOrderId())
                .eq(StringUtils.isNotEmpty(query.getSyncStatus()), "sync_status", query.getSyncStatus())
                .and(StringUtils.isNotEmpty(query.getSearchKeyword()), w -> w
                        .like("tracking_number", query.getSearchKeyword())
                        .or()
                        .like("tracking_company", query.getSearchKeyword()))
                .orderByDesc("create_time");
        return fulfillmentRecordMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentRecord submitFulfillment(FulfillmentSubmitRequest request) {
        validateFulfillmentRequest(request);
        ShopifyOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        Long duplicateCount = fulfillmentRecordMapper.selectCount(new QueryWrapper<FulfillmentRecord>()
                .eq("order_id", request.getOrderId())
                .eq("tracking_number", request.getTrackingNumber())
                .eq("sync_status", STATUS_SUCCESS));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new ServiceException("该订单和运单号已成功回传，请勿重复提交");
        }
        FulfillmentRecord record = createPendingFulfillmentRecord(order, request);
        try {
            List<String> fulfillmentOrderIds = shopifyGraphQLClient.getFulfillmentOrderIds(order.getStoreId(), order.getShopifyOrderId());
            if (fulfillmentOrderIds.isEmpty()) {
                throw new ServiceException("Shopify 未返回可履约的 FulfillmentOrder");
            }
            String fulfillmentOrderId = fulfillmentOrderIds.get(0);
            FulfillmentCreateResult result = shopifyGraphQLClient.createFulfillment(order.getStoreId(), fulfillmentOrderId,
                    request.getTrackingCompany(), request.getTrackingNumber(), request.getTrackingUrl());
            record.setShopifyFulfillmentOrderId(fulfillmentOrderId);
            record.setShopifyFulfillmentId(result.getFulfillmentId());
            record.setSyncStatus(STATUS_SUCCESS);
            record.setFulfilledAt(DateUtils.getNowDate());
            record.setUpdateTime(DateUtils.getNowDate());
            fulfillmentRecordMapper.updateById(record);
            updateOrderFulfillmentStatus(order.getOrderId(), STATUS_SUCCESS);
        } catch (Exception e) {
            record.setSyncStatus(STATUS_FAILED);
            record.setErrorMessage(e.getMessage());
            record.setUpdateTime(DateUtils.getNowDate());
            fulfillmentRecordMapper.updateById(record);
            updateOrderFulfillmentStatus(order.getOrderId(), STATUS_FAILED);
            throw e instanceof ServiceException ? (ServiceException) e : new ServiceException("Shopify 发货回传失败：" + e.getMessage());
        }
        recalculateOrderCosts(order.getOrderId());
        return record;
    }

    @Override
    public List<RefundRecord> selectRefundRecordList(RefundRecordQuery query) {
        QueryWrapper<RefundRecord> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getStoreId() != null, "store_id", query.getStoreId())
                .eq(query.getOrderId() != null, "order_id", query.getOrderId())
                .and(StringUtils.isNotEmpty(query.getSearchKeyword()), w -> w
                        .like("reason", query.getSearchKeyword())
                        .or()
                        .like("responsibility", query.getSearchKeyword())
                        .or()
                        .like("note", query.getSearchKeyword()))
                .orderByDesc("refund_time");
        return refundRecordMapper.selectList(wrapper);
    }

    @Override
    public OrderProfitSummaryVo selectProfitSummary(ProfitReportQuery query) {
        QueryWrapper<ShopifyOrder> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getStoreId() != null, "store_id", query.getStoreId())
                .ge(query.getBeginDate() != null, "placed_at", query.getBeginDate())
                .le(query.getEndDate() != null, "placed_at", query.getEndDate());
        List<ShopifyOrder> orders = orderMapper.selectList(wrapper);
        OrderProfitSummaryVo summary = new OrderProfitSummaryVo();
        summary.setOrderCount((long) orders.size());
        summary.setSalesAmount(sumOrders(orders, "sales"));
        summary.setRefundAmount(sumOrders(orders, "refund"));
        summary.setPurchaseCost(sumOrders(orders, "purchase"));
        summary.setShippingCost(sumOrders(orders, "shipping"));
        summary.setGrossProfit(sumOrders(orders, "profit"));
        if (nvl(summary.getSalesAmount()) > 0) {
            summary.setGrossProfitRate(BigDecimal.valueOf(nvl(summary.getGrossProfit()))
                    .divide(BigDecimal.valueOf(summary.getSalesAmount()), 4, RoundingMode.HALF_UP));
        } else {
            summary.setGrossProfitRate(BigDecimal.ZERO.setScale(4));
        }
        return summary;
    }

    /**
     * 将远端订单写入本地订单、订单行、采购任务和退款记录。
     *
     * @param remote Shopify 远端订单
     * @param store 店铺配置
     * @return 订单写入结果
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderUpsertResult upsertOrder(ShopifyImportedOrder remote, ShopifyStore store) {
        Date now = DateUtils.getNowDate();
        ShopifyCustomer customer = upsertCustomer(remote.getCustomer(), store.getStoreId(), now);
        ShopifyOrder order = upsertOrderHeader(remote, store, customer, now);
        int lineItemCount = upsertOrderLineItems(remote, store, order, now);
        int refundCount = upsertRefunds(remote, store, order, now);
        recalculateOrderCosts(order.getOrderId());
        OrderUpsertResult result = new OrderUpsertResult();
        result.setOrderId(order.getOrderId());
        result.setLineItemCount(lineItemCount);
        result.setRefundCount(refundCount);
        result.setShopifyUpdatedAt(remote.getUpdatedAt());
        return result;
    }

    /**
     * 创建订单同步任务并异步执行。
     *
     * @param storeId 店铺ID
     * @param backfill 是否执行补偿同步
     * @return 任务ID
     */
    private Long startOrderSync(Long storeId, boolean backfill) {
        ShopifyStore store = shopifyStoreService.selectByStoreId(storeId);
        if (store == null) {
            throw new ServiceException("店铺不存在");
        }
        ShopifyTask task = new ShopifyTask();
        task.setStoreId(store.getStoreId());
        task.setShopName(store.getShopName());
        task.setTaskName((backfill ? "补偿同步订单" : "增量同步订单") + DateUtils.getNowDate());
        task.setTaskType(ShopifyTaskType.ORDER_SYNC.getCode());
        task.setTaskStatus(ShopifyTaskStatus.PENDING.getCode());
        task.setProgress(0);
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setPartialCount(0);
        task.setFailedCount(0);
        task.setCreateTime(DateUtils.getNowDate());
        shopifyTaskService.insertShopifyTask(task);
        taskExecutor.execute(() -> executeOrderSync(store.getStoreId(), task.getTaskId(), backfill));
        return task.getTaskId();
    }

    /**
     * 执行订单轮询同步。
     *
     * @param storeId 店铺ID
     * @param taskId 任务ID
     * @param backfill 是否执行补偿同步
     */
    private void executeOrderSync(Long storeId, Long taskId, boolean backfill) {
        ShopifyTask task = shopifyTaskService.selectShopifyTaskByTaskId(taskId);
        ShopifyStore store = shopifyStoreService.selectByStoreId(storeId);
        ShopifyOrderSyncCursor cursor = taskSupport.ensureCursor(storeId);
        Date start = DateUtils.getNowDate();
        Date maxUpdatedAt = cursor.getLastSuccessUpdatedAt();
        int success = 0;
        int failed = 0;
        try {
            taskSupport.markTaskRunning(task, start);
            Date since = calculateQuerySince(cursor, Instant.now(), backfill);
            String pageCursor = null;
            int page = 0;
            do {
                ShopifyOrderPage orderPage = shopifyGraphQLClient.queryUpdatedOrders(storeId, since, pageCursor, ORDER_PAGE_SIZE);
                for (ShopifyImportedOrder remote : orderPage.getOrders()) {
                    try {
                        OrderUpsertResult result = upsertOrder(remote, store);
                        success++;
                        maxUpdatedAt = maxDate(maxUpdatedAt, result.getShopifyUpdatedAt());
                        taskSupport.insertOrderTaskDetail(task, result.getOrderId(), remote.getName(), remote.getId(), ShopifyTaskDetailStatus.SUCCESS.getCode(), null);
                    } catch (Exception e) {
                        failed++;
                        taskSupport.insertOrderTaskDetail(task, null, remote.getName(), remote.getId(), ShopifyTaskDetailStatus.FAILED.getCode(), e.getMessage());
                    }
                }
                pageCursor = orderPage.getEndCursor();
                page++;
                taskSupport.updateTaskProgress(taskId, success, failed, orderPage.isHasNextPage() && page < MAX_PAGE_COUNT);
                if (!orderPage.isHasNextPage() || page >= MAX_PAGE_COUNT) {
                    break;
                }
            } while (true);
            taskSupport.finishTask(taskId, success, failed, start, null);
            taskSupport.markCursorSuccess(cursor, taskId, maxUpdatedAt);
        } catch (Exception e) {
            taskSupport.finishTask(taskId, success, failed, start, e.getMessage());
            taskSupport.markCursorFailed(cursor, taskId, e.getMessage());
            log.error("Shopify 订单同步失败，storeId={}, taskId={}", storeId, taskId, e);
        }
    }

    private QueryWrapper<ShopifyOrder> buildOrderQuery(ShopifyOrderQuery query) {
        QueryWrapper<ShopifyOrder> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getStoreId() != null, "store_id", query.getStoreId())
                .like(StringUtils.isNotEmpty(query.getOrderName()), "order_name", query.getOrderName())
                .eq(StringUtils.isNotEmpty(query.getFinancialStatus()), "financial_status", query.getFinancialStatus())
                .eq(StringUtils.isNotEmpty(query.getFulfillmentStatus()), "fulfillment_status", query.getFulfillmentStatus())
                .eq(StringUtils.isNotEmpty(query.getPurchaseStatus()), "purchase_status", query.getPurchaseStatus())
                .eq(StringUtils.isNotEmpty(query.getFulfillmentSyncStatus()), "fulfillment_sync_status", query.getFulfillmentSyncStatus())
                .ge(query.getBeginDate() != null, "placed_at", query.getBeginDate())
                .le(query.getEndDate() != null, "placed_at", query.getEndDate())
                .and(StringUtils.isNotEmpty(query.getSearchKeyword()), w -> w
                        .like("order_name", query.getSearchKeyword())
                        .or()
                        .like("customer_name", query.getSearchKeyword())
                        .or()
                        .like("email", query.getSearchKeyword()))
                .orderByDesc("placed_at", "create_time");
        return wrapper;
    }

    private List<ShopifyOrderLineItem> selectLineItemsByOrderId(Long orderId) {
        if (orderId == null) {
            return List.of();
        }
        return lineItemMapper.selectList(new QueryWrapper<ShopifyOrderLineItem>().eq("order_id", orderId));
    }

    private ShopifyCustomer upsertCustomer(ShopifyImportedCustomer remote, Long storeId, Date now) {
        if (remote == null || StringUtils.isEmpty(remote.getId())) {
            return null;
        }
        ShopifyCustomer customer = customerMapper.selectOne(new QueryWrapper<ShopifyCustomer>()
                .eq("store_id", storeId)
                .eq("shopify_customer_id", remote.getId()));
        if (customer == null) {
            customer = new ShopifyCustomer();
            customer.setStoreId(storeId);
            customer.setShopifyCustomerId(remote.getId());
            customer.setCreateTime(now);
        }
        customer.setEmail(remote.getEmail());
        customer.setPhone(remote.getPhone());
        customer.setFirstName(remote.getFirstName());
        customer.setLastName(remote.getLastName());
        customer.setDisplayName(remote.getDisplayName());
        customer.setUpdateTime(now);
        if (customer.getCustomerId() == null) {
            customerMapper.insert(customer);
        } else {
            customerMapper.updateById(customer);
        }
        return customer;
    }

    private ShopifyOrder upsertOrderHeader(ShopifyImportedOrder remote, ShopifyStore store, ShopifyCustomer customer, Date now) {
        ShopifyOrder order = orderMapper.selectOne(new QueryWrapper<ShopifyOrder>()
                .eq("store_id", store.getStoreId())
                .eq("shopify_order_id", remote.getId()));
        if (order == null) {
            order = new ShopifyOrder();
            order.setStoreId(store.getStoreId());
            order.setShopName(store.getShopName());
            order.setShopifyOrderId(remote.getId());
            order.setCreateTime(now);
            order.setPurchaseStatus(STATUS_PENDING);
            order.setFulfillmentSyncStatus(STATUS_PENDING);
        }
        copyOrderFields(remote, customer, order);
        order.setLastShopifyImportTime(now);
        order.setUpdateTime(now);
        refreshProfit(order);
        if (order.getOrderId() == null) {
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
        }
        return order;
    }

    private void copyOrderFields(ShopifyImportedOrder remote, ShopifyCustomer customer, ShopifyOrder order) {
        order.setOrderName(remote.getName());
        order.setOrderNumber(remote.getOrderNumber());
        order.setCustomerId(customer == null ? null : customer.getCustomerId());
        order.setCustomerName(customer == null ? null : customer.getDisplayName());
        order.setEmail(StringUtils.isNotEmpty(remote.getEmail()) ? remote.getEmail() : customer == null ? null : customer.getEmail());
        order.setPhone(StringUtils.isNotEmpty(remote.getPhone()) ? remote.getPhone() : customer == null ? null : customer.getPhone());
        order.setShippingName(remote.getShippingName());
        order.setShippingPhone(remote.getShippingPhone());
        order.setShippingCountry(remote.getShippingCountry());
        order.setShippingProvince(remote.getShippingProvince());
        order.setShippingCity(remote.getShippingCity());
        order.setShippingZip(remote.getShippingZip());
        order.setShippingAddress1(remote.getShippingAddress1());
        order.setShippingAddress2(remote.getShippingAddress2());
        order.setFinancialStatus(remote.getFinancialStatus());
        order.setFulfillmentStatus(remote.getFulfillmentStatus());
        order.setCurrencyCode(remote.getCurrencyCode());
        order.setTotalPrice(remote.getTotalPrice());
        order.setSubtotalPrice(remote.getSubtotalPrice());
        order.setTotalTax(remote.getTotalTax());
        order.setTotalShippingPrice(remote.getTotalShippingPrice());
        order.setTotalRefund(remote.getTotalRefund());
        order.setPlacedAt(remote.getCreatedAt());
        order.setShopifyUpdatedAt(remote.getUpdatedAt());
        order.setCancelledAt(remote.getCancelledAt());
        order.setClosedAt(remote.getClosedAt());
    }

    private int upsertOrderLineItems(ShopifyImportedOrder remote, ShopifyStore store, ShopifyOrder order, Date now) {
        int count = 0;
        for (ShopifyImportedOrderLineItem remoteLine : nullSafe(remote.getLineItems())) {
            ProductVariant variant = findLocalVariant(store.getStoreId(), remoteLine);
            ShopifyOrderLineItem lineItem = lineItemMapper.selectOne(new QueryWrapper<ShopifyOrderLineItem>()
                    .eq("store_id", store.getStoreId())
                    .eq("shopify_line_item_id", remoteLine.getId()));
            if (lineItem == null) {
                lineItem = new ShopifyOrderLineItem();
                lineItem.setStoreId(store.getStoreId());
                lineItem.setShopifyLineItemId(remoteLine.getId());
                lineItem.setCreateTime(now);
                lineItem.setPurchaseStatus(STATUS_PENDING);
            }
            copyLineFields(remoteLine, order, variant, lineItem);
            lineItem.setUpdateTime(now);
            if (lineItem.getLineItemId() == null) {
                lineItemMapper.insert(lineItem);
            } else {
                lineItemMapper.updateById(lineItem);
            }
            ensurePurchaseTask(order, lineItem, now);
            count++;
        }
        return count;
    }

    private void copyLineFields(ShopifyImportedOrderLineItem remoteLine, ShopifyOrder order,
                                ProductVariant variant, ShopifyOrderLineItem lineItem) {
        lineItem.setOrderId(order.getOrderId());
        lineItem.setShopifyProductId(remoteLine.getProductId());
        lineItem.setShopifyVariantId(remoteLine.getVariantId());
        lineItem.setSku(StringUtils.isNotEmpty(remoteLine.getSku()) ? remoteLine.getSku() : variant == null ? null : variant.getSku());
        lineItem.setTitle(remoteLine.getTitle());
        lineItem.setVariantTitle(remoteLine.getVariantTitle());
        lineItem.setQuantity(remoteLine.getQuantity());
        lineItem.setUnitPrice(remoteLine.getUnitPrice());
        lineItem.setTotalPrice(remoteLine.getTotalPrice());
        if (variant != null) {
            lineItem.setProductId(variant.getProductId());
            lineItem.setVariantId(variant.getVariantId());
            lineItem.setPurchaseUrl(variant.getPurchaseUrl());
            lineItem.setPurchasePrice(variant.getPurchasePrice());
            lineItem.setPurchaseAmount(nvl(variant.getPurchasePrice()) * nvl(remoteLine.getQuantity()));
        }
    }

    private ProductVariant findLocalVariant(Long storeId, ShopifyImportedOrderLineItem remoteLine) {
        QueryWrapper<ProductVariant> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId);
        if (StringUtils.isNotEmpty(remoteLine.getVariantId())) {
            wrapper.eq("shopify_variant_id", remoteLine.getVariantId());
        } else if (StringUtils.isNotEmpty(remoteLine.getSku())) {
            wrapper.eq("sku", remoteLine.getSku());
        } else {
            return null;
        }
        return productVariantMapper.selectOne(wrapper);
    }

    private void ensurePurchaseTask(ShopifyOrder order, ShopifyOrderLineItem lineItem, Date now) {
        PurchaseTask task = purchaseTaskMapper.selectOne(new QueryWrapper<PurchaseTask>()
                .eq("order_line_item_id", lineItem.getLineItemId()));
        if (task == null) {
            task = new PurchaseTask();
            task.setOrderId(order.getOrderId());
            task.setOrderLineItemId(lineItem.getLineItemId());
            task.setStoreId(order.getStoreId());
            task.setOrderName(order.getOrderName());
            task.setSku(lineItem.getSku());
            task.setItemTitle(lineItem.getTitle());
            task.setQuantity(lineItem.getQuantity());
            task.setPurchaseUrl(lineItem.getPurchaseUrl());
            task.setExpectedPurchaseAmount(lineItem.getPurchaseAmount());
            task.setPurchaseStatus(STATUS_PENDING);
            task.setCreateTime(now);
            task.setUpdateTime(now);
            purchaseTaskMapper.insert(task);
        } else {
            task.setOrderName(order.getOrderName());
            task.setSku(lineItem.getSku());
            task.setItemTitle(lineItem.getTitle());
            task.setQuantity(lineItem.getQuantity());
            task.setPurchaseUrl(lineItem.getPurchaseUrl());
            task.setExpectedPurchaseAmount(lineItem.getPurchaseAmount());
            task.setUpdateTime(now);
            purchaseTaskMapper.updateById(task);
        }
    }

    private int upsertRefunds(ShopifyImportedOrder remote, ShopifyStore store, ShopifyOrder order, Date now) {
        int count = 0;
        for (ShopifyImportedRefund remoteRefund : nullSafe(remote.getRefunds())) {
            RefundRecord refund = refundRecordMapper.selectOne(new QueryWrapper<RefundRecord>()
                    .eq("store_id", store.getStoreId())
                    .eq("shopify_refund_id", remoteRefund.getId()));
            if (refund == null) {
                refund = new RefundRecord();
                refund.setOrderId(order.getOrderId());
                refund.setStoreId(store.getStoreId());
                refund.setShopifyRefundId(remoteRefund.getId());
                refund.setCreateTime(now);
            }
            refund.setRefundAmount(remoteRefund.getAmount());
            refund.setCurrencyCode(remoteRefund.getCurrencyCode());
            refund.setNote(remoteRefund.getNote());
            refund.setRefundTime(remoteRefund.getCreatedAt());
            refund.setUpdateTime(now);
            if (refund.getRefundId() == null) {
                refundRecordMapper.insert(refund);
            } else {
                refundRecordMapper.updateById(refund);
            }
            count++;
        }
        return count;
    }

    private void syncLinePurchaseFromTask(PurchaseTask oldTask, PurchaseTask updateTask) {
        ShopifyOrderLineItem lineItem = lineItemMapper.selectById(oldTask.getOrderLineItemId());
        if (lineItem == null) {
            return;
        }
        lineItem.setPurchaseStatus(updateTask.getPurchaseStatus());
        lineItem.setPurchaseRemark(updateTask.getRemark());
        if (updateTask.getActualPurchaseAmount() != null) {
            lineItem.setPurchaseAmount(updateTask.getActualPurchaseAmount());
            lineItem.setPurchasePrice(nvl(updateTask.getActualPurchaseAmount()) / Math.max(nvl(lineItem.getQuantity()), 1));
        }
        lineItem.setUpdateTime(DateUtils.getNowDate());
        lineItemMapper.updateById(lineItem);
    }

    private void recalculateOrderCosts(Long orderId) {
        ShopifyOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        int purchaseCost = selectLineItemsByOrderId(orderId).stream()
                .map(ShopifyOrderLineItem::getPurchaseAmount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int shippingCost = fulfillmentRecordMapper.selectList(new QueryWrapper<FulfillmentRecord>().eq("order_id", orderId)).stream()
                .map(FulfillmentRecord::getShippingFee)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        order.setPurchaseCost(purchaseCost);
        order.setShippingCost(shippingCost);
        refreshProfit(order);
        order.setUpdateTime(DateUtils.getNowDate());
        orderMapper.updateById(order);
    }

    private FulfillmentRecord createPendingFulfillmentRecord(ShopifyOrder order, FulfillmentSubmitRequest request) {
        FulfillmentRecord record = new FulfillmentRecord();
        record.setOrderId(order.getOrderId());
        record.setStoreId(order.getStoreId());
        record.setShopifyOrderId(order.getShopifyOrderId());
        record.setTrackingCompany(request.getTrackingCompany());
        record.setTrackingNumber(request.getTrackingNumber());
        record.setTrackingUrl(request.getTrackingUrl());
        record.setShippingFee(request.getShippingFee());
        record.setSyncStatus(STATUS_PENDING);
        record.setCreateTime(DateUtils.getNowDate());
        record.setUpdateTime(DateUtils.getNowDate());
        fulfillmentRecordMapper.insert(record);
        return record;
    }

    private void validateFulfillmentRequest(FulfillmentSubmitRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new ServiceException("订单ID不能为空");
        }
        if (StringUtils.isEmpty(request.getTrackingCompany()) || StringUtils.isEmpty(request.getTrackingNumber())) {
            throw new ServiceException("物流公司和运单号不能为空");
        }
    }

    private void updateOrderFulfillmentStatus(Long orderId, String syncStatus) {
        ShopifyOrder order = new ShopifyOrder();
        order.setOrderId(orderId);
        order.setFulfillmentSyncStatus(syncStatus);
        order.setUpdateTime(DateUtils.getNowDate());
        orderMapper.updateById(order);
    }

    private int sumOrders(List<ShopifyOrder> orders, String field) {
        return orders.stream().mapToInt(order -> switch (field) {
            case "sales" -> nvl(order.getTotalPrice());
            case "refund" -> nvl(order.getTotalRefund());
            case "purchase" -> nvl(order.getPurchaseCost());
            case "shipping" -> nvl(order.getShippingCost());
            case "profit" -> nvl(order.getGrossProfit());
            default -> 0;
        }).sum();
    }

    private Date maxDate(Date first, Date second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.after(second) ? first : second;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }

    /**
     * 订单写入结果。
     */
    @Data
    public static class OrderUpsertResult {
        /** 本地订单ID */
        private Long orderId;
        /** 订单行数量 */
        private int lineItemCount;
        /** 退款记录数量 */
        private int refundCount;
        /** Shopify 更新时间 */
        private Date shopifyUpdatedAt;
    }
}

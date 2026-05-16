package com.ruoyi.erp.service;

import com.ruoyi.erp.model.domain.FulfillmentRecord;
import com.ruoyi.erp.model.domain.PurchaseTask;
import com.ruoyi.erp.model.domain.RefundRecord;
import com.ruoyi.erp.model.domain.ShopifyOrder;
import com.ruoyi.erp.model.domain.ShopifyOrderSyncCursor;
import com.ruoyi.erp.model.dto.shopifyOrder.FulfillmentRecordQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.FulfillmentSubmitRequest;
import com.ruoyi.erp.model.dto.shopifyOrder.ProfitReportQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.PurchaseTaskEdit;
import com.ruoyi.erp.model.dto.shopifyOrder.PurchaseTaskQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.RefundRecordQuery;
import com.ruoyi.erp.model.dto.shopifyOrder.ShopifyOrderQuery;
import com.ruoyi.erp.model.vo.shopifyOrder.OrderProfitSummaryVo;

import java.util.List;

/**
 * Shopify 订单轻闭环 Service 接口
 */
public interface IShopifyOrderService {

    /**
     * 查询 Shopify 订单列表。
     *
     * @param query 查询条件
     * @return 订单列表
     */
    List<ShopifyOrder> selectOrderList(ShopifyOrderQuery query);

    /**
     * 查询 Shopify 订单详情。
     *
     * @param orderId 本地订单ID
     * @return 订单详情
     */
    ShopifyOrder selectOrderById(Long orderId);

    /**
     * 发起指定店铺订单增量同步。
     *
     * @param storeId 店铺ID
     * @return 任务ID
     */
    Long startIncrementalSync(Long storeId);

    /**
     * 发起指定店铺近 30 天订单补偿同步。
     *
     * @param storeId 店铺ID
     * @return 任务ID
     */
    Long startBackfillSync(Long storeId);

    /**
     * 轮询所有启用店铺的订单增量。
     */
    void syncIncrementalAllActiveStores();

    /**
     * 补偿同步所有启用店铺近 30 天订单。
     */
    void backfillRecentOrdersAllActiveStores();

    /**
     * 查询店铺订单同步游标。
     *
     * @param storeId 店铺ID
     * @return 游标
     */
    ShopifyOrderSyncCursor getCursor(Long storeId);

    /**
     * 查询人工采购任务列表。
     *
     * @param query 查询条件
     * @return 采购任务列表
     */
    List<PurchaseTask> selectPurchaseTaskList(PurchaseTaskQuery query);

    /**
     * 更新人工采购任务状态和金额。
     *
     * @param edit 编辑请求
     * @return 影响行数
     */
    int updatePurchaseTask(PurchaseTaskEdit edit);

    /**
     * 查询发货回传记录列表。
     *
     * @param query 查询条件
     * @return 发货记录列表
     */
    List<FulfillmentRecord> selectFulfillmentRecordList(FulfillmentRecordQuery query);

    /**
     * 创建发货记录并回传 Shopify 履约。
     *
     * @param request 发货回传请求
     * @return 发货记录
     */
    FulfillmentRecord submitFulfillment(FulfillmentSubmitRequest request);

    /**
     * 查询退款记录列表。
     *
     * @param query 查询条件
     * @return 退款记录列表
     */
    List<RefundRecord> selectRefundRecordList(RefundRecordQuery query);

    /**
     * 汇总订单利润报表。
     *
     * @param query 查询条件
     * @return 利润汇总
     */
    OrderProfitSummaryVo selectProfitSummary(ProfitReportQuery query);
}

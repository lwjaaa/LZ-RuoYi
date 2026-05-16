package com.ruoyi.erp.task;

import com.ruoyi.erp.service.IShopifyOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Shopify 订单轮询定时任务。
 */
@Slf4j
@Component("shopifyOrderPollTask")
public class ShopifyOrderPollTask {

    @Resource
    private IShopifyOrderService shopifyOrderService;

    /**
     * 遍历启用店铺并触发订单增量轮询。
     */
    public void pollOrders() {
        log.info("开始执行 Shopify 订单增量轮询定时任务");
        shopifyOrderService.syncIncrementalAllActiveStores();
        log.info("Shopify 订单增量轮询定时任务触发完成");
    }

    /**
     * 遍历启用店铺并触发近 30 天订单补偿轮询。
     */
    public void backfillRecentOrders() {
        log.info("开始执行 Shopify 订单近 30 天补偿轮询定时任务");
        shopifyOrderService.backfillRecentOrdersAllActiveStores();
        log.info("Shopify 订单近 30 天补偿轮询定时任务触发完成");
    }
}

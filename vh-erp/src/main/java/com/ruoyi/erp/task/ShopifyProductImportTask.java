package com.ruoyi.erp.task;

import com.ruoyi.erp.service.IShopifyProductImportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Shopify 商品反向同步定时任务
 */
@Slf4j
@Component("shopifyProductImportTask")
public class ShopifyProductImportTask {

    @Resource
    private IShopifyProductImportService shopifyProductImportService;

    /**
     * 遍历启用店铺并触发增量导入
     */
    public void syncIncrementalAllStores() {
        log.info("开始执行 Shopify 商品增量导入定时任务");
        shopifyProductImportService.syncIncrementalAllActiveStores();
        log.info("Shopify 商品增量导入定时任务触发完成");
    }
}

package com.ruoyi.erp.shopify.support;

import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.shopify.enums.ShopifyTaskStatus;

/**
 * 汇总 Shopify 同步任务和商品的最终状态。
 */
public final class ShopifyTaskStatusResolver {

    private ShopifyTaskStatusResolver() {
    }

    public static String resolveTaskStatus(int total, int success, int partial, int failed) {
        if (total <= 0) {
            return ShopifyTaskStatus.SUCCESS.getCode();
        }
        if (failed == 0 && partial == 0 && success >= total) {
            return ShopifyTaskStatus.SUCCESS.getCode();
        }
        if (success == 0 && partial == 0 && failed > 0) {
            return ShopifyTaskStatus.FAILED.getCode();
        }
        return ShopifyTaskStatus.PART_SUCCESS.getCode();
    }

    public static String resolveProductSyncStatus(boolean productCreatedOrUpdated, boolean childItemFailed) {
        if (!productCreatedOrUpdated) {
            return ProductConstants.SYNC_STATUS_FAILED;
        }
        return childItemFailed ? ProductConstants.SYNC_STATUS_PART_SUCCESS : ProductConstants.SYNC_STATUS_SUCCESS;
    }
}

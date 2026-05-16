package com.ruoyi.erp.order;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.erp.mapper.ShopifyOrderSyncCursorMapper;
import com.ruoyi.erp.model.domain.ShopifyOrderSyncCursor;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import com.ruoyi.erp.service.IShopifyTaskDetailService;
import com.ruoyi.erp.service.IShopifyTaskService;
import com.ruoyi.erp.shopify.enums.ShopifyTaskDetailItemType;
import com.ruoyi.erp.shopify.enums.ShopifyTaskStatus;
import com.ruoyi.erp.shopify.enums.ShopifyTaskStep;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Shopify 订单同步任务、游标和诊断明细的公共支撑逻辑。
 */
@Component
public class ShopifyOrderTaskSupport {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PART_SUCCESS = "PART_SUCCESS";

    @Resource
    private ShopifyOrderSyncCursorMapper cursorMapper;
    @Resource
    private IShopifyTaskService shopifyTaskService;
    @Resource
    private IShopifyTaskDetailService shopifyTaskDetailService;

    /**
     * 查询或创建店铺订单同步游标。
     *
     * @param storeId 店铺ID
     * @return 店铺订单同步游标
     */
    public ShopifyOrderSyncCursor ensureCursor(Long storeId) {
        ShopifyOrderSyncCursor cursor = cursorMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ShopifyOrderSyncCursor>()
                .eq("store_id", storeId));
        if (cursor != null) {
            return cursor;
        }
        cursor = new ShopifyOrderSyncCursor();
        cursor.setStoreId(storeId);
        cursor.setStatus(STATUS_PENDING);
        cursor.setCreateTime(DateUtils.getNowDate());
        cursorMapper.insert(cursor);
        return cursor;
    }

    /**
     * 将 Shopify 任务标记为运行中。
     *
     * @param task 任务实体
     * @param start 开始时间
     */
    public void markTaskRunning(ShopifyTask task, Date start) {
        task.setTaskStatus(ShopifyTaskStatus.RUNNING.getCode());
        task.setStartTime(start);
        task.setProgress(1);
        task.setUpdateTime(DateUtils.getNowDate());
        shopifyTaskService.updateShopifyTask(task);
    }

    /**
     * 更新订单同步任务的进度数字。
     *
     * @param taskId 任务ID
     * @param success 成功数量
     * @param failed 失败数量
     * @param hasMore 是否还有后续分页
     */
    public void updateTaskProgress(Long taskId, int success, int failed, boolean hasMore) {
        ShopifyTask task = new ShopifyTask();
        task.setTaskId(taskId);
        task.setTotalCount(success + failed);
        task.setSuccessCount(success);
        task.setFailedCount(failed);
        task.setPartialCount(0);
        task.setProgress(hasMore ? 50 : 95);
        shopifyTaskService.updateShopifyTask(task);
    }

    /**
     * 完成订单同步任务并写入最终状态。
     *
     * @param taskId 任务ID
     * @param success 成功数量
     * @param failed 失败数量
     * @param start 开始时间
     * @param errorMessage 总体错误信息
     */
    public void finishTask(Long taskId, int success, int failed, Date start, String errorMessage) {
        ShopifyTask task = new ShopifyTask();
        task.setTaskId(taskId);
        task.setTaskStatus(errorMessage != null ? ShopifyTaskStatus.FAILED.getCode()
                : failed > 0 ? STATUS_PART_SUCCESS : STATUS_SUCCESS);
        task.setTotalCount(success + failed);
        task.setSuccessCount(success);
        task.setFailedCount(failed);
        task.setPartialCount(0);
        task.setProgress(100);
        task.setErrorMessage(errorMessage);
        task.setEndTime(DateUtils.getNowDate());
        task.setExecutionTime(task.getEndTime().getTime() - start.getTime());
        shopifyTaskService.updateShopifyTask(task);
    }

    /**
     * 将订单同步游标标记为成功。
     *
     * @param cursor 游标实体
     * @param taskId 最近任务ID
     * @param maxUpdatedAt 本次同步看到的最大更新时间
     */
    public void markCursorSuccess(ShopifyOrderSyncCursor cursor, Long taskId, Date maxUpdatedAt) {
        cursor.setStatus(STATUS_SUCCESS);
        cursor.setLastTaskId(taskId);
        cursor.setLastSuccessUpdatedAt(maxDate(cursor.getLastSuccessUpdatedAt(), maxUpdatedAt));
        cursor.setLastSuccessSyncTime(DateUtils.getNowDate());
        cursor.setLastErrorSummary(null);
        cursor.setUpdateTime(DateUtils.getNowDate());
        cursorMapper.updateById(cursor);
    }

    /**
     * 将订单同步游标标记为失败。
     *
     * @param cursor 游标实体
     * @param taskId 最近任务ID
     * @param errorMessage 错误摘要
     */
    public void markCursorFailed(ShopifyOrderSyncCursor cursor, Long taskId, String errorMessage) {
        cursor.setStatus(STATUS_FAILED);
        cursor.setLastTaskId(taskId);
        cursor.setLastErrorSummary(errorMessage);
        cursor.setUpdateTime(DateUtils.getNowDate());
        cursorMapper.updateById(cursor);
    }

    /**
     * 写入订单级任务诊断明细。
     *
     * @param task 任务实体
     * @param orderId 本地订单ID
     * @param name 订单名称
     * @param shopifyId Shopify 订单ID
     * @param status 明细状态
     * @param error 错误信息
     */
    public void insertOrderTaskDetail(ShopifyTask task, Long orderId, String name, String shopifyId, String status, String error) {
        ShopifyTaskDetail detail = new ShopifyTaskDetail();
        detail.setTaskId(task.getTaskId());
        detail.setStoreId(task.getStoreId());
        detail.setShopName(task.getShopName());
        detail.setItemType(ShopifyTaskDetailItemType.ORDER.getCode());
        detail.setItemId(orderId);
        detail.setItemName(name);
        detail.setShopifyId(shopifyId);
        detail.setStep(ShopifyTaskStep.ORDER_SYNC.getCode());
        detail.setStatus(status);
        detail.setErrorMessage(error);
        shopifyTaskDetailService.insertShopifyTaskDetail(detail);
    }

    /**
     * 返回两个日期中的较晚值。
     *
     * @param first 第一个日期
     * @param second 第二个日期
     * @return 较晚日期
     */
    private Date maxDate(Date first, Date second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.after(second) ? first : second;
    }
}

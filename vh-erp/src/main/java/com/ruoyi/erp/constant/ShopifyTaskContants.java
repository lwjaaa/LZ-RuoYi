package com.ruoyi.erp.constant;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/4/29 16:53
 **/
public interface ShopifyTaskContants {

    /**
     * 商品管理触发的任务组
     */
    String TASK_GROUP_PRODUCT = "1";

    /**
     * 媒体管理触发的任务组
     */
    String TASK_GROUP_MEDIA = "2";

    /**
     * 商品创建
     */
    String TASK_TYPE_PRODUCT_SYNC = "PRODUCT_SYNC";
    /**
     * 批量商品创建
     */
    String TASK_TYPE_PRODUCT_SYNC_BATCH = "PRODUCT_SYNC_BATCH";

    /**
     * 批量媒体文件同步
     */
    String TASK_TYPE_MEDIA_SYNC_BATCH = "MEDIA_SYNC_BATCH";

    /**
     * 媒体同步
     */
    String TASK_TYPE_MEDIA_SYNC = "MEDIA_SYNC";

    /**
     * 关联业务类型-商品
     */
    String TASK_BUSINESS_TYPE_PRODUCT = "PRODUCT";
    /**
     * 关联业务类型-媒体
     */
    String TASK_BUSINESS_TYPE_MEDIA = "MEDIA";

    /**
     * 任务状态-待执行
     */
    String TASK_STATUS_PENDING = "PENDING";

    /**
     * 任务状态-执行中
     */
    String TASK_STATUS_RUNNING = "RUNNING";

    /**
     * 任务状态-成功
     */
    String TASK_STATUS_SUCCESS = "SUCCESS";
    /**
     * 任务状态-失败
     */
    String TASK_STATUS_FAILED = "FAILED";
    /**
     * 任务状态-部分成功
     */
    String TASK_STATUS_PART_SUCCESS = "PART_SUCCESS";
    /**
     * 任务状态-已取消
     */
    String TASK_STATUS_CANCELLED = "CANCELLED";

}
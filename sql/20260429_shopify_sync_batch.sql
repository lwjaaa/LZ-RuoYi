-- 为 erp_shopify_task 表添加批量推送统计字段
-- 执行日期: 2026-04-29

ALTER TABLE erp_shopify_task
ADD COLUMN `total_count` int(11) DEFAULT 0 COMMENT '总数（批量任务总数）' AFTER `root_task_id`,
ADD COLUMN `success_count` int(11) DEFAULT 0 COMMENT '成功数' AFTER `total_count`,
ADD COLUMN `failed_count` int(11) DEFAULT 0 COMMENT '失败数' AFTER `success_count`;

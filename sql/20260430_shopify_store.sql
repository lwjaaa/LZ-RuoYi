-- Shopify 店铺配置表
-- 用于管理多个 Shopify 店铺的 API 配置信息
-- 执行日期: 2026-04-30

DROP TABLE IF EXISTS `erp_shopify_store`;
CREATE TABLE `erp_shopify_store` (
    `store_id`          bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '店铺主键',
    `store_name`        varchar(100)    NOT NULL COMMENT '店铺名称 (用于展示)',
    `shop_name`         varchar(100)    NOT NULL COMMENT 'Shop 名称 (myshopify.com 前的名称)',
    `api_version`       varchar(20)     NOT NULL DEFAULT '2026-04' COMMENT 'API 版本 (如: 2026-04)',
    `api_key`           varchar(255)    DEFAULT NULL COMMENT 'API Key (用于 OAuth 或 Private App)',
    `api_secret`        varchar(255)    DEFAULT NULL COMMENT 'API Secret (用于 OAuth)',
    `access_token`      varchar(1024)   DEFAULT NULL COMMENT 'Access Token (Private App 或 OAuth 存储)',
    `refresh_token`     varchar(1024)   DEFAULT NULL COMMENT 'Refresh Token (OAuth 刷新令牌)',
    `token_expires_at`  datetime        DEFAULT NULL COMMENT 'Token 过期时间',
    `base_url`          varchar(500)    DEFAULT NULL COMMENT '自定义 API 端点 (通常不需要，Shopify 会自动生成)',
    `inventory_location_id` varchar(255) DEFAULT NULL COMMENT 'Shopify 库存仓库 Location ID',
    `inventory_location_name` varchar(255) DEFAULT NULL COMMENT 'Shopify 库存仓库名称',
    `inventory_tracked` char(1) DEFAULT '0' COMMENT '是否跟踪库存 (0:否, 1:是)',
    `default_inventory_quantity` int(11) DEFAULT 100 COMMENT '默认库存数量',
    `inventory_policy` varchar(20) DEFAULT 'DENY' COMMENT '缺货销售策略: DENY, CONTINUE',
    `publish_publication_ids` text COMMENT '默认发布 Publication ID，英文逗号分隔',
    `publish_publication_names` text COMMENT '默认发布渠道名称，英文逗号分隔',
    `is_active`         char(1)         DEFAULT '1' COMMENT '是否启用 (0:禁用, 1:启用)',
    `is_default`        char(1)         DEFAULT '0' COMMENT '是否默认店铺 (0:否, 1:是)',
    `auth_mode`         varchar(20)     DEFAULT 'PRIVATE_APP' COMMENT '认证模式: PRIVATE_APP, OAUTH',
    `status`            varchar(20)      DEFAULT 'CONNECTED' COMMENT '连接状态: CONNECTED, DISCONNECTED, EXPIRED',
    `last_sync_time`    datetime        DEFAULT NULL COMMENT '最后同步时间',
    `sync_count`        int(11)         DEFAULT 0 COMMENT '同步次数',
    `remark`            varchar(500)     DEFAULT NULL COMMENT '备注',
    `create_by`         varchar(64)      DEFAULT '' COMMENT '创建者',
    `create_time`       datetime        DEFAULT NULL COMMENT '创建时间',
    `update_by`         varchar(64)      DEFAULT '' COMMENT '更新者',
    `update_time`       datetime        DEFAULT NULL COMMENT '更新时间',
    `del_flag`          char(1)          DEFAULT '0' COMMENT '删除标志 (0代表存在 2代表删除)',

    PRIMARY KEY (`store_id`),
    UNIQUE KEY `uk_shop_name` (`shop_name`, `del_flag`),
    KEY `idx_is_default` (`is_default`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shopify 店铺配置表';


-- 添加任务表关联字段
ALTER TABLE `erp_shopify_task` ADD COLUMN `store_id` bigint(20) DEFAULT NULL COMMENT '关联的店铺ID' AFTER `task_id`;
ALTER TABLE `erp_shopify_task` ADD COLUMN `shop_name` varchar(100) DEFAULT NULL COMMENT 'Shopify 店铺名称' AFTER `store_id`;

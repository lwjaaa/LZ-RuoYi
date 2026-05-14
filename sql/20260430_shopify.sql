-- Shopify 店铺配置表
-- 用于管理多个 Shopify 店铺的 API 配置信息

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
    `publish_publication_ids` text COMMENT '自动发布 Publication ID，英文逗号分隔',
    `publish_publication_names` text COMMENT '自动发布渠道名称，英文逗号分隔',
    `default_product_status` varchar(16) DEFAULT 'DRAFT' COMMENT '推送到 Shopify 时的默认商品状态：DRAFT, ACTIVE',
    `required_product_fields` varchar(512) DEFAULT 'TITLE,SPU,MAIN_MEDIA,DESCRIPTION,BODY_HTML,VARIANT,PRICE,FREIGHT,VARIANT_MEDIA,SKU' COMMENT '商品资料必填字段编码，英文逗号分隔',
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


-- shopify数据同步任务表
DROP TABLE IF EXISTS `erp_shopify_task`;
CREATE TABLE `erp_shopify_task`
(
    `task_id`        bigint       NOT NULL COMMENT '任务主键',
    `store_id`       bigint(20)   DEFAULT NULL COMMENT '关联的店铺ID',
    `shop_name`      varchar(100) DEFAULT NULL COMMENT 'Shopify 店铺名称',
    `task_name`      varchar(128) NOT NULL COMMENT '任务名称',
    `task_type`      varchar(32)  NOT NULL COMMENT '任务类型（PRODUCT_CREATE=商品创建，MEDIA_SYNC=媒体同步,PRODUCT_CREATE_BATCH=批量商品创建）',#这个任务在做什么
    -- 执行状态
    `task_status` varchar(16) DEFAULT 'PENDING' COMMENT '任务状态（PENDING=待执行，RUNNING=执行中，SUCCESS=成功，FAILED=失败，PART_SUCCESS=部分成功，CANCELLED=已取消）',
    `progress`       int          DEFAULT '0' COMMENT '执行进度（0-100）',
    `total_count`    int(11)      DEFAULT 0 COMMENT '总数（批量任务总数）',
    `success_count`  int(11)      DEFAULT 0 COMMENT '成功数',
    `partial_count` int(11) DEFAULT 0 COMMENT '部分成功数',
    `failed_count`   int(11)      DEFAULT 0 COMMENT '失败数',
    -- 错误处理
    `error_message`  text COMMENT '错误信息',

    -- 执行结果
    `execution_time` bigint       DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `start_time`     datetime     DEFAULT NULL COMMENT '开始执行时间',
    `end_time`       datetime     DEFAULT NULL COMMENT '结束时间',

    -- 若依标准字段
    `create_by`      varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`task_id`),
    KEY `idx_shopify_task_type` (`task_type`),
    KEY `idx_shopify_task_status` (`task_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Shopify 任务配置表';

-- shopify数据同步任务明细表
CREATE TABLE IF NOT EXISTS `erp_shopify_task_detail`
(
    `detail_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    `task_id`       bigint       NOT NULL COMMENT '任务ID',
    `store_id`      bigint       DEFAULT NULL COMMENT '店铺ID',
    `shop_name`     varchar(100) DEFAULT NULL COMMENT 'Shopify 店铺名称',
    `product_id`    bigint       DEFAULT NULL COMMENT '商品ID',
    `item_type`     varchar(20)  NOT NULL COMMENT '明细类型：PRODUCT、VARIANT、MEDIA',
    `item_id`       bigint       DEFAULT NULL COMMENT '本地商品/变体/媒体ID',
    `item_name`     varchar(255) DEFAULT NULL COMMENT '商品标题、SKU 或媒体文件名',
    `shopify_id`    varchar(128) DEFAULT NULL COMMENT 'Shopify 资源ID',
    `step`          varchar(64)  DEFAULT NULL COMMENT '同步步骤',
    `status`        varchar(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT '明细状态：SUCCESS、FAILED、PART_SUCCESS、SKIPPED',
    `error_code`    varchar(64)  DEFAULT NULL COMMENT '错误编码',
    `error_field`   varchar(255) DEFAULT NULL COMMENT 'Shopify userErrors.field',
    `input_index`   int          DEFAULT NULL COMMENT '批量输入下标',
    `error_message` text         DEFAULT NULL COMMENT '失败原因',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`detail_id`),
    KEY `idx_task_detail_task` (`task_id`),
    KEY `idx_task_detail_product` (`product_id`, `create_time`),
    KEY `idx_task_detail_item` (`item_type`, `item_id`),
    KEY `idx_task_detail_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Shopify 同步任务明细表';

-- Shopify 商品反向同步：仅保留店铺维度同步游标，商品归属直接写入业务表
CREATE TABLE IF NOT EXISTS `erp_shopify_sync_cursor`
(
    `cursor_id`                bigint       NOT NULL COMMENT '游标主键',
    `store_id`                 bigint       NOT NULL COMMENT '店铺ID',
    `sync_mode`                varchar(64)  NOT NULL COMMENT '同步模式，如 PRODUCT_IMPORT',
    `status`                   varchar(16)  DEFAULT 'SUCCESS' COMMENT '游标状态',
    `last_success_updated_at`  datetime     DEFAULT NULL COMMENT '上次成功推进到的 Shopify updatedAt',
    `last_success_sync_time`   datetime     DEFAULT NULL COMMENT '上次成功同步完成时间',
    `last_bulk_operation_id`   varchar(128) DEFAULT NULL COMMENT '最近一次 Bulk Operation ID',
    `last_task_id`             bigint       DEFAULT NULL COMMENT '最近一次任务ID',
    `last_error_summary`       text         DEFAULT NULL COMMENT '最近一次错误摘要',
    `create_time`              datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`              datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`cursor_id`),
    UNIQUE KEY `uk_shopify_cursor_store_mode` (`store_id`, `sync_mode`),
    KEY `idx_shopify_cursor_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Shopify 反向同步游标';

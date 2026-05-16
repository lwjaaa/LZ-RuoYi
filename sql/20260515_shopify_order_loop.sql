-- Shopify 订单轻闭环：订单轮询、人工采购、履约回传、售后和利润

CREATE TABLE IF NOT EXISTS `erp_shopify_order_sync_cursor`
(
    `cursor_id`               bigint       NOT NULL COMMENT '游标主键',
    `store_id`                bigint       NOT NULL COMMENT '店铺ID',
    `status`                  varchar(16)  DEFAULT 'PENDING' COMMENT '游标状态',
    `last_success_updated_at` datetime     DEFAULT NULL COMMENT '上次成功推进到的 Shopify 订单更新时间',
    `last_success_sync_time`  datetime     DEFAULT NULL COMMENT '上次成功同步完成时间',
    `last_task_id`            bigint       DEFAULT NULL COMMENT '最近一次任务ID',
    `last_error_summary`      varchar(500) DEFAULT NULL COMMENT '最近一次错误摘要',
    `create_time`             datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`             datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`cursor_id`),
    UNIQUE KEY `uk_order_cursor_store` (`store_id`),
    KEY `idx_order_cursor_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Shopify 订单轮询游标表';

CREATE TABLE IF NOT EXISTS `erp_shopify_customer`
(
    `customer_id`         bigint       NOT NULL COMMENT '客户主键',
    `store_id`            bigint       NOT NULL COMMENT '店铺ID',
    `shopify_customer_id` varchar(128) NOT NULL COMMENT 'Shopify 客户ID',
    `email`               varchar(255) DEFAULT NULL COMMENT '邮箱',
    `phone`               varchar(64)  DEFAULT NULL COMMENT '手机号',
    `first_name`          varchar(128) DEFAULT NULL COMMENT '名',
    `last_name`           varchar(128) DEFAULT NULL COMMENT '姓',
    `display_name`        varchar(255) DEFAULT NULL COMMENT '展示名',
    `create_time`         datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`         datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`customer_id`),
    UNIQUE KEY `uk_shopify_customer` (`store_id`, `shopify_customer_id`),
    KEY `idx_shopify_customer_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Shopify 客户快照表';

CREATE TABLE IF NOT EXISTS `erp_shopify_order`
(
    `order_id`                 bigint         NOT NULL COMMENT '订单主键',
    `store_id`                 bigint         NOT NULL COMMENT '店铺ID',
    `shop_name`                varchar(100)   DEFAULT NULL COMMENT 'Shopify 店铺名称',
    `shopify_order_id`         varchar(128)   NOT NULL COMMENT 'Shopify 订单ID',
    `order_name`               varchar(64)    DEFAULT NULL COMMENT '订单号',
    `order_number`             bigint         DEFAULT NULL COMMENT '数字订单号',
    `customer_id`              bigint         DEFAULT NULL COMMENT '客户主键',
    `customer_name`            varchar(255)   DEFAULT NULL COMMENT '客户展示名',
    `email`                    varchar(255)   DEFAULT NULL COMMENT '客户邮箱',
    `phone`                    varchar(64)    DEFAULT NULL COMMENT '客户电话',
    `shipping_name`            varchar(255)   DEFAULT NULL COMMENT '收货人',
    `shipping_phone`           varchar(64)    DEFAULT NULL COMMENT '收货电话',
    `shipping_country`         varchar(128)   DEFAULT NULL COMMENT '收货国家',
    `shipping_province`        varchar(128)   DEFAULT NULL COMMENT '收货省份',
    `shipping_city`            varchar(128)   DEFAULT NULL COMMENT '收货城市',
    `shipping_zip`             varchar(64)    DEFAULT NULL COMMENT '收货邮编',
    `shipping_address1`        varchar(500)   DEFAULT NULL COMMENT '收货地址1',
    `shipping_address2`        varchar(500)   DEFAULT NULL COMMENT '收货地址2',
    `financial_status`         varchar(32)    DEFAULT NULL COMMENT '付款状态',
    `fulfillment_status`       varchar(32)    DEFAULT NULL COMMENT '履约状态',
    `purchase_status`          varchar(32)    DEFAULT 'PENDING' COMMENT '采购状态',
    `fulfillment_sync_status`  varchar(32)    DEFAULT 'PENDING' COMMENT '发货回传状态',
    `currency_code`            varchar(16)    DEFAULT NULL COMMENT '币种',
    `total_price`              int            DEFAULT 0 COMMENT '订单总额，单位为分',
    `subtotal_price`           int            DEFAULT 0 COMMENT '商品小计，单位为分',
    `total_tax`                int            DEFAULT 0 COMMENT '税费，单位为分',
    `total_shipping_price`     int            DEFAULT 0 COMMENT 'Shopify 收取运费，单位为分',
    `total_refund`             int            DEFAULT 0 COMMENT '退款金额，单位为分',
    `purchase_cost`            int            DEFAULT 0 COMMENT '采购成本，单位为分',
    `shipping_cost`            int            DEFAULT 0 COMMENT '实际发货成本，单位为分',
    `gross_profit`             int            DEFAULT 0 COMMENT '毛利，单位为分',
    `gross_profit_rate`        decimal(10, 4) DEFAULT 0.0000 COMMENT '毛利率',
    `placed_at`                datetime       DEFAULT NULL COMMENT 'Shopify 下单时间',
    `shopify_updated_at`       datetime       DEFAULT NULL COMMENT 'Shopify 更新时间',
    `cancelled_at`             datetime       DEFAULT NULL COMMENT 'Shopify 取消时间',
    `closed_at`                datetime       DEFAULT NULL COMMENT 'Shopify 关闭时间',
    `last_shopify_import_time` datetime       DEFAULT NULL COMMENT '最近一次订单轮询导入时间',
    `remark`                   varchar(500)   DEFAULT NULL COMMENT '备注',
    `create_time`              datetime       DEFAULT NULL COMMENT '创建时间',
    `update_time`              datetime       DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_shopify_order` (`store_id`, `shopify_order_id`),
    KEY `idx_shopify_order_name` (`order_name`),
    KEY `idx_shopify_order_status` (`financial_status`, `fulfillment_status`, `purchase_status`),
    KEY `idx_shopify_order_updated` (`store_id`, `shopify_updated_at`),
    KEY `idx_shopify_order_placed` (`store_id`, `placed_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Shopify 订单表';

CREATE TABLE IF NOT EXISTS `erp_shopify_order_line_item`
(
    `line_item_id`        bigint       NOT NULL COMMENT '订单行主键',
    `order_id`            bigint       NOT NULL COMMENT '本地订单ID',
    `store_id`            bigint       NOT NULL COMMENT '店铺ID',
    `shopify_line_item_id` varchar(128) NOT NULL COMMENT 'Shopify 订单行ID',
    `shopify_product_id`  varchar(128) DEFAULT NULL COMMENT 'Shopify 商品ID',
    `shopify_variant_id`  varchar(128) DEFAULT NULL COMMENT 'Shopify 变体ID',
    `product_id`          bigint       DEFAULT NULL COMMENT '本地商品ID',
    `variant_id`          bigint       DEFAULT NULL COMMENT '本地变体ID',
    `sku`                 varchar(128) DEFAULT NULL COMMENT 'SKU',
    `title`               varchar(500) DEFAULT NULL COMMENT '商品标题',
    `variant_title`       varchar(255) DEFAULT NULL COMMENT '变体标题',
    `quantity`            int          DEFAULT 0 COMMENT '购买数量',
    `unit_price`          int          DEFAULT 0 COMMENT '单价，单位为分',
    `total_price`         int          DEFAULT 0 COMMENT '行总额，单位为分',
    `source_url`          varchar(1000) DEFAULT NULL COMMENT '商品来源链接',
    `purchase_url`        varchar(1000) DEFAULT NULL COMMENT '采购链接',
    `purchase_price`      int          DEFAULT 0 COMMENT '采购单价，单位为分',
    `purchase_amount`     int          DEFAULT 0 COMMENT '采购总额，单位为分',
    `purchase_status`     varchar(32)  DEFAULT 'PENDING' COMMENT '采购状态',
    `purchase_remark`     varchar(500) DEFAULT NULL COMMENT '采购备注',
    `create_time`         datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`         datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`line_item_id`),
    UNIQUE KEY `uk_order_line_shopify` (`store_id`, `shopify_line_item_id`),
    KEY `idx_order_line_order` (`order_id`),
    KEY `idx_order_line_sku` (`sku`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Shopify 订单行表';

CREATE TABLE IF NOT EXISTS `erp_purchase_task`
(
    `purchase_task_id`        bigint        NOT NULL COMMENT '采购任务主键',
    `order_id`                bigint        NOT NULL COMMENT '本地订单ID',
    `order_line_item_id`      bigint        NOT NULL COMMENT '本地订单行ID',
    `store_id`                bigint        NOT NULL COMMENT '店铺ID',
    `order_name`              varchar(64)   DEFAULT NULL COMMENT '订单号',
    `sku`                     varchar(128)  DEFAULT NULL COMMENT 'SKU',
    `item_title`              varchar(500)  DEFAULT NULL COMMENT '商品标题',
    `quantity`                int           DEFAULT 0 COMMENT '采购数量',
    `purchase_url`            varchar(1000) DEFAULT NULL COMMENT '采购链接',
    `expected_purchase_amount` int          DEFAULT 0 COMMENT '预计采购金额，单位为分',
    `actual_purchase_amount`  int           DEFAULT NULL COMMENT '实际采购金额，单位为分',
    `purchase_status`         varchar(32)   DEFAULT 'PENDING' COMMENT '采购状态',
    `exception_reason`        varchar(500)  DEFAULT NULL COMMENT '采购异常原因',
    `remark`                  varchar(500)  DEFAULT NULL COMMENT '采购备注',
    `purchased_at`            datetime      DEFAULT NULL COMMENT '采购完成时间',
    `create_time`             datetime      DEFAULT NULL COMMENT '创建时间',
    `update_time`             datetime      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`purchase_task_id`),
    UNIQUE KEY `uk_purchase_order_line` (`order_line_item_id`),
    KEY `idx_purchase_task_status` (`purchase_status`, `create_time`),
    KEY `idx_purchase_task_order` (`order_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '人工采购任务表';

CREATE TABLE IF NOT EXISTS `erp_fulfillment_record`
(
    `fulfillment_id`                bigint        NOT NULL COMMENT '发货记录主键',
    `order_id`                      bigint        NOT NULL COMMENT '本地订单ID',
    `store_id`                      bigint        NOT NULL COMMENT '店铺ID',
    `shopify_order_id`              varchar(128)  DEFAULT NULL COMMENT 'Shopify 订单ID',
    `shopify_fulfillment_order_id`  varchar(128)  DEFAULT NULL COMMENT 'Shopify FulfillmentOrder ID',
    `shopify_fulfillment_id`        varchar(128)  DEFAULT NULL COMMENT 'Shopify Fulfillment ID',
    `tracking_company`              varchar(128)  DEFAULT NULL COMMENT '物流公司',
    `tracking_number`               varchar(128)  DEFAULT NULL COMMENT '运单号',
    `tracking_url`                  varchar(1000) DEFAULT NULL COMMENT '物流查询链接',
    `shipping_fee`                  int           DEFAULT 0 COMMENT '物流费用，单位为分',
    `sync_status`                   varchar(32)   DEFAULT 'PENDING' COMMENT '回传状态',
    `error_message`                 text          DEFAULT NULL COMMENT '错误信息',
    `fulfilled_at`                  datetime      DEFAULT NULL COMMENT '发货时间',
    `create_time`                   datetime      DEFAULT NULL COMMENT '创建时间',
    `update_time`                   datetime      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`fulfillment_id`),
    UNIQUE KEY `uk_fulfillment_tracking` (`store_id`, `tracking_number`),
    KEY `idx_fulfillment_order` (`order_id`),
    KEY `idx_fulfillment_status` (`sync_status`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Shopify 发货回传记录表';

CREATE TABLE IF NOT EXISTS `erp_refund_record`
(
    `refund_id`         bigint       NOT NULL COMMENT '退款记录主键',
    `order_id`          bigint       NOT NULL COMMENT '本地订单ID',
    `store_id`          bigint       NOT NULL COMMENT '店铺ID',
    `shopify_refund_id` varchar(128) NOT NULL COMMENT 'Shopify 退款ID',
    `refund_amount`     int          DEFAULT 0 COMMENT '退款金额，单位为分',
    `currency_code`     varchar(16)  DEFAULT NULL COMMENT '币种',
    `reason`            varchar(255) DEFAULT NULL COMMENT '退款原因',
    `note`              varchar(500) DEFAULT NULL COMMENT 'Shopify 备注',
    `responsibility`    varchar(64)  DEFAULT NULL COMMENT '责任归类',
    `refund_time`       datetime     DEFAULT NULL COMMENT '退款时间',
    `create_time`       datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`       datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`refund_id`),
    UNIQUE KEY `uk_refund_shopify` (`store_id`, `shopify_refund_id`),
    KEY `idx_refund_order` (`order_id`),
    KEY `idx_refund_time` (`refund_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Shopify 售后退款记录表';

INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `remark`)
SELECT 'Shopify订单增量轮询', 'DEFAULT', 'shopifyOrderPollTask.pollOrders()', '0 0/5 * * * ?', '3', '1', '0', 'admin', sysdate(), '每 5 分钟轮询 Shopify 最近更新订单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'shopifyOrderPollTask.pollOrders()');

INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `remark`)
SELECT 'Shopify订单近30天补偿轮询', 'DEFAULT', 'shopifyOrderPollTask.backfillRecentOrders()', '0 0 3 * * ?', '3', '1', '0', 'admin', sysdate(), '夜间补拉近 30 天 Shopify 订单防漏单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'shopifyOrderPollTask.backfillRecentOrders()');

SET @erp_parent_id := (SELECT `menu_id` FROM `sys_menu` WHERE `path` = 'erp' AND `menu_type` = 'M' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 'ERP管理', 0, 5, 'erp', NULL, '', 'Erp', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), 'ERP业务目录'
WHERE @erp_parent_id IS NULL;

SET @erp_parent_id := (SELECT `menu_id` FROM `sys_menu` WHERE `path` = 'erp' AND `menu_type` = 'M' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '订单中心', @erp_parent_id, 50, 'order', 'erp/order/index', '', 'ErpOrder', 1, 0, 'C', '0', '0', 'erp:order:list', 'list', 'admin', sysdate(), 'Shopify订单中心'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:order:list' AND `menu_type` = 'C');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '采购任务', @erp_parent_id, 51, 'purchase', 'erp/purchase/index', '', 'ErpPurchase', 1, 0, 'C', '0', '0', 'erp:purchase-task:list', 'shopping', 'admin', sysdate(), '人工采购任务'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:purchase-task:list' AND `menu_type` = 'C');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '发货回传', @erp_parent_id, 52, 'fulfillment', 'erp/fulfillment/index', '', 'ErpFulfillment', 1, 0, 'C', '0', '0', 'erp:fulfillment:list', 'logistics', 'admin', sysdate(), 'Shopify发货回传'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:fulfillment:list' AND `menu_type` = 'C');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '售后记录', @erp_parent_id, 53, 'refund', 'erp/refund/index', '', 'ErpRefund', 1, 0, 'C', '0', '0', 'erp:refund:list', 'message', 'admin', sysdate(), 'Shopify售后退款记录'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:refund:list' AND `menu_type` = 'C');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '订单利润报表', @erp_parent_id, 54, 'profit', 'erp/profit/index', '', 'ErpProfit', 1, 0, 'C', '0', '0', 'erp:profit:list', 'chart', 'admin', sysdate(), '订单利润报表'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:profit:list' AND `menu_type` = 'C');

SET @order_menu_id := (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'erp:order:list' AND `menu_type` = 'C' LIMIT 1);
SET @purchase_menu_id := (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'erp:purchase-task:list' AND `menu_type` = 'C' LIMIT 1);
SET @fulfillment_menu_id := (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'erp:fulfillment:list' AND `menu_type` = 'C' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '订单查询', @order_menu_id, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:query', '#', 'admin', sysdate(), ''
WHERE @order_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:order:query');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '订单同步', @order_menu_id, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:sync', '#', 'admin', sysdate(), ''
WHERE @order_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:order:sync');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '采购编辑', @purchase_menu_id, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:purchase-task:edit', '#', 'admin', sysdate(), ''
WHERE @purchase_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:purchase-task:edit');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '发货回传', @fulfillment_menu_id, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:fulfillment:add', '#', 'admin', sysdate(), ''
WHERE @fulfillment_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'erp:fulfillment:add');

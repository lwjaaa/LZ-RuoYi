DROP TABLE IF EXISTS `erp_product`;
CREATE TABLE `erp_product`
(
    `product_id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '本地主键',
    `shopify_product_id`   varchar(100) DEFAULT NULL COMMENT 'Shopify平台商品ID (唯一映射)',
    `product_title`        varchar(255) DEFAULT NULL COMMENT '商品标题',
    `spu`                  varchar(50) NOT NULL COMMENT 'SPU',
    `category`             varchar(50)  DEFAULT NULL COMMENT '商品类别 (Category)',
    `product_type`         varchar(100) DEFAULT NULL COMMENT '商品类型 (Product Type)',
    `source_url`           varchar(500) DEFAULT NULL COMMENT '来源URL',
    `purchase_url`         varchar(500) DEFAULT NULL COMMENT '采购链接',
    `option_json`          json         DEFAULT NULL COMMENT '商品选项',
    `purchase_option_json` json         DEFAULT NULL COMMENT '采购商品选项',
    `status`               char(1)      DEFAULT '0' COMMENT '发布状态 (0:草稿, 1:已发布, 2:归档)',
    `body_html`            text         DEFAULT NULL COMMENT '商品详情描述 (HTML)',
    `main_media_id`        bigint(20)   DEFAULT NULL COMMENT '主图ID，仅用户erp后台展示',
    `sync_status`          char(1)      DEFAULT '0' COMMENT '同步状态 (0:未同步, 1:同步成功, 2:同步失败, 3:同步中)',
    `sync_message`         varchar(500) DEFAULT NULL COMMENT '最后一次同步错误信息或结果',
    `last_sync_time`       datetime     DEFAULT NULL COMMENT '最后同步时间',
    `version`              int(11)      DEFAULT 0 COMMENT '乐观锁版本号',
    `description`          varchar(500) DEFAULT NULL COMMENT '描述',
    `size`                 varchar(500) DEFAULT NULL COMMENT '大小',
    `material`             varchar(500) DEFAULT NULL COMMENT '材质',
    `note`                 varchar(500) DEFAULT NULL COMMENT '备注',
    `package_include`      varchar(500) DEFAULT NULL COMMENT '包含的包材',
    `image_search_keyword` varchar(50)  DEFAULT NULL COMMENT '图片搜索关键词(目录)',
    /* 若依标准字段 */
    `create_by`            varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`          datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`            varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`          datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`               varchar(500) DEFAULT NULL COMMENT '备注',
    `del_flag`             char(1)      DEFAULT '0' COMMENT '删除标志 (0代表存在 2代表删除)',

    PRIMARY KEY (`product_id`),
    UNIQUE KEY `uk_shopify_product_id` (`shopify_product_id`) COMMENT 'Shopify ID唯一索引',
    KEY `idx_product_title` (`product_title`),
    KEY `idx_spu` (`spu`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='erp商品主表';

DROP TABLE IF EXISTS `erp_product_variant`;
CREATE TABLE `erp_product_variant`
(
    `variant_id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '本地主键',
    `product_id`         bigint(20) NOT NULL COMMENT '关联商品主表ID',
    `shopify_variant_id` varchar(100)   DEFAULT NULL COMMENT 'Shopify平台变体ID',
    `sku`                varchar(50)    DEFAULT NULL COMMENT 'SKU',
    `price`              int(10)        DEFAULT NULL COMMENT '销售价格(美元*100)',
    `compare_at_price`   int(10)        DEFAULT NULL COMMENT '原价/对比价',
    `purchase_price`     int(10)        DEFAULT NULL COMMENT '采购价（分）',
    `purchase_url`       varchar(500)   DEFAULT NULL COMMENT '采购链接',
    `option_values`      json           DEFAULT NULL COMMENT '变体对应的选项',
    `media_id`           bigint(20)     DEFAULT NULL COMMENT '关联的图片ID (若有)',
    `position`           int(11)        DEFAULT 1 COMMENT '排序位置 列表中的第一个位置是 1',
    `pk_width`           int(10)        DEFAULT NULL COMMENT '包装宽度',
    `pk_height`          int(10)        DEFAULT NULL COMMENT '包装高度',
    `pk_length`          int(10)        DEFAULT NULL COMMENT '包装长度',
    `material_weight`    int(10)        DEFAULT NULL COMMENT '材积重',
    `pk_weight`          int(10)        DEFAULT NULL COMMENT '常规包装重量',
    `freight`            int(10)        DEFAULT NULL COMMENT '运费',
    `is_actual_shipment` char(1)        DEFAULT '0' COMMENT '运费是否来自实际发货数据(0:否, 1:是)',
    `unit_cost_price`    int(10)        DEFAULT NULL comment '商品成本价（分）',
    `exchange_rate`      decimal(10, 4) DEFAULT NULL COMMENT '美元汇率',
    `suggested_price`    int(10)        DEFAULT NULL COMMENT '建议销售价格（美分）',
    `profit_rate`        decimal(10, 4) DEFAULT NULL COMMENT '利润率',
    `profit`             int(10)        DEFAULT NULL COMMENT '利润（美分）',
    /* 若依标准字段 */
    `create_by`          varchar(64)    DEFAULT '' COMMENT '创建者',
    `create_time`        datetime       DEFAULT NULL COMMENT '创建时间',
    `update_by`          varchar(64)    DEFAULT '' COMMENT '更新者',
    `update_time`        datetime       DEFAULT NULL COMMENT '更新时间',
    `remark`             varchar(500)   DEFAULT NULL COMMENT '备注',
    `del_flag`           char(1)        DEFAULT '0' COMMENT '删除标志 (0代表存在 2代表删除)',

    PRIMARY KEY (`variant_id`),
    UNIQUE KEY `uk_shopify_variant_id` (`shopify_variant_id`) COMMENT 'Shopify变体ID唯一索引',
    KEY `idx_product_id` (`product_id`),
    KEY `idx_sku` (`sku`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='erp商品变体表';

DROP TABLE IF EXISTS `erp_media`;
CREATE TABLE `erp_media`
(
    `media_id`           bigint(20) NOT NULL AUTO_INCREMENT,
    `product_id`         bigint(20) NOT NULL COMMENT '关联商品ID',
    `shopify_media_id`   varchar(100) DEFAULT NULL COMMENT 'Shopify媒体ID',
    `shopify_media_url`  varchar(500) DEFAULT NULL COMMENT 'Shopify媒体URL',
    `staged_upload_url`  varchar(500) DEFAULT NULL COMMENT 'shopify的暂存上传URL',
    `nas_media_url`      varchar(500) DEFAULT NULL COMMENT '本地nas的媒体URL',
    `filename`           varchar(255) DEFAULT NULL COMMENT '文件名',
    `media_content_type` varchar(20)  DEFAULT NULL COMMENT '媒体类型 (IMAGE:图片, VIDEO:视频，MODEL_3D，EXTERNAL_VIDEO)',
    `alt`                varchar(255) DEFAULT NULL COMMENT '替代文本',
    `position`           int(11)      DEFAULT 1 COMMENT '排序',
    `create_time`        datetime     DEFAULT NULL,
    `update_time`        datetime     DEFAULT NULL,
    `del_flag`           char(1)      DEFAULT '0',

    PRIMARY KEY (`media_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='erp媒体表';

DROP TABLE IF EXISTS `erp_tag_dict`;
CREATE TABLE `erp_tag_dict`
(
    `tag_id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '标签主键',
    /* --- 基础信息 --- */
    `tag_name`        varchar(50) NOT NULL COMMENT '标签名称 (如: 夏季新品, 促销)',
    `tag_code`        varchar(50) NOT NULL COMMENT '标签编码/英文标识 (如: summer_sale, new_arrival)',
    `tag_type`        varchar(50) NOT NULL DEFAULT 'ACTIVITY' COMMENT '标签类型 (MENU:菜单, OTHER:其他)',
    /* --- 树形结构核心字段 (实现上下级) --- */
    `sort_order`      int(11)              DEFAULT 0 COMMENT '排序',
    `parent_id`       bigint(20)           DEFAULT 0 COMMENT '父级ID (0表示顶级菜单)',
    `ancestors`       varchar(500)         DEFAULT '' COMMENT '所有父级ID路径 (如: 0,10,25)，方便快速查询子树',
    `menu_level`      int(11)              DEFAULT NULL COMMENT '菜单层级 (0:一级菜单, 1:二级菜单)',
    /* SPU流水号 */
    `spu_prefix`      varchar(20)          DEFAULT NULL NULL COMMENT 'SPU 前缀',
    `current_max_seq` int(11)              DEFAULT 0 COMMENT '当前最大流水号',

    /* 若依标准字段 */
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建者',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新者',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    `remark`          varchar(500)         DEFAULT NULL COMMENT '备注',
    `del_flag`        char(1)              DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`tag_id`),
    UNIQUE KEY `uk_tag_code` (`tag_code`) COMMENT '标签编码唯一'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='erp标签字典表';

DROP TABLE IF EXISTS `erp_product_tag_rel`;
CREATE TABLE `erp_product_tag_rel`
(
    `rel_id`      bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联主键',
    `product_id`  bigint(20) NOT NULL COMMENT '商品主表ID',
    `tag_id`      bigint(20) NOT NULL COMMENT '标签字典表ID',
    /* 若依标准字段 (关联表通常不需要复杂的审计，但保留以便追踪) */
    `create_time` datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   varchar(64) DEFAULT '' COMMENT '创建者',

    PRIMARY KEY (`rel_id`),
    UNIQUE KEY `uk_product_tag` (`product_id`, `tag_id`) COMMENT '防止重复关联',
    KEY `idx_product_id` (`product_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='erp商品与标签关联表';
#
# DROP TABLE IF EXISTS `erp_shopify_collection`;
# CREATE TABLE `erp_shopify_collection`
# (
#     `collection_id`   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '系列主键',
#     `collection_name` varchar(100) NOT NULL COMMENT '系列名称 (如: 2026春季大促系列)',
#     `collection_type` char(1)      DEFAULT '0' COMMENT '系列类型 (0:手动集合, 1:自动集合/基于规则)',
#     `rule_definition` json         DEFAULT NULL COMMENT '规则定义 (JSON格式，存储包含哪些标签ID，用于自动集合)',
#     `shopify_id`      bigint(20)   DEFAULT NULL COMMENT 'Shopify Collection ID',
#     `shopify_handle`  varchar(100) DEFAULT NULL COMMENT 'Shopify Collection Handle',
#     `publish_status`  char(1)      DEFAULT '0' COMMENT '发布状态 (0:隐藏, 1:发布)',
#     `sort_order`      int(11)      DEFAULT 0 COMMENT '排序',
#     `media_url`       varchar(500) DEFAULT NULL COMMENT '系列封面图',
#     `description`     text         DEFAULT NULL COMMENT '系列描述',
#
#     /* 同步状态 */
#     `sync_status`     char(1)      DEFAULT '0' COMMENT '同步状态 (0:未同步, 1:成功, 2:失败)',
#     `last_sync_time`  datetime     DEFAULT NULL COMMENT '最后同步时间',
#
#     /* 若依标准字段 */
#     `create_by`       varchar(64)  DEFAULT '' COMMENT '创建者',
#     `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
#     `update_by`       varchar(64)  DEFAULT '' COMMENT '更新者',
#     `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
#     `remark`          varchar(500) DEFAULT NULL COMMENT '备注',
#     `del_flag`        char(1)      DEFAULT '0' COMMENT '删除标志',
#
#     PRIMARY KEY (`collection_id`),
#     UNIQUE KEY `uk_shopify_id` (`shopify_id`),
#     KEY `idx_del_flag` (`del_flag`)
# ) ENGINE = InnoDB
#   DEFAULT CHARSET = utf8mb4 COMMENT ='Shopify系列/集合管理表';
-- ----------------------------
-- Table structure for erp_shopify_task
-- ----------------------------
DROP TABLE IF EXISTS `erp_shopify_task`;
CREATE TABLE `erp_shopify_task`
(
    `task_id`        bigint       NOT NULL COMMENT '任务主键',
    `task_name`      varchar(128) NOT NULL COMMENT '任务名称',
    `task_group`     int          DEFAULT '0' COMMENT '任务分组（0=商品管理，1=媒体管理）',#触发任务的来源
    `task_type`      varchar(32)  NOT NULL COMMENT '任务类型（PRODUCT_CREATE=商品创建，MEDIA_SYNC=媒体同步,PRODUCT_CREATE_BATCH=批量商品创建）',#这个任务在做什么

    -- 关联数据
    `business_type`  varchar(32)  DEFAULT NULL COMMENT '关联业务类型（PRODUCT=商品，MEDIA=媒体）',#关联什么单据
    `business_ids`   text COMMENT '关联业务 ID 集合（英文逗号分割，如：123,456）',#关联的单据ID

    -- 任务参数
    `request_path`   varchar(50)  DEFAULT NULL COMMENT '请求地址',
    `request_params` json         DEFAULT NULL COMMENT '请求参数',

    -- 执行状态
    `task_status`    varchar(16)  DEFAULT 'PENDING' COMMENT '任务状态（PENDING=待执行，RUNNING=执行中，SUCCESS=成功，FAILED=失败，PARTIAL_SUCCESS=部分成功，CANCELLED=已取消）',
    `progress`       int          DEFAULT '0' COMMENT '执行进度（0-100）',

    -- 错误处理
    `error_message`  text COMMENT '错误信息',

    -- 执行结果
    `result_data`    json         DEFAULT NULL COMMENT '执行结果数据（JSON 格式）',
    `execution_time` bigint       DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `start_time`     datetime     DEFAULT NULL COMMENT '开始执行时间',
    `end_time`       datetime     DEFAULT NULL COMMENT '结束时间',

    -- 扩展字段
    `parent_task_id` bigint       DEFAULT NULL COMMENT '父任务 ID（用于任务分解）',
    `root_task_id`   bigint       DEFAULT NULL COMMENT '根任务 ID（用于追踪批量任务）',

    -- 若依标准字段
    `remark`         varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by`      varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `del_flag`       char(1)      DEFAULT '0' COMMENT '删除标志（0=存在，1=删除）',
    PRIMARY KEY (`task_id`),
    KEY `idx_shopify_task_type` (`task_type`),
    KEY `idx_shopify_task_status` (`task_status`),
    KEY `idx_shopify_business` (`business_type`, `business_ids`(255)),
    KEY `idx_shopify_parent` (`parent_task_id`),
    KEY `idx_shopify_root` (`root_task_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Shopify 任务配置表';

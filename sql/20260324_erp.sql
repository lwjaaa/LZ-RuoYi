DROP TABLE IF EXISTS `erp_product`;
CREATE TABLE `erp_product`
(
    `product_id`               bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '本地主键',
    `store_id`                 bigint(20)    DEFAULT NULL COMMENT '所属 Shopify 店铺ID',
    `shopify_product_id`       varchar(100)  DEFAULT NULL COMMENT 'Shopify平台商品ID (唯一映射)',
    `shopify_updated_at`       datetime      DEFAULT NULL COMMENT 'Shopify 远端商品更新时间',
    `last_shopify_import_time` datetime      DEFAULT NULL COMMENT '最近一次 Shopify 反向导入成功时间',
    `product_title`            varchar(255)  DEFAULT NULL COMMENT '商品标题',
    `product_name`             varchar(255)  DEFAULT NULL COMMENT '商品名称',
    `spu`                      varchar(50) NULL COMMENT 'SPU',
    `category`                 varchar(50)   DEFAULT NULL COMMENT '商品类别 (Category)',
    `product_type`             varchar(100)  DEFAULT NULL COMMENT '商品类型 (Product Type)',
    `source_url`               varchar(2048) DEFAULT NULL COMMENT '来源URL',
    `purchase_url`             varchar(2048) DEFAULT NULL COMMENT '采购链接',
    `option_json`              json          DEFAULT NULL COMMENT '商品选项',
    `purchase_option_json`     json          DEFAULT NULL COMMENT '采购商品选项',
    `status`                   char(1)       DEFAULT '0' COMMENT '发布状态 (0:草稿, 1:已发布, 2:归档)',
    `body_html`                text          DEFAULT NULL COMMENT '商品详情描述 (HTML)',
    `main_media_id`            bigint(20)    DEFAULT NULL COMMENT '主图ID，仅用户erp后台展示',
    `sync_status`              char(1)       DEFAULT '0' COMMENT '同步状态 (0:未同步, 1:同步成功, 2:同步失败, 3:同步中, 4:部分成功)',
    `sync_message`             text          DEFAULT NULL COMMENT '最后一次同步错误信息或结果',
    `missing_fields`           varchar(512)  DEFAULT '' COMMENT '商品资料缺失字段编码，英文逗号分隔',
    `last_sync_time`           datetime      DEFAULT NULL COMMENT '最后同步时间',
    `version`                  int(11)       DEFAULT 0 COMMENT '乐观锁版本号',
    `description`              varchar(1000) DEFAULT NULL COMMENT '描述',
    `description_cn`           varchar(1000) DEFAULT NULL COMMENT '描述(中文)',
    `size`                     varchar(100)  DEFAULT NULL COMMENT '大小',
    `material`                 varchar(100)  DEFAULT NULL COMMENT '材质',
    `note`                     varchar(2048) DEFAULT NULL COMMENT '备注',
    `note_cn`                  varchar(2048) DEFAULT NULL COMMENT '备注(中文)',
    `package_include`          varchar(1000) DEFAULT NULL COMMENT '包含的包材',
    /* 若依标准字段 */
    `create_by`                varchar(64)   DEFAULT '' COMMENT '创建者',
    `create_time`              datetime      DEFAULT NULL COMMENT '创建时间',
    `update_by`                varchar(64)   DEFAULT '' COMMENT '更新者',
    `update_time`              datetime      DEFAULT NULL COMMENT '更新时间',
    `remark`                   varchar(500)  DEFAULT NULL COMMENT '备注',
    `del_flag`                 char(1)       DEFAULT '0' COMMENT '删除标志 (0代表存在 2代表删除)',

    PRIMARY KEY (`product_id`),
    UNIQUE KEY `uk_shopify_product_store` (`store_id`, `shopify_product_id`) COMMENT '店铺维度 Shopify 商品ID唯一索引',
    KEY `idx_product_store_id` (`store_id`),
    KEY `idx_product_title` (`product_title`),
    KEY `idx_spu` (`spu`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='erp商品主表';

DROP TABLE IF EXISTS `erp_product_variant`;
CREATE TABLE `erp_product_variant`
(
    `variant_id`                bigint(20) NOT NULL AUTO_INCREMENT COMMENT '本地主键',
    `product_id`                bigint(20) NOT NULL COMMENT '关联商品主表ID',
    `store_id`                  bigint(20)     DEFAULT NULL COMMENT '所属 Shopify 店铺ID',
    `shopify_variant_id`        varchar(100)   DEFAULT NULL COMMENT 'Shopify平台变体ID',
    `shopify_inventory_item_id` varchar(100)   DEFAULT NULL COMMENT 'Shopify InventoryItem ID',
    `last_shopify_import_time`  datetime       DEFAULT NULL COMMENT '最近一次 Shopify 反向导入成功时间',
    `sku`                       varchar(50)    DEFAULT NULL COMMENT 'SKU',
    `price`                     int(10)        DEFAULT NULL COMMENT '销售价格(美元*100)',
    `compare_at_price`          int(10)        DEFAULT NULL COMMENT '原价/对比价',
    `purchase_price`            int(10)        DEFAULT NULL COMMENT '采购价（分）',
    `purchase_url`              varchar(2048)  DEFAULT NULL COMMENT '采购链接',
    `is_active_available`       char(1)        DEFAULT '1' COMMENT '是否可用 (0:否, 1:是)',
    `option_values`             json           DEFAULT NULL COMMENT '变体对应的选项',
    `media_id`                  bigint(20)     DEFAULT NULL COMMENT '关联的图片ID (若有)',
    `position`                  int(11)        DEFAULT 1 COMMENT '排序位置 列表中的第一个位置是 1',
    `pk_width`                  int(10)        DEFAULT NULL COMMENT '包装宽度',
    `pk_height`                 int(10)        DEFAULT NULL COMMENT '包装高度',
    `pk_length`                 int(10)        DEFAULT NULL COMMENT '包装长度',
    `material_weight`           int(10)        DEFAULT NULL COMMENT '材积重',
    `pk_weight`                 int(10)        DEFAULT NULL COMMENT '常规包装重量',
    `freight`                   int(10)        DEFAULT NULL COMMENT '运费',
    `is_actual_shipment`        char(1)        DEFAULT '0' COMMENT '运费是否来自实际发货数据(0:否, 1:是)',
    `unit_cost_price`           int(10)        DEFAULT NULL comment '商品成本价（分）',
    `exchange_rate`             decimal(10, 4) DEFAULT NULL COMMENT '美元汇率',
    `suggested_price`           int(10)        DEFAULT NULL COMMENT '建议销售价格（美分）',
    `profit_rate`               decimal(10, 4) DEFAULT NULL COMMENT '利润率',
    `profit`                    int(10)        DEFAULT NULL COMMENT '利润（美分）',
    /* 若依标准字段 */
    `create_by`                 varchar(64)    DEFAULT '' COMMENT '创建者',
    `create_time`               datetime       DEFAULT NULL COMMENT '创建时间',
    `update_by`                 varchar(64)    DEFAULT '' COMMENT '更新者',
    `update_time`               datetime       DEFAULT NULL COMMENT '更新时间',
    `remark`                    varchar(500)   DEFAULT NULL COMMENT '备注',
    `del_flag`                  char(1)        DEFAULT '0' COMMENT '删除标志 (0代表存在 2代表删除)',

    PRIMARY KEY (`variant_id`),
    UNIQUE KEY `uk_shopify_variant_store` (`store_id`, `shopify_variant_id`) COMMENT '店铺维度 Shopify 变体ID唯一索引',
    KEY `idx_variant_store_id` (`store_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_sku` (`sku`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='erp商品变体表';

DROP TABLE IF EXISTS `erp_media`;
CREATE TABLE `erp_media`
(
    `media_id`                 bigint(20) NOT NULL AUTO_INCREMENT,
    `transcoded_media_url`     varchar(2048) DEFAULT NULL COMMENT 'Shopify同步用转码媒体URL',
    `transcode_source_hash`    varchar(64)   DEFAULT NULL COMMENT '转码源文件SHA-256',
    `transcode_profile`        varchar(100)  DEFAULT NULL COMMENT '转码规格版本',
    `transcode_time`           datetime      DEFAULT NULL COMMENT '最后转码时间',
    `product_id`               bigint(20) NOT NULL COMMENT '关联商品ID',
    `store_id`                 bigint(20) NOT NULL COMMENT '所属 Shopify 店铺ID',
    `shopify_media_id`         varchar(100)  DEFAULT NULL COMMENT 'Shopify媒体ID',
    `shopify_media_url`        varchar(2048) DEFAULT NULL COMMENT 'Shopify媒体URL',
    `staged_upload_url`        varchar(2048) DEFAULT NULL COMMENT 'shopify的暂存上传URL',
    `nas_media_url`            varchar(2048) DEFAULT NULL COMMENT '本地nas的媒体URL',
    `filename`                 varchar(255)  DEFAULT NULL COMMENT '文件名',
    `media_content_type`       varchar(20)   DEFAULT NULL COMMENT '媒体类型 (IMAGE:图片, VIDEO:视频，MODEL_3D，EXTERNAL_VIDEO)',
    `alt`                      varchar(255)  DEFAULT NULL COMMENT '替代文本',
    `position`                 int(11)       DEFAULT 1 COMMENT '排序',
    `last_shopify_import_time` datetime      DEFAULT NULL COMMENT '最近一次 Shopify 反向导入成功时间',
    `create_time`              datetime      DEFAULT NULL,
    `update_time`              datetime      DEFAULT NULL,
    `del_flag`                 char(1)       DEFAULT '0',

    PRIMARY KEY (`media_id`),
    UNIQUE KEY `uk_shopify_media_store` (`store_id`, `shopify_media_id`) COMMENT '店铺维度 Shopify 媒体ID唯一索引',
    KEY `idx_media_store_id` (`store_id`),
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
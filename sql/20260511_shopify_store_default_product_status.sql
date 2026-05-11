-- 店铺增加推送到 Shopify 时的默认商品状态，并将发布渠道语义调整为自动发布渠道
ALTER TABLE `erp_shopify_store`
    ADD COLUMN `default_product_status` varchar(16) DEFAULT 'DRAFT' COMMENT '推送到 Shopify 时的默认商品状态：DRAFT, ACTIVE' AFTER `publish_publication_names`;

ALTER TABLE `erp_shopify_store`
    MODIFY COLUMN `publish_publication_ids` text COMMENT '自动发布 Publication ID，英文逗号分隔',
    MODIFY COLUMN `publish_publication_names` text COMMENT '自动发布渠道名称，英文逗号分隔';

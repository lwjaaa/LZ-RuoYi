-- 商品资料缺失字段缓存与店铺必填项配置
-- 执行日期: 2026-05-11

SET @productMissingFieldsColumnExists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_product'
      AND COLUMN_NAME = 'missing_fields'
);

SET @productMissingFieldsAlterSql := IF(@productMissingFieldsColumnExists = 0,
    'ALTER TABLE `erp_product`
        ADD COLUMN `missing_fields` varchar(512) DEFAULT '''' COMMENT ''商品资料缺失字段编码，英文逗号分隔'' AFTER `sync_message`',
    'SELECT 1'
);

PREPARE productMissingFieldsStmt FROM @productMissingFieldsAlterSql;
EXECUTE productMissingFieldsStmt;
DEALLOCATE PREPARE productMissingFieldsStmt;

SET @storeRequiredFieldsColumnExists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_shopify_store'
      AND COLUMN_NAME = 'required_product_fields'
);

SET @storeRequiredFieldsAlterSql := IF(@storeRequiredFieldsColumnExists = 0,
    'ALTER TABLE `erp_shopify_store`
        ADD COLUMN `required_product_fields` varchar(512) DEFAULT ''TITLE,SPU,MAIN_MEDIA,DESCRIPTION,BODY_HTML,VARIANT,PRICE,FREIGHT,VARIANT_MEDIA,SKU'' COMMENT ''商品资料必填字段编码，英文逗号分隔'' AFTER `default_product_status`',
    'SELECT 1'
);

PREPARE storeRequiredFieldsStmt FROM @storeRequiredFieldsAlterSql;
EXECUTE storeRequiredFieldsStmt;
DEALLOCATE PREPARE storeRequiredFieldsStmt;


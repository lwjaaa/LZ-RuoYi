-- Shopify 店铺同步配置扩展
-- 执行日期: 2026-05-07

SET @storeSyncColumnExists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_shopify_store'
      AND COLUMN_NAME = 'inventory_location_id'
);

SET @storeSyncAlterSql := IF(@storeSyncColumnExists = 0,
    'ALTER TABLE `erp_shopify_store`
        ADD COLUMN `inventory_location_id` varchar(255) DEFAULT NULL COMMENT ''Shopify 库存仓库 Location ID'' AFTER `base_url`,
        ADD COLUMN `inventory_location_name` varchar(255) DEFAULT NULL COMMENT ''Shopify 库存仓库名称'' AFTER `inventory_location_id`,
        ADD COLUMN `inventory_tracked` char(1) DEFAULT ''0'' COMMENT ''是否跟踪库存 (0:否, 1:是)'' AFTER `inventory_location_name`,
        ADD COLUMN `default_inventory_quantity` int(11) DEFAULT 100 COMMENT ''默认库存数量'' AFTER `inventory_tracked`,
        ADD COLUMN `inventory_policy` varchar(20) DEFAULT ''DENY'' COMMENT ''缺货销售策略: DENY, CONTINUE'' AFTER `default_inventory_quantity`,
        ADD COLUMN `publish_publication_ids` text COMMENT ''默认发布 Publication ID，英文逗号分隔'' AFTER `inventory_policy`,
        ADD COLUMN `publish_publication_names` text COMMENT ''默认发布渠道名称，英文逗号分隔'' AFTER `publish_publication_ids`',
    'SELECT 1'
);

PREPARE storeSyncStmt FROM @storeSyncAlterSql;
EXECUTE storeSyncStmt;
DEALLOCATE PREPARE storeSyncStmt;

-- 补齐商品推送/发布按钮权限。若已存在相同权限，可按实际菜单 ID 手动跳过。
SELECT @productParentId := menu_id FROM sys_menu WHERE perms IN ('erp:product:list', 'vh-erp:product:list') LIMIT 1;
SELECT @productPushExists := COUNT(*) FROM sys_menu WHERE perms = 'erp:product:push';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 'erp商品推送', @productParentId, '7', '#', '', 1, 0, 'F', '0', '0', 'erp:product:push', '#', 'admin', sysdate(), '', null, ''
WHERE @productParentId IS NOT NULL AND @productPushExists = 0;

-- Shopify 店铺管理菜单
SELECT @storeMenuExists := COUNT(*) FROM sys_menu WHERE perms = 'erp:store:list';
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 'Shopify店铺', '2000', '8', 'store', 'erp/store/index', 1, 0, 'C', '0', '0', 'erp:store:list', 'shop', 'admin', sysdate(), '', null, 'Shopify店铺菜单'
WHERE @storeMenuExists = 0;

SELECT @storeParentId := menu_id FROM sys_menu WHERE perms = 'erp:store:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 'Shopify店铺查询', @storeParentId, '1', '#', '', 1, 0, 'F', '0', '0', 'erp:store:query', '#', 'admin', sysdate(), '', null, ''
WHERE @storeParentId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'erp:store:query') t);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 'Shopify店铺新增', @storeParentId, '2', '#', '', 1, 0, 'F', '0', '0', 'erp:store:add', '#', 'admin', sysdate(), '', null, ''
WHERE @storeParentId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'erp:store:add') t);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 'Shopify店铺修改', @storeParentId, '3', '#', '', 1, 0, 'F', '0', '0', 'erp:store:edit', '#', 'admin', sysdate(), '', null, ''
WHERE @storeParentId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'erp:store:edit') t);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 'Shopify店铺删除', @storeParentId, '4', '#', '', 1, 0, 'F', '0', '0', 'erp:store:remove', '#', 'admin', sysdate(), '', null, ''
WHERE @storeParentId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'erp:store:remove') t);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 'Shopify店铺导出', @storeParentId, '5', '#', '', 1, 0, 'F', '0', '0', 'erp:store:export', '#', 'admin', sysdate(), '', null, ''
WHERE @storeParentId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'erp:store:export') t);

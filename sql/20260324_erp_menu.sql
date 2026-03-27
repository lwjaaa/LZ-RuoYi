-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品', '2000', '1', 'product', 'vh-erp/product/index', 1, 0, 'C', '0', '0', 'vh-erp:product:list', '#', 'admin', sysdate(), '', null, 'erp商品菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:product:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:product:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:product:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:product:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:product:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:product:import',       '#', 'admin', sysdate(), '', null, '');


-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp图片', '2000', '1', 'image', 'vh-erp/image/index', 1, 0, 'C', '0', '0', 'vh-erp:image:list', '#', 'admin', sysdate(), '', null, 'erp图片菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp图片查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:image:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp图片新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:image:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp图片修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:image:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp图片删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:image:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp图片导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:image:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp图片导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:image:import',       '#', 'admin', sysdate(), '', null, '');

-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品与标签关联', '2000', '1', 'ProductTag', 'vh-erp/ProductTag/index', 1, 0, 'C', '0', '0', 'vh-erp:ProductTag:list', '#', 'admin', sysdate(), '', null, 'erp商品与标签关联菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品与标签关联查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:ProductTag:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品与标签关联新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:ProductTag:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品与标签关联修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:ProductTag:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品与标签关联删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:ProductTag:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品与标签关联导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:ProductTag:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品与标签关联导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:ProductTag:import',       '#', 'admin', sysdate(), '', null, '');

-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp标签', '2000', '1', 'tag', 'vh-erp/tag/index', 1, 0, 'C', '0', '0', 'vh-erp:tag:list', '#', 'admin', sysdate(), '', null, 'erp标签菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp标签查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:tag:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp标签新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:tag:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp标签修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:tag:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp标签删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:tag:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp标签导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:tag:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp标签导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:tag:import',       '#', 'admin', sysdate(), '', null, '');

-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品变体', '2000', '1', 'variant', 'vh-erp/variant/index', 1, 0, 'C', '0', '0', 'vh-erp:variant:list', '#', 'admin', sysdate(), '', null, 'erp商品变体菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品变体查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:variant:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品变体新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:variant:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品变体修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:variant:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品变体删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:variant:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品变体导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:variant:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('erp商品变体导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'vh-erp:variant:import',       '#', 'admin', sysdate(), '', null, '');

-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Shopify 任务配置', '2000', '1', 'task', 'erp/task/index', 1, 0, 'C', '0', '0', 'erp:task:list', '#', 'admin', sysdate(), '', null, 'Shopify 任务配置菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Shopify 任务配置查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'erp:task:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Shopify 任务配置新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'erp:task:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Shopify 任务配置修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'erp:task:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Shopify 任务配置删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'erp:task:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Shopify 任务配置导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'erp:task:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Shopify 任务配置导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'erp:task:import',       '#', 'admin', sysdate(), '', null, '');

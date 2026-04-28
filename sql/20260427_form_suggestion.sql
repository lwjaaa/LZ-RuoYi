-- ----------------------------
-- Table structure for erp_form_suggestion
-- ----------------------------
DROP TABLE IF EXISTS `erp_form_suggestion`;
CREATE TABLE `erp_form_suggestion` (
    `suggestion_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `field_name` varchar(50) NOT NULL COMMENT '字段名称 (如: size, material, note, note_cn)',
    `field_value` varchar(2048) NOT NULL COMMENT '字段值内容',
    `last_used_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后使用时间',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志 (0代表存在 1代表删除)',
    PRIMARY KEY (`suggestion_id`),
    UNIQUE KEY `uk_field_value` (`field_name`, `field_value`(255)) COMMENT '字段名和值的唯一索引',
    KEY `idx_field_name_time` (`field_name`, `last_used_time` DESC) COMMENT '字段名和时间复合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单快速提示词表';

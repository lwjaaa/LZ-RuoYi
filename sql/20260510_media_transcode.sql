ALTER TABLE `erp_media`
    ADD COLUMN `transcoded_media_url` varchar(2048) DEFAULT NULL COMMENT 'Shopify同步用转码媒体URL' AFTER `nas_media_url`,
    ADD COLUMN `transcode_source_hash` varchar(64) DEFAULT NULL COMMENT '转码源文件SHA-256' AFTER `transcoded_media_url`,
    ADD COLUMN `transcode_profile` varchar(100) DEFAULT NULL COMMENT '转码规格版本' AFTER `transcode_source_hash`,
    ADD COLUMN `transcode_time` datetime DEFAULT NULL COMMENT '最后转码时间' AFTER `transcode_profile`;

package com.ruoyi.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 4px API SDK 配置类。
 *
 * @author lwj
 * @date 2026-04-26
 */
@Data
@Component
@ConfigurationProperties(prefix = "fpx.api")
public class FpxApiConfig {
    
    /** App Key（在 4px 开发平台申请的 appKey） */
    private String appKey;
    
    /** App Secret（与 appKey 相对应的 appSecret） */
    private String appSecret;
}

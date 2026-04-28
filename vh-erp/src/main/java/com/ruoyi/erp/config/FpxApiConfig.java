package com.ruoyi.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 4px API SDK配置类
 * 
 * @author lwj
 * @date 2026-04-26
 */
@Data
@Component
@ConfigurationProperties(prefix = "fpx.api")
public class FpxApiConfig {
    
    /** App Key（在4px开发平台申请的appKey） */
    private String appKey;
    
    /** App Secret（与appKey相对应的appSecret） */
    private String appSecret;
}

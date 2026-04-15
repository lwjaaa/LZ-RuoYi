package com.ruoyi.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "exchange-rate")
public class ExchangerateConfig {

    /**
     * ExchangeRate-API Key
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl = "https://v6.exchangerate-api.com/v6";

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 10000;

    /**
     * 默认基准货币
     */
    private String defaultBaseCurrency = "USD";

    /**
     * 目标货币列表，多个用英文逗号隔开
     */
    private String targetCurrencies = "CNY";
}

package com.ruoyi.erp.model.dto.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ExchangeRate-API 响应DTO
 * API文档: https://www.exchangerate-api.com/docs
 */
@Data
public class ExchangeRateApiResponse {

    /**
     * 请求结果: "success" 或 "error"
     */
    private String result;

    /**
     * 文档链接
     */
    @JsonProperty("documentation")
    private String documentation;

    /**
     * 使用条款链接
     */
    @JsonProperty("terms_of_use")
    private String termsOfUse;

    /**
     * 最后一次更新时间戳
     */
    @JsonProperty("time_last_update_unix")
    private Long timeLastUpdateUnix;

    /**
     * 最后一次更新时间（UTC）
     */
    @JsonProperty("time_last_update_utc")
    private String timeLastUpdateUtc;

    /**
     * 下次更新时间戳
     */
    @JsonProperty("time_next_update_unix")
    private Long timeNextUpdateUnix;

    /**
     * 下次更新时间（UTC）
     */
    @JsonProperty("time_next_update_utc")
    private String timeNextUpdateUtc;

    /**
     * 基础货币
     */
    @JsonProperty("base_code")
    private String baseCode;

    /**
     * 汇率Map，key为货币代码，value为汇率值
     */
    @JsonProperty("conversion_rates")
    private Map<String, BigDecimal> conversionRates;

    /**
     * 错误类型（仅在result为"error"时存在）
     */
    @JsonProperty("error-type")
    private String errorType;
}

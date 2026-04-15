package com.ruoyi.erp.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.config.ExchangerateConfig;
import com.ruoyi.erp.model.dto.base.ExchangeRateApiResponse;
import com.ruoyi.erp.model.dto.base.ExchangeRateDTO;
import com.ruoyi.erp.service.IExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ExchangeRateServiceImpl implements IExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);

    private static final String REDIS_RATE_KEY = "erp:exchange_rate:";

    private static final int CACHE_EXPIRE_HOURS = 24;


    @Autowired
    private ExchangerateConfig exchangerateConfig;

    @Autowired
    private RedisCache redisCache;

    @Override
    public BigDecimal fetchRatesFromApi(String baseCurrency) {
        if (StringUtils.isBlank(baseCurrency)) {
            baseCurrency = exchangerateConfig.getDefaultBaseCurrency();
        }
        
        // ExchangeRate-API URL格式: https://v6.exchangerate-api.com/v6/{API_KEY}/latest/{BASE_CODE}
        String url = String.format("%s/%s/latest/%s",
                exchangerateConfig.getBaseUrl(),
                exchangerateConfig.getApiKey(),
                baseCurrency);

        try {
            log.info("开始调用ExchangeRate-API获取汇率，baseCurrency: {}", baseCurrency);

            HttpResponse response = HttpRequest.get(url)
                    .timeout(exchangerateConfig.getConnectTimeout())
                    .execute();

            if (!response.isOk()) {
                log.error("调用ExchangeRate-API失败，HTTP状态码: {}", response.getStatus());
                throw new RuntimeException("调用ExchangeRate-API失败: " + response.getStatus());
            }

            String body = response.body();
            log.debug("ExchangeRate-API响应: {}", body);

            ExchangeRateApiResponse apiResponse = JSON.parseObject(body, ExchangeRateApiResponse.class);

            if (apiResponse == null || !"success".equals(apiResponse.getResult())) {
                String errorMsg = apiResponse != null ? apiResponse.getErrorType() : "未知错误";
                log.error("ExchangeRate-API返回错误: {}", errorMsg);
                throw new RuntimeException("ExchangeRate-API返回错误: " + errorMsg);
            }

            Map<String, BigDecimal> allRates = apiResponse.getConversionRates();
            if (allRates == null || allRates.isEmpty()) {
                log.warn("ExchangeRate-API返回的汇率数据为空");
                return  null;
            }

            return allRates.get(exchangerateConfig.getTargetCurrencies());

        } catch (Exception e) {
            log.error("调用ExchangeRate-API异常，baseCurrency: {}", baseCurrency, e);
            throw new RuntimeException("获取汇率数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ExchangeRateDTO getTodayRate(String baseCurrency) {
        if (StringUtils.isBlank(baseCurrency)) {
            baseCurrency = exchangerateConfig.getDefaultBaseCurrency();
        }
        String targetCurrency = exchangerateConfig.getTargetCurrencies();

        final String today = DateUtil.today();
        String cacheKey = REDIS_RATE_KEY + today + ":" + baseCurrency;
        ExchangeRateDTO cachedRate = redisCache.getCacheObject(cacheKey);
        
        if (cachedRate != null) {
            log.debug("从缓存获取汇率: {}", cachedRate);
            return cachedRate;
        }

        try {
            // 获取目标货币列表
            BigDecimal rate = fetchRatesFromApi(baseCurrency);

            if (rate == null) {
                log.warn("未获取到汇率数据，baseCurrency: {}", baseCurrency);
                return null;
            }

            ExchangeRateDTO exchangeRate = new ExchangeRateDTO();
            exchangeRate.setBaseCurrency(baseCurrency);
            exchangeRate.setTargetCurrency(targetCurrency);
            exchangeRate.setRate(rate);
            exchangeRate.setRateDate(today);

            redisCache.setCacheObject(cacheKey, exchangeRate, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("成功获取汇率: {}", exchangeRate);
            
            return exchangeRate;
            
        } catch (Exception e) {
            log.error("获取汇率失败，{} -> {}", baseCurrency, targetCurrency, e);
            throw new RuntimeException("获取汇率失败: " + e.getMessage(), e);
        }
    }


    private void clearCache(String baseCurrency) {
        Collection<String> keys = redisCache.keys(REDIS_RATE_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisCache.deleteObject(keys);
            log.debug("已清除所有汇率缓存");
        }
    }
}

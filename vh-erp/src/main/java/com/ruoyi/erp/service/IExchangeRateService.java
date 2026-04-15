package com.ruoyi.erp.service;

import com.ruoyi.erp.model.dto.base.ExchangeRateDTO;

import java.math.BigDecimal;

/**
 * 汇率服务接口
 * 提供汇率查询、同步、转换等功能
 * 支持人民币(CNY)与英镑(GBP)、美元(USD)之间的汇率操作
 *
 * @author ruoyi
 */
public interface IExchangeRateService {

    /**
     * 从exchangerate-api获取最新汇率数据
     * 根据配置的基准货币，调用exchangerate-api获取实时汇率
     *
     * @param baseCurrency 基准货币代码（如GBP、USD），如果为空则使用配置中的默认值
     * @return 基准货币相对于人民币的汇率
     * @throws RuntimeException 当API调用失败或返回错误时抛出异常
     */
    BigDecimal fetchRatesFromApi(String baseCurrency);

    /**
     * 获取指定货币对的最新汇率
     * 优先从Redis缓存中获取，缓存不存在则调佣接口
     * 缓存有效期为24小时
     *
     * @param baseCurrency   基准货币代码（如GBP、USD），如果为空则使用配置中的默认值
     * @return 汇率对象，包含汇率值和日期等信息；如果未找到返回null
     * @throws IllegalArgumentException 当目标货币为空时抛出异常
     */
    ExchangeRateDTO getTodayRate(String baseCurrency);


}

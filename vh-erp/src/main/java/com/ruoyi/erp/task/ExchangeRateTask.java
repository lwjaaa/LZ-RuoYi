package com.ruoyi.erp.task;

import com.ruoyi.erp.service.IExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 汇率同步定时任务
 * 用于定期从ExchangeRate-API获取最新汇率数据
 */
@Component("exchangeRateTask")
public class ExchangeRateTask {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateTask.class);

    @Autowired
    private IExchangeRateService exchangeRateService;

    /**
     * 每日汇率同步任务
     * 默认同步USD到CNY的汇率
     */
    public void syncDailyRates() {
        log.info("开始执行每日汇率同步任务（USD -> CNY）");
        try {
            // 触发汇率查询，会自动缓存
            exchangeRateService.getTodayRate("USD");
            log.info("每日汇率同步任务执行成功");
        } catch (Exception e) {
            log.error("每日汇率同步任务执行失败", e);
        }
    }

}

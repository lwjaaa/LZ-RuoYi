package com.ruoyi.erp.shopify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Shopify 同步异步执行器配置。
 */
@Configuration
public class ShopifySyncConfig {

    @Bean(name = "shopifySyncThreadPool")
    public ThreadPoolTaskExecutor shopifySyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("shopify-sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}

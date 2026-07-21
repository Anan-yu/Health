package com.rayk.health.common.config;

import java.util.Map;
import java.util.concurrent.Executor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@EnableAsync
@EnableScheduling
@Configuration
public class AsyncConfig {
    @Bean(name = "ocrTaskExecutor")
    Executor ocrTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ocr-task-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setTaskDecorator(
                runnable -> {
                    Map<String, String> context = MDC.getCopyOfContextMap();
                    return () -> {
                        try {
                            if (context != null) {
                                MDC.setContextMap(context);
                            }
                            runnable.run();
                        } finally {
                            MDC.clear();
                        }
                    };
                });
        executor.initialize();
        AsyncTaskExecutor secured = new DelegatingSecurityContextAsyncTaskExecutor(executor);
        return secured;
    }
}


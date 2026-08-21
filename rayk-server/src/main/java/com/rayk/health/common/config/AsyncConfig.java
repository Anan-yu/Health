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

    /**
     * OCR tasks are scheduled on a single worker, but a report can contain many
     * photographed pages.  Keep the task worker available for other reports and
     * run the per-page HTTP calls on a small, bounded pool instead of processing
     * every page serially.
     */
    @Bean(name = "ocrFileExecutor")
    Executor ocrFileExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ocr-file-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}


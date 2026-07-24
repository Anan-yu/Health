package com.rayk.health.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI raykOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("致宇健康 API")
                                .version("0.1.0")
                                .description("健康管理开发接口；模拟评估不构成医学诊断。"));
    }
}


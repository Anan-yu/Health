package com.rayk.health.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {
    @Bean
    @Primary
    MinioClient minioClient(MinioProperties properties) {
        return createClient(properties.endpoint(), properties);
    }

    @Bean
    @Qualifier("minioPublicClient")
    MinioClient minioPublicClient(MinioProperties properties) {
        return createClient(properties.publicEndpoint(), properties);
    }

    private MinioClient createClient(String endpoint, MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(properties.accessKey(), properties.secretKey())
                .region(properties.region())
                .build();
    }
}

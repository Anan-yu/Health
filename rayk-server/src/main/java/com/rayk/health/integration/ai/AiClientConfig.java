package com.rayk.health.integration.ai;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class AiClientConfig {
    @Bean
    WebClient aiWebClient(AiProperties properties) {
        HttpClient client =
                HttpClient.create()
                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                properties.connectTimeoutSeconds() * 1000)
                        .responseTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(client))
                .build();
    }
}


package com.example.stay_hub.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * 공급사별 WebClient 빈 구성.
 * 연결/응답 타임아웃을 명시적으로 건다 — 외부 연동은 실패를 전제로 설계해야 하므로
 * 무한 대기를 허용하지 않는다.
 */
@Configuration
public class SupplierWebClientConfig {

    @Bean
    public WebClient supplierAWebClient(
            @Value("${supplier.a.base-url}") String baseUrl,
            @Value("${supplier.webclient.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${supplier.webclient.response-timeout-ms}") int responseTimeoutMs) {
        return buildWebClient(baseUrl, connectTimeoutMs, responseTimeoutMs);
    }

    @Bean
    public WebClient supplierBWebClient(
            @Value("${supplier.b.base-url}") String baseUrl,
            @Value("${supplier.webclient.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${supplier.webclient.response-timeout-ms}") int responseTimeoutMs) {
        return buildWebClient(baseUrl, connectTimeoutMs, responseTimeoutMs);
    }

    private WebClient buildWebClient(String baseUrl, int connectTimeoutMs, int responseTimeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

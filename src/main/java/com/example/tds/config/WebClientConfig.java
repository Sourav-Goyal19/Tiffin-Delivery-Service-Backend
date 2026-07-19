package com.example.tds.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.function.Consumer;

@Component
public class WebClientConfig {

    private final int connectTimeout;
    private final int responseTimeout;

    public WebClientConfig(
            @Value("${webclient.connect-timeout}") int connectTimeout,
            @Value("${webclient.response-timeout}") int responseTimeout) {
        this.connectTimeout = connectTimeout;
        this.responseTimeout = responseTimeout;
    }

    public WebClient createWebClient(WebClient.Builder webClientBuilder, String baseUrl, Consumer<HttpHeaders> headers) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofSeconds(responseTimeout));

        return webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(headers)
                .build();
    }
}

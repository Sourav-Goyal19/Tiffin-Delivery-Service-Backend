package com.example.tds.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
public class GoogleMapsConfig {
    private final WebClientConfig webClientConfig;
    private final WebClient.Builder webClientBuilder;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    @Bean("googleMapsWebClient")
    public WebClient getGoogleMapsWebClient() {
        String googleApisBaseUrl = "https://routes.googleapis.com";

        Consumer<HttpHeaders> headers = httpHeaders -> {
            httpHeaders.set("X-Goog-Api-Key", googleMapsApiKey);
            httpHeaders.set("Content-Type", "application/json");
        };

        return webClientConfig.createWebClient(
                webClientBuilder,
                googleApisBaseUrl,
                headers
        );
    }
}

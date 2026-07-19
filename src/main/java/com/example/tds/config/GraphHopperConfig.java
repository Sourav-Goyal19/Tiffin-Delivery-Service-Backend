package com.example.tds.config;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.Duration;

@Configuration
public class GraphHopperConfig {

    @Bean(name = "graphHopperWebClient")
    public WebClient graphHopperWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${graphhopper.listener.url}") String graphHopperListenerUrl
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10));

        return webClientBuilder
                .baseUrl(graphHopperListenerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

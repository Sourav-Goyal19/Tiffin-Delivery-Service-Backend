package com.example.tds.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import com.example.tds.controller.DeliveryAgentLocationController;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {
    private final DeliveryAgentLocationController locationController;

    public WebSocketConfig(DeliveryAgentLocationController controller) {
        this.locationController = controller;
    }

    @PostConstruct
    public void init() {
        log.info("DeliveryAgentLocationController created");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("Registering WebSocket endpoint");

        registry.addHandler(locationController, "/api/delivery-agents/location").setAllowedOrigins("*");
    }
}

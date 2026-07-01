package com.example.tds.config;

import com.example.tds.utilities.JwtHandshakeInterceptor;
import com.example.tds.utilities.LocationTrackingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler( locationHandler(), "/tracking")
                .setAllowedOrigins("*")
                .addInterceptors(jwtHandshakeInterceptor);
    }

    @Bean
    public LocationTrackingHandler locationHandler() {
        return new LocationTrackingHandler();
    }
}

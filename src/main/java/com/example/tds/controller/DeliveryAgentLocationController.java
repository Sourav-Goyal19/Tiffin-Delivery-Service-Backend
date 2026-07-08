package com.example.tds.controller;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.tds.service.DeliveryAgentService;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.example.tds.dto.requests.common.UpdateAgentLocationRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAgentLocationController extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final DeliveryAgentService deliveryAgentService;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.info("Connected to web socket: {}", session.getId());
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        UpdateAgentLocationRequest request = objectMapper.readValue(message.getPayload(), UpdateAgentLocationRequest.class);

        switch (request.getType()) {
            case "LOCATION":
                deliveryAgentService.updateDeliveryAgentLocation(
                        request.getLongitude(),
                        request.getLatitude(),
                        request.getDeliveryAgentId()
                );
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("Disconnected from web socket: {}", session.getId());
    }
}

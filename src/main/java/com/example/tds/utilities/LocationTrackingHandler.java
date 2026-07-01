package com.example.tds.utilities;

import com.example.tds.dto.requests.location.RiderLiveLocationDto;
import com.example.tds.service.DeliveryAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LocationTrackingHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    private DeliveryAgentService deliveryAgentService;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connected : {}", session.getId());
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        // :: Converting msg string to dto
        RiderLiveLocationDto tempLocation = getDtoFromMessage(message);

        // :: Get delivery agent id
        UUID deliveryAgentId = (java.util.UUID) session.getAttributes().get("deliveryAgentId");

        if (tempLocation != null && deliveryAgentId != null) {
            deliveryAgentService.updateDeliveryAgentLocation(
                    tempLocation.getLongitude(),
                    tempLocation.getLatitude(),
                    deliveryAgentId
            );
        } else {
            log.warn("Invalid location update or unauthenticated session.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Disconnected : {}", session.getId());
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
        try {
            session.close();
        } catch (Exception ex) {
            log.error("Error closing session {}: {}", session.getId(), ex.getMessage());
        }
    }

    private RiderLiveLocationDto getDtoFromMessage(TextMessage message) {

        RiderLiveLocationDto riderLiveLocationDto = null;

        try {
            riderLiveLocationDto = mapper.readValue(message.getPayload(), RiderLiveLocationDto.class);
        } catch (Exception e) {
            log.info("Error converting message to dto: {} with error {}", message.getPayload(), e);
        }

        return riderLiveLocationDto;
    }
}

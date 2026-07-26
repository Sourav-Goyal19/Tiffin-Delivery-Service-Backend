package com.example.tds.controller.sse;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import com.example.tds.service.DeliveryAgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.UUID;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/delivery-agents/{agentId}")
@RequiredArgsConstructor
public class DeliveryAgentUpdatesController {
    private static final long SSE_TIMEOUT = 1000L * 60L * 30L; // 30 minutes
    private static final long RECONNECT_TIME = 1000L * 5L; // 5 seconds
    private static final String EVENT_NAME = "delivery-agent-location";

    private final DeliveryAgentService deliveryAgentService;
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    @GetMapping(value = "/location/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getLocation(@PathVariable("agentId") UUID agentId, @PathVariable("orderId") UUID orderId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);



        MessageListener listener = (message, pattern) -> {
            try {
                String body = new String(message.getBody());
                if ((DeliveryAgentService.CLOSE_CONNECTION_MESSAGE_PREFIX + orderId).equals(body)) {
                    emitter.complete();
                    return;
                }
                
                if (body.startsWith(DeliveryAgentService.CLOSE_CONNECTION_MESSAGE_PREFIX)) {
                    return;
                }

                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name(EVENT_NAME)
                        .data(body)
                        .reconnectTime(RECONNECT_TIME);

                emitter.send(event.build());
            } catch (IOException e) {
                emitter.completeWithError(e);
            } catch (Exception e) {
                // ignore
            }
        };

        ChannelTopic topic = new ChannelTopic(DeliveryAgentService.AGENT_LOCATION_CHANNEL_PREFIX + agentId);
        redisMessageListenerContainer.addMessageListener(listener, topic);

        Runnable onCompletion = () -> {
            redisMessageListenerContainer.removeMessageListener(listener, topic);
        };

        emitter.onCompletion(onCompletion);
        emitter.onTimeout(onCompletion);
        emitter.onError(e -> onCompletion.run());

        try {
            Object initialLocation = deliveryAgentService.getDeliveryAgentLocation(agentId);
            emitter.send(SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name(EVENT_NAME)
                    .data(initialLocation)
                    .reconnectTime(RECONNECT_TIME));
        } catch (Exception e) {
            // ignore if location not found
        }

        return emitter;
    }
}

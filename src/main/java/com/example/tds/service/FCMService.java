package com.example.tds.service;

import lombok.extern.slf4j.Slf4j;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;

@Slf4j
@Service
public class FCMService {

    // :-: Sends a visible push notification to a single device :-:
    public void sendNotification(String deviceToken, String title, String body) {
        Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Notification sent successfully: {}", response);
        } catch (Exception e) {
            log.error("Failed to send notification to token {}: {}", deviceToken, e.getMessage());
        }
    }

    // :-: Sends a data-only FCM message (no system notification) so the Expo app
    //     can render its own accept/decline UI with full order details :-:
    public void sendDeliveryRequestNotification(String deviceToken, Map<String, String> data) {
        Message.Builder messageBuilder = Message.builder()
                .setToken(deviceToken);

        data.forEach(messageBuilder::putData);

        try {
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("Delivery request sent to token {}: {}", deviceToken, response);
        } catch (Exception e) {
            log.error("Failed to send delivery request to token {}: {}", deviceToken, e.getMessage());
        }
    }
}
package com.example.tds.triggers;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import com.example.tds.service.OrderService;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import com.example.tds.dto.requests.orders.SubscriptionPayloadForOrder;

import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Connection;
import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresNotificationListener {
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @Value("${postgres.listener.url}")
    private String listenerUrl;

    @Value("${postgres.listener.username}")
    private String username;

    @Value("${postgres.listener.password}")
    private String password;

    @PostConstruct
    public void startListening(){
        log.info("Starting PostgreSQL listener...");
        new Thread(this::listen).start();
    }

    private void listen(){
        try(Connection connection = DriverManager.getConnection(listenerUrl, username, password)) {
            PGConnection pgConnection = connection.unwrap(PGConnection.class);

            try(Statement statement = connection.createStatement()) {
                statement.execute("LISTEN new_orders_channel");
            }

            while (true) {
                PGNotification[] notifications = pgConnection.getNotifications();

                if(notifications != null){
                    for(PGNotification notification : notifications){
                        log.info("Received: {}", notification.getParameter());

                        processNotification(notification.getParameter());
                    }
                }

                Thread.sleep(1000);
            }
        }catch (Exception e){
            log.error("Error while listening for notifications", e);
        }
    }

    private void processNotification(String payload) throws JsonProcessingException {
        SubscriptionPayloadForOrder payload1 = objectMapper.readValue(payload, SubscriptionPayloadForOrder.class);

        orderService.createOrder(payload1.getSubscription_id());
    }

}
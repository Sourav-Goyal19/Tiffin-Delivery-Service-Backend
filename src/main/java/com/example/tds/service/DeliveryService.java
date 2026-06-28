package com.example.tds.service;

import com.example.tds.entity.*;
import lombok.extern.slf4j.Slf4j;
import com.example.tds.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.example.tds.exception.ResourceNotFoundException;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final ChefRepository chefRepository;
    private final UserRepository userRepository;
    private final RedisMessageListenerContainer container;
    private final DeliveryAgentService deliveryAgentService;
    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
//    private final NotificationService notificationService;

    @Value("${distance.rate.per.km}")
    private int distanceRatePerKm;

    @Value("${distance.base.fee}")
    private int distanceBaseFee;

    @Value("${delivery.wait.time.seconds:60}")
    private int waitTimeInSeconds;

    public double calculateDeliveryFee(UUID userId, UUID chefId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ChefEntity chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found"));

        double distanceInKm = subscriptionRepository.findDisInKm(user.getLocation(), chef.getLocation()).getDisInKm();
        int distance = (int) Math.ceil(distanceInKm);

        double fees = (distanceBaseFee + (distance * distanceRatePerKm));
        int fee = (int) fees / 10;
        return fee * 10.0;
    }

    public DeliveryAgentEntity getDeliveryAgent(OrderEntity order) {
        int minimumKm = 1;
        int maximumKm = 5;

        SubscriptionEntity subscription = order.getSubscription();
        MealPlanEntity mealPlan = subscription.getMealPlan();
        ChefEntity chef = mealPlan.getChef();

        String channelName = "order_" + order.getOrderId();
        DeliveryAgentEntity assignedAgent = null;

        org.locationtech.jts.geom.Point chefLocation = chef.getLocation();
        Point searchPoint = new Point(chefLocation.getX(), chefLocation.getY());

        // :-: Finds delivery agents under specific radius :-:
        for (int i = minimumKm; i <= maximumKm; i++) {

            List<DeliveryAgentEntity> deliveryAgents = deliveryAgentService.findNearbyActiveAgents(searchPoint, i);

            if (deliveryAgents.isEmpty()) {
                log.info("No delivery agents found under {} km", i);
                continue;
            }

            // :-: TODO: Send push-in notification for the order delivery request to all the found delivery agents. :-:

            assignedAgent = subscribeAndWaitForResponse(channelName, waitTimeInSeconds);

            if (assignedAgent != null) break; // If agent has found, don't find further
        }

        if (assignedAgent == null) {
            throw new ResourceNotFoundException("Delivery agent not found");
        }

        return assignedAgent;
    }

    private DeliveryAgentEntity subscribeAndWaitForResponse(String channelName, int waitTimeInSeconds) {
        // :-: Keeps the static value across different threads like listener thread, main thread, etc. :-:
        AtomicReference<String> deliveryAgentId = new AtomicReference<>(null);

        /* :-: latch is used to pause a main thread, until we found a delivery guy :-:
        *  :-: 1 means 1 event will be paused, it will pause the thread, make it zero to unblock thread. :-: */
        CountDownLatch latch = new CountDownLatch(1);

        // :-: Our listener function that listens when someone publishes :-:
        MessageListener listener = (message, channel) -> {
            String body = new String(message.getBody());

            try{
                if(body.startsWith("delivery_agent_")) {
                    String id = body.substring("delivery_agent_".length());
                    deliveryAgentId.set(id);
                    latch.countDown();
                }
            } catch (Exception e) {
                log.error("Error processing message: {}", e.getMessage());
            }
        };

        try {
            container.addMessageListener(listener, new ChannelTopic(channelName));

            // :-: This pause the thread for a specific timeout :-:
            boolean received = latch.await(waitTimeInSeconds + 5, TimeUnit.SECONDS);

            if (received && deliveryAgentId.get() != null) {
                UUID agentId = UUID.fromString(deliveryAgentId.get());

                return deliveryAgentRepository.findByDeliveryAgentId(agentId)
                        .orElse(null);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Waiting for delivery agent interrupted: {}", e.getMessage());
        } finally {
            container.removeMessageListener(listener, new  ChannelTopic(channelName));
        }

        return null;
    }
}

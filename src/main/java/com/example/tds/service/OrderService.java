package com.example.tds.service;

import com.example.tds.dto.responses.OrderResponse;
import com.example.tds.entity.*;
import com.example.tds.enums.OrderStatus;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.mapper.OrderMapper;
import com.example.tds.repository.OrderRepository;
import com.example.tds.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryService deliveryService;

    @Transactional
    public void createOrder(UUID subscriptionId){
        SubscriptionEntity subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        UserEntity user = subscription.getUser();
        MealPlanEntity mealPlan = subscription.getMealPlan();
        ChefEntity chef = mealPlan.getChef();

        LocalDate startDate = subscription.getStartDate();
        LocalDate endDate = subscription.getEndDate();

        LocalDate currentDate = startDate;

        List<OrderEntity> orders = new ArrayList<>();

        while(!currentDate.isAfter(endDate)){
            OrderEntity order = new OrderEntity();
            order.setSubscription(subscription);
            order.setFromLocation(chef.getAddress());
            order.setToLocation(user.getAddress());
            order.setOrderDate(currentDate);

            orders.add(order);
            currentDate = currentDate.plusDays(1);
        }

        orderRepository.saveAll(orders);
    }

    public OrderResponse assignDeliveryAgent(UUID orderId){
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        SubscriptionEntity subscription = order.getSubscription();
        UserEntity user = subscription.getUser();
        MealPlanEntity mealPlan = subscription.getMealPlan();
        ChefEntity chef = mealPlan.getChef();

        DeliveryAgentEntity deliveryAgent = deliveryService.getDeliveryAgent(order);

        order.setDeliveryAgent(deliveryAgent);
        order.setFromLocation(chef.getAddress());
        order.setToLocation(user.getAddress());
        order.setStatus(OrderStatus.ASSIGNED);

        order = orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }
}

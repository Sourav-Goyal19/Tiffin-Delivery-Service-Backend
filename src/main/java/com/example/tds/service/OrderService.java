package com.example.tds.service;

import com.example.tds.entity.*;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.dto.responses.*;
import com.example.tds.enums.OrderStatus;
import com.example.tds.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import com.example.tds.mapper.DeliveryAgentMapper;
import com.example.tds.repository.OrderRepository;
import com.example.tds.enums.DeliveryAgentCurrentStatus;
import com.example.tds.projection.OrderForChefProjection;
import com.example.tds.repository.SubscriptionRepository;
import com.example.tds.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final DeliveryService deliveryService;
    private final DeliveryAgentMapper agentMapper;
    private final SubscriptionRepository subscriptionRepository;

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

    public OrderForChefResponse getOrdersForChef(UUID chefId) {
        List<OrderForChefProjection> orders = orderRepository.findByChefId(chefId);

        return mapToOrderForChefResponse(orders);
    }

    private OrderForChefResponse mapToOrderForChefResponse(List<OrderForChefProjection> orders) {
        if (orders.isEmpty()) {
            return OrderForChefResponse.builder().build();
        }

        List<OrderWithDetailsResponse> mappedOrders = orders.stream().map(proj -> {
            OrderResponse order = OrderResponse.builder()
                    .orderId(proj.getOrderId())
                    .deliveryAgentId(proj.getDeliveryAgentId())
                    .fromLocation(proj.getFromLocation())
                    .toLocation(proj.getToLocation())
                    .status(proj.getStatus())
                    .orderDate(proj.getOrderDate())
                    .createdAt(proj.getCreatedAt())
                    .updatedAt(proj.getUpdatedAt())
                    .build();

            SubscriptionResponse subscription = SubscriptionResponse.builder()
                    .subscriptionId(proj.getSubscriptionId())
                    .deliveryType(proj.getSubscriptionDeliveryType())
                    .planType(proj.getSubscriptionPlanType())
                    .isActive(proj.getSubscriptionIsActive())
                    .price(proj.getSubscriptionPrice())
                    .startDate(proj.getSubscriptionStartDate())
                    .endDate(proj.getSubscriptionEndData())
                    .createdAt(proj.getSubscriptionCreatedAt())
                    .updatedAt(proj.getSubscriptionUpdatedAt())
                    .build();

            MenuResponse menu = MenuResponse.builder()
                    .menuId(proj.getMenuId())
                    .weekDay(proj.getMenuWeekDay())
                    .mealType(proj.getMenuMealType())
                    .isActive(proj.getMenuIsActive())
                    .chefId(proj.getMenuChefId())
                    .thumbnailUrl(proj.getMenuThumbnailUrl())
                    .createdAt(proj.getMenuCreatedAt())
                    .updatedAt(proj.getMenuUpdatedAt())
                    .items(proj.getMenuItems() != null ? java.util.Arrays.asList(proj.getMenuItems().replace("{", "").replace("}", "").split(",")) : null)
                    .build();

            UserResponse user = UserResponse.builder()
                    .id(proj.getUserId())
                    .name(proj.getUserName())
                    .address(proj.getUserAddress())
                    .createdAt(proj.getUserCreatedAt())
                    .updatedAt(proj.getUserUpdatedAt())
                    .build();
                    
            DeliveryAgentResponse deliveryAgent = null;
            if (proj.getDeliveryAgentId() != null) {
                deliveryAgent = DeliveryAgentResponse.builder()
                        .deliveryAgentId(proj.getDeliveryAgentId())
                        .name(proj.getDeliveryAgentName())
                        .mobileNo(proj.getDeliveryAgentMobileNo())
                        .currentStatus(proj.getDeliveryAgentCurrentStatus() != null ? DeliveryAgentCurrentStatus.valueOf(proj.getDeliveryAgentCurrentStatus()) : null)
                        .createdAt(proj.getDeliveryAgentCreatedAt())
                        .lastActiveAt(proj.getDeliveryAgentLastActiveAt())
                        .build();
            }

            return OrderWithDetailsResponse.builder()
                    .order(order)
                    .subscription(subscription)
                    .menu(menu)
                    .user(user)
                    .deliveryAgent(deliveryAgent)
                    .build();
        }).toList();

        return OrderForChefResponse.builder()
                .orders(mappedOrders)
                .build();
    }

    public DeliveryAgentResponse assignDeliveryAgent(UUID orderId){
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        SubscriptionEntity subscription = order.getSubscription();
        UserEntity user = subscription.getUser();
        MealPlanEntity mealPlan = subscription.getMealPlan();
        ChefEntity chef = mealPlan.getChef();

        DeliveryAgentEntity deliveryAgent = deliveryService.getDeliveryAgent(order);

        log.info("Found deliveryAgent {}", deliveryAgent.getDeliveryAgentId());

        order.setDeliveryAgent(deliveryAgent);
        order.setFromLocation(chef.getAddress());
        order.setToLocation(user.getAddress());
        order.setStatus(OrderStatus.ASSIGNED);

        orderRepository.save(order);

        return agentMapper.toDeliveryAgentResponse(deliveryAgent);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(status);
        order = orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public List<OrderResponse> updateMultipleOrdersStatus(List<UUID> orderIds, OrderStatus status) {
        List<OrderEntity> orders = orderRepository.findAllById(orderIds);

        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found for the given IDs");
        }

        orders.forEach(order -> order.setStatus(status));
        orders = orderRepository.saveAll(orders);

        return orders.stream().map(orderMapper::toOrderResponse).toList();
    }
}

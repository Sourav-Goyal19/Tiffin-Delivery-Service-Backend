package com.example.tds.service;

import com.example.tds.entity.*;
import lombok.extern.slf4j.Slf4j;
import com.example.tds.repository.*;
import lombok.RequiredArgsConstructor;
import com.example.tds.dto.responses.*;
import com.example.tds.enums.OrderStatus;
import com.example.tds.enums.DeliveryType;
import com.example.tds.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import com.example.tds.enums.DeliveryAgentStatus;
import com.example.tds.mapper.DeliveryAgentMapper;
import com.example.tds.exception.BadRequestException;
import com.example.tds.enums.WeekDay;
import com.example.tds.projection.OrderForUserProjection;
import com.example.tds.projection.OrderForChefProjection;
import com.example.tds.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import com.example.tds.dto.requests.orders.OrderDeliveredRequest;
import com.example.tds.projection.OrderForDeliveryAgentProjection;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    private final ChefPaymentRepository chefPaymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final DeliveryPaymentRepository deliveryPaymentRepository;
    private final StringRedisTemplate template;

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
            ChefOrderResponse order = ChefOrderResponse.builder()
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
                        .status(proj.getDeliveryAgentStatus() != null ? DeliveryAgentStatus.valueOf(proj.getDeliveryAgentStatus()) : null)
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

        order.setDeliveryAgent(deliveryAgent);
        order.setFromLocation(chef.getAddress());
        order.setToLocation(user.getAddress());
        order.setStatus(OrderStatus.ASSIGNED);

        deliveryAgent.setStatus(DeliveryAgentStatus.BUSY);

        orderRepository.save(order);
        deliveryAgentRepository.save(deliveryAgent);

        return agentMapper.toDeliveryAgentResponse(deliveryAgent);
    }

    public OrderForDeliveryAgentResponse getOrderForDeliveryAgent(UUID deliveryAgentId, UUID orderId){
        OrderForDeliveryAgentProjection proj = orderRepository.getOrderByOrderIdAndDeliveryAgentId(orderId, deliveryAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return mapToOrderForDeliveryAgentResponse(proj);
    }

    public List<OrderForDeliveryAgentResponse> getOrdersForDeliveryAgent(UUID deliveryAgentId, Boolean activeOrders){
        List<OrderForDeliveryAgentProjection> projections = activeOrders != null && activeOrders
                ? orderRepository.getActiveOrdersByDeliveryAgentId(deliveryAgentId)
                : orderRepository.getOrdersByDeliveryAgentId(deliveryAgentId);

        return projections.stream().map(this::mapToOrderForDeliveryAgentResponse).toList();
    }

    private OrderForDeliveryAgentResponse mapToOrderForDeliveryAgentResponse(OrderForDeliveryAgentProjection proj) {
        DeliveryAgentOrderResponse order = DeliveryAgentOrderResponse.builder()
                .orderId(proj.getOrderId())
                .subscriptionId(proj.getOrderSubscriptionId())
                .deliveryAgentId(proj.getOrderDeliveryAgentId())
                .fromLocation(proj.getOrderFromLocation())
                .toLocation(proj.getOrderToLocation())
                .status(proj.getOrderStatus())
                .orderDate(proj.getOrderDate())
                .deliveryAgentFee(proj.getOrderDeliveryAgentFee())
                .createdAt(proj.getOrderCreatedAt())
                .updatedAt(proj.getOrderUpdatedAt())
                .build();

        UserResponse user = UserResponse.builder()
                .id(proj.getUserId())
                .name(proj.getUserName())
                .mobileNo(proj.getUserMobileNo())
                .address(proj.getUserAddress())
                .longitude(proj.getUserLongitude())
                .latitude(proj.getUserLatitude())
                .createdAt(proj.getUserCreatedAt())
                .updatedAt(proj.getUserUpdatedAt())
                .build();

        ChefResponse chef = ChefResponse.builder()
                .chefId(proj.getChefId())
                .name(proj.getChefName())
                .mobileNo(proj.getChefMobileNo())
                .address(proj.getChefAddress())
                .rating(proj.getChefRating())
                .longitude(proj.getChefLongitude())
                .latitude(proj.getChefLatitude())
                .createdAt(proj.getChefCreatedAt())
                .updatedAt(proj.getChefUpdatedAt())
                .build();

        return OrderForDeliveryAgentResponse.builder()
                .order(order)
                .user(user)
                .chef(chef)
                .build();
    }

    public OrderForUserResponse getOrderForUser(UUID userId, UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
                
        WeekDay weekDay = WeekDay.valueOf(order.getOrderDate().getDayOfWeek().name().toUpperCase());
        
        OrderForUserProjection proj = orderRepository.getOrderByUserIdAndOrderId(userId, orderId, weekDay.name())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return mapToOrderForUserResponse(proj);
    }

    public List<OrderForUserResponse> getOrdersForUser(UUID userId, LocalDate date) {
        List<OrderForUserProjection> projections;
        
        if (date != null) {
            WeekDay weekDay = WeekDay.valueOf(date.getDayOfWeek().name().toUpperCase());
            projections = orderRepository.getOrdersByUserIdAndDate(userId, date, weekDay.name());
        } else {
            projections = orderRepository.getOrdersByUserId(userId);
        }
        
        return projections.stream().map(this::mapToOrderForUserResponse).toList();
    }

    private OrderForUserResponse mapToOrderForUserResponse(OrderForUserProjection proj) {
        CustomerOrderResponse order = CustomerOrderResponse.builder()
                .orderId(proj.getOrderId())
                .subscriptionId(proj.getOrderSubscriptionId())
                .deliveryAgentId(proj.getOrderDeliveryAgentId())
                .fromLocation(proj.getOrderFromLocation())
                .toLocation(proj.getOrderToLocation())
                .status(proj.getOrderStatus())
                .orderDate(proj.getOrderDate())
                .orderOtp(proj.getOrderOtp())
                .createdAt(proj.getOrderCreatedAt())
                .updatedAt(proj.getOrderUpdatedAt())
                .build();

        DeliveryAgentResponse deliveryAgent = null;
        if (proj.getOrderDeliveryAgentId() != null) {
            deliveryAgent = DeliveryAgentResponse.builder()
                    .deliveryAgentId(proj.getOrderDeliveryAgentId())
                    .name(proj.getDeliveryAgentName())
                    .mobileNo(proj.getDeliveryAgentMobileNo())
                    .status(proj.getDeliveryAgentStatus())
                    .createdAt(proj.getDeliveryAgentCreatedAt())
                    .lastActiveAt(proj.getDeliveryAgentLastActiveAt())
                    .build();
        }

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

        ChefResponse chef = ChefResponse.builder()
                .chefId(proj.getChefId())
                .name(proj.getChefName())
                .address(proj.getChefAddress())
                .rating(proj.getChefRating())
                .createdAt(proj.getChefCreatedAt())
                .updatedAt(proj.getChefUpdatedAt())
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

        return OrderForUserResponse.builder()
                .order(order)
                .deliveryAgent(deliveryAgent)
                .subscription(subscription)
                .chef(chef)
                .menu(menu)
                .build();
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(status);
        order = orderRepository.save(order);

        if ((status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) && order.getDeliveryAgent() != null) {
            template.convertAndSend(DeliveryAgentService.AGENT_LOCATION_CHANNEL_PREFIX + order.getDeliveryAgent().getDeliveryAgentId(), DeliveryAgentService.CLOSE_CONNECTION_MESSAGE_PREFIX + order.getOrderId());
        }

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse handleOrderDelivery(UUID orderId, OrderDeliveredRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateOrderDelivery(order, request.getOrderOtp());

        order.setStatus(OrderStatus.DELIVERED);

        if (order.getDeliveryAgent() != null) {
            DeliveryAgentEntity agent = order.getDeliveryAgent();
            agent.setStatus(DeliveryAgentStatus.ACTIVE);
            deliveryAgentRepository.save(agent);
            
            template.convertAndSend(DeliveryAgentService.AGENT_LOCATION_CHANNEL_PREFIX + agent.getDeliveryAgentId(), DeliveryAgentService.CLOSE_CONNECTION_MESSAGE_PREFIX + order.getOrderId());
        }

        order = orderRepository.save(order);

        processPayments(order);

        return orderMapper.toOrderResponse(order);
    }

    private void validateOrderDelivery(OrderEntity order, int providedOtp) {
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order has already processed or cancelled");
        }

        if (providedOtp != order.getOrderOtp()) {
            throw new BadRequestException("Invalid order OTP");
        }

        SubscriptionEntity subscription = order.getSubscription();
        if (subscription.getDeliveryType() == DeliveryType.DELIVERY && order.getStatus() != OrderStatus.PICKED_UP) {
            throw new BadRequestException("Order was never picked up");
        }
        
        if (subscription.getDeliveryType() == DeliveryType.PICKUP && order.getStatus() != OrderStatus.READY) {
            throw new BadRequestException("Order is not ready for pickup");
        }
    }

    private void processPayments(OrderEntity order) {
        SubscriptionEntity subscription = order.getSubscription();

        if (subscription.getDeliveryType() == DeliveryType.DELIVERY) {
            processDeliveryPayment(order, subscription.getDeliveryAgentFee());
        }

        processChefPayment(order, subscription);
    }

    private void processDeliveryPayment(OrderEntity order, double deliveryAgentFee) {
        DeliveryPaymentEntity deliveryPayment = new DeliveryPaymentEntity();
        deliveryPayment.setDeliveryAgent(order.getDeliveryAgent());
        deliveryPayment.setOrder(order);
        deliveryPayment.setAmount(deliveryAgentFee);

        deliveryPaymentRepository.save(deliveryPayment);
    }

    private void processChefPayment(OrderEntity order, SubscriptionEntity subscription) {
        double chefFee = calculateChefFee(subscription);

        ChefPaymentEntity chefPayment = new ChefPaymentEntity();
        chefPayment.setChef(subscription.getMealPlan().getChef());
        chefPayment.setOrder(order);
        chefPayment.setAmount(chefFee);

        chefPaymentRepository.save(chefPayment);
    }

    private double calculateChefFee(SubscriptionEntity subscription) {
        return switch (subscription.getPlanType()) {
            case WEEKLY -> subscription.getPrice() / 7;
            case MONTHLY -> subscription.getPrice() / 30;
            default -> 0d;
        };
    }

    @Transactional
    public List<OrderResponse> updateMultipleOrdersStatus(List<UUID> orderIds, OrderStatus status) {
        List<OrderEntity> orders = orderRepository.findAllById(orderIds);

        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found for the given IDs");
        }

        orders.forEach(order -> {
            order.setStatus(status);
            if ((status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) && order.getDeliveryAgent() != null) {
                template.convertAndSend(DeliveryAgentService.AGENT_LOCATION_CHANNEL_PREFIX + order.getDeliveryAgent().getDeliveryAgentId(), DeliveryAgentService.CLOSE_CONNECTION_MESSAGE_PREFIX + order.getOrderId());
            }
        });
        orders = orderRepository.saveAll(orders);

        return orders.stream().map(orderMapper::toOrderResponse).toList();
    }
}

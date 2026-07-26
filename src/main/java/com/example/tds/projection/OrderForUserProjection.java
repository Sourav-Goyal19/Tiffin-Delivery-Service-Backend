package com.example.tds.projection;

import com.example.tds.enums.DeliveryType;
import com.example.tds.enums.MealType;
import com.example.tds.enums.OrderStatus;
import com.example.tds.enums.PlanType;
import com.example.tds.enums.DeliveryAgentStatus;
import com.example.tds.enums.WeekDay;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderForUserProjection {
    // Order fields
    UUID getOrderId();
    UUID getOrderSubscriptionId();
    UUID getOrderDeliveryAgentId();
    String getOrderFromLocation();
    String getOrderToLocation();
    OrderStatus getOrderStatus();
    LocalDate getOrderDate();
    Integer getOrderOtp();
    LocalDateTime getOrderCreatedAt();
    LocalDateTime getOrderUpdatedAt();

    // Delivery Agent fields
    String getDeliveryAgentName();
    String getDeliveryAgentMobileNo();
    DeliveryAgentStatus getDeliveryAgentStatus();
    LocalDateTime getDeliveryAgentCreatedAt();
    LocalDateTime getDeliveryAgentLastActiveAt();

    // Subscription fields
    UUID getSubscriptionId();
    DeliveryType getSubscriptionDeliveryType();
    PlanType getSubscriptionPlanType();
    Boolean getSubscriptionIsActive();
    Double getSubscriptionPrice();
    LocalDate getSubscriptionStartDate();
    LocalDate getSubscriptionEndData();
    LocalDateTime getSubscriptionCreatedAt();
    LocalDateTime getSubscriptionUpdatedAt();

    // Chef fields
    UUID getChefId();
    String getChefName();
    String getChefAddress();
    Double getChefRating();
    LocalDateTime getChefCreatedAt();
    LocalDateTime getChefUpdatedAt();

    // Menu fields
    UUID getMenuId();
    String getMenuItems();
    UUID getMenuChefId();
    WeekDay getMenuWeekDay();
    String getMenuThumbnailUrl();
    MealType getMenuMealType();
    Boolean getMenuIsActive();
    LocalDateTime getMenuCreatedAt();
    LocalDateTime getMenuUpdatedAt();
}

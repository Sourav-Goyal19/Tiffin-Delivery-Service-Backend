package com.example.tds.projection;

import com.example.tds.enums.DeliveryType;
import com.example.tds.enums.MealType;
import com.example.tds.enums.OrderStatus;
import com.example.tds.enums.PlanType;
import com.example.tds.enums.WeekDay;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderForChefProjection {
    UUID getOrderId();
    UUID getDeliveryAgentId();
    String getDeliveryAgentName();
    String getDeliveryAgentMobileNo();
    String getDeliveryAgentCurrentStatus();
    LocalDateTime getDeliveryAgentCreatedAt();
    LocalDateTime getDeliveryAgentLastActiveAt();
    String getFromLocation();
    String getToLocation();
    OrderStatus getStatus();
    LocalDate getOrderDate();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    UUID getSubscriptionId();
    DeliveryType getSubscriptionDeliveryType();
    PlanType getSubscriptionPlanType();
    Boolean getSubscriptionIsActive();
    Double getSubscriptionPrice();
    LocalDate getSubscriptionStartDate();
    LocalDate getSubscriptionEndData();
    LocalDateTime getSubscriptionCreatedAt();
    LocalDateTime getSubscriptionUpdatedAt();

    UUID getUserId();
    String getUserName();
    String getUserAddress();
    LocalDateTime getUserCreatedAt();
    LocalDateTime getUserUpdatedAt();

    UUID getMenuId();
    String getMenuItems();
    WeekDay getMenuWeekDay();
    String getMenuThumbnailUrl();
    UUID getMenuChefId();
    MealType getMenuMealType();
    Boolean getMenuIsActive();
    LocalDateTime getMenuCreatedAt();
    LocalDateTime getMenuUpdatedAt();
}

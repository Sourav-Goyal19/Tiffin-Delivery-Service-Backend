package com.example.tds.projection;

import com.example.tds.enums.DeliveryType;
import com.example.tds.enums.MealType;
import com.example.tds.enums.PlanType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public interface SubscriptionWithDetailsProjection {
    UUID getSubscriptionId();
    UUID getUserId();
    UUID getMealPlanId();
    DeliveryType getDeliveryType();
    PlanType getPlanType();
    Boolean getIsActive();
    Double getPrice();
    Double getDeliveryFee();
    LocalDate getStartDate();
    LocalDate getEndDate();
    LocalDateTime getSubscriptionCreatedAt();
    LocalDateTime getSubscriptionUpdatedAt();

    String getUserName();
    String getUserMobileNo();
    String getUserAddress();
    LocalDateTime getUserCreatedAt();
    LocalDateTime getUserUpdatedAt();

    UUID getChefId();
    MealType getMealType();
    Double getWeeklyPrice();
    Double getMonthlyPrice();
    LocalTime getTiming();
    Integer getCapacity();
    LocalDateTime getMealPlanCreatedAt();
    LocalDateTime getMealPlanUpdatedAt();

    String getChefName();
    String getChefMobileNo();
    String getChefAddress();
    Double getChefRating();
    LocalDateTime getChefCreatedAt();
    LocalDateTime getChefUpdatedAt();
}

package com.example.tds.projection;

import com.example.tds.enums.MealType;
import com.example.tds.enums.WeekDay;

import java.util.*;
import java.time.LocalDateTime;

public interface MenuWithDistanceProjection {
    UUID getMenuId();
    MealType getMealType();
    List<String> getItems();
    String getThumbnailUrl();
    Boolean getIsActive();
    Integer getCapacity();
    WeekDay getWeekDay();

    UUID getChefId();
    Double getDisInKm();

    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    String getName();
    String getAddress();
    String getMobileNo();
    String getAvatarUrl();
    Double getRating();
    LocalDateTime getChefCreatedAt();
    LocalDateTime getChefUpdatedAt();

    UUID getMealPlanId();
    Double getWeeklyPrice();
    Double getMonthlyPrice();
    Integer getMealPlanCapacity();
    Integer getMealPlanRemainingCapacity();
    java.time.LocalTime getTiming();
    Boolean getMealPlanIsActive();
    LocalDateTime getMealPlanCreatedAt();
    LocalDateTime getMealPlanUpdatedAt();
}
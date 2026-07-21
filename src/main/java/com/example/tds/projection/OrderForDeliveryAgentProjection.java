package com.example.tds.projection;

import com.example.tds.enums.OrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderForDeliveryAgentProjection {
    UUID getOrderId();
    UUID getOrderSubscriptionId();
    UUID getOrderDeliveryAgentId();
    String getOrderFromLocation();
    String getOrderToLocation();
    OrderStatus getOrderStatus();
    LocalDate getOrderDate();
    Double getOrderDeliveryAgentFee();
    LocalDateTime getOrderCreatedAt();
    LocalDateTime getOrderUpdatedAt();

    UUID getUserId();
    String getUserName();
    String getUserMobileNo();
    String getUserAddress();
    Double getUserLongitude();
    Double getUserLatitude();
    LocalDateTime getUserCreatedAt();
    LocalDateTime getUserUpdatedAt();

    UUID getChefId();
    String getChefName();
    String getChefMobileNo();
    String getChefAddress();
    Double getChefRating();
    Double getChefLongitude();
    Double getChefLatitude();
    LocalDateTime getChefCreatedAt();
    LocalDateTime getChefUpdatedAt();
}

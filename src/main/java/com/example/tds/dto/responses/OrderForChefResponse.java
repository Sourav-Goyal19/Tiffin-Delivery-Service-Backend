package com.example.tds.dto.responses;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderForChefResponse {
    private MenuResponse menu;
    private UserResponse user;
    private ChefOrderResponse order;
    private SubscriptionResponse subscription;
    private DeliveryAgentResponse deliveryAgent;
}
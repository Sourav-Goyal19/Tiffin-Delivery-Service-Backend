package com.example.tds.dto.responses;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderForUserResponse {
    private CustomerOrderResponse order;
    private DeliveryAgentResponse deliveryAgent;
    private SubscriptionResponse subscription;
    private ChefResponse chef;
    private MenuResponse menu;
}

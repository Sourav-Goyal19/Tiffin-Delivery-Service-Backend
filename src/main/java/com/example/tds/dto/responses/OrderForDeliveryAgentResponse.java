package com.example.tds.dto.responses;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderForDeliveryAgentResponse {
    private OrderResponse order;
    private UserResponse user;
    private ChefResponse chef;
}

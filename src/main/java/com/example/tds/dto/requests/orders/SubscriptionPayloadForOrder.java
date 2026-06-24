package com.example.tds.dto.requests.orders;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
public class SubscriptionPayloadForOrder {
    private UUID subscription_id;
}

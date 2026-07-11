package com.example.tds.dto.requests.common;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeliveryOrderRespondRequest {
    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Accepted field is required")
    private Boolean accepted;
}

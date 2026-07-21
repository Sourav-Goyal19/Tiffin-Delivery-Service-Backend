package com.example.tds.dto.responses;

import com.example.tds.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private UUID orderId;
    private UUID subscriptionId;
    private UUID deliveryAgentId;
    private String fromLocation;
    private String toLocation;
    private OrderStatus status;
    private LocalDate orderDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

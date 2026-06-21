package com.example.tds.dto.responses;

import com.example.tds.enums.DeliveryType;
import com.example.tds.enums.PlanType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionResponse {
    private UUID subscriptionId;
    private UUID userId;
    private UUID mealPlanId;
    private DeliveryType deliveryType;
    private PlanType planType;
    private Boolean isActive;
    private Double price;
    private Double deliveryFee;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.example.tds.dto.requests.subscriptions;

import com.example.tds.enums.DeliveryType;
import com.example.tds.enums.PlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateSubscriptionRequest {

    @NotNull(message = "A valid meal plan id is required")
    private UUID mealPlanId;

    @NotNull(message = "A proper delivery type is required")
    private DeliveryType deliveryType;

    @NotNull(message = "A proper plan type is required")
    private PlanType planType;
}

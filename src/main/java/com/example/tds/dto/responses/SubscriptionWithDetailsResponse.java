package com.example.tds.dto.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionWithDetailsResponse {
    private SubscriptionResponse subscription;
    private BillResponse bill;
    private PaymentResponse payment;
    private UserResponse user;
    private ChefResponse chef;
    private MealPlanResponse mealPlan;
}

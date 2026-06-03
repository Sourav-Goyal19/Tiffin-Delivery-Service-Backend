package com.example.tds.dto.requests.mealplans;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class UpdateMealPlanRequest {
    @NotNull(message = "weekly price is required")
    private Double weeklyPrice;

    @NotNull(message = "monthly price is required")
    private Double monthlyPrice;

    @NotNull(message = "timing is required")
    private LocalTime timing;

    @NotNull(message = "is active is required")
    private Boolean isActive;
}

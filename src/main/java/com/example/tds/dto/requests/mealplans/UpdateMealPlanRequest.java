package com.example.tds.dto.requests.mealplans;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class UpdateMealPlanRequest {
    private Double weeklyPrice;

    private Double monthlyPrice;

    @NotNull(message = "timing is required")
    private LocalTime timing;

    @NotNull(message = "is active is required")
    private Boolean isActive;

    @NotNull(message = "capacity is required")
    @Positive(message = "capacity can't be 0")
    private Integer capacity;
}

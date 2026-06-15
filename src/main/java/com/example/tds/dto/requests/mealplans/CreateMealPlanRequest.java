package com.example.tds.dto.requests.mealplans;

import com.example.tds.enums.MealType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalTime;

@Getter
@Setter
public class CreateMealPlanRequest {
    @NotNull(message = "A proper meal type is required")
    private MealType mealType;

    private Double weeklyPrice;

    private Double monthlyPrice;

    @NotNull(message = "timing is required")
    private LocalTime timing;

    @NotNull(message = "is active is required")
    private Boolean isActive;
}

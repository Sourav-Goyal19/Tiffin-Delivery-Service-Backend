package com.example.tds.dto.responses;

import com.example.tds.enums.MealType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class MealPlanResponse {
    private UUID mealPlanId;
    private MealType mealType;
    private Double weeklyPrice;
    private Double monthlyPrice;
    private LocalTime timing;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

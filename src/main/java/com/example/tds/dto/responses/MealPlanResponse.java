package com.example.tds.dto.responses;

import com.example.tds.enums.MealType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MealPlanResponse {
    private UUID mealPlanId;
    private MealType mealType;
    private Double weeklyPrice;
    private Double monthlyPrice;
    private Integer capacity;
    private Integer remainingCapacity;
    private LocalTime timing;
    private Boolean isActive;
    private UUID chefId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

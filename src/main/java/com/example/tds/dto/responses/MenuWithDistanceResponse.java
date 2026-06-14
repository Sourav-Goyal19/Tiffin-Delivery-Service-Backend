package com.example.tds.dto.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuWithDistanceResponse {
    private MenuResponse menu;
    private ChefResponse chef;
    private MealPlanResponse mealPlan;
}
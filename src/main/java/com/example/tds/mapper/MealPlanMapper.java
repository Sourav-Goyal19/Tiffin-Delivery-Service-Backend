package com.example.tds.mapper;

import com.example.tds.dto.requests.mealplans.CreateMealPlanRequest;
import com.example.tds.dto.requests.mealplans.UpdateMealPlanRequest;
import com.example.tds.dto.responses.MealPlanResponse;
import com.example.tds.entity.MealPlanEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MealPlanMapper {
    MealPlanEntity toMealPlanEntity(CreateMealPlanRequest mealPlanRequest);
    MealPlanEntity toMealPlanEntity(UpdateMealPlanRequest mealPlanRequest);

    MealPlanResponse toMealPlanResponse(MealPlanEntity mealPlanEntity);
    List<MealPlanResponse> toMealPlanResponse(List<MealPlanEntity> mealPlanEntities);
}

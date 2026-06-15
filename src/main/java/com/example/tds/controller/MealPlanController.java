package com.example.tds.controller;

import com.example.tds.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.dto.responses.*;
import com.example.tds.entity.ChefEntity;
import com.example.tds.mapper.ChefMapper;
import org.springframework.http.HttpStatus;
import com.example.tds.service.MealPlanService;
import org.springframework.http.ResponseEntity;
import com.example.tds.dto.requests.mealplans.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chefs/{chefId}/meal-plans")
public class MealPlanController {
    private final MealPlanService mealPlanService;
    private final ChefMapper chefMapper;

    @PostMapping()
    public ResponseEntity<ApiResponse> createMealPlan(@RequestAttribute("chef") ChefEntity chefEntity, @Valid @RequestBody CreateMealPlanRequest createMealPlanRequest){
        if(createMealPlanRequest.getWeeklyPrice() == null && createMealPlanRequest.getMonthlyPrice() == null){
            throw new BadRequestException("Either weekly or monthly price is required");
        }

        MealPlanResponse response = mealPlanService.handleCreateMealPlan(chefEntity, createMealPlanRequest);

        ChefResponse chefResponse = chefMapper.toChefResponse(chefEntity);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.builder()
                        .success(true)
                        .message("Meal plan created successfully")
                        .data(Map.of("chef", chefResponse, "mealPlan", response))
                        .build()
        );
    }

    @GetMapping("/{mealPlanId}")
    public ResponseEntity<ApiResponse> getMealPlan(@RequestAttribute("chef") ChefEntity chef, @PathVariable("mealPlanId") UUID mealPlanId){
        MealPlanResponse response = mealPlanService.handleGetMealPlan(
                chef.getChefId(), mealPlanId
        );

        ChefResponse chefResponse = chefMapper.toChefResponse(chef);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .success(true)
                        .message("Meal plan found successfully")
                        .data(Map.of("chef", chefResponse, "mealPlan", response))
                        .build()
        );
    }

    @GetMapping()
    public ResponseEntity<ApiResponse> getAllMealPlans(@RequestAttribute("chef") ChefEntity chef){
        List<MealPlanResponse> response = mealPlanService.handleGetAllMealPlans(chef.getChefId());

        ChefResponse chefResponse = chefMapper.toChefResponse(chef);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .success(true)
                        .message("Meal plans found successfully")
                        .data(Map.of("chef", chefResponse, "mealPlans", response))
                        .build()
        );
    }

    @PatchMapping("/{mealPlanId}")
    public ResponseEntity<ApiResponse> updateMealPlan(@RequestAttribute("chef") ChefEntity chef, @PathVariable("mealPlanId") UUID mealPlanId, @Valid @RequestBody UpdateMealPlanRequest updateMealPlanRequest){
        if(updateMealPlanRequest.getWeeklyPrice() == null && updateMealPlanRequest.getMonthlyPrice() == null){
            throw new BadRequestException("Either weekly or monthly price is required");
        }

        MealPlanResponse response = mealPlanService.handleUpdateMealPlan(chef.getChefId(), mealPlanId, updateMealPlanRequest);

        ChefResponse chefResponse = chefMapper.toChefResponse(chef);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .success(true)
                        .message("Meal plan updated successfully")
                        .data(Map.of("chef", chefResponse, "mealPlan", response))
                        .build()
        );
    }

    @DeleteMapping("/{mealPlanId}")
    public ResponseEntity<ApiResponse> deleteMealPlan(@RequestAttribute("chef") ChefEntity chef, @PathVariable("mealPlanId") UUID mealPlanId) {
        mealPlanService.handleDeleteMealPlan(chef.getChefId(), mealPlanId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .success(true)
                        .message("Meal plan deleted successfully")
                        .build()
        );
    }
}
